package models;

import java.util.ArrayList;
import java.util.List;

/**
 * One search hit: a patient whose record matched, plus exactly which keywords
 * were found in that record.
 *
 * Produced by MediSearchService for every patient that Aho-Corasick matched.
 */
public class SearchResult {

    private final Patient patient;
    private final List<String> matchedKeywords;

    public SearchResult(Patient patient, List<String> matchedKeywords) {
        this.patient = patient;
        this.matchedKeywords = matchedKeywords == null
                ? new ArrayList<String>()
                : new ArrayList<String>(matchedKeywords);
    }

    public Patient getPatient() {
        return patient;
    }

    public List<String> getMatchedKeywords() {
        return matchedKeywords;
    }

    /** Number of distinct keywords that matched in this patient's record. */
    public int getMatchCount() {
        return matchedKeywords.size();
    }

    /** Hand-rolled JSON, matching Patient.toJson() for the nested patient object. */
    public String toJson() {
        StringBuilder keywords = new StringBuilder("[");
        for (int i = 0; i < matchedKeywords.size(); i++) {
            if (i > 0) {
                keywords.append(", ");
            }
            keywords.append('"').append(Patient.escapeJson(matchedKeywords.get(i))).append('"');
        }
        keywords.append(']');

        return "{\"patient\": " + patient.toJson()
                + ", \"matchedKeywords\": " + keywords + "}";
    }
}
