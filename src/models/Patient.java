package models;

/**
 * One patient record.
 *
 * Each Patient is stored on disk as a single structured text file inside
 * data/patients/ (for example data/patients/patient_001.txt). The fields below
 * map one-to-one to the "Key: Value" lines of that file.
 */
public class Patient {

    private String patientId;
    private String name;
    private int age;
    private String gender;
    private String bloodGroup;
    private String diagnosis;
    private String symptoms;
    private String medications;
    private String labReports;
    private String appointments;

    public Patient(String patientId, String name, int age, String gender, String bloodGroup,
                   String diagnosis, String symptoms, String medications,
                   String labReports, String appointments) {
        this.patientId = safe(patientId);
        this.name = safe(name);
        this.age = age;
        this.gender = safe(gender);
        this.bloodGroup = safe(bloodGroup);
        this.diagnosis = safe(diagnosis);
        this.symptoms = safe(symptoms);
        this.medications = safe(medications);
        this.labReports = safe(labReports);
        this.appointments = safe(appointments);
    }

    /** Replaces null with an empty string so nothing downstream throws NPE. */
    private static String safe(String value) {
        return value == null ? "" : value;
    }

    // ------------------------------------------------------------------ GETTERS

    public String getPatientId() { return patientId; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getBloodGroup() { return bloodGroup; }
    public String getDiagnosis() { return diagnosis; }
    public String getSymptoms() { return symptoms; }
    public String getMedications() { return medications; }
    public String getLabReports() { return labReports; }
    public String getAppointments() { return appointments; }

    // ------------------------------------------------------- SEARCH TEXT / OUTPUT

    /**
     * The complete textual record that Aho-Corasick scans.
     *
     * Every clinical text field is concatenated into one string and normalised to
     * lowercase, so the search is case-insensitive: searching for "Diabetes"
     * matches a record that stores "diabetes".
     */
    public String getFullRecordText() {
        StringBuilder text = new StringBuilder();
        text.append(patientId).append(' ')
            .append(name).append(' ')
            .append(diagnosis).append(' ')
            .append(symptoms).append(' ')
            .append(medications).append(' ')
            .append(labReports).append(' ')
            .append(appointments);

        return toAsciiLowerCase(text.toString());
    }

    /**
     * Lowercases text and drops characters outside the supported 256-character
     * alphabet, so the Aho-Corasick automaton can index every character directly.
     */
    public static String toAsciiLowerCase(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase();
        StringBuilder cleaned = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            cleaned.append(c < 256 ? c : ' ');
        }
        return cleaned.toString();
    }

    /**
     * Renders this patient as the structured text written to its data file.
     * Keeping the format in one place means reading and writing cannot drift apart.
     */
    public String toFileText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Patient ID: ").append(patientId).append('\n');
        sb.append("Name: ").append(name).append('\n');
        sb.append("Age: ").append(age).append('\n');
        sb.append("Gender: ").append(gender).append('\n');
        sb.append("Blood Group: ").append(bloodGroup).append('\n');
        sb.append("Diagnosis: ").append(diagnosis).append('\n');
        sb.append("Symptoms: ").append(symptoms).append('\n');
        sb.append("Medications: ").append(medications).append('\n');
        sb.append("Lab Reports: ").append(labReports).append('\n');
        sb.append("Appointments: ").append(appointments).append('\n');
        return sb.toString();
    }

    /** Hand-rolled JSON (the project deliberately avoids external JSON libraries). */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append('{')
            .append("\"patientId\": \"").append(escapeJson(patientId)).append("\", ")
            .append("\"name\": \"").append(escapeJson(name)).append("\", ")
            .append("\"age\": ").append(age).append(", ")
            .append("\"gender\": \"").append(escapeJson(gender)).append("\", ")
            .append("\"bloodGroup\": \"").append(escapeJson(bloodGroup)).append("\", ")
            .append("\"diagnosis\": \"").append(escapeJson(diagnosis)).append("\", ")
            .append("\"symptoms\": \"").append(escapeJson(symptoms)).append("\", ")
            .append("\"medications\": \"").append(escapeJson(medications)).append("\", ")
            .append("\"labReports\": \"").append(escapeJson(labReports)).append("\", ")
            .append("\"appointments\": \"").append(escapeJson(appointments)).append("\"")
            .append('}');
        return json.toString();
    }

    /** Escapes the characters that would otherwise break the JSON output. */
    public static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\r", "")
                    .replace("\n", "\\n")
                    .replace("\t", " ");
    }
}
