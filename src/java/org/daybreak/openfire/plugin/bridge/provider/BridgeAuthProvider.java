package org.daybreak.openfire.plugin.bridge.provider;

import org.daybreak.openfire.plugin.bridge.exception.BridgeException;
import org.daybreak.openfire.plugin.bridge.service.BridgeService;
import org.daybreak.openfire.plugin.bridge.service.impl.BridgeServiceImpl;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.auth.AuthProvider;
import org.jivesoftware.openfire.auth.ConnectionException;
import org.jivesoftware.openfire.auth.InternalUnauthenticatedException;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import java.io.IOException;

/**
 * Created by Alan on 2014/4/2.
 */
public class BridgeAuthProvider implements AuthProvider {

    private static final Logger Log = LoggerFactory.getLogger(BridgeAuthProvider.class);

    public BridgeAuthProvider() {

    }

    @Override
    public boolean isPlainSupported() {
        return true;
    }

    @Override
    public boolean isDigestSupported() {
        return false;
    }

    @Override
    public void authenticate(String userId, String password) throws UnauthorizedException, ConnectionException, InternalUnauthenticatedException {
        Log.info("authenticate userId=" + userId + "    " + "password=" + password);
        BridgeService bridgeService = BridgeServiceImpl.getInstance();
        try {
            bridgeService.auth(userId, password);
        } catch (Exception e) {
            Log.error("Authentication failed by bridge api!", e);
            if (e instanceof IOException) {
                throw new ConnectionException();
            } else if (e instanceof BridgeException) {
                BridgeException be = (BridgeException) e;
                if (BridgeException.INVALID_RESOURCE_OWNER.equals(be.getError())) {
                    throw new UnauthorizedException();
                }
            }
            throw new InternalUnauthenticatedException();
        }
    }

    @Override
    public void authenticate(String username, String token, String digest) throws UnauthorizedException, ConnectionException, InternalUnauthenticatedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPassword(String username) throws UserNotFoundException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPassword(String username, String password) throws UserNotFoundException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsPasswordRetrieval() {
        return false;
    }
}
