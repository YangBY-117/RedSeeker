package com.redseeker.place;

import jakarta.validation.constraints.NotNull;

public class PlaceAroundRequest {
    @NotNull
    private Double longitude;
    @NotNull
    private Double latitude;
    private String keywords;
    private String types;
    private Integer radius;
    private Integer page;
    private Integer pageSize;

    // getters and setters
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getTypes() { return types; }
    public void setTypes(String types) { this.types = types; }
    public Integer getRadius() { return radius; }
    public void setRadius(Integer radius) { this.radius = radius; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
