package org.daybreak.openfire.plugin.bridge.utils;

import com.mongodb.*;
import org.daybreak.openfire.plugin.bridge.model.User;
import org.jivesoftware.util.JiveGlobals;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

/**
 * Created by alan on 14-5-12.
 */
public class MongoUtil {

    private static MongoUtil mongoUtil;

    public static final String BRIDGE_DB_NAME = "bridge";

    private Datastore datastore;

    private static final Logger logger = LoggerFactory.getLogger(MongoUtil.class);

    private MongoUtil() {
    }

    public synchronized static MongoUtil getInstance() {
        if (mongoUtil == null) {
            mongoUtil = new MongoUtil();
        }
        return mongoUtil;
    }

    public void init() throws UnknownHostException {
        MongoClient mongoClient = new MongoClient(
                JiveGlobals.getProperty("plugin.bridge.mongo.host", "localhost"),
                JiveGlobals.getIntProperty("plugin.bridge.mongo.port", 27017));
        Morphia morphia = new Morphia();
        morphia.map(User.class);
        Mapper mapper = morphia.getMapper();
        mapper.getOptions().objectFactory = new MorphiaObjectFactory();
        datastore = morphia.createDatastore(mongoClient, BRIDGE_DB_NAME);
    }

    public Datastore getDatastore() {
        return datastore;
    }
}
