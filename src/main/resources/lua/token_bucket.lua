local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = 1

local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(bucket[1])
local last_refill = tonumber(bucket[2])

if tokens == nil then
    -- Bucket doesn't exist, initialize it
    tokens = capacity
    last_refill = now
else
    -- Calculate new tokens based on time elapsed
    local time_elapsed = math.max(0, now - last_refill)
    
    -- Integer division/multiplication for simplicity
    local new_tokens = time_elapsed * refill_rate
    
    if new_tokens > 0 then
        tokens = math.min(capacity, tokens + new_tokens)
        -- Only advance last_refill by the exact seconds we refilled
        -- This prevents losing fractions of a second if requests come fast
        last_refill = last_refill + time_elapsed
    end
end

local allowed = 0
if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
end

-- Save state
redis.call('HMSET', key, 'tokens', tokens, 'last_refill', last_refill)
-- Set TTL so keys don't accumulate forever (capacity / rate gives time to fill up, we double it)
local ttl = math.ceil(capacity / refill_rate) * 2
redis.call('EXPIRE', key, ttl)

return {allowed, tokens}
