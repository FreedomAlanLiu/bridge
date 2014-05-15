package org.daybreak.openfire.plugin.bridge.service.impl;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;
import org.daybreak.openfire.plugin.bridge.BridgePlugin;
import org.daybreak.openfire.plugin.bridge.model.Device;
import org.daybreak.openfire.plugin.bridge.service.BridgeService;
import org.daybreak.openfire.plugin.bridge.exception.BridgeException;
import org.daybreak.openfire.plugin.bridge.model.AccessToken;
import org.daybreak.openfire.plugin.bridge.model.Membership;
import org.daybreak.openfire.plugin.bridge.model.User;
import org.daybreak.openfire.plugin.bridge.utils.HttpConnectionManager;
import org.daybreak.openfire.plugin.bridge.utils.RedisClient;

import java.io.IOException;
import java.util.*;

/**
 * Created by Alan on 2014/3/31.
 */
public class BridgeServiceImpl implements BridgeService {

    private ObjectMapper mapper = new ObjectMapper();

    // 此cache目前作为永久性的cache而存在，考虑使用第三方缓存数据库（如redis）来存储
    /*private Map<String, User> idUserCache;

    public BridgeServiceImpl() {
        idUserCache = new HashMap<String, User>();
    }*/

    @Override
    public List<User> findConnections(String token) throws Exception {
        HttpConnectionManager httpConnectionManager = new HttpConnectionManager();
        try {
            List<NameValuePair> tokenParameters = new ArrayList<NameValuePair>();
            tokenParameters.add(new BasicNameValuePair("access_token", token));
            List<User> connections = mapper.readValue(
                    httpConnectionManager.getHttpRequestAsString("http://"
                            + BridgePlugin.BRIDGE_HOST
                            + "/api/v1/user/connections", tokenParameters),
                    getCollectionType(List.class, User.class)
            );
            return connections;
        } finally {
            httpConnectionManager.close();
        }
    }

    @Override
    public List<Membership> findUserMemberships(String token) throws Exception {
        HttpConnectionManager httpConnectionManager = new HttpConnectionManager();
        try {
            List<NameValuePair> tokenParameters = new ArrayList<NameValuePair>();
            tokenParameters.add(new BasicNameValuePair("access_token", token));
            List<Membership> memberships = mapper.readValue(
                    httpConnectionManager.getHttpRequestAsString("http://"
                            + BridgePlugin.BRIDGE_HOST
                            + "/api/v1/user/memberships", tokenParameters),
                    getCollectionType(List.class, Membership.class)
            );
            return memberships;
        } finally {
            httpConnectionManager.close();
        }
    }

    @Override
    public List<Membership> findGroupMemberships(String groupId, String token) throws Exception {
        HttpConnectionManager httpConnectionManager = new HttpConnectionManager();
        try {
            List<NameValuePair> tokenParameters = new ArrayList<NameValuePair>();
            tokenParameters.add(new BasicNameValuePair("access_token", token));
            List<Membership> memberships = mapper.readValue(
                    httpConnectionManager.getHttpRequestAsString("http://"
                            + BridgePlugin.BRIDGE_HOST
                            + "/api/v1/groups/" + groupId + "/memberships", tokenParameters),
                    getCollectionType(List.class, Membership.class)
            );
            return memberships;
        } finally {
            httpConnectionManager.close();
        }
    }

    @Override
    public User findUser(String token) throws Exception {
        HttpConnectionManager httpConnectionManager = new HttpConnectionManager();
        try {
            List<NameValuePair> tokenParameters = new ArrayList<NameValuePair>();
            tokenParameters.add(new BasicNameValuePair("access_token", token));
            User user = mapper.readValue(
                    httpConnectionManager.getHttpRequestAsString("http://"
                            + BridgePlugin.BRIDGE_HOST
                            + "/api/v1/user", tokenParameters),
                    User.class
            );
            user.setAccessToken(token);
            RedisClient.getInstance().setUser(user);
            return user;
        } finally {
            httpConnectionManager.close();
        }
    }

    @Override
    public User findUser(String id, String token) throws Exception {
        HttpConnectionManager httpConnectionManager = new HttpConnectionManager();
        try {
            List<NameValuePair> tokenParameters = new ArrayList<NameValuePair>();
            tokenParameters.add(new BasicNameValuePair("access_token", token));
            User user = mapper.readValue(
                    httpConnectionManager.getHttpRequestAsString("http://"
                            + BridgePlugin.BRIDGE_HOST
                            + "/api/v1/users/" + id, tokenParameters),
                    User.class
            );
            RedisClient.getInstance().setUser(user);
            return user;
        } finally {
            httpConnectionManager.close();
        }
    }

    @Override
    public String auth(String userId, String password) throws Exception {
        HttpConnectionManager httpConnectionManager = new HttpConnectionManager();
        try {
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            parameters.add(new BasicNameValuePair("grant_type", "password"));
            parameters.add(new BasicNameValuePair("user_id", userId));
            parameters.add(new BasicNameValuePair("password", password));
            parameters.add(new BasicNameValuePair("client_id", "325183e9e2aa1c054f10885b47a1663db43b65ce972e2004886f91e386cfcd3f"));
            parameters.add(new BasicNameValuePair("client_secret", "94d835eec88320e95026596ce4c2db4f552368af2dd5f35e39c28dec903b814c"));
            String response = httpConnectionManager.postHttpRequestAsString("http://"
                    + BridgePlugin.BRIDGE_HOST
                    + "/tokens", parameters);
            AccessToken accessToken = mapper.readValue(response, AccessToken.class);
            if (accessToken != null && accessToken.getAccessToken() != null) {
                User user = loadUser(userId);
                if (user == null) {
                    user = new User();
                    user.setId(userId);
                }
                user.setAccessToken(accessToken.getAccessToken());
                RedisClient.getInstance().setUser(user);
                return accessToken.getAccessToken();
            } else {
                throw new BridgeException(accessToken.getError(), accessToken.getErrorDescription());
            }
        } finally {
            httpConnectionManager.close();
        }
    }

    public JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return mapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }

    @Override
    public String getToken(String userId) throws Exception {
        User user = loadUser(userId);
        if (user == null) {
            return null;
        }
        return user.getAccessToken();
    }

    @Override
    public User loadUser(String userId) throws Exception {
        return RedisClient.getInstance().getUser(userId);
    }

    @Override
    public User getUser(String userId) throws Exception {
        User user = loadUser(userId);
        if (user == null) {
            String token = getOneToken();
            if (token != null) {
                user = findUser(userId, token);
            }
        }
        return user;
    }

    @Override
    public String getOneToken() throws Exception {
        return RedisClient.getInstance().getOneToken();
    }

    @Override
    public List<Device> findDevice(String userId) throws Exception {
        HttpConnectionManager httpConnectionManager = new HttpConnectionManager();
        try {
            List<Device> devices = mapper.readValue(
                    httpConnectionManager.getHttpRequestAsString("http://"
                            + BridgePlugin.BRIDGE_HOST
                            + "/api/v1/user/" + userId + "/devices", null),
                    getCollectionType(List.class, Device.class)
            );
            return devices;
        } finally {
            httpConnectionManager.close();
        }
    }

    public String toJson(Object o) throws IOException {
        return mapper.writeValueAsString(o);
    }
}
