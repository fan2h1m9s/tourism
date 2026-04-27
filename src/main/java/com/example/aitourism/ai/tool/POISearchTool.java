package com.example.aitourism.ai.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class POISearchTool {

    @Value("${amap.api-key:your-amap-api-key}")
    private String amapApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Tool("用于检索指定城市(city)的旅游景点或地点(keyword)，获取实时的地理和交通信息以帮助安排行程。这可以消除编造不存在地点的风险。")
    public String searchPOI(String city, String keyword) {
        log.info("【Tool invoked】模型正在尝试调用高德地图API查询POI: 城市[{}], 关键字[{}]", city, keyword);
        
        if ("your-amap-api-key".equals(amapApiKey) || amapApiKey.isBlank()) {
            return "未配置真实高德API Key，无法返回实时数据。";
        }
        
        try {
            String url = UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/place/text")
                    .queryParam("key", amapApiKey)
                    .queryParam("keywords", keyword)
                    .queryParam("city", city)
                    .queryParam("offset", "5") // 只取前5个最相关的结果
                    .queryParam("page", "1")
                    .queryParam("extensions", "all")
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && "1".equals(response.get("status"))) {
                List<Map<String, Object>> pois = (List<Map<String, Object>>) response.get("pois");
                if (pois == null || pois.isEmpty()) {
                    return "未查询到相关景点信息，请换一个关键字。";
                }
                
                StringBuilder sb = new StringBuilder("找到以下高德地图实时POI推荐结果(幻觉率因此降低)：\n");
                for (Map<String, Object> poi : pois) {
                    sb.append("- 名称: ").append(poi.get("name"))
                      .append(", 地址: ").append(poi.get("address"))
                      .append(", 距离预估中心: ").append(poi.get("distance")).append("米")
                      .append("\n");
                }
                return sb.toString();
            }
            return "高德API查询异常: " + response.get("info");
        } catch (Exception e) {
            log.error("调用高德API失败", e);
            return "调用高德API服务失败，请依靠推测规划或重试。";
        }
    }
}
