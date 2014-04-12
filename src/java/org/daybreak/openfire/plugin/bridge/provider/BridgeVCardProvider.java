package org.daybreak.openfire.plugin.bridge.provider;

import org.daybreak.openfire.plugin.bridge.model.User;
import org.daybreak.openfire.plugin.bridge.service.BridgeService;
import org.daybreak.openfire.plugin.bridge.service.impl.BridgeServiceImpl;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jivesoftware.openfire.vcard.VCardProvider;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.util.AlreadyExistsException;
import org.jivesoftware.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Alan on 2014/4/13.
 */
public class BridgeVCardProvider implements VCardProvider {

    private static final Logger logger = LoggerFactory.getLogger(BridgeVCardProvider.class);

    @Override
    public Element loadVCard(String userId) {
        BridgeService bridgeService = BridgeServiceImpl.getInstance();
        User bridgeUser = bridgeService.loadUser(userId);
        if (bridgeUser == null) {
            String token = bridgeService.getOneToken();
            try {
                bridgeUser = bridgeService.findUser(userId, token);
            } catch (Exception e) {
                logger.error("user finding error", e);
                throw new RuntimeException(e);
            }
        }

        VCard vard = new VCard();
        vard.setNickName(bridgeUser.getUsername() + (bridgeUser.getName() == null ? "" : "(" + bridgeUser.getName() + ")"));
        vard.setEmailHome(bridgeUser.getEmail());
        try {
            vard.setAvatar(new URL(bridgeUser.getAvatarUrl()));
        } catch (MalformedURLException e) {
            logger.error("avatar setting error", e);
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
