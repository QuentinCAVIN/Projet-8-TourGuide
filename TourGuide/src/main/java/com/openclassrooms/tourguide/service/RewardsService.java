package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.model.user.User;
import com.openclassrooms.tourguide.model.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

    // proximity in miles
    private int defaultProximityBuffer = 10;
    private int proximityBuffer = defaultProximityBuffer;
    private int attractionProximityRange = 200;
    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;
    ExecutorService executorService = Executors.newFixedThreadPool(500);

    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
    }

    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    public void setProximityBuffer(int proximityBuffer) {
        this.proximityBuffer = proximityBuffer;
    }

    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }

    public CompletableFuture<Void> calculateRewards(User user) {
        //List<VisitedLocation> userLocations = user.getVisitedLocations();/// Ecrit comme ça a la base...
        //... et remplacé par une CopyOnWriteArrayList pour gérer une ConcurrentModificationException
      CopyOnWriteArrayList<VisitedLocation> userLocations = new CopyOnWriteArrayList<>();
        user.getVisitedLocations().forEach(visitedLocation -> userLocations.add(visitedLocation));

        return CompletableFuture.supplyAsync(() -> gpsUtil.getAttractions(), executorService)
                .thenCompose((attractions -> { // thenCompose permet de chainer des completableFuture entre eux


                    List<CompletableFuture<Void>> futures = new ArrayList<>();
                    for (VisitedLocation visitedLocation : userLocations) {
                        for (Attraction attraction : attractions) {
                            // if valid = quand le nom de l'attraction ne correspond à aucune des attractions visitées par l'utilisateur
                            if (user.getUserRewards().stream().filter(reward -> reward.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
                                if (nearAttraction(visitedLocation, attraction)) {
                                    futures.add(addUserRewardAsync(user, visitedLocation, attraction));
                                    //Un CompletableFuture<Void> ajouté dans la liste "future" à chaque boucle
                                }
                            }
                        }
                    }
                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                    //On retourne un CompletableFuture composé de tout les CompletableFuture mis dans la liste "futures"
                }));
    }

    public CompletableFuture<Void> addUserRewardAsync(User user, VisitedLocation visitedLocation, Attraction attraction) {
        return CompletableFuture.supplyAsync(() -> getRewardPoints(attraction, user), executorService).thenAccept((integer) -> {
            user.addUserReward(new UserReward(visitedLocation, attraction, integer));
        });
    }

    //TODO: Methode en double de calculateReward, a supprimer avant la soutenance
    // cette methode résout le probléme de ConcurrentModificationException dans passer par la CopyOnWriteArrayList
    public void calculateRewards2(User user) {

        List<VisitedLocation> userLocations = user.getVisitedLocations();
        List<Attraction> attractions = gpsUtil.getAttractions();
        List<UserReward> rewards = user.getUserRewards();

        Map<Attraction, VisitedLocation> nearbyAttractions = new HashMap<>();
        for (VisitedLocation visitedLocation : userLocations) {
            for (Attraction attraction : attractions) {
                if (nearAttraction(visitedLocation, attraction)) {
                    nearbyAttractions.put(attraction, visitedLocation);

                }
            }
        }

        for (Map.Entry<Attraction, VisitedLocation> attraction : nearbyAttractions.entrySet()) {
            if (rewards.stream().filter(reward -> reward.attraction.attractionName.equals(attraction.getKey().attractionName)).count() == 0) {
                user.addUserReward(new UserReward(attraction.getValue(), attraction.getKey(), getRewardPoints(attraction.getKey(), user)));
            }
            // Un utilisateur obtient une réduction quand il est proche d'une attraction pour laquelle il n'a pas déja de réduction
        }
    }

    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) > attractionProximityRange ? false : true;
    }

    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
    }

    public int getRewardPoints(Attraction attraction, User user) {
        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }

    public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
    }
}