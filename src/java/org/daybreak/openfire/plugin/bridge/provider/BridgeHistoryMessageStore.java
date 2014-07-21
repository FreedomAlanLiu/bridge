package org.daybreak.openfire.plugin.bridge.provider;

import org.daybreak.openfire.plugin.bridge.model.History;
import org.daybreak.openfire.plugin.bridge.utils.MongoUtil;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketExtension;

import java.io.StringReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

/**
 * Created by alan on 14-5-16.
 */
public class BridgeHistoryMessageStore {

    private static final Logger Log = LoggerFactory.getLogger(BridgeHistoryMessageStore.class);

    private static final int POOL_SIZE = 10;

    /**
     * Pattern to use for detecting invalid XML characters. Invalid XML characters will
     * be removed from the stored offline messages.
     */
    private Pattern pattern = Pattern.compile("&\\#[\\d]+;");

    /**
     * Pool of SAX Readers. SAXReader is not thread safe so we need to have a pool of readers.
     */
    private BlockingQueue<SAXReader> xmlReaders = new LinkedBlockingQueue<SAXReader>(POOL_SIZE);

    private static BridgeHistoryMessageStore bridgeHistoryMessageStore = new BridgeHistoryMessageStore();

    private BridgeHistoryMessageStore() {
    }

    public static BridgeHistoryMessageStore getInstance() {
        return bridgeHistoryMessageStore;
    }

    /**
     * Adds a message to this message store. Messages will be stored and made
     * available for later delivery.
     *
     * @param message the message to store.
     */
    public void addMessage(Message message) {
        if (message == null) {
            return;
        }

        JID sent = message.getFrom();
        JID recipient = message.getTo();

        String fromUserId = sent.getNode();
        String toUserId = recipient.getNode();

        if (fromUserId == null || toUserId == null) {
            return;
        }

        // 如何body为空不存储
        if (message.getBody() == null) {
            return;
        }

        // Get the message in XML format.
        String msgXML = message.getElement().asXML();

        History history = new History();
        history.setFromUserId(fromUserId);
        history.setToUserId(toUserId);
        history.setCreationTime(System.currentTimeMillis());
        history.setMessageSize(msgXML.length());
        history.setStanza(msgXML);

        String broadcastServiceName = JiveGlobals.getProperty("plugin.broadcast.serviceName", "broadcast");
        if (recipient.getDomain().startsWith(broadcastServiceName)) {
            String groupId = recipient.getNode();
            history.setGroupId(groupId);
            history.setId(groupId + "_" + history.getCreationTime());
            history.setMessageType("groupchat");
        } else {
            // 经过broadcast插件解析过后的含有前缀消息不存储
            String prefix = JiveGlobals.getProperty("plugin.broadcast.messagePrefix", "(broadcast)");
            if (message.getBody().startsWith(prefix)) {
                return;
            }

            history.setId(fromUserId + "_" + toUserId + "_" + history.getCreationTime());
            history.setMessageType("chat");
        }

        boolean isSaveSuccess = false;

        // 保存历史消息
        // 是否要重试呢？
        try {
            MongoUtil mongoUtil = MongoUtil.getInstance();
            mongoUtil.getDatastore().save(history);
            isSaveSuccess = true;
        } catch (Exception e) {
            Log.error("Failed to save history message!Try again!", e);
        }

        if (!isSaveSuccess) {
            return;
        }

        // 消息时间回执
        PacketExtension requestTimestampExtension = message.getExtension(TimestampReceiptRequest.ELEMENT_NAME,
                TimestampReceiptRequest.NAMESPACE);
        if (requestTimestampExtension != null) {
            Message response = new Message();
            response.setTo(sent);

            TimestampResponseExtension timestampResponseExtension = new TimestampResponseExtension(message.getID(),
                    history.getCreationTime());
            SAXReader saxReader = new SAXReader();
            try {
                Document doc = saxReader.read(new StringReader(timestampResponseExtension.toXML()));
                response.addExtension(new PacketExtension(doc.getRootElement()));
                XMPPServer.getInstance().getRoutingTable().routePacket(sent, response, true);
            } catch (DocumentException e) {
                Log.error("Error reading extension xml!", e);
            }
        }
    }

    public void start() throws IllegalStateException {
        // Initialize the pool of sax readers
        for (int i = 0; i < POOL_SIZE; i++) {
            SAXReader xmlReader = new SAXReader();
            xmlReader.setEncoding("UTF-8");
            xmlReaders.add(xmlReader);
        }
    }

    public void stop() {
        // Clean up the pool of sax readers
        xmlReaders.clear();
    }
}
