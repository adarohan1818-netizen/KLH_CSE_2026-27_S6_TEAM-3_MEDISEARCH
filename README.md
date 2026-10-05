# MediSearch

**A Searchable Patient Repository for Efficient Medical Record Management**

A DSA-III academic project. Patient records and user credentials are stored as plain text files, and the search engine is a from-scratch implementation of the **Aho-Corasick** multiple-pattern string matching algorithm in Java.

---

## 1. What MediSearch is

MediSearch keeps patient details, diagnoses, prescriptions, lab reports and appointment history in one central repository. A doctor can log in and type several medical keywords at once to immediately see:

* which patients matched,
* exactly which keywords matched in each patient's record,
* the full details of any patient record on demand,
* persistent addition of new patient records.

The entire system is a single Java application (no database, no framework) plus a plain HTML/CSS/JavaScript interface served by the built-in Java `HttpServer`.

```text
                HTML / CSS / JS
                       |
                  User Login / Search
                       |
                       v
                 Java Application
                       |
         File Handling (java.io)
           /                   \
  data/patients/*.txt      data/users/users.txt
          |
          v
   Aho-Corasick Engine
          |
          v
    Search Results
```

---

## 2. The problem it solves

A patient record holds free text: a diagnosis, a list of symptoms, medications, lab notes and appointment history. A doctor often needs to ask questions such as:

* Which of my patients have **both** diabetes and hypertension?
* Who has a fever **and** a cough recorded?
* Which records mention asthma?

Running one independent search per keyword is wasteful: every keyword search re-scans every record from the beginning. With `k` keywords and records of length `N`, that is `O(k × N)` work per record.

MediSearch answers all of those questions in **a single pass per record**, so the cost grows with the length of the record, not with the number of keywords.

---

## 3. Why Aho-Corasick is used

Aho-Corasick is a multi-pattern matching algorithm. Given a set of keywords, it builds one automaton and then scans the text **once**, reporting every keyword that occurs anywhere in the text.

| Approach | Work per record | Notes |
|---|---|---|
| `indexOf()` / `contains()` per keyword | `O(k × N)` | one full scan per keyword |
| KMP per keyword | `O(k × N)` | still one scan per keyword |
| **Aho-Corasick (this project)** | **`O(N + Z)`** | one scan finds all keywords |

The advantage is exactly the feature MediSearch needs: **many keywords, one pass**.

The implementation lives in `src/algorithms/AhoCorasick.java` and uses the classic three structures, named as in the original algorithm:

```text
g[][]   ->  goto function     (the Trie transitions)
f[]     ->  failure links
out[]   ->  output bitmask
```

---

## 4. How the Trie works

**Step 1** of the algorithm builds a Trie (keyword tree) from all the keywords.

Every keyword is inserted character by character, starting at the root. States that correspond to the same prefix are **shared**. For the keywords `he`, `she`, `his`, `hers` the Trie is:

```text
                    (root)
                   /      \
                  h         s
                  |         |
                  e         h
                 (he)       |
                /   \       e
               r     i    (she)      <- "she" is its own state
               |     |       :
               s     s       : f[she] = he   (failure link, NOT a shared state)
            (hers) (his)     :
                              +----> points to the (he) state above
```

In code the Trie is the `g[][]` table: `g[state][ch]` is the state reached from `state` on character `ch`, or `-1` if no such transition exists.

Inserting a keyword ends with `out[finalState] |= (1 << i)`, recording that keyword number `i` finishes at that state.

---

## 5. What failure links do

`f[state]` points to the state representing the **longest proper suffix** of the current state's string that is still a prefix of some keyword.

When the next character cannot extend the current match, a naive algorithm would move back to the start of the text and begin again. Aho-Corasick instead follows failure links — which only ever move to *shallower* states — and then takes the real transition. **The text pointer never moves backwards**, which is why the scan is a single left-to-right pass.

```text
keywords: he, she, his, hers

after matching "she", if the next character is 'r':
  g[she]['r'] does not exist, so "she" cannot be extended
  -> follow the failure link: f[she] = he
  -> g[he]['r'] exists (from "hers"), so take it
  -> we are now in state "her", still having consumed only one new character
```

Every direct child of the root gets `f[child] = 0`, and missing transitions out of the root are set to `0` so the fallback loop always terminates at the root.

---

## 6. What the output array does

`out[state]` is a **bitmask of keyword indices**.

* Bit `i` set means `keywords[i]` occurs ending at this position.
* The bit is set either because the keyword itself ends at this state (set during Trie insertion), or because it was **inherited through a failure link**.
* A single state can therefore carry several bits, which is what lets one scan position report several keywords at once.

During failure-link construction, outputs are merged along failure links:

```java
out[child] |= out[failure];
```

If the failure target is itself the end of a keyword, that keyword is also a suffix of the current match, so the current state inherits its bits.

During the scan, whenever `out[currentState]` is non-zero the bitmask is decoded:

```java
for (int j = 0; j < keywords.length; j++) {
    if ((mask & (1 << j)) != 0) {   // keyword j matched here
        foundIndices.add(j);
    }
}
```

A `LinkedHashSet` collects the indices, so a keyword that appears ten times in one record is still reported **once** for that patient.

> **Limit:** The bitmask is a 32-bit Java `int`, so one search supports **at most 32 keywords**. The system enforces this limit and rejects more than 32 keywords with a clear UI message.

---

## 7. Why BFS is used

Failure links are built **level by level** using a queue (BFS).

The reason is a dependency: `f[state]` is always a **shallower** state than `state` itself. If states are processed in breadth-first order, then by the time a state is dequeued, its own failure link is already final, and the links of its children can be computed from correct information in one pass.

```java
Queue<Integer> queue = new LinkedList<Integer>();
// ... depth 1: children of the root fail to the root ...
while (!queue.isEmpty()) {
    int state = queue.poll();
    // ... compute f[child] for each child of 'state', then enqueue it ...
}
```

---

## 8. How file handling is used

There is **NO database in this project**. All application state lives in plain text files.

### Patient Data Files (`data/patients/*.txt`)
Each patient record is stored in a separate `.txt` file:

```text
Patient ID: P001
Name: Rahul Sharma
Age: 45
Gender: Male
Blood Group: B+
Diagnosis: Diabetes and hypertension
Symptoms: fatigue, headache, increased thirst
Medications: Metformin
Lab Reports: Blood glucose elevated
Appointments: 2026-09-10
```

### User Credential Files (`data/users/users.txt`)
User authentication data is stored as key-value lines:

```text
username=admin
passwordHash=240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
role=doctor
```

`PatientFileService.java` and `UserFileService.java` use standard `java.io` classes only:
* `File`, `FileInputStream`, `FileOutputStream`
* `InputStreamReader`, `OutputStreamWriter`
* `BufferedReader`, `BufferedWriter`

---

## 9. How to run the project

### Demo Credentials
* **Username:** `admin`
* **Password:** `admin123`

### 1. Compile

If `javac` is on your PATH:

```bash
mkdir out
javac -encoding UTF-8 -d out src/Main.java src/algorithms/*.java src/models/*.java src/services/*.java
```

If `javac` is not on PATH (using built-in `ecj.jar`):

```bash
java -jar ecj.jar -1.8 -d out src/Main.java src/algorithms/AhoCorasick.java src/models/Patient.java src/models/SearchResult.java src/models/User.java src/services/PatientFileService.java src/services/UserFileService.java src/services/MediSearchService.java
```

### 2. Run

```bash
java -cp out Main
```

### 3. Open Website

Open **http://localhost:8080** in your browser.

---

## 10. Example searches

Log in with `admin` / `admin123` and try these search queries:

| Search | What it demonstrates |
|---|---|
| `diabetes` | single keyword search |
| `fever, cough, diabetes` | multi-keyword search executed in a single pass |
| `  FEVER ,, cough  ` | whitespace, comma cleanup, case-insensitivity |
| `asthma` | substring match inside clinical text |
| `fever, fever, FEVER` | deduplication of repeated keywords |
| `chest pain` | exact multi-word pattern matching |

---

## 11. Time and space complexity

Let:
* `M` = total length of all keywords
* `N` = length of the text being scanned (one patient record)
* `S` = number of states in the Trie
* `Z` = number of matches reported
* `σ` = alphabet size (256 ASCII)

| Phase | Complexity |
|---|---|
| Build Trie (`g[][]`) | `O(M)` |
| Build failure links + merge outputs (`f[]`, `out[]`), BFS | `O(S × σ)` |
| Scan one record | `O(N + Z)` |
| **Scan all records** | `O(P × (N + Z))`, where `P` = number of patients |

---

## 12. Project structure

```text
D:\DSA
├── data/
│   ├── patients/                 # Patient files (patient_001.txt ... patient_020.txt)
│   └── users/                    # User credential files (users.txt)
├── public/                       # Frontend (HTML, CSS, JS)
│   ├── index.html
│   ├── style.css
│   └── script.js
├── src/
│   ├── Main.java                 # HttpServer + API Endpoints + Session Management
│   ├── algorithms/
│   │   └── AhoCorasick.java      # Trie, failure links, bitmask output
│   ├── models/
│   │   ├── Patient.java          # Patient model + toFileText() + toJson()
│   │   ├── SearchResult.java     # Search match model
│   │   └── User.java              # User account model + SHA-256 password hash
│   └── services/
│       ├── PatientFileService.java # Patient file I/O handling
│       ├── UserFileService.java    # User file I/O handling & auth
│       └── MediSearchService.java  # Keyword sanitization & Aho-Corasick bridge
├── ecj.jar                       # Eclipse Compiler for Java fallback
└── README.md
```
