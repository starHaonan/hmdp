package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;


@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 首页店铺类型
     */
    @Override
    public Result queryTypeLists() {
        //从redis中查询商铺类型
        String shopTypeJson = stringRedisTemplate.opsForList().leftPop(CACHE_SHOP_TYPE_KEY);
        if (StrUtil.isNotBlank(shopTypeJson)) {
            //存在,直接返回
            List<ShopType> shopTypeList = JSONUtil.toList(shopTypeJson, ShopType.class);
            return Result.ok(shopTypeList);
        }

        //不存在,从数据库中查询数据. 并且把查询到的数据,存到redis中(缓存)
        List<ShopType> shopTypesList =
                query().orderByAsc("sort").list();
        //数据库中也没有数据
        if (shopTypesList.isEmpty()) {
            return Result.fail("店铺类型不存在!");
        }
        stringRedisTemplate.opsForList().leftPush(CACHE_SHOP_TYPE_KEY, JSONUtil.toJsonStr(shopTypesList));
        return Result.ok(shopTypesList);
    }
}
