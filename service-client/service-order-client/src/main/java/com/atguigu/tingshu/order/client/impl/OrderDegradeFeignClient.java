package com.atguigu.tingshu.order.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j

@Component
public class OrderDegradeFeignClient implements OrderFeignClient {
    @Override
    public Result<OrderInfo> getOrderInfo(String orderNo) {
        log.error("[订单服务]提供远程调用getOrderInfo服务降级");

        return null;
    }

    @Override
    public Result orderPaySuccess(String orderNo) {
        log.error("[订单服务]提供远程调用orderPaySuccess服务降级");

        return null;
    }
}
