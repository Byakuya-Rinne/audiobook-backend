package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class UserAccountApiController {

	@Autowired
	private UserAccountService userAccountService;

	/**
	 * 查询当前用户可用账户余额
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "查询当前用户可用账户余额")
	@GetMapping("/userAccount/getAvailableAmount")
	public Result<BigDecimal> getAvailableAmount(){
		Long userId = AuthContextHolder.getUserId();
		BigDecimal availavleAmount = userAccountService.getAvailableAmount(userId);
		return Result.ok(availavleAmount);
	}
	/**
	 * 支付方式选择余额支付，执行扣减逻辑
	 *
	 * @param accountDeductVo 扣减信息vo
	 * @return
	 */
	@Operation(summary = "余额扣减（检查并且扣减余额）")
	@PostMapping("/userAccount/checkAndDeduct")
	public Result checkAndDeduct(@RequestBody AccountDeductVo accountDeductVo) {
		userAccountService.checkAndDeduct(accountDeductVo);
		return Result.ok();
	}







}

