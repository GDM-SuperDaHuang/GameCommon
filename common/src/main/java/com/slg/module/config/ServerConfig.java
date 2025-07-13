package com.slg.module.config;

public class ServerConfig {
    private int groupId;
    private int serverId;
    private String host;
    private int port;

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    private int MinProtoId;
    private int MaxProtoId;

    public ServerConfig() {
    }

    public int getMinProtoId() {
        return MinProtoId;
    }

    public int getMaxProtoId() {
        return MaxProtoId;
    }

    public ServerConfig(String host, int port, int serverId) {
        this.host = host;
        this.port = port;
        this.serverId = serverId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setMinProtoId(int minProtoId) {
        MinProtoId = minProtoId;
    }

    public void setMaxProtoId(int maxProtoId) {
        MaxProtoId = maxProtoId;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }
}
