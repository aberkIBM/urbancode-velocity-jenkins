package com.ibm.devops.connect.Endpoints;

public class EndpointsVelocity implements IEndpoints {
    private static final String SYNC_API_ENPOINT = "https://velocity.us-south.containers.mybluemix.net/sync/";
    private static final String AMQP_ENPOINT = "https://velocity.us-south.containers.mybluemix.net/sync/";
    // private static final String SYNC_API_ENPOINT = "http://192.168.1.35:6002/";
    private static final String SYNC_STORE_ENPOINT = "https://bogus/";
    private static final String CONNECT_ENPOINT = "https://bogus";

    public String getSyncApiEndpoint() {
        return SYNC_API_ENPOINT;
    }

    public String getSyncStoreEndpoint() {
        return SYNC_STORE_ENPOINT;
    }

    public String getConnectEndpoint() {
        return CONNECT_ENPOINT;
    }
}