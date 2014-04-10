package org.daybreak.openfire.plugin.bridge.service;

import org.daybreak.openfire.plugin.bridge.model.Group;
import org.daybreak.openfire.plugin.bridge.model.Membership;
import org.daybreak.openfire.plugin.bridge.model.User;

import java.util.List;

/**
 * Created by Alan on 2014/3/30.
 */
public interface BridgeService {

    public String auth(String userId, String password) throws Exception;

    public User findUser(String id, String token) throws Exception;

    public User findUser(String token) throws Exception;

    public List<User> findConnections(String token) throws Exception;

    public List<Membership> findUserMemberships(String token) throws Exception;

    public List<Membership> findGroupMemberships(String groupId, String token) throws Exception;

    public String getToken(String userId);

    public void removeToken(String userId);

    public String getOneToken();
}
