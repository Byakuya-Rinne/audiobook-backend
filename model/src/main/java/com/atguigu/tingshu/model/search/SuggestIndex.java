package com.atguigu.tingshu.model.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.suggest.Completion;

@Data
@Document(indexName = "suggestinfo")
@JsonIgnoreProperties(ignoreUnknown = true)//目的：防止json字符串转成实体对象时因未识别字段报错
public class SuggestIndex {

    /**
     * 跟专辑ID一致
     * 专辑上架 该专辑名称就会出现自动补全待选中
     * 专辑下架 该专辑名称不能被检索到了
     */
    @Id
    private String id;

    //原始内容 郭德纲相声集
    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    //郭德纲相声集
    @CompletionField(analyzer = "standard", searchAnalyzer = "standard", maxInputLength = 20)
    private Completion keyword;

    //guodegangxiangshengji
    @CompletionField(analyzer = "standard", searchAnalyzer = "standard", maxInputLength = 20)
    private Completion keywordPinyin;

    //gdgxsj
    @CompletionField(analyzer = "standard", searchAnalyzer = "standard", maxInputLength = 20)
    private Completion keywordSequence;

}
