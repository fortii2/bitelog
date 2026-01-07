package me.forty2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import me.forty2.dto.Result;
import me.forty2.entity.SeckillVoucher;
import me.forty2.entity.VoucherOrder;

public interface VoucherOrderService extends IService<VoucherOrder> {

    Result orderSeckillVoucher(SeckillVoucher seckillVoucher);

    void submitOrder(VoucherOrder order);
}
