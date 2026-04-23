package com.atguigu.tingshu.user.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.client.impl.UserDegradeFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>
 * 用户模块远程调用API接口
 * </p>
 *
 * @author atguigu
 */

@FeignClient(value = "service-user", fallback = UserDegradeFeignClient.class)
public interface UserFeignClient {


    /**
     * 根据用户ID查询用户/主播基本信息
     * @param userId
     * @return
     */
    @GetMapping("/userInfo/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId);

}
