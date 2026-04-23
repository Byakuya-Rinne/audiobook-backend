package com.atguigu.tingshu.user.api;


import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 微信一键登录
     *
     * @param code 小程序端根据当前微信，生成访问为微信服务端临时凭据
     * @return {token:令牌}
     */
    @Operation(summary = "微信一键登录")
    @GetMapping("/wxLogin/{code}")
    public Result<Map<String, String>> exLogin(@PathVariable("code") String code) {
        Map<String, String> map = userInfoService.wxLogin(code);
        return Result.ok(map);
    }


    /**
     * 获取当前登录用户基本信息
     *
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "获取当前登录用户基本信息")
    @GetMapping("/getUserInfo")
    public Result<UserInfoVo> getUserInfo() {
        Long userId = AuthContextHolder.getUserId();
        UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
        return Result.ok(userInfoVo);
    }



}
