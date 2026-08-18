# API Rate Limiter using Token Bucket

## Aim
Design and Implement an API Rate Limiter using Token Bucket.

## Objective
Implement a Rate Limiter using the Token Bucket algorithm in a Spring Boot application, use Redis to maintain rate-limit information, and analyze request control under normal and burst traffic.

## Technologies Used
* **Java 17**
* **Spring Boot 3** (Spring Web, Spring Data Redis)
* **Maven**
* **Redis** (Dockerized)
* **Redis Lua Scripting** (For atomicity)
* **Python 3** (For load testing)

## System Architecture
1. **Client**: Sends HTTP GET requests to the API with a `clientId`.
2. **Spring Boot App**: Exposes the `/api/test` endpoint.
3. **Redis**: Acts as a centralized data store for rate limit information.
4. **Lua Script**: Executes atomically inside Redis to check tokens and deduct them, preventing race conditions.

## Token Bucket Explanation
The Token Bucket algorithm works conceptually as follows:
* A "bucket" exists for each user with a fixed **Bucket Capacity** (e.g., 5 tokens).
* Tokens are added to the bucket at a fixed **Refill Rate** (e.g., 1 token per second), up to the capacity limit.
* Every incoming request attempts to consume 1 token.
* If the bucket has $\ge 1$ token, the token is removed, and the request is allowed.
* If the bucket is empty, the request is rejected with `HTTP 429 Too Many Requests`.

## Project Structure
```text
src/main/
├── java/com/example/ratelimiter/
│   ├── RateLimiterApplication.java
│   ├── config/RedisConfig.java
│   ├── controller/RateLimitController.java
│   ├── exception/GlobalExceptionHandler.java
│   ├── model/RateLimitResponse.java
│   └── service/RateLimiterService.java
└── resources/
    ├── application.yml
    └── lua/token_bucket.lua
```

## Setup Instructions

### 1. Redis Setup
The application requires Redis. Since you are using Docker, start Redis using the following command:
```bash
docker run --name redis-rate-limiter -p 6379:6379 -d redis
```
To check if Redis is running, run:
```bash
docker ps
```

### 2. How to run Spring Boot
Ensure Maven is installed or use the wrapper if provided (not provided here, assuming system Maven or IDE is used). To build and run from the terminal:
```bash
mvn clean install
mvn spring-boot:run
```

### 3. API Endpoint
**URL**: `GET http://localhost:8080/api/test?clientId={clientId}`

## Testing Instructions

### A. Normal Traffic Testing
Send requests slowly (e.g., 1 per second).
**Command**:
```bash
curl -i "http://localhost:8080/api/test?clientId=test1"
sleep 1
curl -i "http://localhost:8080/api/test?clientId=test1"
```
**Expected Output**: The requests should consistently return `HTTP 200 OK` as the tokens are replenished fast enough.

### B. Burst Traffic Testing
Send multiple requests immediately.
**Command**:
```bash
for i in {1..8}; do curl -i -s "http://localhost:8080/api/test?clientId=test2" & done
```
**Expected Output**: The first 5 requests will return `HTTP 200 OK`. The subsequent 3 requests will return `HTTP 429 Too Many Requests`.

### C. Concurrent Testing
Use the provided python script to test concurrent requests and ensure race conditions are handled.
**Command**:
```bash
python3 load_test.py
```
This script spawns 10 threads hitting the endpoint simultaneously.

### Explanation of Redis Lua Scripting
In a distributed environment, multiple application instances might process requests for the same client simultaneously. If we fetch the token count, calculate new tokens, and then update Redis in separate steps, a **race condition** can occur (e.g., two threads read 1 token and both allow a request, resulting in -1 tokens).
By putting the logic inside a **Redis Lua Script**, Redis executes it as a single, atomic operation. No other Redis command can run while the script is executing, ensuring perfect thread-safety without requiring distributed locks.

## Lab Analysis

**1. Role of:**
* **Token Bucket**: The algorithm governing how requests are rate-limited, allowing short bursts while maintaining a steady average rate.
* **Bucket Capacity**: The maximum number of tokens a bucket can hold. It defines the maximum burst size allowed.
* **Refill Rate**: The rate at which tokens are added back. It defines the steady-state allowed request rate.
* **Redis**: Provides centralized, fast, in-memory storage for the token counts, necessary when multiple application instances are load-balanced.

**2. Explain how the Token Bucket algorithm allows or rejects requests.**
When a request arrives, the algorithm calculates how many tokens should be added based on elapsed time since the last update. It caps the tokens at capacity. It then checks if the bucket has at least 1 token. If yes, it decrements the token count and allows the request. If no, the request is rejected immediately.

**3. Explain why Redis is required when multiple application instances are used.**
If rate limits are stored in-memory (e.g., in a Java `ConcurrentHashMap`), each application instance will have its own counter. A user could hit Instance A 5 times and Instance B 5 times, effectively bypassing the limit. Redis provides a single centralized source of truth.

**4. Compare:**
* **Fixed Window**: Resets the counter at fixed intervals (e.g., top of the minute). Suffers from the "boundary effect" where a burst of traffic at 11:59 and 12:01 can exceed the limit.
* **Sliding Window Log**: Stores timestamps of every request. Very accurate but consumes a lot of memory.
* **Sliding Window Counter**: Hybrid of fixed window and log, smoothing out traffic by weighting the previous window's count.
* **Token Bucket**: Allows bursts of traffic up to the bucket capacity while maintaining a steady average rate. Memory efficient (only stores 2 values: tokens and timestamp).

**5. Analyze behaviour during:**
* **Normal traffic**: Traffic flows steadily, and tokens are replenished as fast as they are consumed. Requests are rarely blocked.
* **Burst traffic**: The initial burst is accommodated up to the bucket capacity. Once empty, subsequent requests are strictly rate-limited to the refill rate.

## Learning Outcomes
* Implement API Rate Limiting using the Token Bucket algorithm.
* Configure bucket capacity and refill rate.
* Use Redis to maintain centralized rate-limit information.
* Handle normal and burst API traffic.
* Understand rate limiting in distributed web applications.

## How to stop the application
Press `Ctrl+C` in the terminal where Spring Boot is running.
Stop the Redis container:
```bash
docker stop redis-rate-limiter
docker rm redis-rate-limiter
```
