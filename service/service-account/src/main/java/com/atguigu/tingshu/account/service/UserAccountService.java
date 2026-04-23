package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Map;

public interface UserAccountService extends IService<UserAccount> {

    /**
     * 用户首次注册成功后，为用户初始账户余额记录
     *
     * @param map 消息对象 包含：用户ID，赠送体验金额，标题，订单编号
     *            	map.put("userId", userInfo.getId());
     * 				map.put("amount", new BigDecimal("100"));
     * 				map.put("title", "新用户专项体验金活动");
     * 				map.put("orderNo", "ZS"+IdUtil.getSnowflakeNextId());
     */
    void initUserAccount(Map<String, Object> map);

    /**
     * 新增账户变动日志
     * @param userId 用户ID
     * @param title 内容
     * @param tradeType 交易类型
     * @param amount 金额
     * @param orderNo 订单编号
     */
    void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo);

}
