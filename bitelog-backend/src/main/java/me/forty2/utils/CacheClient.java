package me.forty2.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {

    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object value, Long expire, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(
                key,
                JSONUtil.toJsonStr(value),
                expire,
                timeUnit
        );
    }

    public void setLogicalExpire(String key, Object value, Long expire, TimeUnit timeUnit) {
        RedisData data = new RedisData();
        data.setData(value);
        data.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expire)));

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(data));
    }

    /**
     * return null obj to fix
     */
    public <R, ID> R getCachePenetration(String keyPrefix, ID id, Long expire, TimeUnit timeUnit, Class<R> clazz, Function<ID, R> dbFallback) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);

        // not blank and not null
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, clazz);
        }

        // not null means blank ""
        if (json != null) {
            return null;
        }

        R byId = dbFallback.apply(id);

        if (byId == null) {
            stringRedisTemplate.opsForValue().set(
                    key,
                    RedisConstants.EMPTY_STR,
                    RedisConstants.CACHE_NULL_TTL,
                    TimeUnit.MINUTES);
            return null;
        }

        this.set(key, byId, expire, timeUnit);
        return byId;
    }


    /**
     * use logic expire to fix
     */
    public <R, ID> R getCacheBreakdown(String keyPrefix, ID id, Long expire, TimeUnit timeUnit, Class<R> clazz, Function<ID, R> dbFallback) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isBlank(json)) {
            return null;
        }

        RedisData data = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) data.getData(), clazz);

        // data not expire
        if (LocalDateTime.now().isBefore(data.getExpireTime())) {
            return r;
        }

        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;

        // rebuild
        if (lock(lockKey)) {
            threadPool.submit(() -> {
                try {
                    R r1 = dbFallback.apply(id);
                    this.setLogicalExpire(key, r1, expire, timeUnit);
                } catch (Exception e) {
                    throw new RuntimeException();
                } finally {
                    unlock(lockKey);
                }
            });
        }

        return r;
    }

    private boolean lock(String key) {
        Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(isLock);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
