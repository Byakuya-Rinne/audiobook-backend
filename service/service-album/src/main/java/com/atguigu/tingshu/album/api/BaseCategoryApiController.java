package com.atguigu.tingshu.album.api;

import cn.hutool.json.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value="/api/album")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {

	@Autowired
	private BaseCategoryService baseCategoryService;

	//  /api/album/category/getBaseCategoryList
	// 获取专辑列表，直接返回三级列表的id、标题

	@GetMapping("/category/getBaseCategoryList")
	public Result<List<JSONObject>> getBaseCategoryList(){
		List<JSONObject> baseCategoryList = baseCategoryService.getBaseCategoryList();
		return Result.ok(baseCategoryList);
	}


}

