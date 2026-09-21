package algorithms;

import models.Patient;
import repository.PatientRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class EditDistance {

    // Time Complexity: O(N * M) where N and M are the lengths of the two strings
    // Space Complexity: O(N * M) for the DP table (can be optimized to O(min(N, M)))
    public static int calculate(String s1, String s2) {
        if (s1 == null || s2 == null) return -1;
        
        int m = s1.length();
        int n = s2.length();
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                // If first string is empty, only option is to insert all characters of second string
                if (i == 0) {
                    dp[i][j] = j;
                }
                // If second string is empty, only option is to remove all characters of second string
                else if (j == 0) {
                    dp[i][j] = i;
                }
                // If last characters are same, ignore last char and recur for remaining string
                else if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                }
                // If last character are different, consider all possibilities and find minimum
                else {
                    dp[i][j] = 1 + min(dp[i][j - 1],      // Insert
                                     dp[i - 1][j],      // Remove
                                     dp[i - 1][j - 1]); // Replace
                }
            }
        }
        return dp[m][n];
    }
    
    private static int min(int x, int y, int z) {
        return Math.min(Math.min(x, y), z);
    }

    public static List<SimilarityResult> findSimilarCases(String query, int maxDistanceThreshold) {
        List<SimilarityResult> results = new ArrayList<>();
        if (query == null || query.isEmpty()) return results;
        
        String q = query.toLowerCase();
        
        for (Patient patient : PatientRepository.getAllPatients()) {
            String target = patient.getDiagnosis().toLowerCase();
            int distance = calculate(q, target);
            
            if (distance <= maxDistanceThreshold) {
                String status = distance == 0 ? "Exact Match" : "Similar Case";
                results.add(new SimilarityResult(patient, distance, status));
            }
        }
        
        // Sort results by distance (closest first)
        Collections.sort(results, (a, b) -> Integer.compare(a.distance, b.distance));
        return results;
    }

    public static class SimilarityResult {
        public Patient patient;
        public int distance;
        public String status;

        public SimilarityResult(Patient p, int d, String s) {
            this.patient = p;
            this.distance = d;
            this.status = s;
        }
        
        public String toJson() {
            return "{" +
                   "\"patient\": " + patient.toJson() + ", " +
                   "\"distance\": " + distance + ", " +
                   "\"status\": \"" + status + "\"" +
                   "}";
        }
    }
}
