-- KEYS[1] = retry:zset
-- KEYS[2] = retry:data

-- ARGV[1] = currentTime
-- ARGV[2] = batchSize

-- Get all job IDs that are due for retry (score <= currentTime)
local dueJobIds = redis.call("ZRANGEBYSCORE", KEYS[1], 0, ARGV[1], "LIMIT", 0, ARGV[2])

local results = {}

-- For each due job, retrieve its payload and add it to results as a flat list
-- Results format: [payload1, payload2, ...] (only payloads, since jobId is inside the payload JSON)
for _, jobId in ipairs(dueJobIds) do
    local payload = redis.call("HGET", KEYS[2], jobId)

    if payload then
        table.insert(results, payload)
    end

    -- Remove the job from the sorted set and hash to claim it
    redis.call("ZREM", KEYS[1], jobId)
    redis.call("HDEL", KEYS[2], jobId)
end

return results