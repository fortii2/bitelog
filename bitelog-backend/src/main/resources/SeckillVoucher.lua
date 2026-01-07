
local voucherStockPrefix = KEYS[1]
local voucherOrderPrefix = KEYS[2]

local voucherId = ARGV[1]
local userId = ARGV[2]

local voucherStockKey = voucherStockPrefix .. voucherId
local voucherOrderKey = voucherOrderPrefix .. voucherId

if (tonumber(redis.call('get', voucherStockKey)) <= 0) then
    return 1
end

if (redis.call('sismember', voucherOrderKey, userId) == 1) then
    return 2
end

redis.call('incrby', voucherStockKey, -1)
redis.call('sadd', voucherOrderKey, userId)
return 0
