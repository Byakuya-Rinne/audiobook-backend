package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class ItemServiceImpl implements ItemService {

    @Autowired
    private Executor threadPoolExecutor;

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;


    /**
     * 根据专辑ID汇总详情页所需参数
     * "albumInfo", albumInfo			获取专辑信息
     * "albumStatVo", albumStatVo		获取专辑统计信息
     * "baseCategoryView", baseCategoryView	获取分类信息
     * "announcer", userInfoVo			获取主播信息
     */
    @Override
    public Map<String, Object> getItem(Long albumId) {
        Map<String, Object> map = new ConcurrentHashMap<String, Object>();
        //启动多个线程，分步骤调用OpenFeign获取四个结果

        //远程调用专辑服务获取专辑信息
        //supplyAsync有返回值
        CompletableFuture<AlbumInfo> albumInfoFuture = CompletableFuture.supplyAsync(
                () -> {
                    AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
                    map.put("albumInfo", albumInfo);
                    return albumInfo;
                }, threadPoolExecutor
        );


        CompletableFuture<Void> baseCategoryViewFuture = albumInfoFuture.thenAccept(
                albumInfo -> {
                    Long category3Id = albumInfo.getCategory3Id();
                    BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(category3Id).getData();
                    map.put("baseCategoryView", baseCategoryView);
                }
        );

        CompletableFuture<Void> announcerFuture = albumInfoFuture.thenAccept(
                albumInfo -> {
                    Long userId = albumInfo.getUserId();
                    UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
                    map.put("announcer", userInfoVo);
                }
        );


        CompletableFuture<Void> albumStatVoFuture = CompletableFuture.runAsync(
                () -> {
                    AlbumStatVo albumStatVo = albumFeignClient.getAlbumStatVo(albumId).getData();
                    map.put("albumStatVo", albumStatVo);
                }, threadPoolExecutor
        );

        CompletableFuture.allOf(
                albumInfoFuture,
                baseCategoryViewFuture,
                announcerFuture,
                albumStatVoFuture
        ).orTimeout(1, TimeUnit.MINUTES)//等这四个任务一分钟，一分钟没完成就算TimeoutException超时异常
                .join();//阻塞当前线程，直到这个组合 future 完成

        return map;
    }
}
