package org.daybreak.openfire.plugin.bridge;

import org.apache.commons.lang3.StringUtils;
import org.daybreak.openfire.plugin.bridge.model.*;
import org.daybreak.openfire.plugin.bridge.service.BaiduYunService;
import org.daybreak.openfire.plugin.bridge.service.BridgeService;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

/**
 * Created by Alan on 2014/4/20.
 */
public class BridgePacketInterceptor implements PacketInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(BridgePacketInterceptor.class);

    @Override
    public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) throws PacketRejectedException {
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

            Article article = new Article();
            article.setMessage(messageBody);
            Article.Aps aps = article.new Aps();
            aps.setAlert(messageBody);
            article.setAps(aps);

            String prefix = JiveGlobals.getProperty("plugin.broadcast.messagePrefix", "(broadcast)");
            if (XMPPServer.getInstance().getServerInfo().getXMPPDomain().equals(toJID.getDomain())) {
                if (messageBody.startsWith(prefix)) {
                    return;
                }
                if (fromJID == null) {
                    return;
                }
                User fromUser = bridgeService.getUser(fromJID.getNode());
                if (fromUser == null) {
                    return;
                }
                User toUser = bridgeService.getUser(toJID.getNode());
                if (toUser == null) {
                    return;
                }
                try {
                    article.setFrom(fromJID.getNode());
                    article.setMessageType(MessageType.friend.toString());
                    List<Device> devices = bridgeService.findDevice(toUser.getId());
                    for (Device device : devices) {
                        baiduYunService.pushMessage(device.getChannelId(), device.getBaiduUserId(),
                                device.getDeviceType(), bridgeService.toJson(article));
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
                article.getAps().setAlert(msg);
                try {
                    article.setFrom(groupId);
                    article.setMessageType(MessageType.broadcast.toString());
                    baiduYunService.pushTagMessage(toJID.getNode(), "android", bridgeService.toJson(article));
                    baiduYunService.pushTagMessage(toJID.getNode(), "ios", bridgeService.toJson(article));
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
        }
    }
}
