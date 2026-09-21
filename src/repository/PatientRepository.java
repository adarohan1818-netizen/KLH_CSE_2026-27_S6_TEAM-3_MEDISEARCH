package repository;

import models.Patient;
import java.util.ArrayList;
import java.util.List;

public class PatientRepository {
    private static final List<Patient> patients = new ArrayList<>();

    static {
        patients.add(new Patient("P001", "John Doe", 45, "Type 2 diabetes, Hypertension", "Metformin 500mg, Lisinopril 10mg", "HbA1c 7.5%, BP 140/90", "Last visit: 2023-01-15"));
        patients.add(new Patient("P002", "Jane Smith", 32, "Asthma, Allergic rhinitis", "Albuterol inhaler, Cetirizine", "Spirometry normal", "Last visit: 2023-03-22"));
        patients.add(new Patient("P003", "Robert Johnson", 58, "Coronary artery disease, Hyperlipidemia", "Atorvastatin 40mg, Aspirin 81mg", "LDL 130 mg/dL", "Last visit: 2023-02-10"));
        patients.add(new Patient("P004", "Emily Davis", 28, "Migraine headache", "Sumatriptan 50mg PRN", "MRI Brain normal", "Last visit: 2023-04-05"));
        patients.add(new Patient("P005", "Michael Brown", 65, "Osteoarthritis knee, mild fever", "Ibuprofen 400mg PRN, Paracetamol", "X-ray shows joint space narrowing", "Last visit: 2023-05-12"));
        patients.add(new Patient("P006", "Sarah Miller", 41, "Hypothyroidism", "Levothyroxine 75mcg", "TSH 4.2 mIU/L", "Last visit: 2023-01-30"));
        patients.add(new Patient("P007", "David Wilson", 50, "Type 2 diabetes, peripheral neuropathy", "Glipizide 5mg, Gabapentin 300mg", "HbA1c 8.1%", "Last visit: 2023-06-18"));
        patients.add(new Patient("P008", "Jessica Moore", 36, "Anxiety, tension headache", "Sertraline 50mg", "Normal blood panel", "Last visit: 2023-02-28"));
        patients.add(new Patient("P009", "William Taylor", 72, "Hypertension, chronic kidney disease", "Amlodipine 5mg", "Creatinine 1.5 mg/dL", "Last visit: 2023-07-01"));
        patients.add(new Patient("P010", "Linda Anderson", 22, "Acute bronchitis, fever", "Amoxicillin 500mg, Paracetamol", "Chest X-ray clear", "Last visit: 2023-07-15"));
    }

    public static List<Patient> getAllPatients() {
        return patients;
    }
}
