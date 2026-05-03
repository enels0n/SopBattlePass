package net.enelson.sopbattlepass.network;

public final class ServerIdentity {

    private final boolean networkEnabled;
    private final String serverId;
    private final String serverGroup;

    public ServerIdentity(boolean networkEnabled, String serverId, String serverGroup) {
        this.networkEnabled = networkEnabled;
        this.serverId = serverId;
        this.serverGroup = serverGroup;
    }

    public boolean isNetworkEnabled() {
        return networkEnabled;
    }

    public String getServerId() {
        return serverId;
    }

    public String getServerGroup() {
        return serverGroup;
    }
}
