package org.daybreak.openfire.plugin.bridge.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class RedisSerializeUtil {

    private static final Logger logger = LoggerFactory.getLogger(RedisSerializeUtil.class);

    public static final Kryo kryo = new Kryo();

    //序列化
    public static byte[] jserialize(Object object) throws IOException, ClassNotFoundException {
        ObjectOutputStream objectOutputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
            return byteArrayOutputStream.toByteArray();
        } finally {
            if (byteArrayOutputStream != null) {
                try {
                    byteArrayOutputStream.close();
                } catch (Exception e) {
                    logger.error("ByteArrayOutputStream closing error", e);
                }
            }
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (Exception e) {
                    logger.error("ObjectOutputStream closing error", e);
                }
            }
        }
    }

    // 反序列化
    public static Object jdeserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = null;
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            byteArrayInputStream = new ByteArrayInputStream(bytes);
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            return objectInputStream.readObject();
        } finally {
            if (byteArrayInputStream != null) {
                try {
                    byteArrayInputStream.close();
                } catch (Exception e) {
                    logger.error("ByteArrayInputStream closing error", e);
                }
            }
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (Exception e) {
                    logger.error("ObjectInputStream closing error", e);
                }
            }
        }
    }

    // 基于kryo序列化
    public static byte[] kryoSerialize(Object t) {
        Output output = null;
        try {
            output = new Output(new ByteArrayOutputStream());
            kryo.writeClassAndObject(output, t);
            return output.toBytes();
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    // 基于kryo反序列化
    public static Object kryoDeserialize(byte[] bytes) {
        Input input = null;
        try {
            input = new Input(bytes);
            return kryo.readClassAndObject(input);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }
}
