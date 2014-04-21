package org.daybreak.openfire.plugin.bridge.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Created by Alan on 2014/4/21.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Device {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("baidu_user_id")
    private String baiduUserId;

    @JsonProperty("channel_id")
    private Long channelId;

    @JsonProperty("device_type")
    private String deviceType;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBaiduUserId() {
        return baiduUserId;
    }

    public void setBaiduUserId(String baiduUserId) {
        this.baiduUserId = baiduUserId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
}
