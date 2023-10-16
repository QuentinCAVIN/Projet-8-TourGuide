package com.openclassrooms.tourguide.model.user;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import gpsUtil.location.VisitedLocation;
import tripPricer.Provider;

public class User {
	private final UUID userId;
	private final String userName;
	private String phoneNumber;
	private String emailAddress;
	private Date latestLocationTimestamp;
	private List<VisitedLocation> visitedLocations = new ArrayList<>();
	private List<UserReward> userRewards = new ArrayList<>();
	private UserPreferences userPreferences = new UserPreferences();
	private List<Provider> tripDeals = new ArrayList<>();
	public User(UUID userId, String userName, String phoneNumber, String emailAddress) {
		this.userId = userId;
		this.userName = userName;
		this.phoneNumber = phoneNumber;
		this.emailAddress = emailAddress;
	}
	
	public UUID getUserId() {
		return userId;
	}
	
	public String getUserName() {
		return userName;
	}
	
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	
	public String getEmailAddress() {
		return emailAddress;
	}
	
	public void setLatestLocationTimestamp(Date latestLocationTimestamp) {
		this.latestLocationTimestamp = latestLocationTimestamp;
	}
	
	public Date getLatestLocationTimestamp() {
		return latestLocationTimestamp;
	}
	
	public void addToVisitedLocations(VisitedLocation visitedLocation) {
		visitedLocations.add(visitedLocation);
	}
	
	public List<VisitedLocation> getVisitedLocations() {
		return visitedLocations;
	}
	
	public void clearVisitedLocations() {
		visitedLocations.clear();
	}

	//TODO: méthode modifié, comparais un String avec un Attraction + !. J'ai l'impression toutefois
	// que cette méthode fait doublon avec la condition if de calculateReward.
	// cette méthode est uniquement appelée à ce moment la. Attendre confirmation avant de supprimer
	// EDIT : quand je la supprime une meme reward est ajouté 2 fois.... POURQUOI?!
	// EDIT de l'EDIT: maitenant que j'ai modifié mes methode en ajoutant des CompletableFuture, une meme reward
	// n'est plus ajouté 2 fois.
	public void addUserReward(UserReward userReward) {
		//TODO pour moi la condition if fait toujours doublon avec la condition if de la methode ou elle est utilisé
		// elle n'est n'est plus utile ici. A confirmer avec Vincent
		// Créé une CurrentModificationException. Pour résoudre le probléme soit je vire la condition, soit je modifie
		// l'attribut UserRewards de list vers CopyOnWriteArrayList
		//if (userRewards.stream().noneMatch(reward -> reward.attraction.attractionName.equals(userReward.attraction.attractionName))) {
			//if(userRewards.stream().filter(r -> r.attraction.attractionName.equals(userReward.attraction)).count() == 0) {
			userRewards.add(userReward);
		}
	//}
	
	public List<UserReward> getUserRewards() {
		return userRewards;
	}
	
	public UserPreferences getUserPreferences() {
		return userPreferences;
	}
	
	public void setUserPreferences(UserPreferences userPreferences) {
		this.userPreferences = userPreferences;
	}

	public VisitedLocation getLastVisitedLocation() {
		return visitedLocations.get(visitedLocations.size() - 1);
	}
	
	public void setTripDeals(List<Provider> tripDeals) {
		this.tripDeals = tripDeals;
	}
	
	public List<Provider> getTripDeals() {
		return tripDeals;
	}

}
