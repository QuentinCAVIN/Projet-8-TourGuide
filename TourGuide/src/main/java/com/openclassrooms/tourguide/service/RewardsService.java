package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}

	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	//TODO: Methode modifiée
	// Le test de la methode générais une ConcurrentModificationException
	public void calculateRewards1(User user) {
		/*List<VisitedLocation> userLocations = user.getVisitedLocations();*/// Ecrit comme ça a la base...
		//... et remplacé par une CopyOnWriteArrayList
		CopyOnWriteArrayList<VisitedLocation> userLocations = new CopyOnWriteArrayList<>();
		user.getVisitedLocations().forEach(visitedLocation -> userLocations.add(visitedLocation));
		// Cela résout le problème mais je me demande si ça ne crée pas des problèmes de performance (50 s pour réaliser le test)

		List<Attraction> attractions = gpsUtil.getAttractions();

		List<UserReward> rewards = user.getUserRewards(); // TODO a supprimer prochain RDV mentorat (04/10)

		List<UserReward> rewardsToAdd = new ArrayList<>();

		for(VisitedLocation visitedLocation : userLocations) { // Echanger l'ordre des boucles ne serait pas plus approprié?
            // On parcourt toutes les endroits visités par l'utilisateur uniquement quand la condition if est valide
            // if valid = quand le nom de l'attraction ne correspond à aucune des attractions visitées par l'utilisateur
			for(Attraction attraction : attractions ) {
				if(rewards.stream().filter(reward -> reward.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
					if(nearAttraction(visitedLocation, attraction)) {
						rewardsToAdd.add(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
						// Le problème Viens de getRewardPoints ci-dessus. Quand on le remplace par une valeur fixe, l'erreur est supprimée
					}
					// Je ne comprends pas pourquoi convertir userLocations (et pas attractions) en CopyOnWriteArrayList résout le problème, et que faire
					// la même chose avec attractions ne le résout pas. Ce n'est pas un problème d'ordre des boucles
				}
			}
		}
		rewardsToAdd.forEach(reward -> user.addUserReward(reward));
	}

	public void calculateRewards(User user) { // TODO: Methode réécrite, choisir la plus claire

		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();
		List<UserReward> rewards = user.getUserRewards();

		Map<Attraction,VisitedLocation> nearbyAttractions = new HashMap<>();
		for (VisitedLocation visitedLocation : userLocations){
			for (Attraction attraction: attractions){
				if (nearAttraction(visitedLocation, attraction)){
					nearbyAttractions.put(attraction, visitedLocation);

				}
			}
		}

		for (Map.Entry<Attraction,VisitedLocation> attraction : nearbyAttractions.entrySet()) {
			if(rewards.stream().filter(reward -> reward.attraction.attractionName.equals(attraction.getKey().attractionName)).count() == 0){
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

	private int getRewardPoints(Attraction attraction, User user) {
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