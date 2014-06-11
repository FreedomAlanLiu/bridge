package org.daybreak.openfire.plugin.bridge.utils;

import org.daybreak.openfire.plugin.bridge.model.Offline;
import org.daybreak.openfire.plugin.bridge.model.User;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;
import java.util.*;

/**
 * Created by alan on 14-5-8.
 */
public class RedisUtil {

    private static RedisUtil redisUtil = new RedisUtil();

    private static final Logger logger = LoggerFactory.getLogger(RedisUtil.class);

    public static final String USER_PREFIX = "user_";

    public static final String OFFLINE_PREFIX = "offline_";

    public static final String ONE_TOKEN = "one_token_951862527";

    private JedisPool jedisPool;//非切片连接池
    private ShardedJedisPool shardedJedisPool;//切片连接池

    private RedisUtil() {
    }

    /**
     * 初始化非切片池
     */
    public void initialPool() {
        // 池基本配置
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(JiveGlobals.getIntProperty("plugin.bridge.jedis.pool.maxTotal", 200));
        config.setMaxIdle(JiveGlobals.getIntProperty("plugin.bridge.jedis.pool.maxIdle", 20));
        config.setMaxWaitMillis(JiveGlobals.getIntProperty("plugin.bridge.jedis.pool.maxWaitMillis", 5000));
        config.setTestOnBorrow(JiveGlobals.getBooleanProperty("plugin.bridge.jedis.pool.testOnBorrow", false));
        jedisPool = new JedisPool(config, JiveGlobals.getProperty("plugin.bridge.jedis.host", "127.0.0.1"),
                JiveGlobals.getIntProperty("plugin.bridge.jedis.port", 6379));
    }

    /**
     * 初始化切片池
     */
    /*private void initialShardedPool() {
        // 池基本配置
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(JiveGlobals.getIntProperty("plugin.bridge.jedis.pool.maxTotal", 200));
        config.setMaxIdle(JiveGlobals.getIntProperty("plugin.bridge.jedis.pool.maxIdle", 20));
        config.setMaxWaitMillis(JiveGlobals.getIntProperty("plugin.bridge.jedis.pool.maxWaitMillis", 5000));
        config.setTestOnBorrow(JiveGlobals.getBooleanProperty("plugin.bridge.jedis.pool.testOnBorrow", false));
        // slave链接
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        shards.add(new JedisShardInfo(JiveGlobals.getProperty("plugin.bridge.jedis.host", "127.0.0.1"),
                JiveGlobals.getIntProperty("plugin.bridge.jedis.port", 6379), "master"));

        // 构造池
        shardedJedisPool = new ShardedJedisPool(config, shards);
    }*/

    public static RedisUtil getInstance() {
        return redisUtil;
    }

    public String setObject(byte[] key, Object obj, int ex) throws Exception {
        Jedis jedis = null;
        boolean borrowOrOprSuccess = true;
        try {
            jedis = this.jedisPool.getResource();
            if (ex == -1) {
                return jedis.set(key, RedisSerializeUtil.kryoSerialize(obj));
            }
            return jedis.setex(key, ex, RedisSerializeUtil.kryoSerialize(obj));
        } catch (JedisException e) {
            borrowOrOprSuccess = false;
            if (jedis != null) {
                //jedis异常，销毁
                jedisPool.returnBrokenResource(jedis);
            }
            throw e;
        } finally {
            if (borrowOrOprSuccess && jedis != null) {
                //需要还回给pool
                jedisPool.returnResource(jedis);
            }
        }
    }

    public Object getObject(byte[] key, Class clazz) throws Exception {
        Jedis jedis = null;
        boolean borrowOrOprSuccess = true;
        try {
            jedis = this.jedisPool.getResource();
            byte[] bytes = jedis.get(key);
            if (bytes == null) {
                return null;
            }
            return RedisSerializeUtil.kryoDeserialize(bytes, clazz);
        } catch (JedisException e) {
            borrowOrOprSuccess = false;
            if (jedis != null) {
                //jedis异常，销毁
                jedisPool.returnBrokenResource(jedis);
            }
            throw e;
        } finally {
            if (borrowOrOprSuccess && jedis != null) {
                //需要还回给pool
                jedisPool.returnResource(jedis);
            }
        }
    }

    public long delObjects(byte[] pkey) {
        Jedis jedis = null;
        boolean borrowOrOprSuccess = true;
        try {
            jedis = this.jedisPool.getResource();
            Set<byte[]> keys = jedis.keys(pkey);
            long count = 0;
            for (byte[] key : keys) {
                count += jedis.del(key);
            }
            return count;
        } catch (JedisException e) {
            borrowOrOprSuccess = false;
            if (jedis != null) {
                //jedis异常，销毁
                jedisPool.returnBrokenResource(jedis);
            }
            throw e;
        } finally {
            if (borrowOrOprSuccess && jedis != null) {
                //需要还回给pool
                jedisPool.returnResource(jedis);
            }
        }
    }

    public List getObjects(String prefix, Class clazz) throws Exception {
        List objects = new ArrayList();
        Jedis jedis = null;
        boolean borrowOrOprSuccess = true;
        try {
            jedis = this.jedisPool.getResource();
            Set<byte[]> keys = jedis.keys((prefix + "*").getBytes());
            for (byte[] key : keys) {
                byte[] value = jedis.get(key);
                Object o = RedisSerializeUtil.kryoDeserialize(value, clazz);
                objects.add(o);
            }
        } catch (JedisException e) {
            borrowOrOprSuccess = false;
            if (jedis != null) {
                //jedis异常，销毁
                jedisPool.returnBrokenResource(jedis);
            }
            throw e;
        } finally {
            if (borrowOrOprSuccess && jedis != null) {
                //需要还回给pool
                jedisPool.returnResource(jedis);
            }
        }
        return objects;
    }

    public int getObjectsSize(String prefix) {
        Jedis jedis = null;
        boolean borrowOrOprSuccess = true;
        try {
            jedis = this.jedisPool.getResource();
            Set<byte[]> keys = jedis.keys((prefix + "*").getBytes());
            return keys.size();
        } catch (JedisException e) {
            borrowOrOprSuccess = false;
            if (jedis != null) {
                //jedis异常，销毁
                jedisPool.returnBrokenResource(jedis);
            }
            throw e;
        } finally {
            if (borrowOrOprSuccess && jedis != null) {
                //需要还回给pool
                jedisPool.returnResource(jedis);
            }
        }
    }

    public String setUser(User user) throws Exception {
        return setObject((USER_PREFIX + user.getId()).getBytes(), user, -1);
    }

    public User getUser(String userId) throws Exception {
        Object o = getObject((USER_PREFIX + userId).getBytes(), User.class);
        if (o instanceof User) {
            return (User) o;
        }
        return null;
    }

    public String setOffline(Offline offline) throws Exception {
        int size = getObjectsSize(OFFLINE_PREFIX + offline.getUsername() + "_" + offline.getMessageID() + "_*");
        if (size == 0) {
            return setObject((OFFLINE_PREFIX + offline.getUsername() + "_" + offline.getMessageID() + "_" + offline.getCreationDate().getTime()).getBytes(),
                    offline,
                    JiveGlobals.getIntProperty("plugin.bridge.jedis.offlineExpire", 604800) + (int) (3600 * Math.random()));
        }
        return null;
    }

    public Offline getOffline(String username, Date createDate) throws Exception {
        Object o = getObject((OFFLINE_PREFIX + username + "_*_" + createDate.getTime()).getBytes(), Offline.class);
        if (o instanceof Offline) {
            return (Offline) o;
        }
        return null;
    }

    public List getOfflineList(String username) throws Exception {
        return getObjects(OFFLINE_PREFIX + username + "_*", Offline.class);
    }

    public Long delOfflineList(String username) {
        return delObjects((OFFLINE_PREFIX + username + "_*").getBytes());
    }

    public Long delOffline(String username, Date createDate) {
        return delObjects((OFFLINE_PREFIX + username + "_*_" + createDate.getTime()).getBytes());
    }

    public int getOfflineListSize(String username) {
        return getObjectsSize(OFFLINE_PREFIX + username + "_*");
    }

    public int getAllOfflineListSize() {
        return getObjectsSize(OFFLINE_PREFIX + "*");
    }

    public String setOneToken(String oneToken) throws Exception {
        int size = getObjectsSize(ONE_TOKEN);
        if (size == 0) {
            return setObject(ONE_TOKEN.getBytes(), oneToken, JiveGlobals.getIntProperty("plugin.bridge.jedis.oneTokenExpire", 3600));
        }
        return null;
    }

    public String getOneToken() throws Exception {
        String oneToken = (String) getObject(ONE_TOKEN.getBytes(), String.class);
        if (oneToken == null) {
            oneToken = getOneTokenFromUsers();
        }
        return oneToken;
    }

    public String getOneTokenFromUsers() throws IOException, ClassNotFoundException {
        Jedis jedis = null;
        boolean borrowOrOprSuccess = true;
        try {
            jedis = this.jedisPool.getResource();
            Set<byte[]> keys = jedis.keys((USER_PREFIX + "*").getBytes());
            for (byte[] key : keys) {
                byte[] value = jedis.get(key);
                Object o = RedisSerializeUtil.kryoDeserialize(value, User.class);
                if (o instanceof User) {
                    User u = (User) o;
                    String token = u.getAccessToken();
                    if (token != null) {
                        return token;
                    }
                }
            }
        } catch (JedisException e) {
            borrowOrOprSuccess = false;
            if (jedis != null) {
                //jedis异常，销毁
                jedisPool.returnBrokenResource(jedis);
            }
            throw e;
        } finally {
            if (borrowOrOprSuccess && jedis != null) {
                //需要还回给pool
                jedisPool.returnResource(jedis);
            }
        }
        return null;
    }
}
