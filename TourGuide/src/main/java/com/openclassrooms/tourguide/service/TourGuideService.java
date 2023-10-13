package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.model.attraction.AttractionInfo;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.model.user.User;
import com.openclassrooms.tourguide.model.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	ExecutorService executorService = Executors.newFixedThreadPool(500);

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}


	//TODO: A VOIR AVEC VINCENT
	//l'utilisation d'un CompletableFuture dans trackUserLocation à simplement déplacé le délai d'exécution ici,
	//même si c'est atténué par l'utilisation d'un opérateur ternaire.
	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
				: trackUserLocation(user).join();
		return visitedLocation;
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}


	//TODO: methode utile pour me rappeler de mon cheminement, à effacer pour la soutenance.
	public VisitedLocation trackUserLocationOrigin(User user) {
		// Ici les deux lignes qui créent des ralentissements ne peuvent pas être lancés de manière asynchrone :
		// calculateReward(user) doit attendre que visitedLocation soit ajouté à user avant d'être lancé.
		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());// Lent
		user.addToVisitedLocations(visitedLocation);
		rewardsService.calculateRewards(user);//Lent
		return visitedLocation;
	}

	//Methode précédente encapsulée dans un CompletableFuture, lancé de manière asynchrone
	// grâce aux threads d'executorService (attribut de la Classe).
	// Cette solution oblige à modifier les méthodes qui font appelle à trackUserLocation qui renvoie CompletableFuture
	public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
		return CompletableFuture.supplyAsync(() ->
						gpsUtil.getUserLocation(user.getUserId()), executorService)
				.thenApply(visitedLocation -> {
					user.addToVisitedLocations(visitedLocation);
					rewardsService.calculateRewards(user);
					return visitedLocation;
				} );
	}

    // TODO Methode a effacer, sauf si l'utilisation d'une map dans le controller est plus adaptée
    public Map<Double, Attraction> getNearByAttractions2(VisitedLocation visitedLocation) {
        Map<Double, Attraction> distanceFromAttractions = new TreeMap<>();
        Map<Double, Attraction> fiveClosestAttractions = new HashMap<>();
        for (Attraction attraction : gpsUtil.getAttractions()) {
            {
                double distanceFromAttraction = rewardsService.getDistance(attraction, visitedLocation.location);
                distanceFromAttractions.put(distanceFromAttraction, attraction);
                //RewardService.getDistance va donner la distance qui sépare deux points en miles
            }

        }
        Iterator<Map.Entry<Double, Attraction>> iterator = distanceFromAttractions.entrySet().iterator();

        for (int i = 0; i < 5 && iterator.hasNext(); i++) {
            Map.Entry<Double, Attraction> entry = iterator.next();
            fiveClosestAttractions.put(entry.getKey(), entry.getValue());
        }
        return fiveClosestAttractions;
    }

    public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
        Map<Attraction, Double> attractionDistance = new HashMap<>();

        for (Attraction attraction : gpsUtil.getAttractions()) {
            {
                double distanceFromAttraction = rewardsService.getDistance(attraction, visitedLocation.location);
                attractionDistance.put(attraction, distanceFromAttraction);
                //RewardService.getDistance va donner la distance qui sépare deux points en miles
            }
        }

        ///////////////  Solution de tri trouvé sur stackOverFlow
        Map<Attraction, Double> sortedAttractionDistance = attractionDistance.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, // Crée une référence à la méthode getKey de chaque entrée du stream?
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));// ??????
        //////////////

        ////////////// Solution de moi, voir si ça fait bien la meme chose
        Map<Attraction, Double> sortedAttractionDistance2 = new LinkedHashMap<>();
                attractionDistance.entrySet().stream().sorted(Map.Entry.comparingByValue())
                .forEach(entry -> sortedAttractionDistance2.put(entry.getKey(),entry.getValue()));
        /////////////
        //TODO : Les deux solutions on l'air de marcher, vérifier avec Vincent, effacer la moins intéressante


        List<Attraction> ListAttractionsByDistance = sortedAttractionDistance.keySet().stream().limit(5).collect(Collectors.toList());
        return ListAttractionsByDistance;

		//TODO: Garder dans un coin de la tête qu'il pourrait être utile de modifier la sortie de la methode
		// pour qu'elle renvoie une Map. La nouvelle fonctionalitée a implémenter dans le controller en serait facilitée
    }

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	// TODO: Methode ajouté et non testée. Ajouter un test?
	public AttractionInfo attractionInfoBuilder (Attraction attraction, User user){

		String attractionName = attraction.attractionName;
		Location attractionLocation = new Location(attraction.latitude,attraction.longitude);
		Location userLocation = getUserLocation(user).location;
		double distanceInMiles = rewardsService.getDistance(attractionLocation, userLocation);
		int rewardPoint = rewardsService.getRewardPoints(attraction,user);

		return new AttractionInfo(attractionName,attractionLocation,distanceInMiles,rewardPoint);
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}