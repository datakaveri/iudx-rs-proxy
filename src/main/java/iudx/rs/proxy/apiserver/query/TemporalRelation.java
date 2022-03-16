package iudx.rs.proxy.apiserver.query;

public class TemporalRelation {

  private String endTime;
  private String timeRel;
  private String time;

  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public String getTimeRel() {
    return timeRel;
  }

  public void setTimeRel(String timeRel) {
    this.timeRel = timeRel;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  @Override
  public String toString() {
    return "TemporalRelation [endTime=" + endTime + ", timeRel=" + timeRel + ", time=" + time + "]";
  }



}
