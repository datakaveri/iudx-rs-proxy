package iudx.rs.proxy.common;

import static iudx.rs.proxy.apiserver.util.ApiServerConstants.*;

public class Api {

  private static volatile Api apiInstance;
  private final String dxApiBasePath;
  private StringBuilder entitiesEndpoint;
  private StringBuilder temporalEndpoint;
  private StringBuilder postEntitiesEndpoint;
  private StringBuilder postTemporalEndpoint;
  private StringBuilder consumerAuditEndpoint;
  private StringBuilder providerAuditEndpoint;
  private StringBuilder asyncPath;
  private StringBuilder asyncSearchEndPoint;
  private StringBuilder asyncStatusEndPoint;

  private Api(String dxApiBasePath) {
    this.dxApiBasePath = dxApiBasePath;
    buildEndpoints();
  }

  public static Api getInstance(String dxApiBasePath) {
    if (apiInstance == null) {
      synchronized (Api.class) {
        if (apiInstance == null) {
          apiInstance = new Api(dxApiBasePath);
        }
      }
    }
    return apiInstance;
  }

  private void buildEndpoints() {
    entitiesEndpoint = new StringBuilder(dxApiBasePath).append(ENTITIES_URL);
    temporalEndpoint = new StringBuilder(dxApiBasePath).append(TEMPORAL_URL);
    postEntitiesEndpoint = new StringBuilder(dxApiBasePath).append(POST_ENTITIES_URL);
    postTemporalEndpoint = new StringBuilder(dxApiBasePath).append(POST_TEMPORAL_URL);
    consumerAuditEndpoint = new StringBuilder(dxApiBasePath).append(CONSUMER_AUDIT_URL);
    providerAuditEndpoint = new StringBuilder(dxApiBasePath).append(PROVIDER_AUDIT_URL);
    asyncPath = new StringBuilder(dxApiBasePath).append(ASYNC);
    asyncSearchEndPoint = new StringBuilder(dxApiBasePath).append(ASYNC + SEARCH);
    asyncStatusEndPoint = new StringBuilder(dxApiBasePath).append(ASYNC + STATUS);
  }

  public String getEntitiesEndpoint() {
    return entitiesEndpoint.toString();
  }

  public String getTemporalEndpoint() {
    return temporalEndpoint.toString();
  }

  public String getPostEntitiesEndpoint() {
    return postEntitiesEndpoint.toString();
  }

  public String getPostTemporalEndpoint() {
    return postTemporalEndpoint.toString();
  }

  public String getConsumerAuditEndpoint() {
    return consumerAuditEndpoint.toString();
  }

  public String getProviderAuditEndpoint() {
    return providerAuditEndpoint.toString();
  }

  public String getAsyncPath() {
    return asyncPath.toString();
  }

  public String getAsyncSearchEndPoint() {
    return asyncSearchEndPoint.toString();
  }

  public String getAsyncStatusEndpoint() {
    return asyncStatusEndPoint.toString();
  }
}
