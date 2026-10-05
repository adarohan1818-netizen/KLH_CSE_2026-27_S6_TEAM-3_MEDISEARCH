package services;

import models.Patient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * FILE HANDLING LAYER.
 *
 * This class is the only place in MediSearch that touches the disk. Patient
 * records live as plain .txt files in data/patients/, one file per patient:
 *
 *     data/patients/patient_001.txt
 *     data/patients/patient_002.txt
 *     ...
 *
 * There is deliberately no database anywhere in this project. Reading and
 * writing use only the standard java.io classes (File, FileReader/BufferedReader,
 * FileWriter/BufferedWriter), which is exactly what is expected from a file
 * handling based DSA project.
 *
 * File format (one "Key: Value" pair per line):
 *
 *     Patient ID: P001
 *     Name: Rahul Sharma
 *     Age: 45
 *     Gender: Male
 *     Blood Group: B+
 *     Diagnosis: Diabetes and hypertension
 *     Symptoms: fatigue, headache, increased thirst
 *     Medications: Metformin
 *     Lab Reports: Blood glucose elevated
 *     Appointments: 2026-09-10
 */
public class PatientFileService {

    /** Folder holding all patient records, relative to the project root. */
    public static final String DEFAULT_DATA_DIRECTORY = "data/patients";

    private final File dataDirectory;

    public PatientFileService() {
        this(DEFAULT_DATA_DIRECTORY);
    }

    public PatientFileService(String dataDirectoryPath) {
        this.dataDirectory = new File(dataDirectoryPath);
    }

    /** The folder being used for patient records. */
    public File getDataDirectory() {
        return dataDirectory;
    }

    /**
     * Creates data/patients/ if it does not exist yet, so the application runs
     * correctly on a fresh clone.
     */
    public boolean ensureDataDirectoryExists() {
        if (dataDirectory.exists()) {
            return dataDirectory.isDirectory();
        }
        return dataDirectory.mkdirs();
    }

    // ------------------------------------------------------------------- READING

    /**
     * Reads every .txt file in data/patients/ and parses it into a Patient.
     *
     * A malformed file is skipped (with a message on the Java console) instead of
     * crashing the application, so one bad record cannot take down the demo.
     *
     * @return all successfully parsed patients, sorted by file name.
     */
    public List<Patient> loadAllPatients() {
        List<Patient> patients = new ArrayList<Patient>();

        if (!ensureDataDirectoryExists()) {
            System.err.println("[PatientFileService] Could not create data directory: "
                    + dataDirectory.getAbsolutePath());
            return patients;
        }

        File[] files = dataDirectory.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("[PatientFileService] Patient directory is empty: "
                    + dataDirectory.getAbsolutePath());
            return patients;
        }

        // Sort so that P001 always comes before P002 in the UI.
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });

        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase().endsWith(".txt")) {
                continue;                          // ignore anything that is not a record
            }

            Patient patient = parsePatientFile(file);
            if (patient != null) {
                patients.add(patient);
            }
        }

        return patients;
    }

    /**
     * Parses one patient file using BufferedReader + FileReader.
     *
     * The parser is tolerant: unknown lines are ignored, missing fields fall back
     * to sensible defaults, and a non-numeric age becomes 0 with a warning.
     *
     * @return the parsed Patient, or null if the file could not be read at all.
     */
    public Patient parsePatientFile(File file) {
        String patientId = "";
        String name = "";
        String ageText = "";
        String gender = "";
        String bloodGroup = "";
        String diagnosis = "";
        String symptoms = "";
        String medications = "";
        String labReports = "";
        String appointments = "";

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                int separator = line.indexOf(':');
                if (separator < 0) {
                    continue;                      // malformed line, skip it quietly
                }

                String key = line.substring(0, separator).trim().toLowerCase();
                String value = line.substring(separator + 1).trim();

                if (key.equals("patient id")) {
                    patientId = value;
                } else if (key.equals("name")) {
                    name = value;
                } else if (key.equals("age")) {
                    ageText = value;
                } else if (key.equals("gender")) {
                    gender = value;
                } else if (key.equals("blood group")) {
                    bloodGroup = value;
                } else if (key.equals("diagnosis")) {
                    diagnosis = value;
                } else if (key.equals("symptoms")) {
                    symptoms = value;
                } else if (key.equals("medications")) {
                    medications = value;
                } else if (key.equals("lab reports")) {
                    labReports = value;
                } else if (key.equals("appointments")) {
                    appointments = value;
                }
            }
        } catch (IOException e) {
            System.err.println("[PatientFileService] Could not read " + file.getName()
                    + ": " + e.getMessage());
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                    // nothing useful to do while closing
                }
            }
        }

        // If we could not even find an ID, treat the file as unusable.
        if (patientId.isEmpty() && name.isEmpty()) {
            System.err.println("[PatientFileService] Skipping malformed file (no ID or name): "
                    + file.getName());
            return null;
        }

        int age = 0;
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            System.err.println("[PatientFileService] Non-numeric age '" + ageText + "' in "
                    + file.getName() + ", defaulting to 0.");
        }

        return new Patient(patientId, name, age, gender, bloodGroup,
                diagnosis, symptoms, medications, labReports, appointments);
    }

    // ------------------------------------------------------------------- WRITING

    /**
     * Writes a new patient record as a new .txt file in data/patients/.
     *
     * The file is named after the patient's ID, e.g. patient_007.txt.
     *
     * @return the file that was written.
     * @throws IOException if the record could not be saved.
     */
    public File savePatient(Patient patient) throws IOException {
        if (!ensureDataDirectoryExists()) {
            throw new IOException("Could not create data directory: "
                    + dataDirectory.getAbsolutePath());
        }

        File target = new File(dataDirectory, fileNameForPatient(patient));

        // Safety net: never overwrite an existing record.
        int suffix = 2;
        while (target.exists()) {
            target = new File(dataDirectory, fileNameForPatient(patient).replace(".txt",
                    "_" + suffix + ".txt"));
            suffix++;
        }

        // FileWriter + BufferedWriter: the plain java.io way to write text files.
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target), "UTF-8"));
            writer.write(patient.toFileText());
            writer.flush();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                    // nothing useful to do while closing
                }
            }
        }

        return target;
    }

    /**
     * Suggests the next free patient ID by looking at the records already on disk
     * (for example P021 when the highest existing ID is P020).
     */
    public String generateNextPatientId() {
        int max = 0;
        for (Patient patient : loadAllPatients()) {
            int number = numericPart(patient.getPatientId());
            if (number > max) {
                max = number;
            }
        }
        return String.format("P%03d", max + 1);
    }

    /** Number of patient record files currently stored. */
    public int countRecordFiles() {
        File[] files = dataDirectory.listFiles();
        if (files == null) {
            return 0;
        }
        int count = 0;
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".txt")) {
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------- HELPERS

    /** patient_001.txt for patient P001, patient_042.txt for P042, etc. */
    private String fileNameForPatient(Patient patient) {
        int number = numericPart(patient.getPatientId());
        if (number <= 0) {
            return "patient_" + sanitizeForFileName(patient.getName()) + ".txt";
        }
        return String.format("patient_%03d.txt", number);
    }

    /** Extracts the numeric part of an ID such as "P007" -> 7. */
    private int numericPart(String id) {
        if (id == null) {
            return 0;
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        if (digits.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Keeps only characters that are safe in a file name. */
    private String sanitizeForFileName(String value) {
        StringBuilder safe = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                safe.append(Character.toLowerCase(c));
            }
        }
        return safe.length() == 0 ? "unknown" : safe.toString();
    }
}
