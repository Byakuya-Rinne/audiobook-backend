package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.repository.SuggestIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
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

    @Autowired
    private SuggestIndexRepository suggestIndexRepository;

    @Autowired
    ElasticsearchClient elasticsearchClient;

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

    /**
     * 构建 提示词文档对象 存入提示词索引库
     *
     * @param id
     * @param albumTitle
     */
    @Override
    public void saveSuggestInfo(Long id, String albumTitle) {
        //1.构建提示词文档对象
        SuggestIndex suggestIndex = new SuggestIndex();
        suggestIndex.setId(String.valueOf(id));
        suggestIndex.setTitle(albumTitle);

        //1.1 存放中文提示词补全字段
        suggestIndex.setKeyword(
                new Completion(
                        new String[]{albumTitle}
                )
        );

        //1.2 存放全拼、拼音首字母提示词补全字段 将汉字转为拼音
        suggestIndex.setKeyword(
                new Completion(
                        new String[]{PinyinUtil.getPinyin(albumTitle,"")}
                )
        );

        suggestIndex.setKeyword(
                new Completion(
                        new String[]{PinyinUtil.getFirstLetter(albumTitle,"")}
                )
        );

        //2.调用提示词持久层接口保存提示词文档对象
        suggestIndexRepository.save(suggestIndex);

    }


    /**
     * 搜索自动补全
     *
     * @param keyword 用户已录入内容：汉字、拼音、拼音首字母
     * @return 自动补全待选文本列表
     */
        @Override
    public List<String> completeSuggest(String keyword) {


            try {
                //searchResponse搜索结果
                SearchResponse<SuggestIndex> searchResponse = elasticsearchClient.search( //发起一次 _search 请求
                        searchRequest -> searchRequest.suggest(
                                suggester ->
                                    suggester.suggesters("keyword-suggest", fieldSuggester -> fieldSuggester.prefix(keyword).completion(
                                            completionSuggester -> completionSuggester.field("keyword").size(10).skipDuplicates(true)))
                                            .suggesters("pinyin-suggest", fs -> fs.prefix("keywordPinyin").completion(cs -> cs.field("keywordPinyin").size(10).skipDuplicates(true)))
                                            .suggesters("letter-suggest", s->s.prefix("keywordSequence").completion(f->f.field("keywordSequence").size(10).skipDuplicates(true)))
                        ),SuggestIndex.class //<-文档映射的 Java 类型
                );

                //搜索结果去重，去重后需要尽量补足10个
                HashSet<String> set = new HashSet<>();
                set.addAll(this.parseSuggestResult(searchResponse,"keyword-suggest"));
                set.addAll(this.parseSuggestResult(searchResponse,"pinyin-suggest"));
                set.addAll(this.parseSuggestResult(searchResponse,"letter-suggest"));

                //2.如果建议词结果数量小于10，尝试采用全文检索补全到10个
                if (set.size() < 10){
                    SearchResponse<AlbumInfoIndex> search = elasticsearchClient.search(s ->
                                    s.query(q -> q.match(
                                                    m -> m.field("albumTitle").query(keyword)
                                            )).size(10)
                                            .source(s1 -> s1.filter(
                                                    f -> f.includes(Arrays.asList("albumTitle"))
                                            ))
                            , AlbumInfoIndex.class);
                    List<Hit<AlbumInfoIndex>> hits = search.hits().hits();
                    if (CollUtil.isNotEmpty(hits)){
                        for (Hit<AlbumInfoIndex> hit : hits) {
                            AlbumInfoIndex albumInfoIndex = hit.source();
                            set.add(albumInfoIndex.getAlbumTitle());
                            if (set.size() >= 10) {
                                break;
                            }
                        }
                    }
                }

                //返回自动补全待选文本列表 最多返回10个结果
                if (set.size() > 10){
                    return new ArrayList<>(set).subList(0,10);
                }else {
                    return new ArrayList<>(set);//实在不足10个就直接返回
                }
            } catch (Exception e) {
                log.error("搜索自动补全异常：", e);
                throw new GuiguException(ResultCodeEnum.FAIL);
            }
    }

    /**
     * 解析自动补全结果
     *
     * @param searchResponse ES的结果
     * @param suggest_name   自定义建议词名称
     * @return 候选文本集合
     */

    @Override
    public Collection<String> parseSuggestResult(SearchResponse<SuggestIndex> searchResponse, String suggest_name) {
        ArrayList<String> list = new ArrayList<>();
        List<Suggestion<SuggestIndex>> suggestions = searchResponse.suggest().get(suggest_name);
        for (Suggestion<SuggestIndex> suggestion : suggestions) {
            List<CompletionSuggestOption<SuggestIndex>> options = suggestion.completion().options();
            for (CompletionSuggestOption<SuggestIndex> option : options) {
                SuggestIndex suggestIndex = option.source();
                String title = suggestIndex.getTitle();
                list.add(title);
            }
        }
        return list;
        /*
        {
          "suggest": {
            "keyword-suggest": [
              {
                "text": "专辑名",
                "offset": 0,
                "length": 3,
                "options": [
                  {
                    "text": "专辑名称一",
                    "_index": "suggest_index",
                    "_source": {
                      "id": "1",
                      "title": "专辑名称一",  // ← 最终拿到的就是这里
                      "keyword": {...},
                      "keywordPinyin": {...},
                      "keywordSequence": {...}
                    }
                  },
                  ...
                ]
              }
            ]
          }
        }

        SearchResponse<SuggestIndex>
         └─ suggest()  →  Map<String, List<Suggestion<SuggestIndex>>>
              └─ get("keyword-suggest")  →  List<Suggestion<SuggestIndex>>
                   └─ for each  Suggestion
                        └─ .completion()  →  CompletionSuggest<SuggestIndex>
                             └─ .options()  →  List<CompletionSuggestOption<SuggestIndex>>
                                  └─ for each  option
                                       └─ .source()  →  SuggestIndex
                                            └─ .getTitle()  →  "专辑标题字符串"
         */
    }
}









































