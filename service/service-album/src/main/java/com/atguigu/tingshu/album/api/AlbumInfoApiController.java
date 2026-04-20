package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

	@Autowired
	private AlbumInfoService albumInfoService;


	/**
	 * TODO 该接口登录才可以访问
	 * 内容创作者或者平台运营人员-保存专辑
	 *
	 * @param albumInfoVo 对象中属性需要进行合法验证，采用Validation框架进行校验
	 * @return
	 */
	@Operation(summary = "内容创作者或者平台运营人员-保存专辑")
	@PostMapping("/albumInfo/saveAlbumInfo")
	public Result saveAlbumInfo(@Validated @RequestBody AlbumInfoVo albumInfoVo) {
		//1.动态获取用户ID
		Long userId = AuthContextHolder.getUserId();
		albumInfoService.saveAlbumInfo(albumInfoVo, userId);
		return Result.ok();
	}

	@Operation(summary = "分页查询当前用户专辑列表")
	@PostMapping("/albumInfo/findUserAlbumPage/{page}/{limit}")
	public Result<IPage<AlbumListVo>> findUserAlbumPageByUserId(@PathVariable Long page,
																@PathVariable Long limit,
																@RequestBody AlbumInfoQuery albumInfoQuery){
		//1.从ThreadLocal获取当前登录用户ID
		Long userId = AuthContextHolder.getUserId();

		//自定义分页方法
		IPage<AlbumListVo> pageInfo = new Page<>(page, limit);
		//3.调用service方法完成条件分页查询,其他分页数据：总记录数、总页数、当前页数据在持久层查询DB后封装
		albumInfoQuery.setUserId(userId);
		pageInfo = albumInfoService.findUserAlbumPageByUserId(pageInfo, albumInfoQuery);
		//4.将分页结果对象响应
		return Result.ok(pageInfo);
	}





}

