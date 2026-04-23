package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {

    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;



    /**
     * 将指定专辑上架到索引库
     *
     * @param albumId 专辑ID
     * @return
     */
    @Override
    public void upperAlbum(Long albumId) {
        //1.创建索引库文档对象 AlbumInfoIndex
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();

        //2.封装文档对象中专辑相关信息及标签列表
        //2.1 远程调用专辑服务获取专辑信息及标签列表
        AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
        Assert.notNull(albumInfo,  "专辑:{}不存在", albumId);
        List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();

        //2.2 封装专辑信息到索引库文档对象中
        BeanUtil.copyProperties(albumInfo, albumInfoIndex);

        //2.3 封装专辑标签列表到索引库文档对象中, 把albumAttributeValueVoList塞给albumInfoIndex的 List<AttributeValueIndex> attributeValueIndexList
        List<AttributeValueIndex> indexList = albumAttributeValueVoList.stream()
                .map(v -> {
                            AttributeValueIndex index = new AttributeValueIndex();
                            BeanUtil.copyProperties(v, index);
                            return index;
                        }
                ).collect(Collectors.toList());
        albumInfoIndex.setAttributeValueIndexList(indexList);

        //3.封装文档对象中分类相关信息
        //3.1 远程调用专辑服务-根据专辑所属3级分类ID查询分类信息
        Long category3Id = albumInfo.getCategory3Id();
        BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(category3Id).getData();

        //3.2 封装分类ID到索引库文档对象中
        albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
        albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());

        //4.封装文档对象中主播相关信息，主播也是用户
        //4.1 远程调用用户服务-根据专辑所属用户ID查询主播信息
        Long userId = albumInfo.getUserId();
        UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
        Assert.notNull(userInfoVo, "用户：{}信息为空", albumInfo.getUserId());

        //4.2 封装主播名称到索引库文档对象中
        albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());

        //5.封装文档对象中统计相关信息 TODO 目前采用随机生成方式
        //5.1 封装播放量数值
        int playStatNum = RandomUtil.randomInt(1000, 2000);
        albumInfoIndex.setPlayStatNum(playStatNum);

        //5.2 封装订阅量数值
        int subscribeStatNum = RandomUtil.randomInt(800, 1000);
        albumInfoIndex.setSubscribeStatNum(subscribeStatNum);


        //5.3 封装购买量数值
        int buyStatNum = RandomUtil.randomInt(100, 500);
        albumInfoIndex.setBuyStatNum(buyStatNum);

        //5.4 封装评论量数值
        int commentStatNum = RandomUtil.randomInt(500, 1000);
        albumInfoIndex.setCommentStatNum(commentStatNum);


        //5.5 基于以上生成统计数值计算出当前文档热度分值  热度=累加（不同统计数值*权重）
        BigDecimal bigDecimal1 = new BigDecimal("0.1").multiply(BigDecimal.valueOf(playStatNum));
        BigDecimal bigDecimal2 = new BigDecimal("0.2").multiply(BigDecimal.valueOf(subscribeStatNum));
        BigDecimal bigDecimal3 = new BigDecimal("0.3").multiply(BigDecimal.valueOf(buyStatNum));
        BigDecimal bigDecimal4 = new BigDecimal("0.4").multiply(BigDecimal.valueOf(commentStatNum));
        BigDecimal hotScore = bigDecimal1.add(bigDecimal2).add(bigDecimal3).add(bigDecimal4);
        albumInfoIndex.setHotScore(hotScore.doubleValue());

        //6.保存专辑索引库文档对象
        albumInfoIndexRepository.save(albumInfoIndex);


    }



}









































