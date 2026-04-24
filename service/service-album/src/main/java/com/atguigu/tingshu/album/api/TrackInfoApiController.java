package com.atguigu.tingshu.album.api;

import cn.hutool.core.io.FileTypeUtil;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.atguigu.tingshu.album.service.VodService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.atguigu.tingshu.common.constant.SystemConstant.AUDIO_TYPES;
import static com.atguigu.tingshu.common.result.ResultCodeEnum.ARGUMENT_VALID_ERROR;
import static com.atguigu.tingshu.common.result.ResultCodeEnum.SERVICE_ERROR;


@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

	@Autowired
	private TrackInfoService trackInfoService;

	@Autowired
	private VodService vodService;

	/**
	 * 音视频文件上传点播平台
	 * @param file
	 * @return {mediaFileId:"文件唯一标识",mediaUrl:"在线播放地址"}
	 */
	@Operation(summary = "音视频文件上传点播平台")
	@PostMapping("/trackInfo/uploadTrack")
	public Result<Map<String, String>> uploadTrack(@RequestParam("file")MultipartFile file){

        try {
			String type = FileTypeUtil.getType(file.getInputStream());
			// 如果不是常见的音频后缀：mp3, wav, m4a, flac, ogg 等;
			if (!AUDIO_TYPES.contains(type.toLowerCase())){
				throw new GuiguException(ARGUMENT_VALID_ERROR);
			}
		} catch (Exception e) {
            throw new GuiguException(SERVICE_ERROR);
        }
        Map<String, String> map = vodService.uploadTrack(file);
		return Result.ok(map);
	}

	/**
	 * TODO 该接口登录才可以访问
	 * 保存声音
	 * @param trackInfoVo
	 * @return
	 */
	@Operation(summary = "保存声音")
	@PostMapping("/trackInfo/saveTrackInfo")
	public Result saveTrackInfo(@Validated @RequestBody TrackInfoVo trackInfoVo, BindingResult bindingResult) {

		// 手动判断是否有校验错误
		if (bindingResult.hasErrors()) {
			String errorMsg = bindingResult.getFieldError().getDefaultMessage();
			// 2. 抛出自定义异常
			throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR.getCode(), errorMsg);
		}
		Long userId = AuthContextHolder.getUserId();

		trackInfoService.saveTrackInfo(trackInfoVo, userId);
		return Result.ok();

	}




	/**
	 * 当前接口必须才能访问
	 * 条件分页查询当前用户声音列表
	 *
	 * @param page           页码
	 * @param limit          页大小
	 * @param trackInfoQuery 查询条件
	 * @return MP分页对象
	 */
//	@GuiGuLogin
	@Operation(summary = "条件分页查询当前用户声音列表")
	@PostMapping("/trackInfo/findUserTrackPage/{page}/{limit}")
	public Result<Page<TrackListVo>> findUserTrackPage(
			@PathVariable Long page,
			@PathVariable Long limit,
			@RequestBody TrackInfoQuery trackInfoQuery
	) {
		//1.获取用户ID
		Long userId = AuthContextHolder.getUserId();
		trackInfoQuery.setUserId(userId);
		//2.封装分页对象、查询对象
		Page<TrackListVo> pageInfo = new Page<>(page, limit);
		//3.调用业务层，分页获取数据
		pageInfo = trackInfoService.findUserTrackPage(pageInfo, trackInfoQuery);
		//4.响应结果
		return Result.ok(pageInfo);
	}



	/**
	 * 根据声音ID查询声音信息
	 * @param id
	 * @return
	 */
	@Operation(summary = "根据声音ID查询声音信息")
	@GetMapping("/trackInfo/getTrackInfo/{id}")
	public Result<TrackInfo> getTrackInfo(@PathVariable Long id){
		TrackInfo trackInfo = trackInfoService.getById(id);
		return Result.ok(trackInfo);
	}

	/**
	 * 修改声音信息
	 * @param id 声音Id
	 * @param trackInfoVo 声音信息VO
	 * @return
	 */
	@Operation(summary = "修改声音信息")
	@PutMapping("/trackInfo/updateTrackInfo/{id}")
	public Result updateTrackInfo(@PathVariable Long id,@Validated @RequestBody TrackInfoVo trackInfoVo){
		trackInfoService.updateTrackInfo(id, trackInfoVo);
		return Result.ok();
	}



	@Operation(summary = "删除声音")
	@DeleteMapping("/trackInfo/removeTrackInfo/{id}")
	public Result removeTrackInfo(@PathVariable Long id){
		trackInfoService.removeTrackInfo(id);
		return Result.ok();
	}




}

