package org.daybreak.openfire.plugin.bridge.utils;

import com.mongodb.*;
import org.daybreak.openfire.plugin.bridge.model.User;
import org.jivesoftware.util.JiveGlobals;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Iterator;

/**
 * Created by alan on 14-5-12.
 */
public class Storage {

    private static Storage storage = new Storage();

    public static final String BRIDGE_DB_NAME = "bridge";

    private Datastore datastore;

    private static final Logger logger = LoggerFactory.getLogger(Storage.class);

    private Storage() {
    }

    public static Storage getInstance() {
        return storage;
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

    public void setUser(User user) {
        datastore.save(user);
        try {
            RedisClient.getInstance().setUser(user);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public User getUser(String userId) {
        User user = null;
        try {
            user = RedisClient.getInstance().getUser(userId);
        } catch (Exception e) {
            logger.error("", e);
        }
        if (user != null) {
            return user;
        }
        user =  datastore.get(User.class, userId);
        if (user != null) {
            try {
                RedisClient.getInstance().setUser(user);
            } catch (Exception e) {
                logger.error("", e);
            }
        } else {
            logger.warn("Can't find the user from mongodb(userId: " + userId + ")");
        }
        return user;
    }

    public String getOneToken() {
        String oneToken = null;
        try {
            oneToken = RedisClient.getInstance().getOneToken();
        } catch (Exception e) {
            logger.error("", e);
        }
        if (oneToken != null) {
            return oneToken;
        }
        Query<User> userQuery = datastore.find(User.class);
        Iterator<User> userIterator = userQuery.iterator();
        while (userIterator.hasNext()) {
            User u = userIterator.next();
            if (u != null && u.getAccessToken() != null) {
                return u.getAccessToken();
            }
        }
        logger.warn("Can't find any token!");
        return null;
    }
}
