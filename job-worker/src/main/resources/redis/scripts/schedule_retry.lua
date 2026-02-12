-- KEYS[1] = retry:zset
-- KEYS[2] = retry:data

-- ARGV[1] = jobId
-- ARGV[2] = executeAt
-- ARGV[3] = payload

redis.call("HSET", KEYS[2], ARGV[1], ARGV[3])
redis.call("ZADD", KEYS[1], ARGV[2], ARGV[1])

return 1
