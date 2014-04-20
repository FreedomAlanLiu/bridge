package org.daybreak.openfire.plugin.bridge;

import org.daybreak.openfire.plugin.bridge.service.BaiduYunService;
import org.daybreak.openfire.plugin.bridge.service.BridgeService;
import org.daybreak.openfire.plugin.bridge.service.impl.BaiduYunServiceImpl;
import org.daybreak.openfire.plugin.bridge.service.impl.BridgeServiceImpl;

/**
 * Created by Alan on 2014/4/20.
 */
public class BridgeServiceFactory {

    public static final String BRIDGE_SERVICE_NAME = "bridgeService";

    public static final String BAIDU_YUN_SERVICE_NAME = "baiduYunService";

    private static final BridgeService bridgeService = new BridgeServiceImpl();

    private static final BaiduYunService baiduYunService = new BaiduYunServiceImpl();

    public static Object getBean(String beanName) {
        if (BRIDGE_SERVICE_NAME.equals(beanName)) {
            return bridgeService;
        } else if (BAIDU_YUN_SERVICE_NAME.equals(beanName)) {
            return baiduYunService;
        }
        return null;
    }
}
