package iudx.rs.proxy.optional.consentlogs;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.rs.proxy.common.ConsentLogType;
import iudx.rs.proxy.optional.consentlogs.dss.PayloadSigningManager;

public class ConsentLoggingServiceImpl implements ConsentLoggingService{
  
  private final PayloadSigningManager payloadSigningManager;
  public ConsentLoggingServiceImpl(PayloadSigningManager signingManager) {
    this.payloadSigningManager=signingManager;
  }

  @Override
  public Future<JsonObject> log(JsonObject request) {
    Promise<JsonObject> promise=Promise.promise();
    if(request.containsKey("logType")) {
      ConsentLogType logType=ConsentLogType.valueOf(request.getString("logType"));
      switch(logType) {
        case DATA_REQUESTED:{
          //TODO: build a log for data requested [add fields required or remove fields not required ]
          DataRequestedLog dataReqLog=new DataRequestedLog();
          //get signed log
          String signedLog=payloadSigningManager.signDocWithPKCS12(dataReqLog.toJsonObject());
          //Insert log Json into RMQ.
          break;
        }
        case DATA_SENT:{
          DataDeliveredLog dataDeliveredLog=new DataDeliveredLog();
          String signedPayload=payloadSigningManager.signDocWithPKCS12(dataDeliveredLog.toJsonObject());
          break;
        }
        case DATA_DENIED:{
          DataDeniedLog dataDeniedLog=new DataDeniedLog();
          String signedPayload=payloadSigningManager.signDocWithPKCS12(dataDeniedLog.toJsonObject());
          break;
        }
        default:
      }
    }
    return promise.future();
  }

}
