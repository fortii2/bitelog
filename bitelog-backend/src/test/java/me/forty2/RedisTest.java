package me.forty2;

import me.forty2.entity.Shop;
import me.forty2.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RedisTest {

    @Autowired
    private ShopServiceImpl shopService;

    @Test
    public void save2RedisTest() {
        shopService.save2Redis(1L, 10L);
    }

    @Test
    public void queryWithLogicalExpireTest() {
        Shop shop = shopService.queryWithLogicalExpire(1L);

    }
}
