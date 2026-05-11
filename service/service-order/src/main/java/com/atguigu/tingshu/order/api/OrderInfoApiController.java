package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order")
@SuppressWarnings({"all"})
public class OrderInfoApiController {

	@Autowired
	private OrderInfoService orderInfoService;

	/**
	 * 三种商品（VIP会员、专辑、声音）订单结算,渲染订单结算页面
	 *
	 * @param tradeVo (购买项目类型、购买项目ID、声音数量)
	 * @return 订单VO信息
	 */
	@GuiGuLogin
	@Operation(summary = "三种商品（VIP会员、专辑、声音）订单结算")
	@PostMapping("/orderInfo/trade")
	public Result<OrderInfoVo> trade(@RequestBody TradeVo tradeVo) {
		OrderInfoVo orderInfoVo = orderInfoService.trade(tradeVo);
		return Result.ok(orderInfoVo);
	}


	/**
	 * 提交订单
	 *
	 * @param orderInfoVo
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "提交订单")
	@PostMapping("/orderInfo/submitOrder")
	public Result<Map<String, String>> submitOrder(@RequestBody @Validated OrderInfoVo orderInfoVo) {
		//1.获取当前登录用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.调用业务逻辑提交订单
		Map<String, String> map = orderInfoService.submitOrder(userId, orderInfoVo);
		return Result.ok(map);
	}

	/**
	 * 根据订单编号查询订单详情（包含订单明细列表，减免列表）
	 * @param orderNo
	 * @return
	 */
	@Operation(summary = "根据订单编号查询订单详情（包含订单明细列表，减免列表）")
	@GetMapping("/orderInfo/getOrderInfo/{orderNo}")
	public Result<OrderInfo> getOrderInfo(@PathVariable String orderNo){
		OrderInfo orderInfo = orderInfoService.getOrderInfo(orderNo);
		return Result.ok(orderInfo);
	}


	/**
	 * 分页查询订单(包含订单明细、减免列表)
	 * @param page
	 * @param limit
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "分页查询某用户的订单")
	@GetMapping("/orderInfo/findUserPage/{page}/{limit}")
	public Result<Page<OrderInfo>> findUserPage(@PathVariable Long page, @PathVariable Long limit){
		Long userId = AuthContextHolder.getUserId();
		Page<OrderInfo> pageInfo = new Page<>(page, limit);
		pageInfo = orderInfoService.findUserPage(pageInfo, userId);
		return Result.ok(pageInfo);
	}



	/**
	 * 用户支付成功后，修改订单状态，虚拟物品发货
	 * @param orderNo
	 * @return
	 */
	@Operation(summary = "用户支付成功后，修改订单状态，虚拟物品发货")
	@GetMapping("/orderInfo/orderPaySuccess/{orderNo}")
	public Result orderPaySuccess(@PathVariable String orderNo){
		orderInfoService.orderPaySuccess(orderNo);
		return Result.ok();
	}





}

