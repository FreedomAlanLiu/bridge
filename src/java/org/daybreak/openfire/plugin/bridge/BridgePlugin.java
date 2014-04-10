package org.daybreak.openfire.plugin.bridge;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.commands.clearspace.SystemAdminAdded;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Created by Alan on 2014/4/2.
 */
public class BridgePlugin implements Plugin {

    private static final Logger Log = LoggerFactory.getLogger(BridgePlugin.class);

    // public static String BRIDGE_HOST = JiveGlobals.getProperty("plugin.bridge.host", "124.205.151.249");
    public static String BRIDGE_HOST = JiveGlobals.getProperty("plugin.bridge.host", "124.205.151.250");

    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        System.out.println("Starting Bridge Plugin");
        hackAuthProvider();
        hackUserProvider();
        hackRosterItemProvider();
        hackGroupProvider();
    }

    public void destroyPlugin() {
    }

    private void hackAuthProvider() {
        String className = JiveGlobals.getProperty("provider.auth.className");
        if (!className.startsWith("org.daybreak")) {
            return;
        }
        try {
            Method method = org.jivesoftware.openfire.auth.AuthFactory.class.getDeclaredMethod("initProvider");
            method.setAccessible(true);
            method.invoke(new org.jivesoftware.openfire.auth.AuthFactory());
            Log.info("hackAuthProvider success!");
        } catch (Exception ex) {
            Log.error(ex.getMessage());
        }
    }

    private void hackUserProvider() {
        String className = JiveGlobals.getProperty("provider.user.className");
        if (!className.startsWith("org.daybreak")) {
            return;
        }
        try {
            Method method = org.jivesoftware.openfire.user.UserManager.class.getDeclaredMethod("initProvider");
            method.setAccessible(true);
            method.invoke(org.jivesoftware.openfire.user.UserManager.getInstance());
            Log.info("hackUserProvider success!");
        } catch (Exception ex) {
            Log.error(ex.getMessage());
        }
    }

    private void hackRosterItemProvider() {
        String className = JiveGlobals.getProperty("provider.roster.className");
        if (!className.startsWith("org.daybreak")) {
            return;
        }
        try {
            Method method = org.jivesoftware.openfire.roster.RosterManager.class.getDeclaredMethod("initProvider");
            method.setAccessible(true);
            method.invoke(XMPPServer.getInstance().getRosterManager());
            Log.info("hackRosterItemProvider success!");
        } catch (Exception ex) {
            Log.error(ex.getMessage());
        }
    }

    private void hackGroupProvider() {
        String className = JiveGlobals.getProperty("provider.group.className");
        if (!className.startsWith("org.daybreak")) {
            return;
        }
        try {
            Method method = org.jivesoftware.openfire.group.GroupManager.class.getDeclaredMethod("initProvider");
            method.setAccessible(true);
            method.invoke(org.jivesoftware.openfire.group.GroupManager.getInstance());
            Log.info("hackGroupProvider success!");
        } catch (Exception ex) {
            Log.error(ex.getMessage());
        }
    }
}
