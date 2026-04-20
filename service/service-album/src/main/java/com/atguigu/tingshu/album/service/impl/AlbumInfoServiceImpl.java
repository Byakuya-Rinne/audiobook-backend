package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;

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
}
