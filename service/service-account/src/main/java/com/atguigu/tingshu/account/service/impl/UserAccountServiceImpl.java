package com.atguigu.tingshu.account.service.impl;

import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

	@Autowired
	private UserAccountMapper userAccountMapper;

	@Autowired
	private UserAccountDetailMapper userAccountDetailMapper;

	/**
	 * 用户首次注册成功后，为用户初始账户余额记录
	 *
	 * @param map 消息对象 包含：用户ID，赠送体验金额，标题，订单编号
	 *        	map.put("userId", userInfo.getId());
	 *			map.put("amount", new BigDecimal("100"));
	 *			map.put("title", "新用户专项体验金活动");
	 *			map.put("orderNo", "ZS"+IdUtil.getSnowflakeNextId());
	 */
	@Override
	public void initUserAccount(Map<String, Object> map) {
		Long userId = (Long) map.get("userId");
		BigDecimal amount = (BigDecimal) map.get("amount");
		String title = (String) map.get("title");
		String orderNo = (String) map.get("orderNo");
		UserAccount userAccount = new UserAccount();
		userAccount.setUserId(userId);
		userAccount.setTotalAmount(amount);
		userAccount.setAvailableAmount(amount);
		userAccount.setTotalIncomeAmount(amount);
		userAccount.setLockAmount(new BigDecimal("0.00"));
		userAccount.setTotalPayAmount(new BigDecimal("0.00"));
		userAccountMapper.insert(userAccount);
		this.saveUserAccountDetail( userId, title, SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT, amount, orderNo);
	}


	/**
	 * 新增账户变动日志
	 * @param userId 用户ID
	 * @param title 内容
	 * @param tradeType 交易类型
	 * @param amount 金额
	 * @param orderNo 订单编号
	 */
	@Override
	public void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo) {
		UserAccountDetail userAccountDetail = new UserAccountDetail();
		userAccountDetail.setUserId(userId);
		userAccountDetail.setTitle(title);
		userAccountDetail.setTradeType(tradeType);
		userAccountDetail.setAmount(amount);
		userAccountDetail.setOrderNo(orderNo);
		userAccountDetailMapper.insert(userAccountDetail);

	}


	@Override
	public BigDecimal getAvailableAmount(Long userId) {
		UserAccount userAccount = userAccountMapper
				.selectOne(new LambdaQueryWrapper<UserAccount>()
						.eq(UserAccount::getUserId, userId));
		Assert.notNull(userAccount, "账户记录不存在");
		BigDecimal availableAmount = userAccount.getAvailableAmount();
		return availableAmount;
	}



	@Override
	public void checkAndDeduct(AccountDeductVo accountDeductVo) {
		BigDecimal deductAmount = accountDeductVo.getAmount();
		Long userId = AuthContextHolder.getUserId();

		//扣减账户余额
		//为了避免并发事务对同一条账户记录造成“超扣”问题，采用MySQL悲观锁 select SQL+for update; 其他并发事务无法操作记录
		UserAccount userAccount = userAccountMapper.selectAccountWithLock(userId, deductAmount);
		if (userAccount == null) {
			throw new GuiguException(500, "余额不足，穷B");
		}

		//扣减账户余额
		int update = userAccountMapper.update(null,
				new LambdaUpdateWrapper<UserAccount>()
						.set(UserAccount::getAvailableAmount, userAccount.getAvailableAmount().subtract(deductAmount))
						.set(UserAccount::getTotalAmount, userAccount.getTotalAmount().subtract(deductAmount))
						.set(UserAccount::getTotalPayAmount, userAccount.getTotalPayAmount().add(deductAmount))
						.eq(UserAccount::getUserId, userId)
		);
		if (update < 1) {
			throw new GuiguException(500, "扣减余额失败！！！");
		}

		//新值账户变动日志
		this.saveUserAccountDetail(
				accountDeductVo.getUserId(),
				accountDeductVo.getContent(),
				SystemConstant.ACCOUNT_TRADE_TYPE_MINUS,
				accountDeductVo.getAmount(),
				accountDeductVo.getOrderNo()
		);
	}







}
