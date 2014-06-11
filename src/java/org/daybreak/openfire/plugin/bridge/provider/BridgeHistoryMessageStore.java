package org.daybreak.openfire.plugin.bridge.provider;

import org.daybreak.openfire.plugin.bridge.model.History;
import org.daybreak.openfire.plugin.bridge.utils.MongoUtil;
import org.daybreak.openfire.plugin.bridge.utils.RedisUtil;
import org.dom4j.io.SAXReader;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.util.*;
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
        JID recipient = message.getTo();
        String username = recipient.getNode();
        // If the username is null (such as when an anonymous user), don't store.
        if (username == null || !UserManager.getInstance().isRegisteredUser(recipient)) {
            return;
        } else if (!XMPPServer.getInstance().getServerInfo().getXMPPDomain().equals(recipient.getDomain())) {
            // Do not store messages sent to users of remote servers
            return;
        }

        // 如何body为空不存储
        if (message.getBody() == null) {
            return;
        }

        // Get the message in XML format.
        String msgXML = message.getElement().asXML();

        History history = new History();
        history.setId(username + "_" + message.getID());
        history.setUsername(username);
        history.setCreationDate(new Date());
        history.setMessageSize(msgXML.length());
        history.setStanza(msgXML);

        try {
            MongoUtil mongoUtil = MongoUtil.getInstance();
            mongoUtil.getDatastore().save(history);
        } catch (Exception e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
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
