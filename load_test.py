import requests
import time
import threading

BASE_URL = "http://localhost:8080/api/test"

def make_request(client_id, req_num):
    try:
        response = requests.get(f"{BASE_URL}?clientId={client_id}")
        data = response.json()
        print(f"[{client_id}] Req: {req_num:2d} | Status: {response.status_code} | Remaining: {data.get('remainingTokens')} | Msg: {data.get('message')}")
    except Exception as e:
        print(f"[{client_id}] Req: {req_num:2d} | Failed: {e}")

def test_normal_traffic(client_id):
    print(f"\n--- Testing Normal Traffic ({client_id}) ---")
    print("Sending 1 request per second (refill rate is 1 token/sec)...")
    for i in range(1, 6):
        make_request(client_id, i)
        time.sleep(1)

def test_burst_traffic(client_id):
    print(f"\n--- Testing Burst Traffic ({client_id}) ---")
    print("Sending 8 requests immediately (bucket capacity is 5)...")
    for i in range(1, 9):
        make_request(client_id, i)

def test_concurrent_requests(client_id):
    print(f"\n--- Testing Concurrent Requests ({client_id}) ---")
    print("Sending 10 requests simultaneously to test Lua script atomicity...")
    threads = []
    for i in range(1, 11):
        t = threading.Thread(target=make_request, args=(client_id, i))
        threads.append(t)
        t.start()
    
    for t in threads:
        t.join()

if __name__ == "__main__":
    print("Starting API Rate Limiter Tests...\n")
    
    # 1. Normal Traffic
    test_normal_traffic("user1")
    
    # Wait for bucket to refill
    print("\nWaiting 6 seconds for bucket to refill...")
    time.sleep(6)
    
    # 2. Burst Traffic
    test_burst_traffic("user1")
    
    # 3. Independent limits (Different client)
    print("\n--- Testing Independent Limits (user2) ---")
    print("user2 should have a full bucket even though user1 was just rate limited.")
    for i in range(1, 4):
        make_request("user2", i)
        
    # Wait for bucket to refill
    print("\nWaiting 6 seconds for bucket to refill...")
    time.sleep(6)

    # 4. Concurrent requests
    test_concurrent_requests("user3")

    print("\nTests completed.")
