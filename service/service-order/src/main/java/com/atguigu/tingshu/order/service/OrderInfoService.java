package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface OrderInfoService extends IService<OrderInfo> {


    OrderInfoVo trade(TradeVo tradeVo);

    Map<String, String> submitOrder(Long userId, OrderInfoVo orderInfoVo);

    OrderInfo saveOrderInfo(Long userId, OrderInfoVo orderInfoVo);

    OrderInfo getOrderInfo(String orderNo);

    Page<OrderInfo> findUserPage(Page<OrderInfo> pageInfo, Long userId);

    void orderPaySuccess(String orderNo);
}
