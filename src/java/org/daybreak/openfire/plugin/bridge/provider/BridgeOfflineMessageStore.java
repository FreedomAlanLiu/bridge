package org.daybreak.openfire.plugin.bridge.provider;

import org.daybreak.openfire.plugin.bridge.model.Offline;
import org.daybreak.openfire.plugin.bridge.utils.RedisUtil;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;
import org.jivesoftware.database.SequenceManager;
import org.jivesoftware.openfire.OfflineMessage;
import org.jivesoftware.openfire.OfflineMessageStore;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.event.UserEventDispatcher;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.util.JiveConstants;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alan on 14-5-16.
 */
public class BridgeOfflineMessageStore extends OfflineMessageStore {

    private static final Logger Log = LoggerFactory.getLogger(BridgeOfflineMessageStore.class);

    private static final int POOL_SIZE = 10;

    /**
     * Pattern to use for detecting invalid XML characters. Invalid XML characters will
     * be removed from the stored offline messages.
     */
    private Pattern pattern = Pattern.compile("&\\#[\\d]+;");

    /**
     * Returns the instance of <tt>OfflineMessageStore</tt> being used by the XMPPServer.
     *
     * @return the instance of <tt>OfflineMessageStore</tt> being used by the XMPPServer.
     */
    public static OfflineMessageStore getInstance() {
        return XMPPServer.getInstance().getOfflineMessageStore();
    }

    /**
     * Pool of SAX Readers. SAXReader is not thread safe so we need to have a pool of readers.
     */
    private BlockingQueue<SAXReader> xmlReaders = new LinkedBlockingQueue<SAXReader>(POOL_SIZE);

    /**
     * Constructs a new offline message store.
     */
    public BridgeOfflineMessageStore() {
        super();
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
        if(!shouldStoreMessage(message)) {
            return;
        }
        JID recipient = message.getTo();
        String username = recipient.getNode();
        // If the username is null (such as when an anonymous user), don't store.
        if (username == null || !UserManager.getInstance().isRegisteredUser(recipient)) {
            return;
        }
        else
        if (!XMPPServer.getInstance().getServerInfo().getXMPPDomain().equals(recipient.getDomain())) {
            // Do not store messages sent to users of remote servers
            return;
        }

        long messageID = SequenceManager.nextID(JiveConstants.OFFLINE);

        // Get the message in XML format.
        String msgXML = message.getElement().asXML();

        Offline offline = new Offline();
        offline.setUsername(username);
        offline.setMessageID(messageID);
        offline.setCreationDate(new Date());
        offline.setMessageSize(msgXML.length());
        offline.setStanza(msgXML);

        try {
            RedisUtil.getInstance().setOffline(offline);
        } catch (Exception e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
    }

    /**
     * Returns a Collection of all messages in the store for a user.
     * Messages may be deleted after being selected from the database depending on
     * the delete param.
     *
     * @param username the username of the user who's messages you'd like to receive.
     * @param delete true if the offline messages should be deleted.
     * @return An iterator of packets containing all offline messages.
     */
    public Collection<OfflineMessage> getMessages(String username, boolean delete) {
        List<OfflineMessage> messages = new ArrayList<OfflineMessage>();
        SAXReader xmlReader = null;
        try {
            // Get a sax reader from the pool
            xmlReader = xmlReaders.take();
            List offlineList = RedisUtil.getInstance().getOfflineList(username);
            for (Object obj : offlineList) {
                if (!(obj instanceof Offline)) {
                    continue;
                }
                Offline offline = (Offline) obj;
                String msgXML = offline.getStanza();
                Date creationDate = offline.getCreationDate();
                OfflineMessage message;
                try {
                    message = new OfflineMessage(creationDate,
                            xmlReader.read(new StringReader(msgXML)).getRootElement());
                } catch (DocumentException e) {
                    // Try again after removing invalid XML chars (e.g. &#12;)
                    Matcher matcher = pattern.matcher(msgXML);
                    if (matcher.find()) {
                        msgXML = matcher.replaceAll("");
                    }
                    message = new OfflineMessage(creationDate,
                            xmlReader.read(new StringReader(msgXML)).getRootElement());
                }

                // Add a delayed delivery (XEP-0203) element to the message.
                Element delay = message.addChildElement("delay", "urn:xmpp:delay");
                delay.addAttribute("from", XMPPServer.getInstance().getServerInfo().getXMPPDomain());
                delay.addAttribute("stamp", XMPPDateTimeFormat.format(creationDate));
                // Add a legacy delayed delivery (XEP-0091) element to the message. XEP is obsolete and support should be dropped in future.
                delay = message.addChildElement("x", "jabber:x:delay");
                delay.addAttribute("from", XMPPServer.getInstance().getServerInfo().getXMPPDomain());
                delay.addAttribute("stamp", XMPPDateTimeFormat.formatOld(creationDate));
                messages.add(message);
            }
            // Check if the offline messages loaded should be deleted, and that there are
            // messages to delete.
            if (delete && !messages.isEmpty()) {
                try {
                    RedisUtil.getInstance().delOfflineList(username);
                }
                catch (Exception e) {
                    Log.error("Error deleting offline messages of username: " + username, e);
                }
            }
        } catch (Exception e) {
            Log.error("Error retrieving offline messages of username: " + username, e);
        } finally {
            // Return the sax reader to the pool
            if (xmlReader != null) {
                xmlReaders.add(xmlReader);
            }
        }
        return messages;
    }

    /**
     * Returns the offline message of the specified user with the given creation date. The
     * returned message will NOT be deleted from the database.
     *
     * @param username the username of the user who's message you'd like to receive.
     * @param creationDate the date when the offline message was stored in the database.
     * @return the offline message of the specified user with the given creation stamp.
     */
    public OfflineMessage getMessage(String username, Date creationDate) {
        OfflineMessage message = null;
        SAXReader xmlReader = null;
        try {
            // Get a sax reader from the pool
            xmlReader = xmlReaders.take();
            Offline offline = RedisUtil.getInstance().getOffline(username, creationDate);
            if (offline != null) {
                String msgXML = offline.getStanza();
                message = new OfflineMessage(creationDate,
                        xmlReader.read(new StringReader(msgXML)).getRootElement());
                // Add a delayed delivery (XEP-0203) element to the message.
                Element delay = message.addChildElement("delay", "urn:xmpp:delay");
                delay.addAttribute("from", XMPPServer.getInstance().getServerInfo().getXMPPDomain());
                delay.addAttribute("stamp", XMPPDateTimeFormat.format(creationDate));
                // Add a legacy delayed delivery (XEP-0091) element to the message. XEP is obsolete and support should be dropped in future.
                delay = message.addChildElement("x", "jabber:x:delay");
                delay.addAttribute("from", XMPPServer.getInstance().getServerInfo().getXMPPDomain());
                delay.addAttribute("stamp", XMPPDateTimeFormat.formatOld(creationDate));
            }
        } catch (Exception e) {
            Log.error("Error retrieving offline messages of username: " + username +
                    " creationDate: " + creationDate, e);
        } finally {
            // Return the sax reader to the pool
            if (xmlReader != null) {
                xmlReaders.add(xmlReader);
            }
        }
        return message;
    }

    /**
     * Deletes all offline messages in the store for a user.
     *
     * @param username the username of the user who's messages are going to be deleted.
     */
    public void deleteMessages(String username) {
        try {
            RedisUtil.getInstance().delOfflineList(username);
        } catch (Exception e) {
            Log.error("Error deleting offline messages of username: " + username, e);
        }
    }

    /**
     * Deletes the specified offline message in the store for a user. The way to identify the
     * message to delete is based on the creationDate and username.
     *
     * @param username the username of the user who's message is going to be deleted.
     * @param creationDate the date when the offline message was stored in the database.
     */
    public void deleteMessage(String username, Date creationDate) {
        try {
            RedisUtil.getInstance().delOffline(username, creationDate);
        } catch (Exception e) {
            Log.error("Error deleting offline messages of username: " + username +
                    " creationDate: " + creationDate, e);
        }
    }

    /**
     * Returns the approximate size (in bytes) of the XML messages stored for
     * a particular user.
     *
     * @param username the username of the user.
     * @return the approximate size of stored messages (in bytes).
     */
    public int getSize(String username) {
        int size = 0;
        try {
            size = RedisUtil.getInstance().getOfflineListSize(username);
        } catch (Exception e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
        return size;
    }

    /**
     * Returns the approximate size (in bytes) of the XML messages stored for all
     * users.
     *
     * @return the approximate size of all stored messages (in bytes).
     */
    public int getSize() {
        int size = 0;
        try {
            size = RedisUtil.getInstance().getAllOfflineListSize();
        } catch (Exception e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
        return size;
    }

    public void userCreated(User user, Map params) {
        //Do nothing
    }

    public void userDeleting(User user, Map params) {
        // Delete all offline messages of the user
        deleteMessages(user.getUsername());
    }

    public void userModified(User user, Map params) {
        //Do nothing
    }

    @Override
    public void start() throws IllegalStateException {
        super.start();
        // Initialize the pool of sax readers
        for (int i=0; i<POOL_SIZE; i++) {
            SAXReader xmlReader = new SAXReader();
            xmlReader.setEncoding("UTF-8");
            xmlReaders.add(xmlReader);
        }
        // Add this module as a user event listener so we can delete
        // all offline messages when a user is deleted
        UserEventDispatcher.addListener(this);
    }

    @Override
    public void stop() {
        super.stop();
        // Clean up the pool of sax readers
        xmlReaders.clear();
        // Remove this module as a user event listener
        UserEventDispatcher.removeListener(this);
    }

    /**
     * Decide whether a message should be stored offline according to XEP-0160 and XEP-0334.
     *
     * @param message
     * @return <code>true</code> if the message should be stored offline, <code>false</code> otherwise.
     */
    static boolean shouldStoreMessage(final Message message) {
        // XEP-0334: Implement the <no-store/> hint to override offline storage
        if (message.getChildElement("no-store", "urn:xmpp:hints") != null) {
            return false;
        }

        switch (message.getType()) {
            case chat:
                // XEP-0160: Messages with a 'type' attribute whose value is "chat" SHOULD be stored offline, with the exception of messages that contain only Chat State Notifications (XEP-0085) [7] content

                // Iterate through the child elements to see if we can find anything that's not a chat state notification or
                // real time text notification
                Iterator<?> it = message.getElement().elementIterator();

                while (it.hasNext()) {
                    Object item = it.next();

                    if (item instanceof Element) {
                        Element el = (Element) item;

                        if (!el.getNamespaceURI().equals("http://jabber.org/protocol/chatstates")
                                && !(el.getQName().equals(QName.get("rtt", "urn:xmpp:rtt:0")))
                                ) {
                            return true;
                        }
                    }
                }

                return false;

            case groupchat:
            case headline:
                // XEP-0160: "groupchat" message types SHOULD NOT be stored offline
                // XEP-0160: "headline" message types SHOULD NOT be stored offline
                return false;

            case error:
                // XEP-0160: "error" message types SHOULD NOT be stored offline,
                // although a server MAY store advanced message processing errors offline
                if (message.getChildElement("amp", "http://jabber.org/protocol/amp") == null) {
                    return false;
                }
                break;

            default:
                // XEP-0160: Messages with a 'type' attribute whose value is "normal" (or messages with no 'type' attribute) SHOULD be stored offline.
                break;
        }
        return true;
    }
}
