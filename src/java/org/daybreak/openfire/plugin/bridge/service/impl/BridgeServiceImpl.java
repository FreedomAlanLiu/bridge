package org.daybreak.openfire.plugin.bridge.service.impl;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;
import org.daybreak.openfire.plugin.bridge.BridgePlugin;
import org.daybreak.openfire.plugin.bridge.service.BridgeService;
import org.daybreak.openfire.plugin.bridge.exception.BridgeException;
import org.daybreak.openfire.plugin.bridge.model.AccessToken;
import org.daybreak.openfire.plugin.bridge.model.Group;
import org.daybreak.openfire.plugin.bridge.model.Membership;
import org.daybreak.openfire.plugin.bridge.model.User;
import org.daybreak.openfire.plugin.bridge.utils.HttpConnectionManager;
import org.jivesoftware.openfire.commands.clearspace.SystemAdminAdded;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alan on 2014/3/31.
 */
public class BridgeServiceImpl implements BridgeService {

    private static BridgeServiceImpl bridgeServiceImpl = new BridgeServiceImpl();

    private ObjectMapper mapper = new ObjectMapper();

    private Cache<String, String> userIdTokenCache;

    private Cache<String, Group> nameGroupCache;

    private BridgeServiceImpl() {
        userIdTokenCache = CacheFactory.createCache("BridgeUsernameToken");
        nameGroupCache = CacheFactory.createCache("BridgeNameGroup");
    }

    public static BridgeServiceImpl getInstance() {
        return bridgeServiceImpl;
    }

    @Override
    public List<User> findConnections(String token) throws Exception {
        List<NameValuePair> tokenParameters = new ArrayList<NameValuePair>();
        tokenParameters.add(new BasicNameValuePair("access_token", token));
        List<User> connections = mapper.readValue(
                HttpConnectionManager.getHttpRequestAsString("http://"
                        + BridgePlugin.BRIDGE_HOST
                        + "/api/v1/user/connections", tokenParameters),
                getCollectionType(List.class, User.class)
        );
        return connections;
    }

    @Override
    public List<Membership> findUserMemberships(String token) throws Exception {
        List<NameValuePair> tokenParameters = new ArrayList<NameValuePair>();
        tokenParameters.add(new BasicNameValuePair("access_token", token));
        List<Membership> memberships = mapper.readValue(
                HttpConnectionManager.getHttpRequestAsString("http://"
                        + BridgePlugin.BRIDGE_HOST
                        + "/api/v1/user/memberships", tokenParameters),
                getCollectionType(List.class, Membership.class)
        );
        return memberships;
    }

    @Override
    public List<Membership> findGroupMemberships(String groupId, String token) throws Exception {
        List<NameValuePair> tokenParameters = new ArrayList<NameValuePair>();
        tokenParameters.add(new BasicNameValuePair("access_token", token));
        List<Membership> memberships = mapper.readValue(
                HttpConnectionManager.getHttpRequestAsString("http://"
                        + BridgePlugin.BRIDGE_HOST
                        + "/api/v1/groups/" + groupId + "/memberships", tokenParameters),
                getCollectionType(List.class, Membership.class)
        );
        return memberships;
    }

    @Override
    public User findUser(String token) throws Exception {
        List<NameValuePair> tokenParameters = new ArrayList<NameValuePair>();
        tokenParameters.add(new BasicNameValuePair("access_token", token));
        User user = mapper.readValue(
                HttpConnectionManager.getHttpRequestAsString("http://"
                        + BridgePlugin.BRIDGE_HOST
                        + "/api/v1/user", tokenParameters),
                User.class
        );
        return user;
    }

    @Override
    public User findUser(String id, String token) throws Exception {
        List<NameValuePair> tokenParameters = new ArrayList<NameValuePair>();
        tokenParameters.add(new BasicNameValuePair("access_token", token));
        User user = mapper.readValue(
                HttpConnectionManager.getHttpRequestAsString("http://"
                        + BridgePlugin.BRIDGE_HOST
                        + "/api/v1/users/" + id, tokenParameters),
                User.class
        );
        return user;
    }

    @Override
    public String auth(String userId, String password) throws Exception {
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("grant_type", "password"));
        parameters.add(new BasicNameValuePair("user_id", userId));
        parameters.add(new BasicNameValuePair("password", password));
        parameters.add(new BasicNameValuePair("client_id", "325183e9e2aa1c054f10885b47a1663db43b65ce972e2004886f91e386cfcd3f"));
        parameters.add(new BasicNameValuePair("client_secret", "94d835eec88320e95026596ce4c2db4f552368af2dd5f35e39c28dec903b814c"));
        String response = HttpConnectionManager.postHttpRequestAsString("http://"
                + BridgePlugin.BRIDGE_HOST
                + "/tokens", parameters);
        AccessToken accessToken = mapper.readValue(response, AccessToken.class);
        if (accessToken != null && accessToken.getAccessToken() != null) {
            userIdTokenCache.put(userId, accessToken.getAccessToken());
            return accessToken.getAccessToken();
        } else {
            throw new BridgeException(accessToken.getError(), accessToken.getErrorDescription());
        }
    }

    public JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return mapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }

    @Override
    public String getToken(String userId) {
        return userIdTokenCache.get(userId);
    }

    @Override
    public Group getBridgeGroup(String groupName) {
        return nameGroupCache.get(groupName);
    }

    @Override
    public void setBridgeGroup(String groupName, Group group) {
        nameGroupCache.put(groupName, group);
    }

    @Override
    public void removeToken(String userId) {
        nameGroupCache.remove(userId);
    }

    @Override
    public void removeGroup(String groupname) {
        nameGroupCache.remove(groupname);
    }
}
