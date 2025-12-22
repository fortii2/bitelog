package me.forty2.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.forty2.dto.Result;
import me.forty2.entity.Shop;
import me.forty2.mapper.ShopMapper;
import me.forty2.service.ShopService;
import me.forty2.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        String key = RedisConstants.CACHE_SHOP_KEY + id;

        try {
            String shopJson = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(shopJson)) {

                return Result.ok(objectMapper.readValue(shopJson.getBytes(StandardCharsets.UTF_8), Shop.class));
            }

            Shop byId = this.getById(id);
            if (byId == null) {
                return Result.fail("Cannot find target shop.");
            }

            stringRedisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(byId),
                    RedisConstants.CACHE_SHOP_TTL,
                    TimeUnit.MINUTES);

            return Result.ok(byId);
        } catch (IOException exception) {
            return Result.fail("JSON error.");
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
}
