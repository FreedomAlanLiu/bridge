package org.daybreak.openfire.plugin.bridge.provider;

import org.apache.commons.lang3.StringUtils;
import org.daybreak.openfire.plugin.bridge.model.Membership;
import org.daybreak.openfire.plugin.bridge.service.BridgeService;
import org.daybreak.openfire.plugin.bridge.BridgeServiceFactory;
import org.jivesoftware.openfire.group.AbstractGroupProvider;
import org.jivesoftware.openfire.group.Group;
import org.jivesoftware.openfire.group.GroupManager;
import org.jivesoftware.openfire.group.GroupNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.xmpp.packet.JID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Alan on 2014/4/3.
 */
public class BridgeGroupProvider extends AbstractGroupProvider {

    @Override
    public Group getGroup(String name) throws GroupNotFoundException {
        Collection<JID> members = new ArrayList<JID>();
        Collection<JID> administrators = new ArrayList<JID>();
        BridgeService bridgeService = (BridgeService) BridgeServiceFactory.getBean("bridgeService");
        try {
            String token = bridgeService.getOneToken();
            if (StringUtils.isNotEmpty(token)) {
                List<Membership> memberships = bridgeService.findGroupMemberships(name, token);
                for (Membership membership : memberships) {
                    JID jid = new JID(membership.getUser().getId()
                            + "@" + JiveGlobals.getProperty("xmpp.domain", "127.0.0.1"));
                    members.add(jid);
                }
                return new Group(name, "", members, administrators);
            }
        } catch (Exception e) {
            throw new GroupNotFoundException("can't find group: " + name, e);
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
        BridgeService bridgeService = (BridgeService) BridgeServiceFactory.getBean("bridgeService");
        try {
            String token = bridgeService.getToken(user.getNode());
            if (StringUtils.isNotEmpty(token)) {
                List<Membership> membershipList = bridgeService.findUserMemberships(token);
                for (Membership membership : membershipList) {
                    org.daybreak.openfire.plugin.bridge.model.Group bridgeGroup = membership.getGroup();
                    groupNames.add(bridgeGroup.getId());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return groupNames;
    }

    @Override
    public void addMember(String groupName, JID user, boolean administrator) {
        Cache groupCache = CacheFactory.createCache("Group");
        Group group = (Group) groupCache.get(groupName);
        if (group != null) {
            Collection<JID> members = group.getMembers();
            boolean isContainsUser = false;
            for (JID member : members) {
                if (member.getNode().equals(user.getNode())
                        && member.getDomain().equals(user.getDomain())) {
                    isContainsUser = true;
                    break;
                }
            }
            if (!isContainsUser) {
                members.add(user);
            }
        }
    }

    @Override
    public void deleteMember(String groupName, JID user) {
        Cache groupCache = CacheFactory.createCache("Group");
        Group group = (Group) groupCache.get(groupName);
        if (group != null) {
            Collection<JID> members = group.getMembers();
            Iterator<JID> memberIterator = members.iterator();
            while (memberIterator.hasNext()) {
                JID member = memberIterator.next();
                if (member.getNode().equals(user.getNode())
                        && member.getDomain().equals(user.getDomain())) {
                    memberIterator.remove();
                    break;
                }
            }
        }
    }

    @Override
    public void deleteGroup(String name) {
        Cache groupCache = CacheFactory.createCache("Group");
        Group group = (Group) groupCache.get(name);
        if (group != null) {
            groupCache.remove(name);
        }
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }
}
