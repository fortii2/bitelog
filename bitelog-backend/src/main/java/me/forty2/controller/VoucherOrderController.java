package me.forty2.controller;


import me.forty2.dto.Result;
import me.forty2.entity.SeckillVoucher;
import me.forty2.service.SeckillVoucherService;
import me.forty2.service.VoucherOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Autowired
    private SeckillVoucherService seckillVoucherService;

    @Autowired
    private VoucherOrderService voucherOrderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        return voucherOrderService.orderSeckillVoucher(seckillVoucher);
    }
}
