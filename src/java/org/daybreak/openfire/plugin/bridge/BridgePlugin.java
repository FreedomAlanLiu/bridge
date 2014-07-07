package org.daybreak.openfire.plugin.bridge;

import org.daybreak.openfire.plugin.bridge.provider.BridgeHistoryMessageStore;
import org.daybreak.openfire.plugin.bridge.provider.BridgeMXParser;
import org.daybreak.openfire.plugin.bridge.provider.BridgeOfflineMessageStore;
import org.daybreak.openfire.plugin.bridge.resource.MessageResource;
import org.daybreak.openfire.plugin.bridge.utils.MongoUtil;
import org.daybreak.openfire.plugin.bridge.utils.RedisUtil;
import org.dom4j.io.XMPPPacketReader;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.jivesoftware.openfire.OfflineMessageStore;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.clearspace.ClearspaceManager;
import org.jivesoftware.openfire.container.Module;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.net.MXParser;
import org.jivesoftware.openfire.vcard.VCardManager;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;

/**
 * Created by Alan on 2014/4/2.
 */
public class BridgePlugin implements Plugin {

    private static final Logger Log = LoggerFactory.getLogger(BridgePlugin.class);

    //public static String BRIDGE_HOST = JiveGlobals.getProperty("plugin.bridge.host", "124.205.151.249");
    public static String BRIDGE_HOST = JiveGlobals.getProperty("plugin.bridge.host", "124.205.151.250");

    public static String RESOURCE_BASE_URI = JiveGlobals.getProperty("plugin.bridge.resource.base.uri", "http://localhost:8080/bridge/");

    private PacketInterceptor bridgePacketInterceptor;

    private HttpServer httpServer;

    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        System.out.println("Starting Bridge Plugin");
        // hack providers
        hackAuthProvider();
        hackUserProvider();
        hackRosterItemProvider();
        hackGroupProvider();
        hackVCardProvider();

        // 初始化redis & mongo相关
        try {
            RedisUtil.getInstance().initialPool();
            MongoUtil.getInstance().init();
            BridgeHistoryMessageStore.getInstance().start();
        } catch (Exception e) {
            Log.error("", e);
        }

        // hack offline message store
        hackOfflineMessageStore();

        // 添加消息拦截器
        bridgePacketInterceptor = new BridgePacketInterceptor();
        InterceptorManager.getInstance().addInterceptor(bridgePacketInterceptor);

        // 启动http服务器
        httpServer = startHttpServer();
        Log.info("Grizzly HTTP server启动成功！");
    }

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startHttpServer() {
        // create a resource config that scans for JAX-RS resources and providers
        // in com.example package
        final ResourceConfig rc = new ResourceConfig();
        rc.register(MessageResource.class);

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(RESOURCE_BASE_URI), rc);
    }

    public void destroyPlugin() {
        // 移除消息拦截器
        InterceptorManager.getInstance().removeInterceptor(bridgePacketInterceptor);

        // 关闭http服务
        httpServer.shutdownNow();
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

    private void hackVCardProvider() {
        String className = JiveGlobals.getProperty("provider.vcard.className");
        if (!className.startsWith("org.daybreak")) {
            return;
        }
        VCardManager.getInstance().initialize(XMPPServer.getInstance());
    }

    private void hackOfflineMessageStore() {
        try {
            BridgeOfflineMessageStore offlineMessageStore = new BridgeOfflineMessageStore();
            Field field = XMPPServer.class.getDeclaredField("modules");
            field.setAccessible(true);
            Map<Class, Module> modules = (Map<Class, Module>) field.get(XMPPServer.getInstance());
            modules.put(OfflineMessageStore.class, offlineMessageStore);
            for (Module module : modules.values()) {
                module.stop();
                module.initialize(XMPPServer.getInstance());
                module.start();
            }
            XMPPServer.getInstance().getOfflineMessageStrategy().initialize(XMPPServer.getInstance());
            System.out.println("hackOfflineMessageStore success!");
            Log.info("hackOfflineMessageStore success!");
        } catch (Exception ex) {
            Log.error(ex.getMessage());
            ex.printStackTrace();
        }
    }

    /*private void hackMXParser() {
        try {
            ThreadLocal<XMPPPacketReader> localParser = new ThreadLocal<XMPPPacketReader>() {
                @Override
                protected XMPPPacketReader initialValue() {
                    XmlPullParserFactory factory = null;
                    try {
                        factory = XmlPullParserFactory.newInstance(BridgeMXParser.class.getName(), null);
                        factory.setNamespaceAware(true);
                    }
                    catch (XmlPullParserException e) {
                        Log.error("Error creating a parser factory", e);
                    }
                    XMPPPacketReader parser = new XMPPPacketReader();
                    factory.setNamespaceAware(true);
                    parser.setXPPFactory(factory);
                    return parser;
                }
            };

            Field localParserField = ClearspaceManager.class.getDeclaredField("localParser");
            localParserField.setAccessible(true);
            localParserField.set(ClearspaceManager.getInstance(), localParser);
            Log.info("hackMXParser success!");
        } catch (NoSuchFieldException e) {
            Log.error(e.getMessage());
        } catch (IllegalAccessException e) {
            Log.error(e.getMessage());
        }
    }*/


}
