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
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
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
import java.io.StringReader;
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
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (incoming && !processed && (packet instanceof Message)) {
                    JID toJID = packet.getTo();
                    String body = ((Message) packet).getBody();
                    try {
                        if (toJID != null && body != null) {
                            logger.info("incoming message:" + packet.toXML());
                            BridgeHistoryMessageStore.getInstance().addMessage((Message) packet);
                        }
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }

                if (incoming && processed && (packet instanceof Message)) {
                    logger.info("processed message:" + packet.toXML());
                    Message message = (Message) packet;
                    PacketExtension requestTimestampExtension = message.getExtension(TimestampReceiptRequest.ELEMENT_NAME,
                            TimestampReceiptRequest.NAMESPACE);
                    if (requestTimestampExtension != null) {
                        Message response = new Message();
                        response.setFrom("server@" + XMPPServer.getInstance().getServerInfo().getXMPPDomain());
                        response.setTo(message.getTo());
                        response.setType(Message.Type.chat);

                        Datastore datastore = MongoUtil.getInstance().getDatastore();
                        Query<History> query = datastore.createQuery(History.class)
                                .filter("messageId =", message.getID());
                        List<History> historyList = query.asList();
                        if (historyList.size() > 0) {
                            History history = historyList.get(0);
                            TimestampResponseExtension timestampResponseExtension = new TimestampResponseExtension(message.getID(),
                                    history.getCreationTime());
                            SAXReader saxReader = new SAXReader();
                            try {
                                Document doc = saxReader.read(new StringReader(timestampResponseExtension.toXML()));
                                PacketExtension timestampExtension = new PacketExtension(doc.getRootElement());
                                response.addExtension(timestampExtension);
                                XMPPServer.getInstance().getRoutingTable().routePacket(message.getTo(), response, true);
                            } catch (DocumentException e) {
                                logger.error("Error reading extension xml!", e);
                            }
                        }
                    }
                }

                boolean bccsActivate = JiveGlobals.getBooleanProperty("plugin.bridge.bccs.activate", false);
                if (!bccsActivate || !incoming || !processed) {
                    return;
                }

                if (packet instanceof Message) {
                    Message message = (Message) packet;
                    JID fromJID = message.getFrom();
                    JID toJID = message.getTo();
                    String messageBody = message.getBody();
                    if (StringUtils.isEmpty(messageBody)) {
                        return;
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
                            baiduYunService.pushBroadcastMessage(toJID.getNode(), bridgeService.toJson(article));
                        } catch (IOException e) {
                            logger.error("", e);
                        }
                    }
                }
            }
        });
    }
}
