package com.atguigu.tingshu.album.impl;


import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AlbumDegradeFeignClient implements AlbumFeignClient {


    @Override
    public Result<AlbumInfo> getAlbumInfo(Long id) {
        log.error("[专辑服务]提供远程调用方法getAlbumInfo执行服务降级");
        return null;
    }

    @Override
    public Result<BaseCategoryView> getCategoryView(Long category3Id) {
        log.error("[专辑服务]提供远程调用方法getCategoryView执行服务降级");
        return null;
    }

    @Override
    public Result<AlbumStatVo> getAlbumStatVo(Long albumId) {
        log.error("[专辑服务]远程调用getAlbumStatVo执行服务降级");
        return null;
    }

    @Override
    public Result<List<TrackInfo>> findPaidTrackInfoList(Long trackId, Integer trackCount) {
        log.error("[专辑服务]远程调用findPaidTrackInfoList执行服务降级");
        return null;
    }

    @Override
    public Result<TrackInfo> getTrackInfo(Long id) {
        log.error("[专辑服务]提供远程调用接口getTrackInfo执行了服务降级");
        return null;
    }
}
