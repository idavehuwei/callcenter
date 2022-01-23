package org.cti.cc.request;

import javax.validation.constraints.NotNull;

/**
 * Created by caoliang on 2022/1/18
 */
public class AdminLogin {

    @NotNull
    private String username;

    @NotNull
    private String passwd;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }
}
