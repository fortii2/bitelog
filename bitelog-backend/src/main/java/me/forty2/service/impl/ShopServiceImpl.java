package me.forty2.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.forty2.dto.Result;
import me.forty2.entity.Shop;
import me.forty2.mapper.ShopMapper;
import me.forty2.service.ShopService;
import me.forty2.utils.CacheClient;
import me.forty2.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        Shop shop = cacheClient.getCachePenetration(
                RedisConstants.CACHE_SHOP_KEY,
                id,
                RedisConstants.CACHE_SHOP_TTL,
                TimeUnit.MINUTES,
                Shop.class,
                this::getById
        );

        if (shop == null) {
            return Result.fail("error.");
        }

        return Result.ok(shop);
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
}
