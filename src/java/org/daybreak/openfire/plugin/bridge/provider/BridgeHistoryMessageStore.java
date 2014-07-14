package org.daybreak.openfire.plugin.bridge.provider;

import org.daybreak.openfire.plugin.bridge.model.History;
import org.daybreak.openfire.plugin.bridge.utils.MongoUtil;
import org.dom4j.io.SAXReader;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

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

        // If the username is null (such as when an anonymous user), don't store.
        /*if (fromUserId == null || !UserManager.getInstance().isRegisteredUser(sent)) {
            return;
        } else if (!XMPPServer.getInstance().getServerInfo().getXMPPDomain().equals(sent.getDomain())) {
            // Do not store messages sent to users of remote servers
            return;
        }*/
        if (fromUserId == null
                || !XMPPServer.getInstance().getServerInfo().getXMPPDomain().equals(sent.getDomain())) {
            return;
        }

        // If the username is null (such as when an anonymous user), don't store.
        /*if (toUserId == null || !UserManager.getInstance().isRegisteredUser(recipient)) {
            return;
        } else if (!XMPPServer.getInstance().getServerInfo().getXMPPDomain().equals(recipient.getDomain())) {
            // Do not store messages sent to users of remote servers
            return;
        }*/
        if (toUserId == null
                || !XMPPServer.getInstance().getServerInfo().getXMPPDomain().equals(recipient.getDomain())) {
            return;
        }

        // 如何body为空不存储
        if (message.getBody() == null) {
            return;
        }

        // Get the message in XML format.
        String msgXML = message.getElement().asXML();

        History history = new History();
        history.setId(fromUserId + "_" + toUserId + "_" + message.getID());
        history.setFromUserId(fromUserId);
        history.setToUserId(toUserId);
        history.setCreationTime(System.currentTimeMillis());
        history.setMessageSize(msgXML.length());
        history.setStanza(msgXML);

        // 保存历史消息
        try {
            MongoUtil mongoUtil = MongoUtil.getInstance();
            mongoUtil.getDatastore().save(history);
        } catch (Exception e1) {
            Log.error("Failed to save history message!Try again!", e1);
            try {
                MongoUtil mongoUtil = MongoUtil.getInstance();
                mongoUtil.getDatastore().save(history);
            } catch (Exception e2) {
                Log.error("Failed to save history message!Try again!", e2);
                try {
                    MongoUtil mongoUtil = MongoUtil.getInstance();
                    mongoUtil.getDatastore().save(history);
                } catch (Exception e3) {
                    Log.error("Failed to save history message!", e3);
                }
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
