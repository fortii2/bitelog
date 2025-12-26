package me.forty2.service;

import com.baomidou.mybatisplus.extension.service.IService;
import me.forty2.dto.Result;
import me.forty2.entity.Voucher;

public interface VoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
