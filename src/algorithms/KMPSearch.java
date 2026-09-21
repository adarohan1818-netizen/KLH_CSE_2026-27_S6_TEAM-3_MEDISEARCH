package algorithms;

import models.Patient;
import repository.PatientRepository;
import java.util.ArrayList;
import java.util.List;

public class KMPSearch {

    // Time Complexity: O(N + M) where N is text length and M is pattern length
    // Space Complexity: O(M) for the LPS array
    public static List<Patient> search(String query) {
        List<Patient> results = new ArrayList<>();
        if (query == null || query.isEmpty()) return results;
        
        String pattern = query.toLowerCase();
        int[] lps = computeLPSArray(pattern);
        
        for (Patient patient : PatientRepository.getAllPatients()) {
            String text = patient.getFullRecordText();
            if (kmpSearch(pattern, text, lps)) {
                results.add(patient);
            }
        }
        return results;
    }
    
    private static boolean kmpSearch(String pattern, String text, int[] lps) {
        int m = pattern.length();
        int n = text.length();
        
        int i = 0; // index for text
        int j = 0; // index for pattern
        
        while ((n - i) >= (m - j)) {
            if (pattern.charAt(j) == text.charAt(i)) {
                j++;
                i++;
            }
            if (j == m) {
                return true; // Match found
            } else if (i < n && pattern.charAt(j) != text.charAt(i)) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }
        return false;
    }

    private static int[] computeLPSArray(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];
        int len = 0;
        int i = 1;
        lps[0] = 0;
        
        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = len;
                    i++;
                }
            }
        }
        return lps;
    }
}
