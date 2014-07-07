package org.daybreak.openfire.plugin.bridge.service;

import com.baidu.yun.channel.client.BaiduChannelClient;
import org.daybreak.openfire.plugin.bridge.model.Device;

/**
 * Created by Alan on 2014/4/20.
 */
public interface BaiduYunService {

    public void pushMessage(Device device, String message);

    public void pushMessage(BaiduChannelClient baiduChannelClient, Long channelId, String userId, String deviceTypeStr,
                            String message);

    public void pushTagMessage(String tagName, String deviceTypeStr, String message);
}
