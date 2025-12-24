package me.forty2.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.forty2.dto.RedisDataWithExpireTime;
import me.forty2.dto.Result;
import me.forty2.entity.Shop;
import me.forty2.mapper.ShopMapper;
import me.forty2.service.ShopService;
import me.forty2.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Result queryById(Long id) {
        Shop shop = queryByIdFixedCacheBreakdownAndCachePenetration(id);
        if (shop == null) {
            return Result.fail("error.");
        }

        return Result.ok(shop);
    }

    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public Shop queryWithLogicalExpire(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isBlank(shopJson)) {
            return null;
        }

        RedisDataWithExpireTime data = JSONUtil.toBean(shopJson, RedisDataWithExpireTime.class);

        // data not expire
        if (LocalDateTime.now().isBefore(data.getExpireTime())) {
            return (Shop) data.getData();
        }

        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;

        // rebuild
        if (lock(lockKey)) {
            threadPool.submit(() -> {
                try {
                    save2Redis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException();
                } finally {
                    unlock(lockKey);
                }
            });
        }

        return (Shop) data.getData();
    }

    public void save2Redis(Long id, Long expire) {
        Shop shop = this.getById(id);
        RedisDataWithExpireTime data = new RedisDataWithExpireTime();

        data.setData(shop);
        data.setExpireTime(LocalDateTime.now().plusSeconds(expire));

        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(data));
    }


    public Shop queryByIdFixedCacheBreakdownAndCachePenetration(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;

        try {
            String shopJson = stringRedisTemplate.opsForValue().get(key);

            // not blank and not null
            if (StrUtil.isNotBlank(shopJson)) {
                return objectMapper.readValue(shopJson.getBytes(StandardCharsets.UTF_8), Shop.class);
            }

            // not null means blank ""
            if (shopJson != null) {
                return null;
            }

            // not penetration, so cache breakdown happens
            while (!lock(lockKey)) {
                Thread.sleep(100);
            }

            // when get lock, need to double check cache. Therefore can properly get the data from cache rather than do db again

            shopJson = stringRedisTemplate.opsForValue().get(key);

            // not blank and not null
            if (StrUtil.isNotBlank(shopJson)) {
                return objectMapper.readValue(shopJson.getBytes(StandardCharsets.UTF_8), Shop.class);
            }

            // not null means blank ""
            if (shopJson != null) {
                return null;
            }

            Shop byId = this.getById(id);
            if (byId == null) {
                stringRedisTemplate.opsForValue().set(
                        key,
                        RedisConstants.EMPTY_STR,
                        RedisConstants.CACHE_NULL_TTL,
                        TimeUnit.MINUTES);
                return null;
            }

            stringRedisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(byId),
                    RedisConstants.CACHE_SHOP_TTL,
                    TimeUnit.MINUTES);

            return byId;
        } catch (Exception exception) {
            return null;
        } finally {
            unlock(lockKey);
        }
    }


    // return null obj
    public Shop queryByIdFixedCachePenetration(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;

        try {
            String shopJson = stringRedisTemplate.opsForValue().get(key);

            // not blank and not null
            if (StrUtil.isNotBlank(shopJson)) {
                return objectMapper.readValue(shopJson.getBytes(StandardCharsets.UTF_8), Shop.class);
            }

            // not null means blank ""
            if (shopJson != null) {
                return null;
            }

            Shop byId = this.getById(id);
            if (byId == null) {
                stringRedisTemplate.opsForValue().set(
                        key,
                        RedisConstants.EMPTY_STR,
                        RedisConstants.CACHE_NULL_TTL,
                        TimeUnit.MINUTES);
                return null;
            }

            stringRedisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(byId),
                    RedisConstants.CACHE_SHOP_TTL,
                    TimeUnit.MINUTES);

            return byId;
        } catch (IOException exception) {
            return null;
        }
    }

    @Override
    @Transactional
    public Result saveById(Shop shop) {
        if (shop.getId() == null) {
            return Result.fail("id empty.");
        }

        log.info("updating..");
        boolean updated = this.updateById(shop);
        if (!updated) {
            return Result.fail("update failed.");
        }
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());

        return Result.ok();
    }

    private boolean lock(String key) {
        Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(isLock);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
