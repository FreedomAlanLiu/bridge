package org.daybreak.openfire.plugin.bridge.service;

/**
 * Created by Alan on 2014/4/20.
 */
public interface BaiduYunService {

    public void pushMessage(Long channelId, String userId, String message);

    public void pushTagMessage(String tagName, String message);
}
