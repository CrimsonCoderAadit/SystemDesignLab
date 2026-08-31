# EX 7: Design and Simulate an Autocomplete Search System using Prefix Matching

**Register Number:** 3122245001003  
**Name:** AAKASH BALASUBRAMANIAN  
**Class:** CSE - A  

**GitHub Repository:**  
https://github.com/CrimsonCoderAadit/SystemDesignLab/tree/AUTOCOMPLETE-LAB7

---

### Aim:
To design and implement an autocomplete search system using prefix matching in a Spring Boot application, integrate Redis for caching frequently searched prefixes, and evaluate the performance of the system using JMeter.

### Software Installed / Used:
* Java JDK 17
* Spring Boot 3.x
* Maven
* Redis
* Docker Desktop
* Docker Compose
* VS Code
* Postman (or Terminal/curl)
* Apache JMeter
* Git

---

### Algorithm:

*(You can handwrite this section based on the following logic)*

**Algorithm for Autocomplete with Redis and Trie:**
1. **Initialize:** Load the dataset of search terms and their frequencies into the Trie data structure on application startup.
2. **Receive Request:** Accept a `GET` request containing a `prefix` from the user/client.
3. **Check Cache (Redis):** 
   - Look up the `prefix` in the Redis cache.
   - **If Cache Hit (prefix exists in Redis):**
     - Retrieve the Top-K suggestions from Redis.
     - Go to Step 6.
   - **If Cache Miss (prefix does not exist in Redis):**
     - Proceed to Step 4.
4. **Trie Search:**
   - Traverse the Trie using the characters of the `prefix`.
   - If the prefix node is found, recursively find all complete words branching from that node.
   - Sort the retrieved words descendingly based on their frequency.
   - Extract the Top-K results.
5. **Update Cache:**
   - Store the generated Top-K suggestions in the Redis cache with the `prefix` as the key.
6. **Return Output:** Send an HTTP 200 response with the Top-K suggestions in JSON format.

---

### System Pipeline:

*(You can draw a flowchart based on this flow)*

```text
[ Client / User ] 
       |
  (Sends API Request: /api/autocomplete?prefix=app)
       v
[ Spring Boot Controller ]
       |
       v
[ Redis Cache Check ] -----> (If Cache Hit) -----> [ Return Cached Output ]
       |
  (If Cache Miss)
       v
[ Trie Data Structure ]
       |
  (Perform Prefix Search & Rank by Frequency)
       v
[ Store Results in Redis ]
       |
       v
[ Return Output to Client ]
```

---

### Test Cases / Output Screenshots to Capture:

*(Take screenshots of these operations on your machine)*

1. **Terminal / Docker Start:** 
   - Screenshot of `docker compose up --build` showing the Spring Boot app and Redis container running successfully.
2. **First Search Request (Cache Miss):**
   - Screenshot of Postman (or terminal) making a `GET` request to `http://localhost:8080/api/autocomplete?prefix=app`.
   - Screenshot of the Spring Boot application terminal showing the log: `"Cache miss for prefix: 'app'. Searching in Trie..."`
3. **Second Search Request (Cache Hit):**
   - Screenshot of Postman making the exact same `GET` request.
   - Note/Highlight that the result was returned instantly and no "Cache miss" log appeared in the terminal.
4. **Performance Evaluation (JMeter):**
   - Screenshot of the JMeter GUI showing the "Autocomplete Test Plan" successfully running with concurrent threads.

---

### Learning Outcomes:

*(You can handwrite this section)*

1. Developed an autocomplete search system using Spring Boot.
2. Implemented prefix-based searching efficiently using a Trie data structure.
3. Returned Top-K search suggestions based on term frequency.
4. Integrated Redis caching into the application using Spring Data Redis (`@Cacheable`).
5. Analyzed the performance improvement and reduced response times achieved through caching frequent queries.
