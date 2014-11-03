package com.keybox.manage.model;


import java.util.Date;
import java.util.List;

/**
 * value object for terminal logs and history
 */
public class SessionAudit {
    Long id;
    List<HostSystem> hostSystemList;
    User user;
    Date sessionTm;


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getSessionTm() {
        return sessionTm;
    }

    public void setSessionTm(Date sessionTm) {
        this.sessionTm = sessionTm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<HostSystem> getHostSystemList() {
        return hostSystemList;
    }

    public void setHostSystemList(List<HostSystem> hostSystemList) {
        this.hostSystemList = hostSystemList;
    }
}