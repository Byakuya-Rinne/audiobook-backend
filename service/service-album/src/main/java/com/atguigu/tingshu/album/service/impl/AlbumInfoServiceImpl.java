package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.*;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;
import static com.atguigu.tingshu.common.constant.SystemConstant.*;
import static com.atguigu.tingshu.common.result.ResultCodeEnum.ALBUM_NODE_ERROR;
import static com.atguigu.tingshu.common.result.ResultCodeEnum.DATA_ERROR;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

	@Autowired
	private AlbumInfoMapper albumInfoMapper;

	@Autowired
	AlbumAttributeValueMapper albumAttributeValueMapper;

	@Autowired
	private AlbumAttributeValueService albumAttributeValueService;

	@Autowired
	private TrackInfoMapper trackInfoMapper;

	@Autowired
	private AuditService auditService;

	@Autowired
	private RabbitService rabbitService;

	@Autowired
	private UserFeignClient userFeignClient;


	/**
	 * TODO 该接口登录才可以访问
	 * 内容创作者或者平台运营人员-保存专辑
	 *
	 * @param albumInfoVo 对象中属性需要进行合法验证，采用Validation框架进行校验
	 * @return
	 */
	@Override
	public void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId) {
		//标签和专辑多对多，插入专辑表和中间表

		//最终插入类型：AlbumInfo
		AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
		albumInfo.setUserId(userId);

		String payType = albumInfoVo.getPayType();
		//对于付费资源，或VIP免费的资源，设置试听条数
		if (ALBUM_PAY_TYPE_VIPFREE.equals(payType) || ALBUM_PAY_TYPE_REQUIRE.equals(payType)){
			albumInfo.setTracksForFree(1);
			albumInfo.setSecondsForFree(20);
		}
		//暂未审核通过
		albumInfo.setStatus(ALBUM_STATUS_NO_PASS);

		//保存专辑，得到专辑ID
		albumInfoMapper.insert(albumInfo);
		Long albumId = albumInfo.getId();


		//albumAttributeValueVoList里已经有attributeId和valueId，关联表还需要albumId
		List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();


		//循环插入单条，性能差
//		if (CollUtil.isNotEmpty(albumAttributeValueVoList)) {
//			//2.2 为专辑标签关联专辑ID，"批量"新增专辑标签关系
//			for (AlbumAttributeValueVo albumAttributeValueVo : albumAttributeValueVoList) {
//				AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(albumAttributeValueVo, AlbumAttributeValue.class);
//				albumAttributeValue.setAlbumId(albumId);
//				albumAttributeValueMapper.insert(albumAttributeValue);
//			}
//		}

		List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueVoList.stream().map(vo -> {
			AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
			BeanUtil.copyProperties(vo, albumAttributeValue);
			albumAttributeValue.setAlbumId(albumId);
			return albumAttributeValue;
		}).collect(Collectors.toList());

		albumAttributeValueService.saveBatch(albumAttributeValueList);
	}



	@Autowired
	private AlbumStatMapper albumStatMapper;
	/**
	 * 保存专辑统计信息
	 *
	 * @param albumId  专辑ID
	 * @param statType 统计类型
	 * @param statNum  统计数值 0401-播放量 0402-订阅量 0403-购买量 0403-评论数'
	 */
	@Override
	public void saveAlbumInfoStat(Long albumId, String statType, int statNum) {
		AlbumStat albumStat = new AlbumStat();
		albumStat.setAlbumId(albumId);
		albumStat.setStatType(statType);
		albumStat.setStatNum(statNum);
		albumStatMapper.insert(albumStat);
	}

	@Override
	public IPage<AlbumListVo> findUserAlbumPageByUserId(IPage<AlbumListVo> pageInfo, AlbumInfoQuery albumInfoQuery) {
		return albumInfoMapper.findUserAlbumPageByUserId(pageInfo, albumInfoQuery);
	}

	@Override
	public List<AlbumInfo> findUserAllAlbumList(Long userId) {
		return albumInfoMapper.selectList(
			new LambdaQueryWrapper<AlbumInfo>()
					.select(AlbumInfo::getId, AlbumInfo::getAlbumTitle)
					.eq(AlbumInfo::getUserId, userId)
					.orderByDesc(AlbumInfo::getId)
					.last("limit 50")
		);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeAlbumInfo(Long id) {
		//1.判断该专辑下是否关联有声音，如果存在则不允许删除
		if (albumInfoMapper.selectCount(new LambdaQueryWrapper<AlbumInfo>().eq(AlbumInfo::getId, id)) > 0) {
			throw new GuiguException(ALBUM_NODE_ERROR);
		}

		//2.根据专辑主键ID 删除专辑
		albumInfoMapper.deleteById(id);

		//3.删除专辑标签关系
		albumAttributeValueMapper.delete(
				new LambdaQueryWrapper<>(AlbumAttributeValue.class)
						.eq(AlbumAttributeValue::getAlbumId, id)
		);

		//4.删除专辑统计信息
		albumStatMapper.delete(
				new LambdaQueryWrapper<AlbumStat>()
						.eq(AlbumStat::getAlbumId, id)
		);

		//TODO 同时将存在在ES索引库中专辑一并删除

	}

	/**
	 * 查询专辑信息（包含标签列表）
	 * @param id
	 * @return
	 */
	@Override
	public AlbumInfo getAlbumInfo(Long id) {
		AlbumInfo albumInfo = albumInfoMapper.selectById(id);
		//还剩 List<AlbumAttributeValue> albumAttributeValueVoList

		if (albumInfo != null){
			List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueMapper.selectList(
					new LambdaQueryWrapper<AlbumAttributeValue>()
							.eq(AlbumAttributeValue::getAlbumId, id)
			);
			albumInfo.setAlbumAttributeValueVoList(albumAttributeValueList);
		}


		return albumInfo;
	}


	/**
	 * 更新专辑信息
	 * @param id
	 * @param albumInfoVo
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo) {
		//转VO为PO
		AlbumInfo albumInfo = new AlbumInfo();
		BeanUtil.copyProperties(albumInfoVo, albumInfo);
		albumInfo.setId(id);
		albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
		albumInfoMapper.updateById(albumInfo);

		//删除原有标签列表
		albumAttributeValueService.remove(
				new LambdaQueryWrapper<AlbumAttributeValue>()
						.eq(AlbumAttributeValue::getAlbumId, id)
		);

		//保存VO中的AlbumAttributeValueVo
		List<AlbumAttributeValueVo> voList = albumInfoVo.getAlbumAttributeValueVoList();
		if (voList != null && voList.size() > 0) {
			List<AlbumAttributeValue> list = new ArrayList<>();
			for (AlbumAttributeValueVo albumAttributeValueVo : voList) {
				AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(albumAttributeValueVo, AlbumAttributeValue.class);
				albumAttributeValue.setAlbumId(id);
				list.add(albumAttributeValue);
			}
			albumAttributeValueService.saveBatch(list);
		}

		//申鹤妹妹就是萌
		//把所有文本扔到一起，丢给申鹤
		String allText = albumInfoVo.getAlbumTitle() + "，" + albumInfoVo.getAlbumIntro();
		//auditService
//		String suggest = auditService.auditText(allText);
//		if("block".equals(suggest)){
//			albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
//			rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_LOWER, id);
//		}else if("review".equals(suggest)){
//			albumInfo.setStatus(ALBUM_STATUS_ARTIFICIAL);
//		}else if("pass".equals(suggest)){
//			albumInfo.setStatus(ALBUM_STATUS_PASS);
//			rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_UPPER, id);
//		}
		albumInfoMapper.updateById(albumInfo);



	}

	@Override
	public AlbumStatVo getAlbumStatVo(Long albumId) {
		AlbumStatVo albumStatVo = albumStatMapper.getAlbumStatVo(albumId);
		return albumStatVo;
	}


	/**
	 * 如未登录，只返回声音列表
	 * 如果用户已登录，根据当前用户身份、购买情况、专辑付费类型综合判断：付费标识
	 * 分页获取专辑声音列表（动态判断付费标识）
	 * @param albumId
	 * @param page
	 * @param limit
	 * @return Page<AlbumTrackListVo>
	 */
	@Override
	public Page<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, Long userId, Long albumId) {
		/*	Long trackId; 		声音id
			String trackTitle;	标题
			BigDecimal mediaDuration;声音媒体时长，单位秒
			Integer orderNum;	排序
			Integer playStatNum;播放量
			Integer commentStatNum;评论数
			Date createTime;	发布时间
			Boolean isShowPaidMark = false;是否显示付费标识*/

		//不管咋样，先准备好声音列表
		pageInfo = trackInfoMapper.findAlbumTrackPage(pageInfo, albumId);

			//根据专辑ID查询专辑信息 得到专辑付费类型、试听集数
			AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);

			//试听集数
			Integer tracksForFree = albumInfo.getTracksForFree();


		//免费专辑不需要付费
		/*
			("付费类型: 0101-免费、0102-vip免费、0103-付费") String payType;
		 */
		if (albumInfo != null){
			if ("0101".equals(albumInfo.getPriceType())){
				return pageInfo;
			}
		}else {//专辑信息为空
			throw new GuiguException(DATA_ERROR);
		}


		if (userId != null){
			//已登录，考虑付费类型
			//所在专辑是VIP免费，如果用户是VIP则不需要付费
				Boolean isVIP = false;
				//远程调用 用户服务 获取当前用户身份 确定是普通用户还是VIP用户
				UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
				if (userInfoVo.getIsVip() == 1){
					isVIP = true;
				}
				/*
				("付费类型: 0101-免费、0102-vip免费、0103-付费") String payType;
				 */
				if("0102".equals(albumInfo.getPriceType()) && isVIP){
					return pageInfo;
				}

			//所在专辑是专辑付费，如果用户已经购买专辑则不显示
			//所在单击需要单曲购买，如果用户已经购买该单曲则不显示
				//查用户买了哪些专辑和声音

					//专辑中免费试听之后的声音需要检查
					List<Long> needCheckPayStatusTrackIdList = pageInfo.getRecords()
							//⬆ List<AlbumTrackListVo>
							.stream()
							.filter(vo -> vo.getOrderNum() > tracksForFree)
							.map(AlbumTrackListVo::getTrackId)
							.collect(Collectors.toList());

				//用户买了的声音是{trackId:1}，没买是{trackId:0}
				Map<Long, Integer> checkedMap = userFeignClient.userIsPaidTrack(userId, albumId, needCheckPayStatusTrackIdList).getData();

				pageInfo.getRecords().stream()
						//⬆ List<AlbumTrackListVo>
						.forEach(albumTrackListVo -> {
							Long trackId = albumTrackListVo.getTrackId();
							if (checkedMap.containsKey(trackId)){
								albumTrackListVo.setIsShowPaidMark(false);
							}else {
								albumTrackListVo.setIsShowPaidMark(true);
							}});
				return pageInfo;

		}else {
			//未登录，或其他情况，显示付费标识
			//指定的前几免费试听声音不需要付费
			pageInfo.getRecords()
					//⬆ List<AlbumTrackListVo>
					.stream()
					.filter( vo -> vo.getOrderNum() <= tracksForFree )
					.forEach(vo -> vo.setIsShowPaidMark(false));

			//专辑中免费试听之后的声音需要展示
			pageInfo.getRecords()
					//⬆ List<AlbumTrackListVo>
					.stream()
					.filter(vo -> vo.getOrderNum() > tracksForFree)
					.forEach(vo -> vo.setIsShowPaidMark(true));

		}


		return pageInfo;
	}
}
