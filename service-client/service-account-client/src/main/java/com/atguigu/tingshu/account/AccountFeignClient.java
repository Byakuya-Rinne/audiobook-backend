package com.atguigu.tingshu.account;

import com.atguigu.tingshu.account.impl.AccountDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>
 * 账号模块远程调用API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-account", path = "api/account", fallback = AccountDegradeFeignClient.class)
public interface AccountFeignClient {
    /**
     * 支付方式选择余额支付，执行扣减逻辑
     *
     * @param accountDeductVo 扣减信息vo
     * @return
     */
    @PostMapping("/userAccount/checkAndDeduct")
    public Result checkAndDeduct(@RequestBody AccountDeductVo accountDeductVo);

}
