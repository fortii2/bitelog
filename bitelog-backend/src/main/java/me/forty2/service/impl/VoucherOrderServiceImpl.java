package me.forty2.service.impl;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {

    public static final String ORDER = "order";

    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper;

    @Autowired
    private IdGenerator idGenerator;

    @Override
    @Transactional
    public Result orderSeckillVoucher(SeckillVoucher seckillVoucher) {
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }

        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
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
