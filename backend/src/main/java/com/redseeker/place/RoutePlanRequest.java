package com.redseeker.place;

import java.util.List;

public class RoutePlanRequest {
    private PlaceLocation origin;
    private PlaceLocation destination;
    private List<String> candidateIds; // 景点ID列表

    public PlaceLocation getOrigin() { return origin; }
    public void setOrigin(PlaceLocation origin) { this.origin = origin; }
    public PlaceLocation getDestination() { return destination; }
    public void setDestination(PlaceLocation destination) { this.destination = destination; }
    public List<String> getCandidateIds() { return candidateIds; }
    public void setCandidateIds(List<String> candidateIds) { this.candidateIds = candidateIds; }
}
