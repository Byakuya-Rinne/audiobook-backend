package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaUserService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.*;
import com.atguigu.tingshu.user.mapper.*;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

	@Autowired
	private UserPaidAlbumMapper userPaidAlbumMapper;

	@Autowired
	private UserPaidTrackMapper userPaidTrackMapper;

	@Autowired
	private AlbumFeignClient albumFeignClient;

	@Autowired
	private UserVipServiceMapper userVipServiceMapper;

	@Autowired
	private VipServiceConfigMapper vipServiceConfigMapper;

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

	/**
	 * 查询指定用户某个专辑下声音购买状态
	 *
	 * @param userId                       用户ID
	 * @param albumId                      专辑ID
	 * @param needCheckPayStateTrackIdList 待检查购买状态声音ID列表
	 * @return {声音ID：购买状态1(已购买) 0 (未购买)}
	 */
	@Override
	public Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckPayStatusTrackIdList) {
		Map<Long, Integer> map = new HashMap<>();

		//如果用户购买了整张专辑，则把传入的list全设为已购买
			//查询用户是否已经购买本专辑
		List<UserPaidAlbum> userPaidAlbums = userPaidAlbumMapper
				.selectList(
						new LambdaQueryWrapper<UserPaidAlbum>()
								.eq(UserPaidAlbum::getUserId, userId)
								.eq(UserPaidAlbum::getAlbumId, albumId));
		//如果用户已经购买整个专辑，则把该专辑所属-所有需要检查付费状态的声音，全标为已购买
		if (CollUtil.isNotEmpty(userPaidAlbums)){
			needCheckPayStatusTrackIdList.stream().forEach(t->{
				map.put(t,1);
			});
			return map;
		}

		//查询用户买过的本专辑声音
		List<UserPaidTrack> userPaidTracks = userPaidTrackMapper.selectList(
				new LambdaQueryWrapper<UserPaidTrack>()
						.eq(UserPaidTrack::getUserId, userId)
						.eq(UserPaidTrack::getAlbumId, albumId)
						.select(UserPaidTrack::getTrackId));
		List<Long> paidTrackIds = userPaidTracks.stream().map(track -> track.getTrackId()).collect(Collectors.toList());
		if (CollUtil.isEmpty(paidTrackIds)){
			//用户都没买
			needCheckPayStatusTrackIdList.stream().forEach(t->{
				map.put(t,0);
			});
			return map;
		}
		//如果传入的待检查声音，在购买过的本专辑声音里，则把它标为已购买
		for (Long trackId : needCheckPayStatusTrackIdList) {
			//遍历需要检查的id
			if (paidTrackIds.contains(trackId)){
				//如果需要检查的id在已购买的本专辑声音id里，则把该id放到map里，标记已购买
				map.put(trackId,1);
			}else {
				//如果需要检查的id不在已购买的本专辑声音id里，则把该id放到map里，标记未购买
				map.put(trackId,0);
			}
		}
		return map;
	}


	@Override
	public Boolean userIsPaidAlbum(Long userId, Long albumId) {
		Long count = userPaidAlbumMapper.selectCount(
				new LambdaQueryWrapper<UserPaidAlbum>()
						.eq(UserPaidAlbum::getAlbumId, albumId)
						.eq(UserPaidAlbum::getUserId, userId)
		);
		return count > 0;
	}

	@Override
	public List<Long> findUserPaidTrackIdList(Long userId, Long albumId) {
		List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(
				new LambdaQueryWrapper<UserPaidTrack>()
						.eq(UserPaidTrack::getAlbumId, albumId)
						.eq(UserPaidTrack::getUserId, userId)
						.select(UserPaidTrack::getTrackId)
		);
		if (CollUtil.isNotEmpty(userPaidTrackList)) {
			List<Long> paidTrackIdList = userPaidTrackList.stream()
					.map(UserPaidTrack::getTrackId)
					.collect(Collectors.toList());
			return paidTrackIdList;
		}
		return List.of();
	}


	@Override
	public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
		//项目类型: 1001-专辑 1002-声音 1003-vip会员
		String itemType = userPaidRecordVo.getItemType();
		if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(itemType)){
			//1.处理购买类型是专辑
			//1.1 根据订单编号查询专辑购买记录表，验证这比订单是否重复处理
			Long count = userPaidAlbumMapper.selectCount(new LambdaQueryWrapper<UserPaidAlbum>()
					.eq(UserPaidAlbum::getOrderNo, userPaidRecordVo.getOrderNo())
			);
			if (count == 0){//本次订单不重复，执行插入；否则略过不报错
				UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
				userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
				userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
				userPaidAlbum.setAlbumId(//专辑一次只能买一张，list长度一定是1
						userPaidRecordVo.getItemIdList().get(0)
				);
				userPaidAlbumMapper.insert(userPaidAlbum);
			}

		}else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(itemType)){
			//2.处理购买类型是声音
			//2.1 根据订单编号查询声音购买记录，验证这比订单是否重复处理
			Long count = userPaidTrackMapper.selectCount(
					new LambdaQueryWrapper<UserPaidTrack>()
							.eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo())
			);
			if (count == 0) {
				//2.1 新增声音购买记录 可能存在多条声音购买记录
				List<Long> itemIdList = userPaidRecordVo.getItemIdList();
				//2.2 远程调用"专辑服务获取声音信息" 得到专辑ID
				TrackInfo trackInfo = albumFeignClient.getTrackInfo(itemIdList.get(0)).getData();
				Long albumId = trackInfo.getAlbumId();
				//2.2 新增声音购买记录（等同于发放权益）
				for (Long itemId : itemIdList) {
					UserPaidTrack userPaidTrack = new UserPaidTrack();
					userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
					userPaidTrack.setUserId(userPaidRecordVo.getUserId());
					userPaidTrack.setAlbumId(albumId);
					userPaidTrack.setTrackId(itemId);
					userPaidTrackMapper.insert(userPaidTrack);
				}
			}

		}else if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(itemType)){
			//3.处理购买类型是VIP会员
			//3.1 根据订单编号查询会员购买记录，验证这比订单是否重复处理
			Long count = userVipServiceMapper.selectCount(
					new LambdaQueryWrapper<UserVipService>()
							.eq(UserVipService::getOrderNo, userPaidRecordVo.getOrderNo())
			);
			if (count == 0) {
				//3.2 获取当前用户身份，是否为VIP会员
				Boolean isVIP = false;
				UserInfoVo userInfoVo = this.getUserInfo(userPaidRecordVo.getUserId());
				if (userInfoVo.getIsVip().intValue() == 1 && userInfoVo.getVipExpireTime().after(new Date())) {
					isVIP = true;
				}
				//3.3 封装会员购买记录
				UserVipService userVipService = new UserVipService();
				userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
				userVipService.setUserId(userPaidRecordVo.getUserId());
				//3.3.2 根据用户选择套餐ID查询套餐信息
				VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(userPaidRecordVo.getItemIdList().get(0));
				Integer serviceMonth = vipServiceConfig.getServiceMonth();
				//3.3.1 本次会员起始时间 如果用户是普通用户=当前时间 如果是VIP获取当前用户会员失效时间+1天
				//      本次会员过期时间 如果用户是普通用户=当前时间+服务月数 如果是VIP=现有会员过期时间+服务月数
				if (isVIP){
					DateTime startTime = DateUtil.offsetDay(userInfoVo.getVipExpireTime(), 1);
					userVipService.setStartTime(startTime);
					userVipService.setExpireTime(DateUtil.offsetMonth(startTime, serviceMonth));
				}else {
					userVipService.setStartTime(new Date());
					userVipService.setExpireTime(DateUtil.offsetMonth(new Date(), serviceMonth));
				}

				//userVipService.setIsAutoRenew();
				//userVipService.setNextRenewTime();

				//3.4 新增会员购买记录
				userVipServiceMapper.insert(userVipService);


				//3.5 更新用户信息表会员标识以及过期时间
				UserInfo userInfo = new UserInfo();
				userInfo.setId(userPaidRecordVo.getUserId());
				userInfo.setIsVip(1);
				userInfo.setVipExpireTime(userVipService.getExpireTime());
				userInfoMapper.updateById(userInfo);

			}
		}
	}

	@Override
	public void updateVipExpireStatus(Date now) {
//		//1.找出会员已失效用户列表
//		List<UserInfo> userInfoList = userInfoMapper.selectList(
//				new LambdaQueryWrapper<UserInfo>()
//						.eq(UserInfo::getIsVip, 1)
//						.lt(UserInfo::getVipExpireTime, now)
//						.select(UserInfo::getId)
//		);
//		//2.更新会员标识
//		if(CollUtil.isNotEmpty(userInfoList)){
//			for (UserInfo userInfo : userInfoList) {
//				userInfo.setIsVip(0);
//				userInfoMapper.updateById(userInfo);
//			}
//		}

		LambdaUpdateWrapper<UserInfo> wrapper = new LambdaUpdateWrapper<>();
		wrapper.set(UserInfo::getIsVip, 0)
				.eq(UserInfo::getIsVip, 1)
				.lt(UserInfo::getVipExpireTime, now);
		userInfoMapper.update(null, wrapper);
	}

}
