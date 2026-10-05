package services;

import models.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * FILE HANDLING LAYER FOR USER AUTHENTICATION.
 *
 * All user credentials and authentication data live as plain text in data/users/users.txt.
 * There is NO database used.
 *
 * File format:
 *   username=admin
 *   passwordHash=240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
 *   role=doctor
 *   ---
 */
public class UserFileService {

    public static final String DEFAULT_USERS_DIRECTORY = "data/users";
    public static final String USERS_FILE_NAME = "users.txt";

    private final File usersDirectory;
    private final File usersFile;

    public UserFileService() {
        this(DEFAULT_USERS_DIRECTORY);
    }

    public UserFileService(String usersDirectoryPath) {
        this.usersDirectory = new File(usersDirectoryPath);
        this.usersFile = new File(usersDirectory, USERS_FILE_NAME);
    }

    public File getUsersDirectory() {
        return usersDirectory;
    }

    public boolean ensureUsersDirectoryAndFileExists() {
        if (!usersDirectory.exists()) {
            if (!usersDirectory.mkdirs()) {
                return false;
            }
        }

        if (!usersFile.exists()) {
            try {
                // Create default admin account: admin / admin123
                User admin = new User("admin", User.hashPassword("admin123"), "doctor");
                saveUser(admin);
                System.out.println("[UserFileService] Initialized default user file with 'admin' account.");
            } catch (IOException e) {
                System.err.println("[UserFileService] Failed to create default users.txt file: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    /**
     * Reads all users from data/users/users.txt file.
     */
    public List<User> loadAllUsers() {
        List<User> users = new ArrayList<User>();
        if (!ensureUsersDirectoryAndFileExists()) {
            return users;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(usersFile), "UTF-8"));
            String line;
            String username = null;
            String passwordHash = null;
            String role = "doctor";

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.equals("---")) {
                    if (username != null && passwordHash != null) {
                        users.add(new User(username, passwordHash, role));
                    }
                    username = null;
                    passwordHash = null;
                    role = "doctor";
                    continue;
                }

                int eqPos = line.indexOf('=');
                if (eqPos > 0) {
                    String key = line.substring(0, eqPos).trim();
                    String val = line.substring(eqPos + 1).trim();

                    if (key.equalsIgnoreCase("username")) {
                        username = val;
                    } else if (key.equalsIgnoreCase("passwordHash")) {
                        passwordHash = val;
                    } else if (key.equalsIgnoreCase("role")) {
                        role = val;
                    }
                }
            }

            // Handle last record if no trailing "---"
            if (username != null && passwordHash != null) {
                users.add(new User(username, passwordHash, role));
            }

        } catch (IOException e) {
            System.err.println("[UserFileService] Error reading users file: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {}
            }
        }
        return users;
    }

    /**
     * Finds a user by username.
     */
    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        for (User user : loadAllUsers()) {
            if (user.getUsername().equalsIgnoreCase(username.trim())) {
                return user;
            }
        }
        return null;
    }

    /**
     * Authenticates a user with username and plain-text password.
     * @return User object if valid, null if invalid.
     */
    public User authenticate(String username, String password) {
        User user = findByUsername(username);
        if (user != null && user.verifyPassword(password)) {
            return user;
        }
        return null;
    }

    /**
     * Appends a user to data/users/users.txt file.
     */
    public synchronized void saveUser(User user) throws IOException {
        if (!usersDirectory.exists()) {
            usersDirectory.mkdirs();
        }

        boolean fileExisted = usersFile.exists() && usersFile.length() > 0;
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(usersFile, true), "UTF-8"));
            if (fileExisted) {
                writer.write("\n---\n");
            }
            writer.write(user.toFileText());
            writer.flush();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
