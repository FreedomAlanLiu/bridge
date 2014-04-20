package org.daybreak.openfire.plugin.bridge;

import org.apache.commons.lang3.StringUtils;
import org.daybreak.openfire.plugin.bridge.model.User;
import org.daybreak.openfire.plugin.bridge.service.BaiduYunService;
import org.daybreak.openfire.plugin.bridge.service.BridgeService;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.JiveGlobals;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.text.MessageFormat;

/**
 * Created by Alan on 2014/4/20.
 */
public class BridgePacketInterceptor implements PacketInterceptor {

    public static final String PUSHING_MESSAGE_JSON = "{\"type\":\"chat\",\"value\":\"{0}\"}";

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
            User fromUser = bridgeService.loadUser(fromJID.getNode());
            if (fromUser == null) {
                return;
            }
            String pushingMessageValue = fromUser.getUsername()
                    + (StringUtils.isBlank(fromUser.getName()) ? "" : "(" + fromUser.getName() + ")")
                    + "：" + messageBody;
            String pushingMessage = MessageFormat.format(PUSHING_MESSAGE_JSON, pushingMessageValue);

            BaiduYunService baiduYunService = (BaiduYunService) BridgeServiceFactory.getBean("baiduYunService");
            if (XMPPServer.getInstance().getServerInfo().getXMPPDomain().equals(toJID.getDomain())) {
                User toUser = bridgeService.loadUser(toJID.getNode());
                if (toUser == null) {
                    return;
                }
                baiduYunService.pushMessage(toUser.getChannelId(), toUser.getBaiduUserId(), pushingMessage);
            } else {
                baiduYunService.pushTagMessage(toJID.getNode(), pushingMessage);
            }
        }
    }
}