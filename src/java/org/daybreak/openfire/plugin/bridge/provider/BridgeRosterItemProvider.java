package org.daybreak.openfire.plugin.bridge.provider;

import com.google.common.hash.Hashing;
import org.apache.commons.lang3.StringUtils;
import org.daybreak.openfire.plugin.bridge.service.BridgeService;
import org.daybreak.openfire.plugin.bridge.BridgeServiceFactory;
import org.jivesoftware.openfire.group.Group;
import org.jivesoftware.openfire.group.GroupManager;
import org.jivesoftware.openfire.roster.RosterItem;
import org.jivesoftware.openfire.roster.RosterItemProvider;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.xmpp.packet.JID;

import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by Alan on 2014/4/3.
 */
public class BridgeRosterItemProvider implements RosterItemProvider {

    private static final String grap = "<>";

    private static final String at = "@";

    @Override
    public RosterItem createItem(String username, RosterItem item) throws UserAlreadyExistsException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateItem(String username, RosterItem item) throws UserNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteItem(String username, long rosterItemID) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<String> getUsernames(String jid) {
        List<String> usernames = new ArrayList<String>();
        int atIndex = jid.lastIndexOf('@');
        if (atIndex > -1) {
            String username = JID.unescapeNode(jid.substring(0, atIndex));
            for (org.daybreak.openfire.plugin.bridge.model.User connection : getConnections(username)) {
                usernames.add(connection.getUsername());
            }
        }
        return usernames.iterator();
    }

    @Override
    public int getItemCount(String username) {
        return getConnections(JID.unescapeNode(username)).size();
    }

    @Override
    public Iterator<RosterItem> getItems(String userId) {
        LinkedList<RosterItem> itemList = new LinkedList<RosterItem>();
        Map<Long, RosterItem> itemsByID = new HashMap<Long, RosterItem>();

        try {
            BridgeService bridgeService = (BridgeService) BridgeServiceFactory.getBean("bridgeService");;
            String token = bridgeService.getToken(userId);
            if (StringUtils.isNotEmpty(token)) {
                org.daybreak.openfire.plugin.bridge.model.User user = bridgeService.findUser(token);

                for (org.daybreak.openfire.plugin.bridge.model.User connection : getConnections(userId)) {
                    // Create a new RosterItem (ie. user contact) from the stored information
                    long itemId = Hashing.md5().hashString(userId + grap + connection.getId(), Charset.defaultCharset()).asLong();
                    RosterItem item = new RosterItem(
                            itemId,
                            new JID(connection.getId()
                                    + at + JiveGlobals.getProperty("xmpp.domain", "127.0.0.1")),
                            RosterItem.SubType.getTypeFromInt(3),
                            RosterItem.AskType.getTypeFromInt(-1),
                            RosterItem.RecvType.getTypeFromInt(-1),
                            connection.getUsername() + (StringUtils.isBlank(connection.getName()) ? "" : "(" + connection.getName() + ")"),
                            null
                    );
                    // Add the loaded RosterItem (ie. user contact) to the result
                    itemList.add(item);
                    itemsByID.put(itemId, item);
                }

                JID currentJID = new JID(user.getId()
                        + at + JiveGlobals.getProperty("xmpp.domain", "127.0.0.1"));
                // 首先擦除对应的group meta缓存
                Cache groupMetaCache = CacheFactory.createCache("Group Metadata Cache");
                Collection<String> cachedGroupNames = (Collection<String>)groupMetaCache.get(currentJID.toBareJID());
                groupMetaCache.remove(currentJID.toBareJID());
                // 擦除掉用户所有的group缓存
                if (cachedGroupNames != null) {
                    Cache groupCache = CacheFactory.createCache("Group");
                    for (String cachedGroupName : cachedGroupNames) {
                        groupCache.remove(cachedGroupName);
                    }
                }
                Collection<Group> groups = GroupManager.getInstance().getGroups(currentJID);
                for (Group group : groups) {
                    Collection<JID> jidCollection = new ArrayList<JID>();
                    jidCollection.addAll(group.getAdmins());
                    jidCollection.addAll(group.getMembers());
                    for (JID jid : jidCollection) {
                        if (userId.equals(jid.getNode())) {
                            continue;
                        }
                        long itemId = Hashing.md5().hashString(userId + grap + jid.getNode(), Charset.defaultCharset()).asLong();
                        RosterItem item = itemsByID.get(itemId);
                        if (item != null) {
                            item.getGroups().add(group.getName());
                        } else {
                            String nickname = jid.getNode();
                            org.daybreak.openfire.plugin.bridge.model.User u = bridgeService.findUser(jid.getNode(), token);
                            if (u != null) {
                                nickname = u.getUsername() + (StringUtils.isBlank(u.getName()) ? "" : "(" + u.getName() + ")");
                            }
                            List<String> groupNameList = new ArrayList<String>();
                            groupNameList.add(group.getName());
                            item = new RosterItem(
                                    itemId,
                                    jid,
                                    RosterItem.SubType.getTypeFromInt(3),
                                    RosterItem.AskType.getTypeFromInt(-1),
                                    RosterItem.RecvType.getTypeFromInt(-1),
                                    nickname,
                                    groupNameList
                            );
                            itemList.add(item);
                            itemsByID.put(itemId, item);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return itemList.iterator();
    }

    private List<org.daybreak.openfire.plugin.bridge.model.User> getConnections(String userId) {
        try {
            BridgeService bridgeService = (BridgeService) BridgeServiceFactory.getBean("bridgeService");;
            String token = bridgeService.getToken(userId);
            if (StringUtils.isNotEmpty(token)) {
                List<org.daybreak.openfire.plugin.bridge.model.User> connections = bridgeService.findConnections(token);
                return connections;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<org.daybreak.openfire.plugin.bridge.model.User>();
    }
}
