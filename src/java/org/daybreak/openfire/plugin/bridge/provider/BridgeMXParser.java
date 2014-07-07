package org.daybreak.openfire.plugin.bridge.provider;

import org.jivesoftware.openfire.net.MXParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Created by alan on 14-6-18.
 */
public class BridgeMXParser extends MXParser {

    private boolean highSurrogateSeen = false;

    @Override
    protected char more() throws IOException, XmlPullParserException {
        final char codePoint = super.more(); // note - this does NOT return a codepoint now, but simply a (double byte) character!
        boolean validCodepoint = false;
        boolean isLowSurrogate = Character.isLowSurrogate(codePoint);
        if ((codePoint == 0x0) ||  // 0x0 is not allowed, but flash clients insist on sending this as the very first character of a stream. We should stop allowing this codepoint after the first byte has been parsed.
                (codePoint == 0x9) ||
                (codePoint == 0xA) ||
                (codePoint == 0xD) ||
                ((codePoint >= 0x20) && (codePoint <= 0xD7FF)) ||
                ((codePoint >= 0xE000) && (codePoint <= 0xFFFD)) ||
                ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF))) {
            validCodepoint = true;

        } else if (highSurrogateSeen) {
            if (isLowSurrogate) {
                validCodepoint = true;
            } else {
                throw new XmlPullParserException("High surrogate followed by non low surrogate '0x" + String.format("%x", (int) codePoint) + "'");
            }
        } else if (isLowSurrogate) {
            throw new XmlPullParserException("Low surrogate '0x " + String.format("%x", (int) codePoint) + " without preceeding high surrogate");
        } else if (Character.isHighSurrogate(codePoint)) {
            highSurrogateSeen = true;
            // Return here so that highSurrogateSeen is not reset
            return codePoint;
        }
        // Always reset high surrogate seen
        highSurrogateSeen = false;
        if (validCodepoint)
            return codePoint;

        throw new XmlPullParserException("Illegal XML character '0x" + String.format("%x", (int) codePoint) + "'");
    }
}
