package org.daybreak.openfire.plugin.bridge.provider;

import org.apache.commons.lang3.StringUtils;
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
        String token = bridgeService.getOneToken();
        if (StringUtils.isNotEmpty(token)) {
            try {
                List<Membership> memberships = bridgeService.findGroupMemberships(name, token);
                for (Membership membership : memberships) {
                    JID jid = new JID(membership.getUser().getId()
                            + "@" + JiveGlobals.getProperty("xmpp.domain", "127.0.0.1"));
                    members.add(jid);
                }
                return new Group(name, "", members, administrators);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new GroupNotFoundException();
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
                groupNames.add(bridgeGroup.getId());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return groupNames;
    }
}
