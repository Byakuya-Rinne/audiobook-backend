package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search")
@SuppressWarnings({"all"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;

    /**
     * 上架专辑，把指定专辑添加到索引库
     * @param albumId 专辑ID
     * @return
     */
    @Operation(summary = "将指定专辑上架到索引库")
    @GetMapping("/albumInfo/upperAlbum/{albumId}")
    public Result upperAlbum(@PathVariable Long albumId){
        searchService.upperAlbum(albumId);
        return Result.ok();
    }

    /**
     * 搜索自动补全
     *
     * @param keyword 用户已录入内容：汉字、拼音、拼音首字母
     * @return 自动补全待选文本列表
     */
    @Operation(summary = "搜索自动补全")
    @GetMapping("/albumInfo/completeSuggest/{keyword}")
    public Result<List<String>> completeSuggest(@PathVariable String keyword) {
        List<String> list = searchService.completeSuggest(keyword);
        return Result.ok(list);
    }











    }

