package com.redseeker.place;

import java.util.List;
import java.util.Map;

public class RoutePlanResponse {
    private int totalDistance; // 单位米
    private int totalDuration; // 单位秒
    private List<Map<String, Object>> points; // 按顺序的经纬度及标记

    public int getTotalDistance() { return totalDistance; }
    public void setTotalDistance(int totalDistance) { this.totalDistance = totalDistance; }
    public int getTotalDuration() { return totalDuration; }
    public void setTotalDuration(int totalDuration) { this.totalDuration = totalDuration; }
    public List<Map<String, Object>> getPoints() { return points; }
    public void setPoints(List<Map<String, Object>> points) { this.points = points; }
}
