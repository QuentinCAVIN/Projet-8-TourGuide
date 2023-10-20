package com.openclassrooms.tourguide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.openclassrooms.tourguide.model.attraction.AttractionInfo;
import gpsUtil.location.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.model.user.User;
import com.openclassrooms.tourguide.model.user.UserReward;

import tripPricer.Provider;

@RestController
public class TourGuideController {

    @Autowired
    TourGuideService tourGuideService;

    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }

    @RequestMapping("/getLocation")
    public VisitedLocation getLocation(@RequestParam String userName) {
        return tourGuideService.getUserLocation(getUser(userName));
    }

    //  Return a new JSON object that contains:
    // Name of Tourist attraction,
    // Tourist attractions lat/long,
    // The user's location lat/long,
    // The distance in miles between the user's location and each of the attractions.
    // The reward points for visiting each Attraction.
    @RequestMapping("/getNearbyAttractions")
    public Map<String, Object> getNearbyAttractions(@RequestParam String userName) {
        Map<String, Object> nearbyAttractions = new HashMap<>();
        User user = tourGuideService.getUser(userName);

        VisitedLocation visitedLocation = tourGuideService.getUserLocation(user);
        List<Attraction> fiveNearestAttractions = tourGuideService.getNearByAttractions(visitedLocation);

        List<AttractionInfo> attractionInfoList = new ArrayList<>();
        for (Attraction attraction : fiveNearestAttractions) {
            attractionInfoList.add(
                    tourGuideService.attractionInfoBuilder(attraction, user));
        }
        nearbyAttractions.put("attraction information", attractionInfoList);

        Location userLocation = visitedLocation.location;
        nearbyAttractions.put("user location", userLocation);

        return nearbyAttractions;
    }

    @RequestMapping("/getRewards")
    public List<UserReward> getRewards(@RequestParam String userName) {
        return tourGuideService.getUserRewards(getUser(userName));
    }

    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
        return tourGuideService.getTripDeals(getUser(userName));
    }

    private User getUser(String userName) {
        return tourGuideService.getUser(userName);
    }
}