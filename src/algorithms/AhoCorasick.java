package algorithms;

import models.Patient;
import repository.PatientRepository;

import java.util.*;

public class AhoCorasick {
    private static final int MAX_STATES = 1000;
    private static final int MAX_CHARS = 256;
    private int[] out = new int[MAX_STATES];
    private int[] f = new int[MAX_STATES];
    private int[][] g = new int[MAX_STATES][MAX_CHARS];
    private int states = 1;
    private String[] words;

    // Time Complexity: O(N + M + Z) where N is text length, M is sum of lengths of all keywords, Z is number of matches
    // Space Complexity: O(MAX_STATES * MAX_CHARS)
    public AhoCorasick(String[] arr) {
        this.words = arr;
        for (int i = 0; i < MAX_STATES; i++) {
            Arrays.fill(g[i], -1);
        }
        Arrays.fill(out, 0);
        buildMatchingMachine(arr);
    }

    private void buildMatchingMachine(String[] arr) {
        int k = arr.length;
        for (int i = 0; i < k; ++i) {
            String word = arr[i];
            int currentState = 0;
            for (int j = 0; j < word.length(); ++j) {
                int ch = word.charAt(j);
                if (g[currentState][ch] == -1) {
                    g[currentState][ch] = states++;
                }
                currentState = g[currentState][ch];
            }
            out[currentState] |= (1 << i);
        }

        for (int ch = 0; ch < MAX_CHARS; ++ch) {
            if (g[0][ch] == -1) {
                g[0][ch] = 0;
            }
        }

        Arrays.fill(f, -1);
        Queue<Integer> q = new LinkedList<>();
        for (int ch = 0; ch < MAX_CHARS; ++ch) {
            if (g[0][ch] != 0) {
                f[g[0][ch]] = 0;
                q.add(g[0][ch]);
            }
        }

        while (!q.isEmpty()) {
            int state = q.poll();
            for (int ch = 0; ch < MAX_CHARS; ++ch) {
                if (g[state][ch] != -1) {
                    int failure = f[state];
                    while (g[failure][ch] == -1) {
                        failure = f[failure];
                    }
                    failure = g[failure][ch];
                    f[g[state][ch]] = failure;
                    out[g[state][ch]] |= out[failure];
                    q.add(g[state][ch]);
                }
            }
        }
    }

    private int findNextState(int currentState, char nextInput) {
        int answer = currentState;
        int ch = nextInput;
        while (g[answer][ch] == -1) {
            answer = f[answer];
        }
        return g[answer][ch];
    }

    public List<String> searchInText(String text) {
        List<String> matchedWords = new ArrayList<>();
        int currentState = 0;
        Set<Integer> foundIndices = new HashSet<>();
        
        for (int i = 0; i < text.length(); ++i) {
            currentState = findNextState(currentState, text.charAt(i));
            if (out[currentState] == 0) continue;
            
            for (int j = 0; j < words.length; ++j) {
                if ((out[currentState] & (1 << j)) > 0) {
                    foundIndices.add(j);
                }
            }
        }
        
        for (int idx : foundIndices) {
            matchedWords.add(words[idx]);
        }
        return matchedWords;
    }

    public static List<SearchResult> search(String[] keywords) {
        List<SearchResult> results = new ArrayList<>();
        if (keywords == null || keywords.length == 0) return results;

        // Convert to lowercase
        String[] lowerKeywords = new String[keywords.length];
        for (int i = 0; i < keywords.length; i++) {
            lowerKeywords[i] = keywords[i].trim().toLowerCase();
        }

        AhoCorasick ac = new AhoCorasick(lowerKeywords);

        for (Patient patient : PatientRepository.getAllPatients()) {
            String text = patient.getFullRecordText();
            List<String> found = ac.searchInText(text);
            if (!found.isEmpty()) {
                results.add(new SearchResult(patient, found));
            }
        }
        return results;
    }

    public static class SearchResult {
        public Patient patient;
        public List<String> foundKeywords;

        public SearchResult(Patient p, List<String> f) {
            this.patient = p;
            this.foundKeywords = f;
        }
        
        public String toJson() {
            StringBuilder keywordsJson = new StringBuilder("[");
            for (int i = 0; i < foundKeywords.size(); i++) {
                keywordsJson.append("\"").append(foundKeywords.get(i)).append("\"");
                if (i < foundKeywords.size() - 1) keywordsJson.append(", ");
            }
            keywordsJson.append("]");
            
            return "{" +
                   "\"patient\": " + patient.toJson() + ", " +
                   "\"foundKeywords\": " + keywordsJson.toString() +
                   "}";
        }
    }
}
