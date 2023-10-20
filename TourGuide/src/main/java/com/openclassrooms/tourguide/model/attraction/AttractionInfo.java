package com.openclassrooms.tourguide.model.attraction;

import gpsUtil.location.Location;

public class AttractionInfo {
    private final String attractionName;
    private final Location attractionLocation;
    private double distanceInMilesFromUser;
    private int rewardPoints;

    public AttractionInfo(String attractionName, Location attractionLocation,
                          double distanceInMilesFromUser, int rewardPoints) {
        this.attractionName = attractionName;
        this.attractionLocation = attractionLocation;
        this.distanceInMilesFromUser = distanceInMilesFromUser;
        this.rewardPoints = rewardPoints;
    }

    public String getAttractionName() {
        return attractionName;
    }

    public double getDistanceInMilesFromUser() {
        return distanceInMilesFromUser;
    }

    public void setDistanceInMilesFromUser(double distanceInMilesFromUser) {
        this.distanceInMilesFromUser = distanceInMilesFromUser;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    public Location getAttractionLocation() {
        return attractionLocation;
    }
}
