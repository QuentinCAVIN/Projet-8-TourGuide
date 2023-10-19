package com.openclassrooms.tourguide;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.model.user.User;

public class TestPerformance {

    /*
     * A note on performance improvements:
     *
     * The number of users generated for the high volume tests can be easily
     * adjusted via this method:
     *
     * InternalTestHelper.setInternalUserNumber(100000);
     *
     *
     * These tests can be modified to suit new solutions, just as long as the
     * performance metrics at the end of the tests remains consistent.
     *
     * These are performance metrics that we are trying to hit:
     *
     * highVolumeTrackLocation: 100,000 users within 15 minutes:
     * assertTrue(TimeUnit.MINUTES.toSeconds(15) >=
     * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     *
     * highVolumeGetRewards: 100,000 users within 20 minutes:
     * assertTrue(TimeUnit.MINUTES.toSeconds(20) >=
     * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     */

    @Disabled // Pour tester la mise en place du pipeline d'intégration continue
    @Test
    public void highVolumeTrackLocation() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
        // Users should be incremented up to 100,000, and test finishes within 15
        // minutes
        InternalTestHelper.setInternalUserNumber(100000);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

        List<User> allUsers = tourGuideService.getAllUsers();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        List<CompletableFuture<VisitedLocation>> futures = new ArrayList<>();

        for (User user : allUsers) {
            futures.add(tourGuideService.trackUserLocation(user));
        }

        CompletableFuture<Void> combinedFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        combinedFutures.join();
        // Avec .allOf on regroupe tout les futures généré par trackUserLocation
        // et on attend qu'ils se terminent avec .join

        stopWatch.stop();
        tourGuideService.tracker.stopTracking();

        System.out.println("highVolumeTrackLocation: Time Elapsed: "
                + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
        assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
    }

    @Disabled // Pour tester la mise en place du pipeline d'intégration continue
    @Test
    public void highVolumeGetRewards() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

        // Users should be incremented up to 100,000, and test finishes within 20
        // minutes
        InternalTestHelper.setInternalUserNumber(100000);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

        Attraction attraction = gpsUtil.getAttractions().get(0);
        List<User> allUsers = tourGuideService.getAllUsers();
        allUsers.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));
        List<CompletableFuture<Void>> futuresCalculateRewards = new ArrayList<>();
        allUsers.forEach(u -> futuresCalculateRewards.add(rewardsService.calculateRewards(u)));

		/*ExecutorService executorService = rewardsService.getExecutorService();
		executorService.shutdown();
		while (!executorService.isTerminated() ){}*/

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futuresCalculateRewards.toArray(new CompletableFuture[0]));
        combinedFuture.join();

        for (User user : allUsers) {
            assertTrue(user.getUserRewards().size() > 0);
        }
        stopWatch.stop();
        tourGuideService.tracker.stopTracking();

        System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime())
                + " seconds.");
        assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
    }


   /* public class QuentinTest {

        @Test
        public void testNonBlocking() {
            Instant begin =
                    Instant.now();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            System.out.printf("BEGIN %s\n",
                    Instant.now
                            ().toString());

            for (int i = 0; i < 20; i++) {
                futures.add(calculateRewardNonBlocking());
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            Instant end =
                    Instant.now
                            ();
            System.out.printf("END %s\n",
                    Instant.now
                            ().toString());
            System.out.printf("DURATION %s\n", Duration.between(begin, end));
        }

        private CompletableFuture<Void> calculateRewardNonBlocking() {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Long timeout = (long) (Math.random() * 1000);
                    System.out.printf("Timeout of : %s\n", timeout);
                    Thread.sleep((long) (Math.random() * 1000));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("FIN DU TIMEOUT");
                return null;
            });
        }

        @Test
        public void testBlocking() {
            Instant begin =
                    Instant.now
                            ();
            System.out.printf("BEGIN %s\n",
                    Instant.now
                            ().toString());
            for (int i = 0; i < 20; i++) {
                calculateRewardNonBlocking().join();
            }
            Instant end =
                    Instant.now
                            ();
            System.out.printf("END %s\n",
                    Instant.now
                            ().toString());
            System.out.printf("DURATION %s\n", Duration.between(begin, end));
        }


    } */

}
