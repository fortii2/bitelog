package me.forty2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.forty2.dto.Result;
import me.forty2.entity.SeckillVoucher;
import me.forty2.entity.VoucherOrder;
import me.forty2.mapper.SeckillVoucherMapper;
import me.forty2.mapper.VoucherOrderMapper;
import me.forty2.service.VoucherOrderService;
import me.forty2.utils.IdGenerator;
import me.forty2.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {

    public static final String ORDER = "order";

    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private VoucherOrderService voucherOrderService;

    @Override
    public Result orderSeckillVoucher(SeckillVoucher seckillVoucher) {
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }

        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }

        RLock lock = redissonClient.getLock("lock:voucher:" + UserHolder.getUser().getId());
        boolean isLock = lock.tryLock();

        if (!isLock) {
            return Result.fail("秒杀券限购一个");
        }

        try {
            return voucherOrderService.tryBuy(seckillVoucher);
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public Result tryBuy(SeckillVoucher seckillVoucher) {
        List<VoucherOrder> voucherOrders = query().getBaseMapper().selectList(
                new LambdaQueryWrapper<VoucherOrder>()
                        .eq(VoucherOrder::getUserId, UserHolder.getUser().getId())
                        .eq(VoucherOrder::getVoucherId, seckillVoucher.getVoucherId())
        );

        if (!voucherOrders.isEmpty()) {
            return Result.fail("秒杀券限购一个");
        }

        int update = seckillVoucherMapper.update(new LambdaUpdateWrapper<SeckillVoucher>()
                .eq(SeckillVoucher::getVoucherId, seckillVoucher.getVoucherId())
                .gt(SeckillVoucher::getStock, 0)
                .setSql("stock = stock - 1")
        );

        if (update < 1) {
            return Result.fail("暂时售空了..");
        }

        long orderId = idGenerator.nextId(ORDER);

        this.save(new VoucherOrder()
                .setId(orderId)
                .setUserId(UserHolder.getUser().getId())
                .setVoucherId(seckillVoucher.getVoucherId())
        );

        return Result.ok(orderId);
    }
}
