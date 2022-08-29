package com.freedy.expression.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Freedy
 * @date 2022/8/9 0:53
 */
@ConfigurationProperties("fun.start")
public class ScriptStarterProperties {
    private String aesKey;
    private String auth;
    private int port;
    private int retryTime=5;
    private boolean enableLocal=true;
    private boolean enableRemote=false;

    public int getRetryTime() {
        return retryTime;
    }

    public void setRetryTime(int retryTime) {
        this.retryTime = retryTime;
    }

    public boolean isEnableLocal() {
        return enableLocal;
    }

    public void setEnableLocal(boolean enableLocal) {
        this.enableLocal = enableLocal;
    }

    public boolean isEnableRemote() {
        return enableRemote;
    }

    public void setEnableRemote(boolean enableRemote) {
        this.enableRemote = enableRemote;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAesKey() {
        return aesKey;
    }

    public String getAuth() {
        return auth;
    }

    public int getPort() {
        return port;
    }
}
