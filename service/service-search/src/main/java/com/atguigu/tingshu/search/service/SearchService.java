package com.atguigu.tingshu.search.service;

public interface SearchService {

    /**
     * 将指定专辑上架到索引库
     *
     * @param albumId 专辑ID
     * @return
     */
    void upperAlbum(Long albumId);
}
