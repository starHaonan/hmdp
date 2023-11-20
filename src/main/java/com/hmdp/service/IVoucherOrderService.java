package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 优惠券秒杀
     *
     * @param voucherId 用户正在抢购的优惠券id
     * @return 订单id
     */
    Result seckillVoucher(Long voucherId);

    /**
     * 动态代理,实现事务功能
     *
     */
    void createVoucherOrder(VoucherOrder voucherOrder);
}
