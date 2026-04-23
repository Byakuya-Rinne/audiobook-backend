package com.atguigu.tingshu.account.receiver;

import cn.hutool.core.collection.CollUtil;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class AccountReceiver {

    @Autowired
    private UserAccountService userAccountService;

    /**
     * 用户首次注册成功后，为用户初始账户余额记录
     *
     * @param map   消息对象 包含：用户ID，赠送体验金额，标题，订单编号
     *              map.put("userId", userInfo.getId());
     * 				map.put("amount", new BigDecimal("100"));
     * 				map.put("title", "新用户专项体验金活动");
     * 				map.put("orderNo", "ZS"+IdUtil.getSnowflakeNextId());
     * @param channel
     * @param message
     */
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_USER, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_USER_REGISTER, durable = "true"),
            key = MqConst.ROUTING_USER_REGISTER
    ))
    public void initUserAccount(Map<String, Object> map, Channel channel, Message message) {
        log.info("[账户服务]用户首次注册成功后，为用户初始账户余额记录：{}", map);

        try {
            if (CollUtil.isNotEmpty(map)){
                userAccountService.initUserAccount(map);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            log.error("为用户初始账户余额记录失败，异常信息：{}", e.getMessage());
            throw new GuiguException(500,"为用户初始账户余额记录失败，异常信息：" + e.getMessage());
        }
    }










    }
