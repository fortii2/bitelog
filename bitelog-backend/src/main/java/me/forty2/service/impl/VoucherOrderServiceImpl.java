package me.forty2.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import me.forty2.dto.Result;
import me.forty2.entity.SeckillVoucher;
import me.forty2.entity.VoucherOrder;
import me.forty2.mapper.SeckillVoucherMapper;
import me.forty2.mapper.VoucherOrderMapper;
import me.forty2.service.VoucherOrderService;
import me.forty2.utils.IdGenerator;
import me.forty2.utils.RedisConstants;
import me.forty2.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {

    public static final String ORDER = "order";

    private BlockingQueue<VoucherOrder> voucherOrdersQueue = new ArrayBlockingQueue<>(65535);

    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    private class VoucherOrderHandler implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    VoucherOrder order = voucherOrdersQueue.take();

                    voucherOrderService.submitOrder(order);
                } catch (Exception e) {
                    log.error("order handler error.");
                }
            }
        }
    }

    @Transactional
    public void submitOrder(VoucherOrder order) {
        seckillVoucherMapper.update(new LambdaUpdateWrapper<SeckillVoucher>()
                .eq(SeckillVoucher::getVoucherId, order.getVoucherId())
                .setSql("stock = stock - 1")
        );

        this.save(order);
    }

    @Lazy // ensure representer is created well during post-construction period
    @Autowired
    private VoucherOrderService voucherOrderService;

    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("SeckillVoucher.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    @Override
    public Result orderSeckillVoucher(SeckillVoucher seckillVoucher) {
        long result = stringRedisTemplate.execute(SECKILL_SCRIPT, List.of(RedisConstants.SECKILL_STOCK_KEY, RedisConstants.SECKILL_ORDER_KEY), seckillVoucher.getVoucherId().toString(), UserHolder.getUser().getId().toString());

        if (result == 1) {
            return Result.fail("stock is 0.");
        }

        if (result == 2) {
            return Result.fail("can only buy one item.");
        }

        long orderId = idGenerator.nextId(ORDER);

        // put into blocking queue
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setVoucherId(seckillVoucher.getVoucherId());
        voucherOrdersQueue.add(voucherOrder);

        return Result.ok(orderId);
    }

    /**
     * @Override public Result orderSeckillVoucher(SeckillVoucher seckillVoucher) {
     * if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
     * return Result.fail("秒杀尚未开始");
     * }
     * <p>
     * if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
     * return Result.fail("秒杀已经结束");
     * }
     * <p>
     * RLock lock = redissonClient.getLock("lock:voucher:" + UserHolder.getUser().getId());
     * boolean isLock = lock.tryLock();
     * <p>
     * if (!isLock) {
     * return Result.fail("秒杀券限购一个");
     * }
     * <p>
     * try {
     * return voucherOrderService.tryBuy(seckillVoucher);
     * } finally {
     * lock.unlock();
     * }
     * }
     **/

    /**
     @Transactional public Result tryBuy(SeckillVoucher seckillVoucher) {
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
     **/
}
