package org.daybreak.openfire.plugin.bridge.resource;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.daybreak.openfire.plugin.bridge.BridgeServiceFactory;
import org.daybreak.openfire.plugin.bridge.model.History;
import org.daybreak.openfire.plugin.bridge.model.User;
import org.daybreak.openfire.plugin.bridge.service.BridgeService;
import org.daybreak.openfire.plugin.bridge.utils.MongoUtil;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.group.Group;
import org.jivesoftware.openfire.group.GroupManager;
import org.jivesoftware.openfire.group.GroupNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.component.ComponentException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketExtension;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * Created by alan on 14-5-30.
 */
@Path("message")
public class MessageResource {

    private static final Logger logger = LoggerFactory.getLogger(MessageResource.class);

    private ObjectMapper mapper = new ObjectMapper();

    String broadcastServiceName = JiveGlobals.getProperty("plugin.broadcast.serviceName", "broadcast");
    String broadMessagePrefix = JiveGlobals.getProperty("plugin.broadcast.messagePrefix", "(broadcast)");

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@QueryParam("fromUserId") String fromUserId,
                      @QueryParam("toUserId") String toUserId,
                      @QueryParam("token") String token,
                      @QueryParam("startTime") long startTime,
                      @QueryParam("start") int start,
                      @QueryParam("length") int length) throws IOException {

        BridgeService bridgeService = (BridgeService) BridgeServiceFactory.getBean("bridgeService");
        try {
            User u = bridgeService.findUser(fromUserId, token);
            if (u == null) {
                u = bridgeService.findUser(toUserId, token);
            }
            if (u == null) {
                logger.warn("token validation failed!");
                return "{\"result\": \"error\", \"cause\": \"token validation failed!\"}";
            }
        } catch (Exception e) {
            logger.error("", e);
        }

        Datastore datastore = MongoUtil.getInstance().getDatastore();
        Query query = datastore.createQuery(History.class)
                .filter("fromUserId =", fromUserId)
                .filter("toUserId =", toUserId);
        if (startTime > 0) {
            query = query.filter("creationTime <=", startTime);
        }
        query = query.offset(start).limit(length);

        List<History> historyList = query.asList();
        return mapper.writeValueAsString(historyList);
    }

    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public String post(@FormParam("from") String from,
                     @FormParam("to") String to,
                     @FormParam("body") String body,
                     @FormParam("type") String type,
                     @FormParam("extType") String extType,
                     @FormParam("extContent") String extContent) {

        logger.info("post message: [from: " + from + ", to: " + to + ", body: " + body + ", type: "
                + type + ", extType: " + extType + ", extContent: " + extContent + "]");
        try {
            JID fromJid = new JID(from);
            JID toJid = new JID(to);
            Message.Type messageType = Message.Type.chat;
            if ("groupchat".equals(type)) {
                messageType = Message.Type.groupchat;
            }
            pushMessage(fromJid, toJid, body, messageType, extType, extContent);
        } catch (Exception e) {
            logger.error("error posting message!", e);
            return "{\"result\": \"error\", \"cause\": \"" + e.getMessage() + "\"}";
        }
        return "{\"result\": \"success\"}";
    }

    private void pushMessage(JID from, JID to, String body, Message.Type messageType, String extType, String extContent) throws ComponentException {
        Message message = new Message();
        message.setType(messageType);

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

        if (to.getDomain().startsWith(broadcastServiceName + ".")) {
            GroupManager groupManager = GroupManager.getInstance();
            try {
                logger.info("group id: " + to.getNode());
                Group group = groupManager.getGroup(to.getNode());
                message.setFrom(from);
                message.setBody(broadMessagePrefix + " " + to.getNode() + "#GID#" + body);
                for (JID jid : group.getMembers()) {
                    message.setTo(jid);
                    XMPPServer.getInstance().getRoutingTable().routePacket(jid, message, true);
                    logger.info("HTTP server route message: " + message.toXML());
                }
            } catch (GroupNotFoundException e) {
                logger.error("", e);
            }
        } else {
            message.setFrom(from);
            message.setTo(to);
            message.setBody(body);
            XMPPServer.getInstance().getRoutingTable().routePacket(to, message, true);
            logger.info("HTTP server route message: " + message.toXML());
        }
    }

}
