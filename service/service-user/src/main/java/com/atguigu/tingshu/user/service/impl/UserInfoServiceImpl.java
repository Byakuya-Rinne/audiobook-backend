package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaUserService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.atguigu.tingshu.common.result.ResultCodeEnum.FAIL;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

	@Autowired
	private UserInfoMapper userInfoMapper;

	@Autowired
	private WxMaService wxMaService;

	@Autowired
	private RabbitService rabbitService;
    @Qualifier("redisTemplate")
    @Autowired
    private RedisTemplate redisTemplate;


	/**
	 * 微信一键登录
	 *
	 * @param code 小程序端根据当前微信，生成访问为微信服务端临时凭据
	 * @return
	 */

	@Override
	public Map<String, String> wxLogin(String code) {

		try {
			//1.拿着临时凭据+应用ID+应用秘钥 调用微信接口 获取当前微信账户唯一标识：openId
			//1.1 微信账户信息业务类
			WxMaUserService userService = wxMaService.getUserService();
			//1.2 获取会话信息
			WxMaJscode2SessionResult sessionInfo = userService.getSessionInfo(code);
			//1.3 获取微信账号唯一标识
			String openid = sessionInfo.getOpenid();
			//2.根据微信账户唯一标识，查询数据库，看当前微信是否已经注册
			UserInfo userInfo = userInfoMapper.selectOne(
					new LambdaQueryWrapper<UserInfo>()
							.eq(UserInfo::getWxOpenId, openid)
			);
			if (userInfo == null){
				//3.如果微信账户首次注册
				// 	新增用户记录，为用户初始化账户记录用于后续订单支付
				//3.1 userInfo表新增用户记录 绑定微信账户唯一标识openid
				userInfo = new UserInfo();
				userInfo.setWxOpenId(openid);
				userInfo.setNickname(IdUtil.nanoId());
				userInfo.setAvatarUrl("http://192.168.200.6:9000/tingshu/2025-07-19/20108a4b-1ad0-4191-8605-dd12d54c8641.png");
				userInfoMapper.insert(userInfo);
				//3.2 TODO 为当前注册用户初始化账户记录
				// 方案一：Openfeign远程调用 分布式事务问题  方案二：采用MQ可靠性消息队列实现数据最终一致✔
				//3.2.1 构建消息对象 注意：如果是VO对象一定要实现序列化接口以及生成序列化版本号
				Map<String, Object> map = new HashMap<>();
				map.put("userId", userInfo.getId());
				map.put("amount", new BigDecimal("100"));
				map.put("title", "新用户专项体验金活动");
				map.put("orderNo", "ZS"+IdUtil.getSnowflakeNextId());
				//3.2.2 发送消息到MQ
				rabbitService.sendMessage(MqConst.EXCHANGE_USER, MqConst.ROUTING_USER_REGISTER, map);
				//未注册用户完成注册
			}
			//处理已注册用户的登录
			//4.基于当前用户信息生成令牌
			//4.1 创建令牌
			String token = IdUtil.randomUUID();

			//4.2 构建登录成功后Redis的Key 形式为：user:login:token
			String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;

			//4.3 构建登录成功后Redis的Value 形式为：userInfoVo
			UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);

			//4.4 存入Redis 设置有效期：7天
			redisTemplate.opsForValue().set(loginKey, userInfoVo, 7, TimeUnit.DAYS);

			//5.封装令牌{token:令牌}返回给前端
			Map<String, String> map = new HashMap<>();
			map.put("token", token);
			return map;


		} catch (Exception e){
			log.error("微信登录失败");
			throw new GuiguException(500, "微信登录失败");
		}
	}


	@Override
	public UserInfoVo getUserInfo(Long userId) {
		UserInfo userInfo = userInfoMapper.selectById(userId);
		if (userInfo != null) {
			return BeanUtil.copyProperties(userInfo, UserInfoVo.class);
		}
		return null;
	}

	/**
	 * 修改当前用户基本信息
	 * @param userInfoVo
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateUser(Long userId, UserInfoVo userInfoVo) {
		//只允许修改昵称、头像!!!
		UserInfo userInfo = userInfoMapper.selectById(userId);
		userInfo.setNickname(userInfoVo.getNickname());
		userInfo.setAvatarUrl(userInfoVo.getAvatarUrl());
		int i = userInfoMapper.updateById(userInfo);
		if (i != 1){
			throw new GuiguException(FAIL);
		}
	}



}
