package algorithms;

import models.Patient;
import repository.PatientRepository;
import java.util.ArrayList;
import java.util.List;

public class RabinKarpSearch {
    public final static int d = 256; // Number of characters in the input alphabet

    // Time Complexity: Average O(N + M), Worst case O(N * M)
    // Space Complexity: O(1)
    public static List<Patient> search(String query) {
        List<Patient> results = new ArrayList<>();
        if (query == null || query.isEmpty()) return results;

        String pattern = query.toLowerCase();
        int q = 101; // A prime number
        
        for (Patient patient : PatientRepository.getAllPatients()) {
            String text = patient.getFullRecordText();
            if (rabinKarpSearch(pattern, text, q)) {
                results.add(patient);
            }
        }
        return results;
    }

    private static boolean rabinKarpSearch(String pattern, String text, int q) {
        int m = pattern.length();
        int n = text.length();
        int i, j;
        int p = 0; // hash value for pattern
        int t = 0; // hash value for txt
        int h = 1;

        if (m > n) return false;

        // The value of h would be "pow(d, M-1)%q"
        for (i = 0; i < m - 1; i++)
            h = (h * d) % q;

        // Calculate the hash value of pattern and first window of text
        for (i = 0; i < m; i++) {
            p = (d * p + pattern.charAt(i)) % q;
            t = (d * t + text.charAt(i)) % q;
        }

        // Slide the pattern over text one by one
        for (i = 0; i <= n - m; i++) {
            // Check the hash values of current window of text
            // and pattern. If the hash values match then only
            // check for characters one by one
            if (p == t) {
                // Check for characters one by one
                for (j = 0; j < m; j++) {
                    if (text.charAt(i + j) != pattern.charAt(j))
                        break;
                }

                // if p == t and pattern[0...M-1] = text[i, i+1, ...i+M-1]
                if (j == m)
                    return true;
            }

            // Calculate hash value for next window of text: Remove
            // leading digit, add trailing digit
            if (i < n - m) {
                t = (d * (t - text.charAt(i) * h) + text.charAt(i + m)) % q;

                // We might get negative value of t, converting it
                // to positive
                if (t < 0)
                    t = (t + q);
            }
        }
        return false;
    }
}
