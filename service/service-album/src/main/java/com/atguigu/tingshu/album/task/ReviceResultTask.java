package com.atguigu.tingshu.album.task;

import cn.hutool.core.collection.CollectionUtil;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;
import static com.atguigu.tingshu.common.constant.SystemConstant.TRACK_STATUS_PASS;

@Slf4j
@Component
public class ReviceResultTask {

    @Autowired
    TrackInfoMapper trackInfoMapper;

    @Autowired
    AuditService auditService;

    @Scheduled(cron = "0 5 * * * *")//秒 分 时 日 月 周 年
    public void reviceResultJob(){
        log.info("定时任务获取声音审核结果");
        //1.根据条件：1.审核中 TRACK_STATUS_REVIEWING 状态  2.限制数量 3.查询声音ID跟审核任务ID
        //获取TrackInfo列表
        List<TrackInfo> trackInfos = trackInfoMapper.selectList(
                new LambdaQueryWrapper<TrackInfo>()
                        .eq(TrackInfo::getStatus, TRACK_STATUS_REVIEWING)
                        .orderByAsc(TrackInfo::getId)
                        .last("limit 10")
        );

        //更新数据库的trackInfo审核状态
        //去auditService向审核服务器询问审核状态
        if (CollectionUtil.isNotEmpty(trackInfos)){
            for (TrackInfo trackInfo : trackInfos) {
                String suggestion = auditService.getReviewTaskResult(trackInfo.getReviewTaskId());
                if (suggestion != null){
                    if ("block".equals(suggestion)){
                        trackInfo.setStatus(TRACK_STATUS_NO_PASS);
                    }else if("review".equals(suggestion)){
                        trackInfo.setStatus(TRACK_STATUS_ARTIFICIAL);
                    }else if("pass".equals(suggestion)){
                        trackInfo.setStatus(TRACK_STATUS_PASS);
                    }
                }else {
                    throw new GuiguException(ResultCodeEnum.DATA_ERROR);
                }
                trackInfoMapper.updateById(trackInfo);
            }
        }



    }








}
