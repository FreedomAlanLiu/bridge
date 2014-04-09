package org.daybreak.openfire.plugin.bridge.provider;

import org.daybreak.openfire.plugin.bridge.model.Membership;
import org.daybreak.openfire.plugin.bridge.service.BridgeService;
import org.daybreak.openfire.plugin.bridge.service.impl.BridgeServiceImpl;
import org.jivesoftware.openfire.group.AbstractGroupProvider;
import org.jivesoftware.openfire.group.Group;
import org.jivesoftware.openfire.group.GroupNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.xmpp.packet.JID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Alan on 2014/4/3.
 */
public class BridgeGroupProvider extends AbstractGroupProvider {

    @Override
    public Group getGroup(String name) throws GroupNotFoundException {
        Collection<JID> members = new ArrayList<JID>();
        Collection<JID> administrators = new ArrayList<JID>();
        BridgeService bridgeService = BridgeServiceImpl.getInstance();
        org.daybreak.openfire.plugin.bridge.model.Group bridgeGroup = bridgeService.getBridgeGroup(name);
        String description = "";
        if (bridgeGroup != null) {
            description = bridgeGroup.getDescription();
            for (Membership membership : bridgeGroup.getMemberships()) {
                JID jid = new JID(membership.getUser().getId()
                        + "@" + JiveGlobals.getProperty("xmpp.domain", "127.0.0.1"));
                if ("manage".equals(membership.getAccess())) {
                    administrators.add(jid);
                } else {
                    members.add(jid);
                }
            }
        }
        return new Group(name, description, members, administrators);
    }

    @Override
    public int getGroupCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getGroupNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getGroupNames(int startIndex, int numResults) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getGroupNames(JID user) {
        List<String> groupNames = new ArrayList<String>();
        BridgeService bridgeService = BridgeServiceImpl.getInstance();
        String token = bridgeService.getToken(user.getNode());
        try {
            List<Membership> membershipList = bridgeService.findUserMemberships(token);
            for (Membership membership : membershipList) {
                org.daybreak.openfire.plugin.bridge.model.Group bridgeGroup = membership.getGroup();
                List<Membership> groupMemberships = bridgeService.findGroupMemberships(bridgeGroup.getId(), token);
                bridgeGroup.setMemberships(groupMemberships);
                groupNames.add(bridgeGroup.getId());
                bridgeService.setBridgeGroup(bridgeGroup.getId(), bridgeGroup);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return groupNames;
    }
}
