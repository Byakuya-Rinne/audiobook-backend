package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TrackInfoService extends IService<TrackInfo> {

    void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId);

    void saveTrackStat(Long trackId, String statType, int statNum);

    void updateTrackInfo(Long id, TrackInfoVo trackInfoVo);
}
