package org.daybreak.openfire.plugin.bridge.provider;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * Created by hzn on 14-6-5.
 */
public class TimestampResponseExtension implements PacketExtension {
    public static final String ELEMENT_NAME = "timestamp_response";
    public static final String NAMESPACE = "www.ibridgelearn.com";

    private String messageId;
    private long timestamp;

    private static final String XML = "<" + ELEMENT_NAME + " xmlns='" +
            NAMESPACE + "'><messageId>%s</messageId><timestamp>%s</timestamp></" +
            ELEMENT_NAME + ">";

    public TimestampResponseExtension(String messageId, long timestamp) {
        this.messageId = messageId;
        this.timestamp = timestamp;
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
        return String.format(XML, messageId, timestamp);
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public static final class Provider implements PacketExtensionProvider {

        @Override
        public PacketExtension parseExtension(XmlPullParser xmlPullParser) throws Exception {
            String messageId = null;
            Long timestamp = null;
            while (true) {
                int n = xmlPullParser.next();
                if (n == xmlPullParser.START_TAG) {
                    if ("messageId".equals(xmlPullParser.getName())) {
                        messageId = xmlPullParser.nextText();
                    } else if ("timestamp".equals(xmlPullParser.getName())) {
                        timestamp = Long.valueOf(xmlPullParser.nextText());
                    }
                } else if (n == xmlPullParser.END_TAG) {
                    if (TimestampResponseExtension.ELEMENT_NAME.equals(xmlPullParser.getName())) {
                        break;
                    }
                }
            }

            if (messageId != null && timestamp != null) {
                return new TimestampResponseExtension(messageId, timestamp);
            }

            return null;
        }
    }
}
