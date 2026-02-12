-- KEYS[1] = retry:count (hash key storing retry counts per job)
-- ARGV[1] = jobId

-- Atomically increment the retry count for a job and return the new value
return redis.call("HINCRBY", KEYS[1], ARGV[1], 1)
