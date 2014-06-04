package org.daybreak.openfire.plugin.bridge.resource;

import org.apache.commons.lang3.StringUtils;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.group.Group;
import org.jivesoftware.openfire.group.GroupManager;
import org.jivesoftware.openfire.group.GroupNotFoundException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * Created by alan on 14-5-30.
 */
@Path("message")
public class MessageResource {

    @POST
    @Consumes("application/x-www-form-urlencoded")
    public void post(@FormParam("from") String from,
                     @FormParam("to") String to,
                     @FormParam("body") String body,
                     @FormParam("type") String type) {
        JID fromJid = new JID(from);
        JID toJid = new JID(to);
        Message.Type messageType = Message.Type.chat;
        if ("groupchat".equals(type)) {
            messageType = Message.Type.groupchat;
        }
        pushMessage(fromJid, toJid, body, messageType);
    }

    private static void pushMessage(JID from, JID to, String body,
                                    Message.Type type) {

        Message message = new Message();
        message.setType(type);

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
            }
        } else {
            message.setFrom(from);
            message.setTo(to);
            message.setBody(body);
            XMPPServer.getInstance().getRoutingTable().routePacket(to, message, true);
        }
    }

}
