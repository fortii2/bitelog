package me.forty2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import me.forty2.dto.Result;
import me.forty2.entity.ShopType;

public interface ShopTypeService extends IService<ShopType> {

    Result queryType();
}
