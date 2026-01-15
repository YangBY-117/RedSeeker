package com.redseeker.place;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Service
public class RoutePlanServiceImpl implements RoutePlanService {
    private static final String AMAP_KEY = "2039f165180b1ece6c8cfb1ae448339b";
    private static final String API_URL = "https://restapi.amap.com/v4/direction/batch?key=" + AMAP_KEY;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public RoutePlanResponse planRoute(RoutePlanRequest request) {
        // 1. 按历史背景排序景点（假设已按 candidateIds 顺序传入，实际可查数据库）
        // 2. 构造高德API请求参数
        List<String> allPoints = new ArrayList<>();
        allPoints.add(request.getOrigin().getLongitude() + "," + request.getOrigin().getLatitude());
        // 景点
        for (String id : request.getCandidateIds()) {
            // 这里应查数据库获取景点经纬度，简化为直接用ID（实际需补充）
            // allPoints.add(...);
        }
        allPoints.add(request.getDestination().getLongitude() + "," + request.getDestination().getLatitude());
        // 3. 调用高德API多点规划
        // 这里只做伪代码，实际需补充景点经纬度和API参数
        RoutePlanResponse resp = new RoutePlanResponse();
        resp.setTotalDistance(0);
        resp.setTotalDuration(0);
        List<Map<String, Object>> points = new ArrayList<>();
        for (String pt : allPoints) {
            Map<String, Object> flag = new HashMap<>();
            String[] arr = pt.split(",");
            flag.put("longitude", Double.parseDouble(arr[0]));
            flag.put("latitude", Double.parseDouble(arr[1]));
            flag.put("type", "flag");
            points.add(flag);
        }
        resp.setPoints(points);
        // TODO: 实际应解析高德API返回的总路程、总时间、路线点
        return resp;
    }
}
