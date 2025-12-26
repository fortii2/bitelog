package me.forty2.utils;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RedisLock {

    private final String key;
    private final String threadIdPrefix;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisLock(String serviceName, StringRedisTemplate stringRedisTemplate) {
        this.key = "lock:" + serviceName;
        this.threadIdPrefix = UUID.randomUUID() + "-";
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean tryLock(long expireTime) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue()
                        .setIfAbsent(
                                key,
                                threadIdPrefix + Thread.currentThread().getName(),
                                expireTime,
                                TimeUnit.SECONDS)
        );
    }

    public void unlock() {
        String threadId = threadIdPrefix + Thread.currentThread().getName();

        String lua = "if redis.call('get', KEYS[1]) == ARGV[1] "
                + "then return redis.call('del', KEYS[1]) else return 0 end";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(Long.class);

        stringRedisTemplate.execute(script, Collections.singletonList(key), threadId);
    }
}
