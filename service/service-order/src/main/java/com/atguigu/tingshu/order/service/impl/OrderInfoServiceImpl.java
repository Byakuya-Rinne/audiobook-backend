package com.atguigu.tingshu.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderDerateService;
import com.atguigu.tingshu.order.service.OrderDetailService;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;
import static com.atguigu.tingshu.common.rabbit.constant.MqConst.EXCHANGE_CANCEL_ORDER;
import static com.atguigu.tingshu.common.rabbit.constant.MqConst.ROUTING_CANCEL_ORDER;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private AccountFeignClient accountFeignClient;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private OrderDerateService orderDerateService;

    @Autowired
    private RabbitService rabbitService;

    @Value("${order.cancel}")
    private Integer cancelOrderTTL;

    /**
     * 三种商品（VIP会员、专辑、声音）订单结算,渲染订单结算页面
     *
     * @param tradeVo (购买项目类型、购买项目ID、声音数量)
     * @return 订单VO信息
     */
    @Override
    public OrderInfoVo trade(TradeVo tradeVo) {
        /*
        TradeVo：
            @Schema(description = "付款项目类型: 1001-专辑 1002-声音 1003-vip会员", requiredMode = Schema.RequiredMode.REQUIRED)
            private String itemType;
            @Schema(description = "付款项目类型Id", requiredMode = Schema.RequiredMode.REQUIRED)
            private Long itemId;
            @Schema(description = "针对声音购买，购买当前集往后多少集", required = false)
            private Integer trackCount;
         */

        /*
        OrderInfoVo：
            @Schema(description = "交易号", requiredMode = Schema.RequiredMode.REQUIRED)
            private String tradeNo;

            @Schema(description = "支付方式：1101-微信 1102-支付宝 1103-账户余额", requiredMode = Schema.RequiredMode.REQUIRED)
            private String payWay;

            @Schema(description = "付款项目类型: 1001-专辑 1002-声音 1003-vip会员", requiredMode = Schema.RequiredMode.REQUIRED)
            private String itemType;

            @Schema(description = "订单原始金额", requiredMode = Schema.RequiredMode.REQUIRED)
            private BigDecimal originalAmount;

            @Schema(description = "减免总金额", requiredMode = Schema.RequiredMode.REQUIRED)
            private BigDecimal derateAmount;

            @Schema(description = "订单总金额", requiredMode = Schema.RequiredMode.REQUIRED)
            private BigDecimal orderAmount;

            @Schema(description = "订单明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
            private List<OrderDetailVo> orderDetailVoList;

            @Schema(description = "订单减免明细列表")
            private List<OrderDerateVo> orderDerateVoList;

            @Schema(description = "时间戳", requiredMode = Schema.RequiredMode.REQUIRED)
            private Long timestamp;

            @Schema(description = "签名", requiredMode = Schema.RequiredMode.REQUIRED)
            private String sign;
         */
        Long userId = AuthContextHolder.getUserId();

        OrderInfoVo orderInfoVo = new OrderInfoVo();
        orderInfoVo.setItemType(tradeVo.getItemType());
        orderInfoVo.setDerateAmount(BigDecimal.ZERO);

        //声明2个集合：商品明细、优惠列表
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);

        //处理不同要买的东西：
        //ORDER_ITEM_TYPE_VIP   ORDER_ITEM_TYPE_ALBUM   ORDER_ITEM_TYPE_TRACK
        if (ORDER_ITEM_TYPE_VIP.equals(tradeVo.getItemType())){
            VipServiceConfig vipServiceConfig = userFeignClient.getVipServiceConfig(tradeVo.getItemId()).getData();
            Assert.notNull(vipServiceConfig, "套餐：{}不存在", tradeVo.getItemId());
            BigDecimal price = vipServiceConfig.getPrice();//原价
            BigDecimal discountPrice = vipServiceConfig.getDiscountPrice();//折后价

            orderInfoVo.setOriginalAmount(price);
            orderInfoVo.setOrderAmount(discountPrice);

            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemPrice(price);//订单细节price填原价
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());//图片链接url
            orderDetailVo.setItemName("套餐：" + vipServiceConfig.getName());
            orderDetailVoList.add(orderDetailVo);
            orderInfoVo.setOrderDetailVoList(orderDetailVoList);

            BigDecimal agio = price.subtract(discountPrice);
            //差价不等于0，发生了折扣，否则没有折扣
            if (agio.compareTo(BigDecimal.ZERO) != 0){
                orderInfoVo.setDerateAmount(agio);

                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateAmount(agio);
                orderDerateVo.setDerateType(ORDER_DERATE_VIP_SERVICE_DISCOUNT);
                orderDerateVo.setRemarks("限时套餐优惠");
                orderDerateVoList.add(orderDerateVo);
                orderInfoVo.setOrderDerateVoList(orderDerateVoList);

            }

        }else if (ORDER_ITEM_TYPE_ALBUM.equals(tradeVo.getItemType())){
            //处理项目类型是：专辑
            //远程调用"用户服务"判断是否重复购买专辑
            Boolean isPaidAlbum = userFeignClient.userIsPaidAlbum(tradeVo.getItemId()).getData();
            if (isPaidAlbum){//用户买过这个专辑了
                throw new GuiguException(500, "您已购买本专辑，请勿他妈的重复购买");
            }//没买，现在买

            //远程调用"专辑"服务获取专辑信息,得到价格、以及折扣（普通用户，VIP折扣）
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(tradeVo.getItemId()).getData();
            BigDecimal price = albumInfo.getPrice();
            BigDecimal discount = albumInfo.getDiscount();//普通用户折扣
            BigDecimal vipDiscount = albumInfo.getVipDiscount();//VIP用户折扣
            BigDecimal derateAmount = BigDecimal.valueOf(0l);//优惠了多少钱

            //封装"商品"相关价格
            BigDecimal originalAmount = price;
            BigDecimal orderAmount = originalAmount;

            //远程调用"用户服务"获取用户身份用于确认折扣信息
            if (vipDiscount != null && (!(new BigDecimal("-1").equals(vipDiscount)))){//VIP有特殊打折才去查用户是不是vip
                UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
                Assert.notNull(userInfoVo, "用户：{}不存在", userId);
                Boolean isVIP = false;
                //如果用户是VIP，且他妈的过期时间比现在晚
                if (userInfoVo.getIsVip().intValue() == 1
                        && userInfoVo.getVipExpireTime().after(new Date())) {
                    isVIP = true;
                }
                //如果存在会员用户折扣且当前用户为VIP用户
                if (isVIP){
                    orderAmount = originalAmount.multiply(vipDiscount)
                            .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                    derateAmount = originalAmount.subtract(orderAmount);
                }
            }

            //如果VIP不享受折扣，而普通用户享受折扣，则VIP也享受普通用户的折扣
            //如果orderAmount等于originalAmount，说明已未享受VIP折扣，进入普通用户路线，否则跳过
            if (orderAmount.equals(originalAmount)){
                //如果存在普通用户折扣
                if (discount != null && (!(new BigDecimal("-1").equals(discount)))){
                    orderAmount = originalAmount.multiply(discount)
                            .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                    derateAmount = originalAmount.subtract(orderAmount);
                }
            }

            //封装"商品"列表及商品优惠列表
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName("专辑：" + albumInfo.getAlbumTitle());
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemPrice(originalAmount);
            orderDetailVoList.add(orderDetailVo);

            //封装优惠细节list，如果有优惠才往里装，没有就空着
            if ( new BigDecimal(0L).compareTo(derateAmount) > 0 ){
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateType(ORDER_DERATE_ALBUM_DISCOUNT);
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setRemarks("专辑限时优惠");
                orderDerateVoList.add(orderDerateVo);
            }

            orderInfoVo.setOriginalAmount(originalAmount);
            orderInfoVo.setOrderAmount(orderAmount);
            orderInfoVo.setDerateAmount(derateAmount);

        }else if (ORDER_ITEM_TYPE_TRACK.equals(tradeVo.getItemType())){
            //处理项目类型是：声音
            //远程调用"专辑服务"获取待购买声音列表，将声音作为商品展示结算页
            Long trackId = tradeVo.getItemId();
            List<TrackInfo> waitBuyTrackInfoList = albumFeignClient.findPaidTrackInfoList(trackId, tradeVo.getTrackCount()).getData();
            Assert.notNull(waitBuyTrackInfoList, "暂无结算声音");

            Long albumId = waitBuyTrackInfoList.get(0).getAlbumId();
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            //如果是购买单集，则priceType就是按单集购买的类型，且price是单集价格
            BigDecimal trackPrice = albumInfo.getPrice();

            //订单总价格就是待购买单集数量*单集价格
            //声音不支持折扣，优惠价0，最后价格=原价
            BigDecimal totalPrice = BigDecimal.valueOf(waitBuyTrackInfoList.size()).multiply(trackPrice);
            orderInfoVo.setOrderAmount(totalPrice);
            orderInfoVo.setOrderAmount(totalPrice);
            orderInfoVo.setDerateAmount(BigDecimal.ZERO);
            orderInfoVo.setOrderDerateVoList(List.of());

            orderDetailVoList = waitBuyTrackInfoList.stream()
                    .map(trackInfo -> {
                        OrderDetailVo orderDetailVo = new OrderDetailVo();
                        orderDetailVo.setItemPrice(trackPrice);
                        orderDetailVo.setItemId(trackInfo.getId());
                        orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                        orderDetailVo.setItemName("声音：" + trackInfo.getTrackTitle());
                        return orderDetailVo;
                    })
                    .collect(Collectors.toList());

            orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        }
/*
        @Schema(description = "交易号", requiredMode = Schema.RequiredMode.REQUIRED)
        private String tradeNo;

        @Schema(description = "支付方式：1101-微信 1102-支付宝 1103-账户余额", requiredMode = Schema.RequiredMode.REQUIRED)
        private String payWay;

        @Schema(description = "时间戳", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long timestamp;

        @Schema(description = "签名", requiredMode = Schema.RequiredMode.REQUIRED)
        private String sign;
*/

        //生成订单流水号，作用：防止重复提交订单，五分钟过期
        String tradeKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        String tradeNo = IdUtil.fastUUID();

        redisTemplate.opsForValue().set(tradeKey, tradeNo, 5, TimeUnit.MINUTES);
        orderInfoVo.setTradeNo(tradeNo);
        orderInfoVo.setTimestamp(System.currentTimeMillis());


        Map<String, Object> map = BeanUtil.beanToMap(orderInfoVo, false, true);
        String sign = SignHelper.getSign(map);
        orderInfoVo.setSign(sign);

        return orderInfoVo;
    }


    /**
     * 提交/结算订单（处理余额支付逻辑）
     *
     * @param userId      用户ID
     * @param orderInfoVo 订单vo信息
     * @return {"orderNo":"本次订单保存后订单编号"} 用于后续对接微信支付或者展示订单详情
     */
    @Override
    public Map<String, String> submitOrder(Long userId, OrderInfoVo orderInfoVo) {
        //校验流水号
        //前端传入流水号，去redis查传入的和里面的是不是一样
        //一样就是没重复提交，用完删掉；否则不一样就是重复提交订单，报错
        String tradeNo = orderInfoVo.getTradeNo();
        String tradeKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        String lua = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                "then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end";

        DefaultRedisScript<Long> script = new DefaultRedisScript(lua, Long.class);
        Long redisResult =  (Long) redisTemplate.execute(script, CollUtil.toList(tradeKey), tradeNo);
        if (redisResult.intValue() == 0) {
            throw new GuiguException(500, "流水号校验失败");
        }

        //校验签名
//        Map<String, Object> map = BeanUtil.beanToMap(orderInfoVo, false, true);
//        String sign = SignHelper.getSign(map);
//        orderInfoVo.setSign(sign);
        //使用之前加签名一样方式，获得新签名
        Map<String, Object> map = BeanUtil.beanToMap(orderInfoVo, false, true);
        //之前没有带payWay，这次也不能带
        map.remove("payWay");
        //调用工具类验证签名
        SignHelper.checkSign(map);

        //校验通过
        //保存订单
        OrderInfo orderInfo = this.saveOrderInfo(userId, orderInfoVo);

        //如果支付方式为余额支付 立即扣减账户余额、余额扣减修改订单状态：已支付 并且发放权益
        //支付方式：1101-微信 1102-支付宝 1103-账户余额
        String payWay = orderInfoVo.getPayWay();

        //余额
        if (ORDER_PAY_ACCOUNT.equals(payWay)) {
            //远程调用"账户服务"扣减账户余额
            AccountDeductVo vo = new AccountDeductVo();
            vo.setOrderNo(orderInfo.getOrderNo());
            vo.setAmount(orderInfo.getOrderAmount());
            vo.setUserId(userId);
            vo.setContent(orderInfo.getOrderTitle());

            Result accountFeignResult = accountFeignClient.checkAndDeduct(vo);

            if ( accountFeignResult.getCode() != 200){
                throw new GuiguException(accountFeignResult.getCode(), accountFeignResult.getMessage());
            }

            //余额扣减成功，将订单状态改为：已支付
            orderInfo.setOrderStatus(ORDER_STATUS_PAID);
            orderInfoMapper.updateById(orderInfo);

            //远程调用"用户服务"进行相关权益发放（虚拟物品发货）
            //创建用于虚拟物品发货vo对象
            UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
            userPaidRecordVo.setOrderNo(orderInfo.getOrderNo());
            userPaidRecordVo.setUserId(orderInfo.getUserId());
            userPaidRecordVo.setItemType(orderInfo.getItemType());
            List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
            if (CollUtil.isNotEmpty(orderDetailVoList)) {
                List<Long> itemIdList = orderDetailVoList.stream().map(orderDetailVo -> {
                    Long itemId = orderDetailVo.getItemId();
                    return itemId;
                }).collect(Collectors.toList());
                userPaidRecordVo.setItemIdList(itemIdList);
                Result result = userFeignClient.savePaidRecord(userPaidRecordVo);
                //4.3.3 判断业务状态码是否为200
                if (result.getCode() != 200){
                    throw new GuiguException(result.getCode(), result.getMessage());
                }
            }
        }//余额支付处理完成

        //5.无论是哪种付款方式，采用延迟消息自动将超时未支付订单取消掉  自动关单时间阈值：15分钟
        //方案一：采用RabbitMQ延迟消息  方案二:采用定时任务  方案三：不做处理 当进行查询判断订单是否过期
        rabbitService.sendDealyMessage(EXCHANGE_CANCEL_ORDER, ROUTING_CANCEL_ORDER, orderInfo.getId(), cancelOrderTTL);

        //6.返回本次订单订单编号，用于后续支付成功后查询订单、或者基于订单编号对接微信支付
        return Map.of("orderNo", orderInfo.getOrderNo());
    }





    /**
     * 保存订单信息
     *
     * @param userId      用户ID
     * @param orderInfoVo 订单VO信息
     * @return 订单对象
     */
    @Override
    public OrderInfo saveOrderInfo(Long userId, OrderInfoVo orderInfoVo) {
        //保存订单信息

        //将订单VO转为订单PO对象
        OrderInfo orderInfo = BeanUtil.copyProperties(orderInfoVo, OrderInfo.class);
        orderInfo.setUserId(userId);

        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
        if (CollUtil.isNotEmpty(orderDetailVoList)) {
            String itemName = orderDetailVoList.get(0).getItemName();
            orderInfo.setOrderTitle(itemName);
        }

        //设置订单序号 要求：全局唯一趋势递增 形式=日期+雪花算法
        String orderNo = DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId();
        orderInfo.setOrderNo(orderNo);
        //设置订单状态：订单状态：0901-未支付 0902-已支付 0903-已取消
        orderInfo.setOrderStatus(ORDER_STATUS_UNPAID);
        //先保存不完整的订单信息 得到订单ID，后面再补充细节
        orderInfoMapper.insert(orderInfo);
        Long orderId = orderInfo.getId();


        //	private List<OrderDetail> orderDetailList;
        //	private List<OrderDerate> orderDerateList;

        if (CollUtil.isNotEmpty(orderDetailVoList)){
            List<OrderDetail> orderDetailList = orderDetailVoList.stream()
                    .map(vo -> {
                        OrderDetail orderDetail = BeanUtil.copyProperties(vo, OrderDetail.class);
                        orderDetail.setOrderId(orderId);
                        return orderDetail;
                    }).collect(Collectors.toList());
            orderDetailService.saveBatch(orderDetailList);
            orderInfo.setOrderDetailList(orderDetailList);
        }


        //专辑和VIP才有折扣，买单曲没有
        List<OrderDerateVo> orderDerateVoList = orderInfoVo.getOrderDerateVoList();
        if (CollUtil.isNotEmpty(orderDerateVoList)){
            List<OrderDerate> orderDerateList = orderDerateVoList.stream()
                    .map(vo -> {
                        OrderDerate orderDerate = BeanUtil.copyProperties(vo, OrderDerate.class);
                                orderDerate.setOrderId(orderId);
                                return orderDerate;
                            }
                    ).collect(Collectors.toList());
            orderDerateService.saveBatch(orderDerateList);
            orderInfo.setOrderDerateList(orderDerateList);
        }else {
            orderInfo.setOrderDerateList(List.of());
        }

        return orderInfo;
    }

    /**
     * 根据订单编号查询订单详情（包含订单明细列表，减免列表）
     *
     * @param orderNo
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getOrderNo, orderNo)
        );

        /*
            @Schema(description = "订单明细列表")
            @TableField(exist = false)
            private List<OrderDetail> orderDetailList;

            @Schema(description = "订单减免明细列表")
            @TableField(exist = false)
            private List<OrderDerate> orderDerateList;

            @TableField(exist = false)
            private String orderStatusName;
            @TableField(exist = false)
            private String payWayName;
         */
        List<OrderDetail> orderDetailList = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>()
                        .eq(OrderDetail::getOrderId, orderInfo.getId())
        );
        orderInfo.setOrderDetailList(orderDetailList);

        List<OrderDerate> orderDerateList = orderDerateService.list(
                new LambdaQueryWrapper<OrderDerate>()
                        .eq(OrderDerate::getOrderId, orderInfo.getId())
        );
        orderInfo.setOrderDerateList(orderDerateList);
        return orderInfo;
    }

    @Override
    public Page<OrderInfo> findUserPage(Page<OrderInfo> pageInfo, Long userId) {
//        pageInfo = orderInfoMapper.findUserPage(pageInfo, userId);
        Page<OrderInfo> orderInfoPage = orderInfoMapper.selectPage(pageInfo,
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getUserId, userId)
                        .orderByDesc(OrderInfo::getId));
        if (CollUtil.isNotEmpty(orderInfoPage.getRecords())) {
            return orderInfoPage;
        }

//      List<OrderDetail> orderDetailList;未赋值
//TODO 作业：获取本页订单，获取订单ID列表， 根据订单ID列表查询订单明细集合 转为 Map<订单ID, List<订单明细>> 组装订单中明细属性
        List<Long> orderIdList = orderInfoPage.getRecords().stream()
                .map(orderInfo -> {
                    Long orderId = orderInfo.getId();
                    return orderId;
                }).collect(Collectors.toList());

        List<OrderDetail> orderDetailList = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>()
                        .in(OrderDetail::getOrderId, orderIdList)
        );

        Map<Long, List<OrderDetail>> detailMap = orderDetailList.stream().collect(Collectors.groupingBy(OrderDetail::getOrderId));
        orderInfoPage.getRecords().forEach(info -> {
            Long orderId = info.getId();
            List<OrderDetail> orderDetails = detailMap.get(orderId);
            info.setOrderDetailList(orderDetails);
        });

        return orderInfoPage;
    }


    @Override
    public void orderPaySuccess(String orderNo) {
        //1.更新订单状态
        int update = orderInfoMapper.update(
                null,
                new LambdaUpdateWrapper<OrderInfo>()
                        .eq(OrderInfo::getOrderNo, orderNo)
                        .eq(OrderInfo::getOrderStatus, ORDER_STATUS_UNPAID)
                        .set(OrderInfo::getOrderStatus, ORDER_STATUS_PAID)
        );
        if (update > 0) {
            //2.虚拟物品发货
            //2.1 构建虚拟物品发货记录VO对象
            OrderInfo orderInfo =
                    orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
            UserPaidRecordVo vo = new UserPaidRecordVo();
            vo.setOrderNo(orderNo);
            vo.setUserId(orderInfo.getUserId());
            vo.setItemType(orderInfo.getItemType());
            List<OrderDetail> orderDetailList = orderDetailService.list(
                    new LambdaQueryWrapper<OrderDetail>()
                            .eq(OrderDetail::getOrderId, orderInfo.getId())
            );
            List<Long> itemIdList = orderDetailList.stream().map(OrderDetail::getItemId).collect(Collectors.toList());
            vo.setItemIdList(itemIdList);

            //2.2 远程调用"用户服务"虚拟物品发货
            Result result = userFeignClient.savePaidRecord(vo);
            //2.3 判断响应业务状态码
            if (result.getCode().intValue() != 200) {
                throw new GuiguException(result.getCode(), result.getMessage());
            }
        }
    }
}
