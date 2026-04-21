package com.atguigu.tingshu.album.api;

import cn.hutool.core.io.FileTypeUtil;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
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












	}

