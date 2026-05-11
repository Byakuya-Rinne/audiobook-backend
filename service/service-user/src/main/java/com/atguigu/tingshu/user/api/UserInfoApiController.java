package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserInfoApiController {

	@Autowired
	private UserInfoService userInfoService;
	/**
	 * 根据用户ID查询用户/主播基本信息
	 * @param userId
	 * @return
	 */
	@Operation(summary = "根据用户ID查询用户/主播基本信息")
	@GetMapping("/userInfo/getUserInfoVo/{userId}")
	public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId){
		UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
		return Result.ok(userInfoVo);
	}


	/**
	 * 提交需要检查购买状态声音ID列表，响应每个声音购买状态
	 * @param userId
	 * @param albumId
	 * @param needCheckPayStatusTrackIdList 待检查购买状态声音ID列表
	 * @return
	 */
	@Operation(summary = "提交需要检查购买状态声音ID列表，响应每个声音购买状态")
	@PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
	public Result<Map<Long, Integer>> userIsPaidTrack(@PathVariable Long userId,
													  @PathVariable Long albumId,
													  @RequestBody List<Long> needCheckPayStatusTrackIdList){
		Map<Long, Integer> map = userInfoService.userIsPaidTrack(userId, albumId, needCheckPayStatusTrackIdList);
		return Result.ok(map);
	}

	/**
	 * 根据专辑id+用户ID获取用户已购买声音id列表
	 * @param albumId
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "根据专辑id+用户ID获取用户已购买声音id列表")
	@GetMapping("/userInfo/findUserPaidTrackList/{albumId}")
	public Result<List<Long>> findUserPaidTrackIdList(@PathVariable Long albumId){
		//1.获取当前用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.调用业务
		List<Long> list = userInfoService.findUserPaidTrackIdList(userId, albumId);
		//3.响应结果
		return Result.ok(list);
	}


	/**
	 * 为了获取当前用户ID，确保调用方请求头比如有：token
	 * 判断当前用户是否购买指定专辑
	 * @param albumId
	 * @return 购买状态：true:已购买专辑、 false:未购买专辑
	 */
	@GuiGuLogin  //
	@Operation(summary = "判断当前用户是否购买指定专辑")
	@GetMapping("/userInfo/isPaidAlbum/{albumId}")
	public Result<Boolean> isPaidAlbum(@PathVariable Long albumId){
		//1.获取当前用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.调用业务
		Boolean isPaid = userInfoService.userIsPaidAlbum(userId, albumId);
		//3.响应结果
		return Result.ok(isPaid);
	}

	/**
	 * 由于后续微信支付成功后，同样需要进行权益发放，微信异步回调没有token令牌 故不要加@GuiGuLogin注解
	 * 支付成功后权益方法（虚拟物品发货）
	 * @param userPaidRecordVo
	 * @return
	 */
	@Operation(summary = "支付成功后权益方法（虚拟物品发货）")
	@PostMapping("/userInfo/savePaidRecord")
	public Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo){
		userInfoService.savePaidRecord(userPaidRecordVo);
		return Result.ok();
	}


	/**
	 * 更新VIP状态：处理过期会员
	 * @return
	 */
	@Operation(summary = "更新VIP状态：处理过期会员")
	@GetMapping("/updateVipExpireStatus")
	public Result updateVipExpireStatus(){
		Date now = new Date();
		userInfoService.updateVipExpireStatus(now);
		return Result.ok();
	}


}

