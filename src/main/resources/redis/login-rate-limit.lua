local key = KEYS[1]
local limit = tonumber(ARGV[1])
local windowMillis = tonumber(ARGV[2])

if not limit or limit < 1 or limit % 1 ~= 0 then
    return redis.error_reply("Invalid request limit")
end

if not windowMillis
        or windowMillis < 1000
        or windowMillis > 86400000
        or windowMillis % 1 ~= 0 then
    return redis.error_reply("Invalid window duration")
end

local stored = redis.call("GET", key)

if not stored then
    redis.call("SET", key, 1, "PX", windowMillis)
    return {1, limit - 1, 0}
end

local current = tonumber(stored)

if not current or current < 1 or current % 1 ~= 0 then
    return redis.error_reply("Invalid counter state")
end

local ttl = redis.call("PTTL", key)

if ttl < 0 then
    return redis.error_reply("Counter expiration is missing")
end

if current >= limit then
    return {0, 0, math.max(ttl, 1)}
end

local updated = redis.call("INCR", key)

return {1, limit - updated, 0}