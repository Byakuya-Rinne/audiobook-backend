package com.atguigu.tingshu.account.impl;


import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccountDegradeFeignClient implements AccountFeignClient {

    @Override
    public Result checkAndDeduct(AccountDeductVo accountDeductVo) {
        log.error("[账户服务]执行checkAndDeduct服务降级");
        return null;
    }
}
