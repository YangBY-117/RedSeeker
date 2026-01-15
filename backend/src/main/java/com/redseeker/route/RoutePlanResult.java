package com.redseeker.route;

import java.util.List;

/**
 * 路线规划结果（前端格式）
 */
public class RoutePlanResult {
  private List<List<Double>> path;  // 路径点数组 [[lng, lat], ...]
  private Integer total_distance;   // 总距离（米）
  private Integer total_duration;    // 总时间（秒）

  public List<List<Double>> getPath() {
    return path;
  }

  public void setPath(List<List<Double>> path) {
    this.path = path;
  }

  public Integer getTotal_distance() {
    return total_distance;
  }

  public void setTotal_distance(Integer total_distance) {
    this.total_distance = total_distance;
  }

  public Integer getTotal_duration() {
    return total_duration;
  }

  public void setTotal_duration(Integer total_duration) {
    this.total_duration = total_duration;
  }
}
