package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 */
public interface IShopService extends IService<Shop> {

    /**
     * 查询商户信息
     * 如缓存未命中,存入redis中
     *
     * @param id 根据id
     */
    Result queryById(Long id);

    /**
     * 更新数据库(保证缓存一致性)
     * 当数据更新,删除缓存(redis中的数据)
     *
     * @param shop 要更新的商品信息
     * @return 成功或者失败
     */
    Result updateShop(Shop shop);
}
