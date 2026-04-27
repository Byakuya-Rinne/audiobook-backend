package com.atguigu.tingshu.search.service;

import java.util.Map;

public interface ItemService {

    /**
     * 根据专辑ID汇总详情页所需参数
     * "albumInfo", albumInfo			获取专辑信息
     * "albumStatVo", albumStatVo		获取专辑统计信息
     * "baseCategoryView", baseCategoryView	获取分类信息
     * "announcer", userInfoVo			获取主播信息
     */
    Map<String, Object> getItem(Long albumId);
}
