package com.ibm.devops.connect.Endpoints;

public class EndpointManager {

    // TODO: Make configurable at build time or otherwise
    private static String profile = "Velocity";
    //private static String profile = "YS1";

    private IEndpoints endpointProvider;

    public EndpointManager() {
        endpointProvider = new EndpointsVelocity();
    }

    public String getSyncApiEndpoint() {
        return endpointProvider.getSyncApiEndpoint();
    }

    public String getQualityDataEndpoint() {
        return endpointProvider.getQualityDataEndpoint();
    }

    public String getSyncApiEndpoint(String baseUrl) {
        return endpointProvider.getSyncApiEndpoint(baseUrl);
    }

    public String getReleaseEvensApiEndpoint() {
        return endpointProvider.getReleaseEvensApiEndpoint();
    }

    public String getDotsEndpoint() {
        return endpointProvider.getDotsEndpoint();
    }

    public String getSyncStoreEndpoint() {
        return endpointProvider.getSyncStoreEndpoint();
    }

    public String getConnectEndpoint() {
        return endpointProvider.getConnectEndpoint();
    }

    public String getVelocityHostname() {
        return endpointProvider.getVelocityHostname();
    }
}
