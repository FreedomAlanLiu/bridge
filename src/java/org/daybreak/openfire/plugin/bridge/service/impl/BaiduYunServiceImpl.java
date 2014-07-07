package org.daybreak.openfire.plugin.bridge.service.impl;

import com.baidu.yun.channel.auth.ChannelKeyPair;
import com.baidu.yun.channel.client.BaiduChannelClient;
import com.baidu.yun.channel.exception.ChannelClientException;
import com.baidu.yun.channel.exception.ChannelServerException;
import com.baidu.yun.channel.model.PushTagMessageRequest;
import com.baidu.yun.channel.model.PushTagMessageResponse;
import com.baidu.yun.channel.model.PushUnicastMessageRequest;
import com.baidu.yun.channel.model.PushUnicastMessageResponse;
import com.baidu.yun.core.log.YunLogEvent;
import com.baidu.yun.core.log.YunLogHandler;
import org.daybreak.openfire.plugin.bridge.model.Device;
import org.daybreak.openfire.plugin.bridge.model.DeviceType;
import org.daybreak.openfire.plugin.bridge.service.BaiduYunService;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Alan on 2014/4/20.
 */
public class BaiduYunServiceImpl implements BaiduYunService {

    private static final Logger logger = LoggerFactory.getLogger(BaiduYunServiceImpl.class);

    private BaiduChannelClient testChannelClient;
    private BaiduChannelClient appstoreChannelClinet;
    private BaiduChannelClient nonappstoreChannelCinet;

    private int iosDeployStatus;
    private int iosMessageType;

    public BaiduYunServiceImpl() {
        iosDeployStatus = JiveGlobals.getIntProperty("plugin.bridge.bccs.IOSDeployStatus", 1); // 默认开发模式
        iosMessageType = JiveGlobals.getIntProperty("plugin.bridge.bccs.IOSMessageType", 1); // 默认消息类型为notification
    }

    private BaiduChannelClient getTestChannelClient() {
        if (testChannelClient == null) {
            String ak = JiveGlobals.getProperty("plugin.bridge.bccs.test.ak", "FOVIVsKp1WTQRQHzW1jvtD8F");
            String sk = JiveGlobals.getProperty("plugin.bridge.bccs.test.sk", "q6ixGI1EiAG2SW1QPQWTaVfZZWWeQtz4");

            testChannelClient = new BaiduChannelClient(new ChannelKeyPair(ak, sk));
            testChannelClient.setChannelLogHandler(new YunLogHandler() {
                @Override
                public void onHandle(YunLogEvent event) {
                    logger.info(event.getMessage());
                }
            });
        }
        return testChannelClient;
    }

    private BaiduChannelClient getAppstoreChannelClinet() {
        if (appstoreChannelClinet == null) {
            String ak = JiveGlobals.getProperty("plugin.bridge.bccs.appstore.ak", "FOVIVsKp1WTQRQHzW1jvtD8F");
            String sk = JiveGlobals.getProperty("plugin.bridge.bccs.appstore.sk", "q6ixGI1EiAG2SW1QPQWTaVfZZWWeQtz4");

            appstoreChannelClinet = new BaiduChannelClient(new ChannelKeyPair(ak, sk));
            appstoreChannelClinet.setChannelLogHandler(new YunLogHandler() {
                @Override
                public void onHandle(YunLogEvent event) {
                    logger.info(event.getMessage());
                }
            });
        }
        return appstoreChannelClinet;
    }

    public BaiduChannelClient getNonappstoreChannelCinet() {
        if (nonappstoreChannelCinet == null) {
            String ak = JiveGlobals.getProperty("plugin.bridge.bccs.nonappstore.ak", "FOVIVsKp1WTQRQHzW1jvtD8F");
            String sk = JiveGlobals.getProperty("plugin.bridge.bccs.nonappstore.sk", "q6ixGI1EiAG2SW1QPQWTaVfZZWWeQtz4");

            nonappstoreChannelCinet = new BaiduChannelClient(new ChannelKeyPair(ak, sk));
            nonappstoreChannelCinet.setChannelLogHandler(new YunLogHandler() {
                @Override
                public void onHandle(YunLogEvent event) {
                    logger.info(event.getMessage());
                }
            });
        }
        return nonappstoreChannelCinet;
    }

    @Override
    public void pushMessage(Device device, String message) {
        if ("appstore".equals(device.getPushType())) {
            pushMessage(getAppstoreChannelClinet(),
                    device.getChannelId(),
                    device.getBaiduUserId(),
                    device.getDeviceType(),
                    message);
            return;
        } else if ("nonappstore".equals(device.getPushType())) {
            pushMessage(getNonappstoreChannelCinet(),
                    device.getChannelId(),
                    device.getBaiduUserId(),
                    device.getDeviceType(),
                    message);
            return;
        }
        pushMessage(getTestChannelClient(),
                device.getChannelId(),
                device.getBaiduUserId(),
                device.getDeviceType(),
                message);
    }

    @Override
    public void pushMessage(BaiduChannelClient baiduChannelClient, Long channelId, String userId, String deviceTypeStr,
                            String message) {
        try {
            // 创建请求类对象
            PushUnicastMessageRequest request = new PushUnicastMessageRequest();

            for (DeviceType deviceType : DeviceType.values()) {
                if (deviceType.toString().equalsIgnoreCase(deviceTypeStr)) {
                    if (deviceType == DeviceType.IOS) {
                        // 设备为ios的时候设置开发模式
                        request.setDeployStatus(iosDeployStatus);
                        // 设备为ios的时候消息类型为notification
                        request.setMessageType(iosMessageType);
                    }
                    // device_type => 1: web 2: pc 3:android 4:ios 5:wp
                    request.setDeviceType(deviceType.ordinal());
                    break;
                }
            }

            request.setChannelId(channelId);
            request.setUserId(userId);

            request.setMessage(message);

            // 调用pushMessage接口
            PushUnicastMessageResponse response = baiduChannelClient
                    .pushUnicastMessage(request);

            // 认证推送成功
            logger.info("push amount : " + response.getSuccessAmount());
        } catch (ChannelClientException e) {
            // 处理客户端错误异常
            logger.error("channel client error", e);
        } catch (ChannelServerException e) {
            // 处理服务端错误异常
            logger.error(String.format(
                    "request_id: %d, error_code: %d, error_message: %s",
                    e.getRequestId(), e.getErrorCode(), e.getErrorMsg()));
        }
    }

    @Override
    public void pushTagMessage(String tagName, String deviceTypeStr,
                               String message) {

        // 创建请求类对象
        PushTagMessageRequest request = new PushTagMessageRequest();
        for (DeviceType deviceType : DeviceType.values()) {
            if (deviceType.toString().equalsIgnoreCase(deviceTypeStr)) {
                if (deviceType == DeviceType.IOS) {
                    // 设备为ios的时候设置开发模式
                    request.setDeployStatus(iosDeployStatus);
                    // 设备为ios的时候消息类型为notification
                    request.setMessageType(iosMessageType);
                }
                // device_type => 1: web 2: pc 3:android 4:ios 5:wp
                request.setDeviceType(deviceType.ordinal());
                break;
            }
        }
        request.setTagName(tagName);
        request.setMessage(message);

        // 调用pushMessage接口
        try {
            PushTagMessageResponse response = getTestChannelClient()
                    .pushTagMessage(request);
            // 认证推送成功
            logger.info("push amount : " + response.getSuccessAmount());
        } catch (ChannelClientException e) {
            // 处理客户端错误异常
            logger.error("channel client error", e);
        } catch (ChannelServerException e) {
            // 处理服务端错误异常
            logger.error(String.format(
                    "request_id: %d, error_code: %d, error_message: %s",
                    e.getRequestId(), e.getErrorCode(), e.getErrorMsg()));
        }

        try {
            PushTagMessageResponse response = getAppstoreChannelClinet()
                    .pushTagMessage(request);
            // 认证推送成功
            logger.info("push amount : " + response.getSuccessAmount());
        } catch (ChannelClientException e) {
            // 处理客户端错误异常
            logger.error("channel client error", e);
        } catch (ChannelServerException e) {
            // 处理服务端错误异常
            logger.error(String.format(
                    "request_id: %d, error_code: %d, error_message: %s",
                    e.getRequestId(), e.getErrorCode(), e.getErrorMsg()));
        }

        try {
            PushTagMessageResponse response = getNonappstoreChannelCinet()
                    .pushTagMessage(request);
            // 认证推送成功
            logger.info("push amount : " + response.getSuccessAmount());
        } catch (ChannelClientException e) {
            // 处理客户端错误异常
            logger.error("channel client error", e);
        } catch (ChannelServerException e) {
            // 处理服务端错误异常
            logger.error(String.format(
                    "request_id: %d, error_code: %d, error_message: %s",
                    e.getRequestId(), e.getErrorCode(), e.getErrorMsg()));
        }
    }
}
