package me.forty2.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.forty2.dto.Result;
import me.forty2.entity.ShopType;
import me.forty2.mapper.ShopTypeMapper;
import me.forty2.service.ShopTypeService;
import me.forty2.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
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
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements ShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Result queryType() {
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;

        try {
            String typeFromRedisStr = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(typeFromRedisStr)) {
                return Result.ok(objectMapper.readValue(typeFromRedisStr, new TypeReference<List<ShopType>>() {
                }));
            }

            List<ShopType> types = this.query().orderByAsc("sort").list();
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(types), RedisConstants.CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
            return Result.ok(types);

        } catch (Exception e) {
            return Result.fail("error.");
        }
    }
}
