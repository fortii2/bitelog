package me.forty2.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;


/**
 * create id with 32 bits time & 32 bits 序列号
 */
@Component
public class IdGenerator {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final long beginningTime = 978048000L;

    public static final String IdPrefix = "icr:id:";

    public long nextId(String service) {
        LocalDateTime now = LocalDateTime.now();
        long time = now.plusSeconds(beginningTime).toEpochSecond(ZoneOffset.UTC);

        String key = IdPrefix + service + ":";
        long increment = stringRedisTemplate.opsForValue().increment(key);

        return time << 32 | increment;
    }
}
