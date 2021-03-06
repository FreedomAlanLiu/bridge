package org.daybreak.openfire.plugin.bridge.provider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.daybreak.openfire.plugin.bridge.BridgePlugin;
import org.daybreak.openfire.plugin.bridge.model.User;
import org.daybreak.openfire.plugin.bridge.service.BridgeService;
import org.daybreak.openfire.plugin.bridge.BridgeServiceFactory;
import org.daybreak.openfire.plugin.bridge.utils.HttpConnectionManager;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jivesoftware.openfire.vcard.VCardProvider;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.util.AlreadyExistsException;
import org.jivesoftware.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

/**
 * Created by Alan on 2014/4/13.
 */
public class BridgeVCardProvider implements VCardProvider {

    private static final Logger logger = LoggerFactory.getLogger(BridgeVCardProvider.class);

    @Override
    public Element loadVCard(String userId) {
        VCard vard = new VCard();

        BridgeService bridgeService = (BridgeService) BridgeServiceFactory.getBean("bridgeService");
        BufferedInputStream in = null;
        ByteArrayOutputStream out = null;
        HttpConnectionManager httpConnectionManager = new HttpConnectionManager();
        try {
            User bridgeUser = bridgeService.getUser(userId);
            if (bridgeUser == null) {
                throw new RuntimeException("Not found the user!");
            }

            vard.setNickName(bridgeUser.getUsername() + (bridgeUser.getName() == null ? "" : "(" + bridgeUser.getName() + ")"));
            vard.setEmailHome(bridgeUser.getEmail());

            HttpResponse response = httpConnectionManager.getHttpRequest("http://" + BridgePlugin.BRIDGE_HOST + bridgeUser.getAvatarUrl(), null);
            HttpEntity entity = response.getEntity();
            in = new BufferedInputStream((entity.getContent()));
            out = new ByteArrayOutputStream();
            int b = in.read();
            while (b != -1) {
                out.write(b);
                b = in.read();
            }
            vard.setAvatar(out.toByteArray());
        } catch (Exception e) {
            logger.error("avatar setting error", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.warn(e.getMessage());
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.warn(e.getMessage());
                }
            }
            try {
                httpConnectionManager.close();
            } catch (IOException e) {
                logger.warn(e.getMessage());
            }
        }

        String xml = vard.getChildElementXML();
        SAXReader xmlReader = new SAXReader();
        xmlReader.setEncoding("UTF-8");
        try {
            return xmlReader.read(new StringReader(xml)).getRootElement();
        } catch (DocumentException e) {
            logger.error("vcard parsing error", e);
            return null;
        }
    }

    @Override
    public Element createVCard(String username, Element vCardElement) throws AlreadyExistsException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Element updateVCard(String username, Element vCardElement) throws NotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteVCard(String username) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
