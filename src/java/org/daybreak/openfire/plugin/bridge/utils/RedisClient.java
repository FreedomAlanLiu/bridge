package org.daybreak.openfire.plugin.bridge.utils;

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
public class RedisClient {

    private static RedisClient redisClient = new RedisClient();

    private static final Logger logger = LoggerFactory.getLogger(RedisClient.class);

    public static final String USER_PREFIX = "user_";

    public static final String ONE_TOKEN = "one_token_951862527";

    private JedisPool jedisPool;//非切片连接池
    private ShardedJedisPool shardedJedisPool;//切片连接池

    private RedisClient() {
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

    public static RedisClient getInstance() {
        return redisClient;
    }

    public String setUser(User user) throws IOException, ClassNotFoundException {
        Jedis jedis = null;
        boolean borrowOrOprSuccess = true;
        try {
            jedis = this.jedisPool.getResource();
            return jedis.setex((USER_PREFIX + user.getId()).getBytes(), JiveGlobals.getIntProperty("plugin.bridge.jedis.userExpire", 604800)
                    + (int) (3600 * Math.random()), RedisSerializeUtil.kryoSerialize(user)); // 默认生存时间一个星期 + [0 - 1h)
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

    public User getUser(String userId) throws IOException, ClassNotFoundException {
        Jedis jedis = null;
        boolean borrowOrOprSuccess = true;
        try {
            jedis = this.jedisPool.getResource();
            byte[] bytes = jedis.get((USER_PREFIX + userId).getBytes());
            if (bytes == null) {
                return null;
            }
            Object o = RedisSerializeUtil.kryoDeserialize(bytes);
            if (o instanceof User) {
                User user = (User) o;
                jedis.setex((USER_PREFIX + user.getId()).getBytes(), JiveGlobals.getIntProperty("plugin.bridge.jedis.userExpire", 604800)
                        + (int) (3600 * Math.random()), RedisSerializeUtil.kryoSerialize(user)); // 默认生存时间一个星期 + [0 - 1h)
                return user;
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

    public String getOneToken() throws IOException, ClassNotFoundException {
        Jedis jedis = null;
        boolean borrowOrOprSuccess = true;
        try {
            jedis = this.jedisPool.getResource();
            Set<byte[]> keys = jedis.keys((USER_PREFIX + "*").getBytes());
            for (byte[] key : keys) {
                byte[] value = jedis.get(key);
                Object o = RedisSerializeUtil.kryoDeserialize(value);
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
