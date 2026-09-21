package models;

public class Patient {
    private String id;
    private String name;
    private int age;
    private String diagnosis;
    private String prescription;
    private String labReport;
    private String appointmentHistory;

    public Patient(String id, String name, int age, String diagnosis, String prescription, String labReport, String appointmentHistory) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.labReport = labReport;
        this.appointmentHistory = appointmentHistory;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getDiagnosis() { return diagnosis; }
    public String getPrescription() { return prescription; }
    public String getLabReport() { return labReport; }
    public String getAppointmentHistory() { return appointmentHistory; }

    // Helper method to convert patient to JSON without using external libraries
    public String toJson() {
        return "{" +
                "\"id\": \"" + escapeJson(id) + "\"," +
                "\"name\": \"" + escapeJson(name) + "\"," +
                "\"age\": " + age + "," +
                "\"diagnosis\": \"" + escapeJson(diagnosis) + "\"," +
                "\"prescription\": \"" + escapeJson(prescription) + "\"," +
                "\"labReport\": \"" + escapeJson(labReport) + "\"," +
                "\"appointmentHistory\": \"" + escapeJson(appointmentHistory) + "\"" +
                "}";
    }

    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
    
    // Method used for full-text search across all text fields
    public String getFullRecordText() {
        return (id + " " + name + " " + diagnosis + " " + prescription + " " + labReport + " " + appointmentHistory).toLowerCase();
    }
}
