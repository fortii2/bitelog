package me.forty2.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.forty2.dto.Result;
import me.forty2.entity.SeckillVoucher;
import me.forty2.entity.Voucher;
import me.forty2.mapper.VoucherMapper;
import me.forty2.service.SeckillVoucherService;
import me.forty2.service.VoucherService;
import me.forty2.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements VoucherService {

    @Autowired
    private SeckillVoucherService seckillVoucherService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        log.info("starting seckillVoucher..");
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        log.info("saving seckillVoucher..");
        seckillVoucherService.save(seckillVoucher);
        log.info("finished seckillVoucher..");

        stringRedisTemplate.opsForValue().set(
                RedisConstants.SECKILL_STOCK_KEY + seckillVoucher.getVoucherId().toString(),
                seckillVoucher.getStock().toString()
        );
    }
}
