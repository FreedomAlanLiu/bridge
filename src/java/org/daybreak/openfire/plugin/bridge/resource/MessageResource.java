package org.daybreak.openfire.plugin.bridge.resource;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.group.Group;
import org.jivesoftware.openfire.group.GroupManager;
import org.jivesoftware.openfire.group.GroupNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketExtension;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.StringReader;

/**
 * Created by alan on 14-5-30.
 */
@Path("message")
public class MessageResource {

    private static final Logger logger = LoggerFactory.getLogger(MessageResource.class);

    @POST
    @Consumes("application/x-www-form-urlencoded")
    public void post(@FormParam("from") String from,
                     @FormParam("to") String to,
                     @FormParam("body") String body,
                     @FormParam("type") String type,
                     @FormParam("extType") String extType,
                     @FormParam("extContent") String extContent) {
        JID fromJid = new JID(from);
        JID toJid = new JID(to);
        Message.Type messageType = Message.Type.chat;
        if ("groupchat".equals(type)) {
            messageType = Message.Type.groupchat;
        }
        pushMessage(fromJid, toJid, body, messageType, extType, extContent);
    }

    private static void pushMessage(JID from, JID to, String body,
                                    Message.Type type, String extType, String extContent) {

        Message message = new Message();
        message.setType(type);

        if (StringUtils.isNotEmpty(extContent)) {
            MessageExtension messageExtension = new MessageExtension(extType, extContent);
            SAXReader saxReader = new SAXReader();
            try {
                Document doc = saxReader.read(new StringReader(messageExtension.toXML()));
                message.addExtension(new PacketExtension(doc.getRootElement()));
            } catch (DocumentException e) {
                logger.error("", e);
            }
        }

        if (type == Message.Type.groupchat) {
            GroupManager groupManager = GroupManager.getInstance();
            try {
                Group group = groupManager.getGroup(to.getNode());
                message.setFrom(from);
                message.setBody("(broadcast) " + to.getNode() + "#GID#" + body);
                for (JID jid : group.getMembers()) {
                    message.setTo(jid);
                    XMPPServer.getInstance().getRoutingTable().routePacket(jid, message, true);
                }
            } catch (GroupNotFoundException e) {
                logger.error("", e);
            }
        } else {
            message.setFrom(from);
            message.setTo(to);
            message.setBody(body);
            XMPPServer.getInstance().getRoutingTable().routePacket(to, message, true);
        }
    }

}
