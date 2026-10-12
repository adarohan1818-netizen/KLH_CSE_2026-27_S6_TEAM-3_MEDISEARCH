MediSearch --- Intelligent Patient Record Retrieval System
A DSA-III academic project for searching, comparing, and exploring
synthetic patient records through a browser-based interface. The
application combines a Java backend with an HTML, CSS, and JavaScript
frontend, and demonstrates seven data-structure and algorithm techniques
across four main areas: text searching, record similarity, capacity-flow
analysis, and primality testing.
Academic demonstration: MediSearch uses synthetic/demo patient
data and file-based storage. It is not intended for real clinical use
or production healthcare.

1. What MediSearch Does
MediSearch provides a central interface for working with patient
records. Depending on the enabled feature, a user can:
- Log in to the application.
- Search patient records using clinical keywords or text patterns.
- Use fuzzy matching to help find text despite small spelling
  differences.
- Compare records to identify shared text.
- Run a maximum-flow calculation on a simplified hospital-capacity
  network.
- Test whether a number is prime or composite.
- View patient records and add new records, which are stored in files.
2. Application Architecture
The browser interface communicates with a Java HTTP server. The Java
application handles requests, loads and saves records, invokes the
relevant algorithm, and returns results to the frontend.
Browser UI (HTML / CSS / JavaScript)
                 |
                 v
          Java HTTP Server
                 |
                 v
       Services and File Handling
          /       |        \
         v        v         v
  Patient Data  User Data  Algorithm Services
                              |
             +----------------+----------------+
             |                |                |
         Text Search     Record Similarity  Other DSA Tools
         - Aho-Corasick  - Suffix Array     - Dinic
         - KMP             + Kasai LCP      - Miller-Rabin
         - Rabin-Karp
         - Wagner-Fischer
The exact set of endpoints and class names should match the source code
in the submitted repository.
3. The Seven Algorithms and Where They Are Used
3.1 Aho--Corasick --- Multi-keyword Clinical Search
Definition: Aho--Corasick searches for multiple patterns in a text
during a single scan. It builds a trie containing the keywords and uses
failure links to continue matching efficiently when the current path
cannot be extended.
How it works: 1. Inserts all search keywords into a trie. 2. Builds
failure links using breadth-first search (BFS). 3. Scans the
patient-record text from left to right. 4. Reports keywords that end at
each matching position.
Use in MediSearch: Clinical keyword search.
Benefit: A query such as fever, cough, diabetes can look for
several terms in a patient record during one scan, rather than
independently scanning the record for every keyword.
3.2 KMP (Knuth--Morris--Pratt) --- Exact Pattern Matching
Definition: KMP finds an exact pattern inside a larger text. It
preprocesses the pattern into an LPS table (Longest Proper Prefix that
is also a Suffix) so that, after a mismatch, it can reuse information
from the characters already matched.
How it works: 1. Builds the LPS table for the pattern. 2. Compares
the pattern with the text. 3. On a mismatch, uses the LPS table to
determine how far the pattern can shift without rechecking known
matches.
Use in MediSearch: Exact pattern-search functionality, where this
implementation is connected.
Benefit: Avoids repeatedly comparing characters that are already
known to match. KMP is an exact matcher; it does not itself correct
spelling mistakes.
3.3 Rabin--Karp --- Hash-based Pattern Matching
Definition: Rabin--Karp searches for a pattern by comparing its hash
value with the hash of text windows of the same length. It uses a
rolling hash so that the hash for the next window can be updated
from the previous window instead of recalculating it from scratch.
How it works: 1. Calculates a hash for the search pattern. 2.
Calculates a hash for the first text window of the same length. 3.
Slides the window through the text and updates its hash using the
rolling-hash formula. 4. When the hashes match, compares the actual
characters to confirm the match. This final check matters because
different strings can produce the same hash (a hash collision).
Use in MediSearch: Hash-based pattern-search functionality, where
this implementation is connected.
Benefit: Demonstrates a hash-based approach to finding candidate
matches efficiently. A matching hash is not treated as proof by itself;
the actual text must be checked.
3.4 Wagner--Fischer Edit Distance --- Fuzzy Matching
Definition: Wagner--Fischer uses dynamic programming to calculate
the minimum number of single-character insertions, deletions, and
substitutions needed to transform one string into another. This number
is called the edit distance.
How it works: 1. Creates a dynamic-programming table for prefixes of
the two strings. 2. Calculates the minimum cost of inserting, deleting,
or substituting a character. 3. Uses the final distance to measure how
different the strings are.
Use in MediSearch: Fuzzy-search functionality, where approximate
matching is enabled.
Benefit: Can help match a term with a small spelling difference. For
example, diabtes is one deletion away from diabetes. Whether it is
returned depends on the application's matching threshold and matching
logic.
3.5 Suffix Array + Kasai LCP --- Patient Record Similarity
Definition: A suffix array stores the starting positions of all
suffixes of a text in lexicographically sorted order. Kasai's algorithm
efficiently computes the Longest Common Prefix (LCP) between
neighboring suffixes in that sorted order.
How it works: 1. Builds a suffix array, commonly by sorting suffixes
through prefix doubling. 2. Uses Kasai's algorithm to calculate LCP
values efficiently. 3. Uses the common-text information to support
similarity analysis.
Use in MediSearch: Patient Record Similarity.
Benefit: Helps identify shared text between records and provides a
basis for displaying common-substring or similarity information. Shared
text alone is not a medical diagnosis and should not be interpreted as
clinical equivalence.
3.6 Dinic's Algorithm --- Maximum Capacity Flow
Definition: Dinic's algorithm finds the maximum possible flow from a
source node to a destination node in a directed network whose edges have
capacity limits. It uses BFS to construct a level graph and DFS to send
flow through that graph until no more blocking flow can be sent.
How it works: 1. Uses BFS to assign levels to nodes reachable
through edges with remaining capacity. 2. Uses DFS to send flow along
valid paths in the level graph. 3. Repeats until the sink cannot be
reached from the source in the residual network.
Use in MediSearch: Capacity Flow simulation.
Benefit: Calculates the maximum flow possible through the configured
network while respecting each connection's capacity. In this project it
is a simplified network simulation, not a live hospital-bed or
patient-allocation system.
3.7 Miller--Rabin --- Primality Testing
Definition: Miller--Rabin is a primality-testing algorithm that uses
modular exponentiation and repeated witness tests to determine whether a
number is composite or behaves like a prime. A witness can establish
that a number is composite; passing a probabilistic round means the
number is probably prime. A deterministic set of bases can guarantee
the result within a supported numeric range.
How it works: 1. Handles small numbers and simple cases. 2.
Expresses the relevant part of n − 1 as a power of two multiplied by
an odd number. 3. Performs modular-exponentiation tests using selected
bases (witnesses). 4. Reports composite when a test proves
compositeness; otherwise reports prime or probably prime according to
the implementation's guarantees.
Use in MediSearch: Primality Testing.
Benefit: Tests large integers much more efficiently than checking
every possible divisor. The result's deterministic or probabilistic
status depends on the numeric range and bases used by the
implementation.
4. Quick Algorithm Summary
  Algorithm               Main purpose            MediSearch feature
  Aho--Corasick           Find multiple keywords  Clinical keyword search
                          in one scan             
  KMP                     Find one exact pattern  Exact pattern search
                          using an LPS table      
  Rabin--Karp             Find candidate matches  Hash-based pattern
                          using rolling hashes,   search
                          then verify characters  
  Wagner--Fischer         Calculate minimum edit  Fuzzy search
                          distance between
                          strings                 
  Suffix Array + Kasai    Organize suffixes and   Patient record
  LCP                     calculate common        similarity
                          prefixes                
  Dinic                   Calculate maximum flow  Capacity-flow
                          under edge-capacity     simulation
                          limits                  
  Miller--Rabin           Test numbers for        Primality testing
                      primality               
The first four algorithms operate on text, but they demonstrate
different approaches: multiple-pattern matching, exact matching with
prefix information, hash-based matching, and approximate matching. The
other three address record similarity, network flow, and number theory.
5. Other Application Features
- Login and sessions: Provides access to the application through
  the login interface.
- Patient records: Displays stored patient information and
  supports adding records when enabled.
- File-based persistence: Patient and user information are stored
  in files rather than a database.
- Benchmarks: If enabled in the current build, the benchmark view
  reports timings for the algorithms being measured. Timings depend on
  input size and the machine running the application; they should not
  be treated as universal performance results.
6. Data Storage and Project Structure
The project uses file-based storage rather than a database. In the
repository layout shown in the supplied project snapshot:
data/
  patients/        # Patient-record text files
  users/           # User credential file(s)
public/
  index.html       # Frontend page
  style.css        # Frontend styling
  script.js        # Frontend behavior and API calls
src/
  Main.java        # Java HTTP server and request handling
  algorithms/      # Algorithm implementations
  models/          # Patient, user, and result data models
  services/        # File handling and application services
ecj.jar            # Eclipse Compiler for Java, if needed
README.md
The algorithm classes and service classes listed in this overview should
be checked against the final submitted source tree before submission,
because class names and files may change between versions.
7. Running the Application
Use the commands and credentials that match the final repository. If the
project still uses the same Java entry point and Eclipse Compiler for
Java setup, the general flow is:
1. Open a terminal in the project directory.
2. Compile the Java source files with the repository's documented
   command.
3. Start the Java application using its main class.
4. Open the local address configured by the server in a browser.
5. Sign in using the demo credentials configured in the local user
   file.
Do not publish real patient information or production credentials in the
repository. Confirm the final compile command, port, and demo
credentials from the current source before presenting or submitting this
README.
8. Suggested Demonstration
A short demo can show the app's features in this order:
1. Login and patient records: Sign in and open a patient record.
2. Clinical keyword search: Search for multiple terms such as
   fever, cough, diabetes to demonstrate Aho--Corasick.
3. Exact and fuzzy matching: Show exact-pattern and typo-tolerant
   searches if those options are present in the current interface.
4. Record similarity: Compare two records and show the shared-text
   result.
5. Capacity flow: Run the configured network and explain that Dinic
   calculates the maximum flow under capacity limits.
6. Primality test: Test a prime such as 17 and a composite such
   as 561, explaining Miller--Rabin's result.
Only demonstrate a feature if it is present and working in the build
being submitted.
9. Scope and Limitations
- This is an academic DSA demonstration, not a production healthcare
  application.
- File-based storage is simple for a student project but does not
  provide the protections and concurrent-access guarantees expected
  from a production database.
- The supplied repository description indicates that credential data
  is stored in files. Review password hashing, session management,
  transport security, and access control before exposing the
  application beyond a local demo.
- Synthetic example records must not be mistaken for real patient
  data.
- Algorithm performance depends on input size, implementation, and
  hardware.
- Similarity between text records does not establish that two patients
  have the same condition or require the same treatment.
10. Project Summary
MediSearch demonstrates how classic DSA techniques can support different
functions in a patient-record application. Aho--Corasick, KMP,
Rabin--Karp, and Wagner--Fischer represent four different text-matching
approaches; Suffix Array with Kasai LCP supports shared-text analysis;
Dinic handles a capacity-constrained network; and Miller--Rabin performs
primality testing. Together, these features provide an academic
demonstration of string algorithms, dynamic programming, graph
algorithms, and number-theoretic algorithms in one interface.
