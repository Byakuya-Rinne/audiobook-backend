package com.atguigu.tingshu.search.service;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.model.search.SuggestIndex;

import java.util.Collection;
import java.util.List;

public interface SearchService {

    /**
     * 将指定专辑上架到索引库
     *
     * @param albumId 专辑ID
     * @return
     */
    void upperAlbum(Long albumId);

    void saveSuggestInfo(Long id, String albumTitle);

    List<String> completeSuggest(String keyword);

    Collection<String> parseSuggestResult(SearchResponse<SuggestIndex> searchResponse, String suggest_name);
}
