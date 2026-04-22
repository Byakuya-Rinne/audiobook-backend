package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	@Autowired
	private AuditService auditService;

	@Autowired
	private TrackStatMapper trackStatMapper;


	/**
	 * 保存声音
	 *
	 * @param trackInfoVo
	 * @param userId
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId) {
		//1.根据专辑ID查询专辑信息 用于更新声音数量
		AlbumInfo oldAlbumInfo = albumInfoMapper.selectById(trackInfoVo.getAlbumId());
		//2.保存声音记录、更新专辑内包含声音数量
		//2.1 将声音VO转为PO对象
		TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
		if (trackInfo == null){
			throw new GuiguException(ResultCodeEnum.SERVICE_ERROR);
		}

		//2.2 封装声音属性信息
		//2.2.1 基础：用户ID、状态、来源、封面图片
		trackInfo.setUserId(userId);

		//2.2.2 设置序号=专辑包含声音数量+1
		trackInfo.setOrderNum(oldAlbumInfo.getIncludeTrackCount() + 1);

		//2.2.3 从腾讯点播平台获取，声音时长、大小、类型
		TrackMediaInfoVo mediaInfoVo = vodService.getMediaInfo(trackInfo.getMediaFileId());
		if (mediaInfoVo != null) {
			trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfoVo.getDuration()));
			trackInfo.setMediaSize(mediaInfoVo.getSize());
			trackInfo.setMediaType(mediaInfoVo.getType());
		}

		//2.2.4 来源：用户上传
		trackInfo.setSource(TRACK_SOURCE_USER);

		//2.2.5 状态：待审核
		trackInfo.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);

		//2.2.6 封面图片 如果未提交使用所属专辑封面
		if(trackInfoVo.getCoverUrl() == null || "".equals(trackInfoVo.getCoverUrl().trim())){
			trackInfo.setCoverUrl(oldAlbumInfo.getCoverUrl());
		}



		//2.4 更新专辑包含声音数量
		oldAlbumInfo.setIncludeTrackCount(oldAlbumInfo.getIncludeTrackCount() + 1);


		//4.对声音中文本进行内容审核 TODO: 审核声音、新更改的图片等
		String text = trackInfo.getTrackTitle() + trackInfo.getTrackIntro();
		String suggest = auditService.auditText(text);
		if("block".equals(suggest)){
			trackInfo.setStatus(TRACK_STATUS_NO_PASS);
		}else if("review".equals(suggest)){
			trackInfo.setStatus(TRACK_STATUS_ARTIFICIAL);
		}else if("pass".equals(suggest)){
			trackInfo.setStatus(TRACK_STATUS_PASS);
			//5.对上传的声音文件发起审核任务ID，关联审核任务ID
			String taskId = auditService.startReviewTask(trackInfo.getMediaFileId());
			trackInfo.setReviewTaskId(taskId);
			trackInfo.setStatus(TRACK_STATUS_REVIEWING);
		}
		//2.3 保存声音得到声音ID
		trackInfoMapper.insert(trackInfo);
		Long trackInfoId = trackInfo.getId();

		//3.新增统计信息
		this.saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_PLAY, 0);
		this.saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_COLLECT, 0);
		this.saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_PRAISE, 0);
		this.saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_COMMENT, 0);
		trackInfoMapper.updateById(trackInfo);
	}





	@Override
	public void saveTrackStat(Long trackId, String statType, int statNum) {
		TrackStat trackStat = new TrackStat();
		trackStat.setTrackId(trackId);
		trackStat.setStatType(statType);
		trackStat.setStatNum(statNum);
		trackStatMapper.insert(trackStat);
	}



	/**
	 * 修改声音信息
	 *
	 * @param id          声音Id
	 * @param trackInfoVo 声音信息VO
	 * @return
	 */
	@Override
	public void updateTrackInfo(Long id, TrackInfoVo trackInfoVo) {
		Boolean isNeedReview = false;
		//1.判断音频文件是否更新，如果更新，再次获取新音频文件详情，TODO 对新音频再次进行审核
		//1.1 根据声音ID查询声音信息，获取原来的音频ID
		TrackInfo trackInfo = this.getById(id);
		String oldMediaFileId = trackInfo.getMediaFileId();

		//1.2 封装更新后：标题、简介、封面图片、音频URL、唯一标识
		BeanUtil.copyProperties(trackInfoVo, trackInfo);

		//1.3 对比声音vo中音频ID判断是否变更
		if (!oldMediaFileId.equals(trackInfoVo.getMediaFileId())) {
			//1.4 如果变更，再次调用平台接口获取音频详情，更新音频相关信息：时长、大小、类型、播放地址、
			TrackMediaInfoVo oldMediaInfo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
			trackInfo.setMediaDuration(BigDecimal.valueOf(oldMediaInfo.getDuration()));
			trackInfo.setMediaType(oldMediaInfo.getType());
			trackInfo.setMediaSize(oldMediaInfo.getSize());
			//1.5 将旧音频文件从点播平台删除
			vodService.deleteMedia(oldMediaFileId);
			isNeedReview = true;
		}



		//2.更新声音信息，修改后文本同样需要进行内容审核
		trackInfo.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);

		//3.对声音中文本进行内容审核
		String suggest = auditService.auditText(trackInfo.getTrackTitle()
				+ ", "
				+ trackInfo.getTrackIntro());
		switch (suggest){
			case "block": trackInfo.setStatus(TRACK_STATUS_NO_PASS);break;
			case "review": trackInfo.setStatus(TRACK_STATUS_ARTIFICIAL);break;
			case "pass":{
				if (isNeedReview == true){
//					图片
//					String coverImageSuggest = auditService.auditImage(trackInfo.getCoverUrl());
//					if ( "pass".equals(coverImageSuggest)){
//						trackInfo.setStatus(TRACK_STATUS_PASS);break;
//					}else if ("review".equals(coverImageSuggest)){
//						trackInfo.setStatus(TRACK_STATUS_REVIEWING);break;
//					}else if ("block".equals(coverImageSuggest)){
//						trackInfo.setStatus(TRACK_STATUS_NO_PASS);break;
//					}
					//文字通过，审核音频
					String taskId = auditService.startReviewTask(trackInfo.getMediaFileId());
					trackInfo.setReviewTaskId(taskId);
					trackInfo.setStatus(TRACK_STATUS_REVIEWING);
				}else {
					trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);break;
				}
			}
		}
		trackInfoMapper.updateById(trackInfo);
	}


	/**
	 * 条件分页查询当前用户声音列表
	 *
	 * @param pageInfo       分页对象
	 * @param trackInfoQuery 查询条件
	 * @return 分页对象
	 */
	@Override
	public Page<TrackListVo> findUserTrackPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery) {
		pageInfo = trackInfoMapper.findUserTrackPage(pageInfo, trackInfoQuery);
		return pageInfo;
	}






	}
