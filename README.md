# MediSearch - Intelligent Patient Record Retrieval System

## 1. Project Title
MediSearch: DSA-3 Project for Medical Case Searching.

## 2. Problem Statement
Doctors and medical researchers often need to sift through thousands of patient records to find specific cases, combinations of symptoms, or historically similar diagnoses to make informed medical decisions. Traditional database queries can be rigid and lack intelligent text processing.

## 3. Objectives
The objective of this project is to build an intelligent, fast, and simple patient record retrieval system that allows users to:
- Find patients matching a specific keyword (e.g., a disease or medication).
- Search for multiple symptoms simultaneously.
- Find past cases that are similar to a newly described medical condition using string similarity.

## 4. Technologies Used
- **Backend:** Java (using `com.sun.net.httpserver.HttpServer` for a lightweight server)
- **Frontend:** HTML5, CSS3, Vanilla JavaScript
- **No External Dependencies:** Zero usage of Spring Boot, databases, or external JSON libraries (Gson/Jackson). 

## 5. DSA Algorithms Used
The system relies entirely on native data structures and implements the following algorithms from the DSA-3 syllabus:
- **Knuth-Morris-Pratt (KMP):** For efficient single-keyword searching.
- **Rabin-Karp:** For single-keyword searching using rolling hashes.
- **Aho-Corasick:** For multi-pattern/multi-keyword searching using a Trie and failure links.
- **Edit Distance (Wagner-Fischer):** Dynamic programming algorithm to calculate string similarity for finding "similar cases."

## 6. Features
- **Dashboard:** Overview of total patients and available algorithms.
- **Patient Search (KMP & Rabin-Karp):** Enter a single keyword to find matching patient records.
- **Multi-Keyword Search (Aho-Corasick):** Enter multiple keywords separated by commas to find patients with those exact symptoms/diseases.
- **Similar Case Search (Edit Distance):** Enter a misspelled or loosely matched diagnosis to find the closest historical medical records.
- **All Patients List:** View all mock patient data.

## 7. Project Structure
```
dsa-medisearch/
├── public/                 # Static frontend assets
│   ├── index.html
│   ├── style.css
│   └── script.js
├── src/                    # Java Backend Source Code
│   ├── algorithms/
│   │   ├── AhoCorasick.java
│   │   ├── EditDistance.java
│   │   ├── KMPSearch.java
│   │   └── RabinKarpSearch.java
│   ├── models/
│   │   └── Patient.java
│   ├── repository/
│   │   └── PatientRepository.java
│   └── server/
│       └── MediSearchServer.java
└── README.md
```

## 8. How to Run
1. Make sure you have Java installed (`javac` and `java` commands must be available).
2. Open a terminal/command prompt in the root directory of this project.
3. Compile all Java files into an `out` directory:
   ```bash
   javac -d out src/**/*.java
   ```
   *(Note for Windows users: `javac -d out src\models\*.java src\repository\*.java src\algorithms\*.java src\server\*.java` or simply `javac -d out src/*/*.java`)*
4. Run the backend server:
   ```bash
   java -cp out server.MediSearchServer
   ```
5. Open your web browser and navigate to: [http://localhost:8080](http://localhost:8080)

## 9. Time Complexity
- **KMP:** $O(N + M)$
- **Rabin-Karp:** Average $O(N + M)$, Worst $O(N \times M)$
- **Aho-Corasick:** $O(N + M + Z)$
- **Edit Distance:** $O(N \times M)$
*(Where N is text length, M is pattern/keyword length, and Z is the number of matches)*
