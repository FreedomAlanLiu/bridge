package org.daybreak.openfire.plugin.bridge.resource;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * Created by hzn on 14-6-5.
 */
public class MessageExtension implements PacketExtension {
    public static final String ELEMENT_NAME = "ctalk_ext";
    public static final String NAMESPACE = "www.ibridgelearn.com";

    private final String mType;
    private final String mContent;

    private static final String XML = "<" + ELEMENT_NAME + " xmlns='" +
            NAMESPACE + "'><type>%s</type><content>%s</content></" +
            ELEMENT_NAME + ">";

    public MessageExtension(String mime, String content) {
        mType = mime;
        mContent = content;
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
        return String.format(XML, mType, mContent);
    }

    public String getType() {
        return mType;
    }

    public String getContent() {
        return mContent;
    }

    public static final class Provider implements PacketExtensionProvider {

        @Override
        public PacketExtension parseExtension(XmlPullParser xmlPullParser) throws Exception {
            String mime = null;
            String content = null;
            while (true) {
                int n = xmlPullParser.next();
                if (n == xmlPullParser.START_TAG) {
                    if ("type".equals(xmlPullParser.getName())) {
                        mime = xmlPullParser.nextText();
                    } else if ("content".equals(xmlPullParser.getName())) {
                        content = xmlPullParser.nextText();
                    }
                } else if (n == xmlPullParser.END_TAG) {
                    if (MessageExtension.ELEMENT_NAME.equals(xmlPullParser.getName())) {
                        break;
                    }
                }
            }

            if (mime != null && content != null) {
                return new MessageExtension(mime, content);
            }

            return null;
        }
    }
}
