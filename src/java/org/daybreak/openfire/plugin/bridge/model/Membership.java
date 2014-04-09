package org.daybreak.openfire.plugin.bridge.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Created by Alan on 2014/3/31.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Membership implements Serializable {

    private String id;

    private User user;

    private Group group;

    private String access;//manager, write, read

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }
}
