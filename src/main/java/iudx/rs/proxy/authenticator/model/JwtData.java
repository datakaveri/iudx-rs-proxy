package iudx.rs.proxy.authenticator.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true, publicConverter = false)
public final class JwtData {

  private String access_token;
  private String sub;
  private String iss;
  private String aud;
  private Integer exp;
  private Integer iat;
  private String iid;
  private String role;
  private JsonObject cons;
  private JsonObject apd;
  private String drl;
  private String did;

  public JwtData() {
    super();
  }

  public JwtData(JsonObject json) {
    JwtDataConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    JwtDataConverter.toJson(this, json);
    return json;
  }

  public String getDrl() {
    return drl;
  }

  public void setDrl(String drl) {
    this.drl = drl;
  }

  public String getDid() {
    return did;
  }

  public void setDid(String did) {
    this.did = did;
  }

  public String getAccess_token() {
    return access_token;
  }

  public void setAccess_token(String access_token) {
    this.access_token = access_token;
  }

  public String getSub() {
    return sub;
  }

  public void setSub(String sub) {
    this.sub = sub;
  }

  public String getIss() {
    return iss;
  }

  public void setIss(String iss) {
    this.iss = iss;
  }

  public String getAud() {
    return aud;
  }

  public void setAud(String aud) {
    this.aud = aud;
  }

  public String getIid() {
    return iid;
  }

  public void setIid(String iid) {
    this.iid = iid;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public JsonObject getCons() {
    return cons;
  }

  public void setCons(JsonObject cons) {
    this.cons = cons;
  }

  public Integer getExp() {
    return exp;
  }

  public void setExp(Integer exp) {
    this.exp = exp;
  }

  public Integer getIat() {
    return iat;
  }

  public void setIat(Integer iat) {
    this.iat = iat;
  }

  public JsonObject getApd() {
    return apd;
  }

  public void setApd(JsonObject apd) {
    this.apd = apd;
  }

  @Override
  public String toString() {
    return "JwtData [access_token="
        + access_token
        + ", sub="
        + sub
        + ", iss="
        + iss
        + ", aud="
        + aud
        + ", exp="
        + exp
        + ", iat="
        + iat
        + ", iid="
        + iid
        + ", role="
        + role
        + ", cons="
        + cons
        + ", apd="
        + apd
        + ", drl="
        + drl
        + ", did="
        + did
        + "]";
  }
}
