package org.daybreak.openfire.plugin.bridge.provider;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * Created by hzn on 14-7-21.
 */
public class TimestampReceiptRequest implements PacketExtension {
    public static final String ELEMENT_NAME = "message_receipt_request";
    public static final String NAMESPACE = "www.ibridgelearn.com";

    private final String mId;

    private static final String XML = "<" + ELEMENT_NAME + " xmlns='" +
            NAMESPACE + "' id='%s' />";

    public TimestampReceiptRequest(String id) {
        mId = id;
    }

    @Override
    public String getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String toXML() {
        return String.format(XML, mId);
    }

    public String getId() {
        return mId;
    }


    public static final class Provider implements PacketExtensionProvider {
        @Override
        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            String id = null;

            int eventType = parser.next();
            if (eventType == XmlPullParser.END_TAG) {
                if (ELEMENT_NAME.equals(parser.getName())) {
                    id = parser.getAttributeValue(null, "id");
                }
            }

            if (id != null)
                return new TimestampReceiptRequest(id);
            return null;
        }
    }
}
