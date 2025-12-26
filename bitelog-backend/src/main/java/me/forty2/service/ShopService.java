package me.forty2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import me.forty2.dto.Result;
import me.forty2.entity.Shop;

public interface ShopService extends IService<Shop> {

    Result queryById(Long id);

    Result saveById(Shop shop);
}
