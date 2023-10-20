package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    private final List<Attraction> attractions;
    ExecutorService executorService = Executors.newFixedThreadPool(1000);

    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
        this.attractions = gpsUtil.getAttractions(); // ajouté au constructeur afin de charger la liste une seule fois
    }

    public void setProximityBuffer(int proximityBuffer) {
        this.proximityBuffer = proximityBuffer;
    }

    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }

    public CompletableFuture<Void> calculateRewards(User user) {

        List<VisitedLocation> userLocations = user.getVisitedLocations();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        //Pour chaque attraction de l'app, on parcourt toutes les positions de l'utilisateur
        for (Attraction attraction : attractions) {
            for (VisitedLocation visitedLocation : userLocations) {

                // if true = user n'a pas encore de Reward pour cette attraction
                if (user.getUserRewards().stream().filter(reward -> reward.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
                    if (nearAttraction(visitedLocation, attraction)) {

                        //Un CompletableFuture<Void> ajouté dans la liste "futures" à chaque boucle
                        futures.add(addUserRewardAsync(user, visitedLocation, attraction));
                        //Quand une nouvelle récompense est ajoutée, on passe immédiatement à l'attraction suivante
                        break;
                    }
                }
            }
        }
        //On retourne un CompletableFuture composé de tout les CompletableFuture mis dans la liste "futures" (.allOf)
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public CompletableFuture<Void> addUserRewardAsync(User user, VisitedLocation visitedLocation, Attraction attraction) {
        return CompletableFuture.supplyAsync(() -> getRewardPoints(attraction, user), executorService).thenAccept((integer) -> {
            user.addUserReward(new UserReward(visitedLocation, attraction, integer));
        });
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