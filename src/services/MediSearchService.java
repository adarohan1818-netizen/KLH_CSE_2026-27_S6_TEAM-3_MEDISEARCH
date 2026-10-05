package services;

import algorithms.AhoCorasick;
import models.Patient;
import models.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SEARCH LAYER - the bridge between the file storage and the Aho-Corasick engine.
 *
 * Responsibilities:
 *   1. Sanitise the raw keyword text typed by the user (split, trim, lowercase,
 *      remove blanks, remove duplicates).
 *   2. Build a single Aho-Corasick automaton from ALL keywords at once.
 *   3. Scan every patient record from data/patients/ and collect the matches.
 *
 * This is the class that proves the project uses Aho-Corasick for the real work:
 * all keywords are matched in one single pass per record, not by calling
 * contains() / indexOf() / one KMP run per keyword.
 */
public class MediSearchService {

    private final PatientFileService fileService;

    public MediSearchService(PatientFileService fileService) {
        this.fileService = fileService;
    }

    /** All patient records currently stored on disk. */
    public List<Patient> getAllPatients() {
        return fileService.loadAllPatients();
    }

    /** Number of record files on disk. */
    public int getTotalRecords() {
        return fileService.countRecordFiles();
    }

    /**
     * Searches every patient record for all supplied keywords using
     * Aho-Corasick.
     *
     * @param rawKeywords comma separated keywords, e.g. "fever, cough, diabetes".
     * @return one SearchResult per matching patient, with the matched keywords.
     *         Empty list when no record matches.
     * @throws IllegalArgumentException when no usable keyword was supplied, or
     *         when more keywords are given than the bitmask can represent.
     */
    public List<SearchResult> search(String rawKeywords) {
        String[] keywords = sanitizeKeywords(rawKeywords);

        if (keywords.length == 0) {
            throw new IllegalArgumentException(
                    "No keywords entered. Please type at least one keyword, separated by commas.");
        }

        // Build ONE automaton for all keywords: Trie + failure links + output masks.
        AhoCorasick automaton = new AhoCorasick(keywords);

        List<Patient> patients = fileService.loadAllPatients();
        List<SearchResult> results = new ArrayList<SearchResult>();

        // Single pass per record: every keyword is detected simultaneously.
        for (Patient patient : patients) {
            List<String> matched = automaton.searchInText(patient.getFullRecordText());
            if (!matched.isEmpty()) {
                results.add(new SearchResult(patient, matched));
            }
        }

        System.out.println("[MediSearchService] Keywords=" + keywords.length
                + ", Trie states=" + automaton.getStateCount()
                + ", Records scanned=" + patients.size()
                + ", Matched patients=" + results.size());

        return results;
    }

    /**
     * Cleans up raw user input into a usable keyword array.
     *
     * Handles: commas, extra whitespace, uppercase/lowercase differences,
     * empty entries (e.g. "fever,,cough") and duplicate keywords.
     *
     * @return lowercased, trimmed, de-duplicated keywords.
     */
    public static String[] sanitizeKeywords(String rawKeywords) {
        if (rawKeywords == null) {
            return new String[0];
        }

        String[] parts = rawKeywords.split(",");

        // LinkedHashSet removes duplicates while keeping the user's order.
        Set<String> unique = new LinkedHashSet<String>();
        for (String part : parts) {
            String keyword = collapseSpaces(Patient.toAsciiLowerCase(part));

            if (keyword.isEmpty()) {
                continue;                          // skip "fever,,cough" style blanks
            }
            unique.add(keyword);
        }

        return unique.toArray(new String[unique.size()]);
    }

    /**
     * Adds a new patient record.
     *
     * The ID is generated from the files already on disk, so adding a patient
     * creates a brand new file in data/patients/.
     *
     * @throws IllegalArgumentException when required fields are missing or invalid.
     * @throws IOException              when the record could not be written.
     */
    public Patient addPatient(String name, String ageText, String gender, String bloodGroup,
                              String diagnosis, String symptoms, String medications,
                              String labReports, String appointments)
            throws IOException {

        if (isBlank(name)) {
            throw new IllegalArgumentException("Patient name is required.");
        }
        if (isBlank(diagnosis)) {
            throw new IllegalArgumentException("Diagnosis is required.");
        }

        int age = 0;
        if (!isBlank(ageText)) {
            try {
                age = Integer.parseInt(ageText.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Age must be a whole number, for example 45.");
            }
            if (age < 0 || age > 130) {
                throw new IllegalArgumentException("Age must be between 0 and 130.");
            }
        }

        String patientId = fileService.generateNextPatientId();

        Patient patient = new Patient(
                patientId,
                name.trim(),
                age,
                orNotRecorded(gender),
                orNotRecorded(bloodGroup),
                diagnosis.trim(),
                orNotRecorded(symptoms),
                orNotRecorded(medications),
                orNotRecorded(labReports),
                orNotRecorded(appointments));

        fileService.savePatient(patient);
        return patient;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Trims the text and collapses runs of whitespace into single spaces, so
     * "chest    pain" still matches "chest pain".
     *
     * Done with a plain loop rather than a regular expression, because the whole
     * project avoids regex (Aho-Corasick does the real matching work).
     */
    private static String collapseSpaces(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder collapsed = new StringBuilder(text.length());
        boolean previousWasSpace = true;           // also strips leading whitespace
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isSpace = (c == ' ' || c == '\t' || c == '\n' || c == '\r');
            if (isSpace) {
                if (!previousWasSpace) {
                    collapsed.append(' ');
                }
                previousWasSpace = true;
            } else {
                collapsed.append(c);
                previousWasSpace = false;
            }
        }
        // Drop the trailing space that a run of whitespace may have left behind.
        int length = collapsed.length();
        if (length > 0 && collapsed.charAt(length - 1) == ' ') {
            collapsed.setLength(length - 1);
        }
        return collapsed.toString();
    }

    private static String orNotRecorded(String value) {
        return isBlank(value) ? "Not recorded" : value.trim();
    }
}
