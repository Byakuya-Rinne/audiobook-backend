package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

	@Autowired
	private TrackInfoMapper trackInfoMapper;

	@Autowired
	private AlbumInfoMapper albumInfoMapper;

	@Autowired
	private VodService vodService;

	/**
	 * 保存声音
	 *
	 * @param trackInfoVo
	 * @param userId
	 */
	@Override
	public void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId) {

		//1.根据专辑ID查询专辑信息 用于更新声音数量
		AlbumInfo oldAlbumInfo = albumInfoMapper.selectById(trackInfoVo.getAlbumId());

		//2.新增声音记录
		//2.1 将声音VO转为PO
		TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);

		//2.2 给属性赋值
		//2.2.1 设置用户ID
		trackInfo.setUserId(userId);

		//2.2.2 设置声音序号 要求从1开始递增
		trackInfo.setAlbumId(oldAlbumInfo.getId() + 1);

		//2.2.3 调用腾讯点播平台获取音频详情信息：时长、大小、类型
		TrackMediaInfoVo mediaInfoVo = vodService.getMediaInfo(trackInfo.getMediaFileId());
		if (mediaInfoVo != null) {
			trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfoVo.getDuration()));
			trackInfo.setMediaSize(mediaInfoVo.getSize());
			trackInfo.setMediaType(mediaInfoVo.getType());
		}

		//2.2.4 来源：用户上传
		trackInfo.setSource(TRACK_SOURCE_USER);

		//2.2.5 状态：待审核


		//2.2.6 封面图片 如果未提交使用所属专辑封面
		if(trackInfoVo.getCoverUrl() == null || "".equals(trackInfoVo.getCoverUrl())){
			trackInfo.setCoverUrl(oldAlbumInfo.getCoverUrl());
		}

		//2.3 新增声音记录

		//3. 更新专辑信息：包含声音数量

		//4.新增声音统计记录

		//5.TODO 对点播平台音频文件进行审核（异步审核）



	}
}
