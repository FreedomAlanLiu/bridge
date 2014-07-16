package org.daybreak.openfire.plugin.bridge.resource;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.group.GroupManager;
import org.jivesoftware.openfire.group.GroupProvider;
import org.xmpp.packet.JID;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Created by alan on 14-7-14.
 */
@Path("/groups")
public class GroupResource {

    @POST
    @Path("/deleteGroup")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteGroup(@FormParam("groupId") String groupId) {
        try {
            GroupManager groupManager = GroupManager.getInstance();
            GroupProvider groupProvider = groupManager.getProvider();
            groupProvider.deleteGroup(groupId);
        } catch (Exception e) {
            return "{\"result\": \"error\", \"cause\": \"" + e.getMessage() + "\"}";
        }
        return "{\"result\": \"success\"}";
    }

    @POST
    @Path("/addMember")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public String addMember(@FormParam("groupId") String groupId,
                          @FormParam("userId") String userId) {
        try {
            GroupManager groupManager = GroupManager.getInstance();
            GroupProvider groupProvider = groupManager.getProvider();
            groupProvider.addMember(
                    groupId,
                    new JID(userId + "@" + XMPPServer.getInstance().getServerInfo().getXMPPDomain()),
                    false);
        } catch (Exception e) {
            return "{\"result\": \"error\", \"cause\": \"" + e.getMessage() + "\"}";
        }
        return "{\"result\": \"success\"}";
    }

    @POST
    @Path("/deleteMember")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteMember(@FormParam("groupId") String groupId,
                            @FormParam("userId") String userId) {
        try {
            GroupManager groupManager = GroupManager.getInstance();
            GroupProvider groupProvider = groupManager.getProvider();
            groupProvider.deleteMember(
                    groupId,
                    new JID(userId + "@" + XMPPServer.getInstance().getServerInfo().getXMPPDomain()));
        } catch (Exception e) {
            return "{\"result\": \"error\", \"cause\": \"" + e.getMessage() + "\"}";
        }
        return "{\"result\": \"success\"}";
    }
}
