package org.daybreak.openfire.plugin.bridge;

import org.apache.commons.lang3.StringUtils;
import org.daybreak.openfire.plugin.bridge.model.*;
import org.daybreak.openfire.plugin.bridge.provider.BridgeHistoryMessageStore;
import org.daybreak.openfire.plugin.bridge.provider.TimestampReceiptRequest;
import org.daybreak.openfire.plugin.bridge.provider.TimestampResponseExtension;
import org.daybreak.openfire.plugin.bridge.resource.MessageExtension;
import org.daybreak.openfire.plugin.bridge.service.BaiduYunService;
import org.daybreak.openfire.plugin.bridge.service.BridgeService;
import org.daybreak.openfire.plugin.bridge.utils.MongoUtil;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.JiveGlobals;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketExtension;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Alan on 2014/4/20.
 */
public class BridgePacketInterceptor implements PacketInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(BridgePacketInterceptor.class);

    ExecutorService executorService = Executors.newCachedThreadPool();

    public void interceptPacket(final Packet packet, final Session session, final boolean incoming,
                                final boolean processed) throws PacketRejectedException {

        if (incoming && !processed && (packet instanceof Message)) {
            JID toJID = packet.getTo();
            String body = ((Message) packet).getBody();
            try {
                if (toJID != null) {
                    logger.info("incoming message:" + packet.toXML());
                    BridgeHistoryMessageStore.getInstance().addMessage((Message) packet);
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }

        if (incoming && processed && (packet instanceof Message)) {
            logger.info("processed message:" + packet.toXML());
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                boolean bccsActivate = JiveGlobals.getBooleanProperty("plugin.bridge.bccs.activate", false);
                if (!bccsActivate || !incoming || !processed) {
                    return;
                }

                if (packet instanceof Message) {
                    Message message = (Message) packet;
                    JID fromJID = message.getFrom();
                    JID toJID = message.getTo();
                    String messageBody = message.getBody();
                    if (messageBody == null) {
                        messageBody = "";
                    }

                    BridgeService bridgeService = (BridgeService) BridgeServiceFactory.getBean("bridgeService");
                    BaiduYunService baiduYunService = (BaiduYunService) BridgeServiceFactory.getBean("baiduYunService");

                    if (fromJID == null) {
                        return;
                    }
                    User fromUser = null;
                    try {
                        fromUser = bridgeService.getUser(fromJID.getNode());
                    } catch (Exception e) {
                    }
                    if (fromUser == null) {
                        return;
                    }

                    PacketExtension extension = message.getExtension(MessageExtension.ELEMENT_NAME, MessageExtension.ELEMENT_NAME);
                    if (extension != null) {
                        return;
                    }

                    PacketExtension dataExtension = message.getExtension("data", "urn:xmpp:bob");
                    if (dataExtension != null) {
                        Attribute typeAttr = dataExtension.getElement().attribute("type");
                        if (typeAttr != null && typeAttr.getValue().startsWith("image")) {
                            messageBody += "[图片]";
                        }
                    }

                    if (StringUtils.isEmpty(messageBody)) {
                        return;
                    }

                    Article article = new Article();
                    article.setMessage(messageBody);
                    Article.Aps aps = article.new Aps();
                    article.setAps(aps);

                    String prefix = JiveGlobals.getProperty("plugin.broadcast.messagePrefix", "(broadcast)");
                    if (XMPPServer.getInstance().getServerInfo().getXMPPDomain().equals(toJID.getDomain())) {
                        if (messageBody.startsWith(prefix)) {
                            return;
                        }
                        User toUser = null;
                        try {
                            toUser = bridgeService.getUser(toJID.getNode());
                        } catch (Exception e) {
                        }
                        if (toUser == null) {
                            return;
                        }
                        try {
                            article.getAps().setAlert(fromUser.getName() + "：" + messageBody);
                            article.setFrom(fromJID.getNode());
                            article.setMessageType(MessageType.friend.toString());
                            List<Device> devices = bridgeService.findDevice(toUser.getId());
                            logger.info("devices size: " + devices.size());
                            for (Device device : devices) {
                                logger.info(devices.toString());
                                baiduYunService.pushMessage(device, bridgeService.toJson(article));
                            }
                        } catch (Exception e) {
                            logger.error("", e);
                        }
                    } else {
                        if (!messageBody.startsWith(prefix)) {
                            return;
                        }
                        String msg = messageBody.substring(prefix.length());
                        int gidEndIndex = msg.indexOf("#GID#");
                        if (gidEndIndex < 0) {
                            return;
                        }
                        String groupId = msg.substring(0, gidEndIndex).trim();
                        msg = msg.substring(gidEndIndex + 5);
                        article.setMessage(msg);
                        article.getAps().setAlert(fromUser.getName() + "：" + msg);
                        try {
                            article.setFrom(groupId);
                            article.setMessageType(MessageType.broadcast.toString());
                            //baiduYunService.pushTagMessage(toJID.getNode(), "android", bridgeService.toJson(article));
                            //baiduYunService.pushTagMessage(toJID.getNode(), "ios", bridgeService.toJson(article));
                            baiduYunService.pushBroadcastMessage(fromJID.getNode(), toJID.getNode(), bridgeService.toJson(article));
                        } catch (IOException e) {
                            logger.error("", e);
                        }
                    }
                }
            }
        });
    }
}
