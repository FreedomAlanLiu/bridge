package org.daybreak.openfire.plugin.bridge.provider;

import org.apache.commons.lang3.StringUtils;
import org.daybreak.openfire.plugin.bridge.service.BridgeService;
import org.daybreak.openfire.plugin.bridge.BridgeServiceFactory;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.openfire.user.UserProvider;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

/**
 * Created by Alan on 2014/4/2.
 */
public class BridgeUserProvider implements UserProvider {

    @Override
    public User loadUser(String userId) throws UserNotFoundException {
        BridgeService bridgeService = (BridgeService) BridgeServiceFactory.getBean("bridgeService");;
        String token = bridgeService.getToken(userId);
        try {
            if (StringUtils.isNotEmpty(token)) {
                org.daybreak.openfire.plugin.bridge.model.User bridgeUser = bridgeService.findUser(token);
                if (bridgeUser == null) {
                    throw new UserNotFoundException();
                }
                // 擦除对应的Roster缓存
                Cache rosterCache = CacheFactory.createCache("Roster");
                rosterCache.remove(bridgeUser.getId());
                return new User(bridgeUser.getId(),
                        bridgeUser.getUsername() + (StringUtils.isBlank(bridgeUser.getName()) ? "" : "(" + bridgeUser.getName() + ")"),
                        bridgeUser.getEmail(), new Date(), new Date());

            } else {
                token = bridgeService.getOneToken();
                if (StringUtils.isEmpty(token)) {
                    throw new UserNotFoundException();
                }
                org.daybreak.openfire.plugin.bridge.model.User bridgeUser = bridgeService.findUser(userId, token);
                if (bridgeUser == null) {
                    throw new UserNotFoundException();
                }
                return new User(bridgeUser.getId(),
                        bridgeUser.getUsername() + (StringUtils.isBlank(bridgeUser.getName()) ? "" : "(" + bridgeUser.getName() + ")"),
                        bridgeUser.getEmail(), new Date(), new Date());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public User createUser(String username, String password, String name, String email) throws UserAlreadyExistsException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteUser(String username) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getUserCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<User> getUsers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getUsernames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<User> getUsers(int startIndex, int numResults) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setName(String username, String name) throws UserNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEmail(String username, String email) throws UserNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCreationDate(String username, Date creationDate) throws UserNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setModificationDate(String username, Date modificationDate) throws UserNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getSearchFields() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<User> findUsers(Set<String> fields, String query) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<User> findUsers(Set<String> fields, String query, int startIndex, int numResults) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isNameRequired() {
        return false;
    }

    @Override
    public boolean isEmailRequired() {
        return false;
    }
}
