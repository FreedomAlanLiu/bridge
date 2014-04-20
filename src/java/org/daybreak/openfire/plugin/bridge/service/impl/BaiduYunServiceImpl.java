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

    private BaiduChannelClient channelClient;

    private DeviceType requestDeviceType = DeviceType.ALL;

    public BaiduYunServiceImpl() {
        // 设置developer平台的ApiKey/SecretKey
        String apiKey = JiveGlobals.getProperty("plugin.bridge.bccs.ak", "FOVIVsKp1WTQRQHzW1jvtD8F");
        String secretKey = JiveGlobals.getProperty("plugin.bridge.bccs.sk", "q6ixGI1EiAG2SW1QPQWTaVfZZWWeQtz4");
        ChannelKeyPair pair = new ChannelKeyPair(apiKey, secretKey);

        // 创建BaiduChannelClient对象实例
        channelClient = new BaiduChannelClient(pair);

        // 若要了解交互细节，请注册YunLogHandler类
        channelClient.setChannelLogHandler(new YunLogHandler() {
            @Override
            public void onHandle(YunLogEvent event) {
                logger.info(event.getMessage());
            }
        });

        String device = JiveGlobals.getProperty("plugin.bridge.bccs.device", "all");
        for (DeviceType deviceType : DeviceType.values()) {
            if (deviceType.toString().equals(device.toUpperCase())) {
                requestDeviceType = deviceType;
                break;
            }
        }
    }

    @Override
    public void pushMessage(Long channelId, String userId, String message) {
        try {
            // 创建请求类对象
            PushUnicastMessageRequest request = new PushUnicastMessageRequest();
            if (requestDeviceType != DeviceType.ALL) {
                request.setDeviceType(requestDeviceType.ordinal()); // device_type => 1: web 2: pc 3:android
                // 4:ios 5:wp
            }

            request.setChannelId(channelId);
            request.setUserId(userId);

            request.setMessage(message);

            // 调用pushMessage接口
            PushUnicastMessageResponse response = channelClient
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
    public void pushTagMessage(String tagName, String message) {
        try {
            // 创建请求类对象
            PushTagMessageRequest request = new PushTagMessageRequest();
            if (requestDeviceType != DeviceType.ALL) {
                request.setDeviceType(requestDeviceType.ordinal()); // device_type => 1: web 2: pc 3:android
                // 4:ios 5:wp
            }
            request.setTagName(tagName);
            request.setMessage(message);

            // 若要通知，
            // request.setMessageType(1);
            // request.setMessage("{\"title\":\"Notify_title_danbo\",\"description\":\"Notify_description_content\"}");

            // 调用pushMessage接口
            PushTagMessageResponse response = channelClient
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
