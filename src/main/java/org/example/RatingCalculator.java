package org.example;

public class RatingCalculator {
    /**
     * Calculates the average rating from an array of ratings.
     *
     * @param ratings an array of ratings
     * @return the average rating, or 0 if the input array is null or empty
     */
    public static double calculateAverageRating(int[] ratings) {
        if (ratings == null || ratings.length == 0) {
            return 0;
        }
        int sum = 0;
        for (int rating : ratings) {
            sum += rating;
        }
        return (double) sum / ratings.length;
    }
}