package org.daybreak.openfire.plugin.bridge.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

/**
 * Created by Alan on 2014/4/22.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Article implements Serializable {

    private String type = "chat";

    @JsonProperty("message_type")
    private String messageType;

    private String from;

    private String message;

    private Aps aps;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Aps getAps() {
        return aps;
    }

    public void setAps(Aps aps) {
        this.aps = aps;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class Aps implements Serializable {

        private String alert = "Message From Baidu Push";

        private String sound = "";

        private String badge = "0";

        public String getAlert() {
            return alert;
        }

        public void setAlert(String alert) {
            this.alert = alert;
        }

        public String getSound() {
            return sound;
        }

        public void setSound(String sound) {
            this.sound = sound;
        }

        public String getBadge() {
            return badge;
        }

        public void setBadge(String badge) {
            this.badge = badge;
        }
    }
}
