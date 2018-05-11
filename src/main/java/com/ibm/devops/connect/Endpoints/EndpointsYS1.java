package com.ibm.devops.connect.Endpoints;

public class EndpointsYS1 implements IEndpoints {
    private static final String SYNC_API_ENPOINT = "https://ucreporting-sync-api-stage1.stage1.mybluemix.net/";
    private static final String SYNC_STORE_ENPOINT = "https://bogus";
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