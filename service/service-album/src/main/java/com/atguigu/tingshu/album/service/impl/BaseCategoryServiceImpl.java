package com.atguigu.tingshu.album.service.impl;

import cn.hutool.json.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory2;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

	@Autowired
	private BaseCategory1Mapper baseCategory1Mapper;

	@Autowired
	private BaseCategory2Mapper baseCategory2Mapper;

	@Autowired
	private BaseCategory3Mapper baseCategory3Mapper;

	@Autowired
	private BaseCategoryViewMapper baseCategoryViewMapper;

	@Autowired
	private BaseAttributeMapper baseAttributeMapper;


	@Override
	public List<JSONObject> getBaseCategoryList() {
		// return: [{"categoryId"：123, "categoryName": "name", "categoryChild": { } }, { ... }]
		List<BaseCategoryView> allCategoryList = baseCategoryViewMapper.selectList(null);
//		System.out.println(allCategoryList);

		//按一级分类分组
		Map<Long, List<BaseCategoryView>> category1Map = allCategoryList.stream()
				.collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
//		System.out.println(category1Map);
		List<JSONObject> list1 = new ArrayList<>();

		for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {

			// jb1: {id:..., name:..., ...}
			long id1 = entry1.getKey();
			String name1 = entry1.getValue().get(0).getCategory1Name();

			JSONObject jb1 = new JSONObject();
			jb1.put("categoryId", id1);
			jb1.put("categoryName", name1);

//			System.out.println(jb1);
//			{"categoryName":"有声图书","categoryId":11}

			// 构建category2
			List<JSONObject> category2 = new ArrayList<>();
			Map<Long, List<BaseCategoryView>> category2Map = entry1.getValue().stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
			for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
//				entry2:
//				190=[BaseCategoryView(category1Id=15, category1Name=新红色频道, category2Id=190, category2Name=党史, category3Id=1400, category3Name=党史人物)]
				Long id2 = entry2.getKey();
				String name2 = entry2.getValue().get(0).getCategory2Name();

				JSONObject jb2 = new JSONObject();
				jb2.put("categoryId", id2);
				jb2.put("categoryName", name2);

				List<JSONObject> category3 = new ArrayList<>();

				for (BaseCategoryView baseCategoryView : entry2.getValue()) {
					Long id3 = baseCategoryView.getCategory3Id();
					String name3 = baseCategoryView.getCategory3Name();
					JSONObject jb3 = new JSONObject();
					jb3.put("categoryId", id3);
					jb3.put("categoryName", name3);
					category3.add(jb3);
				}
				// 分组可以去重
//				Map<Long, List<BaseCategoryView>> category3Map = entry2.getValue().stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
//				for (Map.Entry<Long, List<BaseCategoryView>> entry3 : category3Map.entrySet()) {
//					Long id3 = entry3.getKey();
//					String name3 = entry3.getValue().get(0).getCategory3Name();
//					JSONObject jb3 = new JSONObject();
//					jb3.put("categoryId", id3);
//					jb3.put("categoryName", name3);
//					category3.add(jb3);
//				}
				jb2.put("categoryChild", category3);
				category2.add(jb2);
			}

			jb1.put("categoryChild",category2);
//			System.out.println(jb1);
//			{"categoryName":"新红色频道","categoryChild":[{"categoryName":"先锋人物","categoryChild":[{"categoryName":"党员风采","categoryId":192}],"categoryId":192},{"categoryName":"党史","categoryChild":[{"categoryName":"党史人物","categoryId":190}],"categoryId":190}],"categoryId":15}
			list1.add(jb1);
		}



		return list1;
	}

	@Override
	public List<BaseAttribute> getAttributesByCategory1Id(Long category1Id) {
		List<BaseAttribute> baseAttributes = baseAttributeMapper.getAttributesByCategory1Id(category1Id);
		return baseAttributes;
	}


	/**
	 * 根据3级分类ID查询分类视图对象
	 * @param category3Id
	 * @return
	 */
	@Override
	public BaseCategoryView getCategoryView(Long category3Id) {
		return baseCategoryViewMapper.selectById(category3Id);
	}

	/**
	 * 查询1级分类下包含所有二级以及三级分类
	 *
	 * @param category1Id
	 * @return
	 */
	@Override
	public JSONObject getBaseCategoryListByCategory1Id(Long category1Id) {
		/*
		categoryId
		categoryName
		categoryChild{
				categoryId
				categoryName
				categoryChild{
					categoryId
					categoryName
				}
			}
		 */
		//1.处理1级分类
		//1.1 创建1级分类JSON对象
		JSONObject jsonObject = new JSONObject();

		//1.2 根据1级分类ID查询分类视图得到"1级"分类列表
		List<BaseCategoryView> categoryViewList = baseCategoryViewMapper.selectList(new LambdaQueryWrapper<BaseCategoryView>().eq(BaseCategoryView::getCategory1Id, category1Id));

		// 防御：如果无数据，返回空对象或抛出自定义业务异常
		if (CollectionUtils.isEmpty(categoryViewList)) {
			// 返回一个仅含 categoryId 的 JSON，或直接返回 null（调用方需判空）
			JSONObject emptyResult = new JSONObject();
			emptyResult.put("categoryId", category1Id);
			emptyResult.put("categoryName", "未知分类");
			emptyResult.put("categoryChild", new ArrayList<>());
			log.error("我草，用category1Id查baseCategoryViewMapper，啥球也没查到");
			return emptyResult;
		}

		// 取第一条记录的 category1Id 进行校验（可选）
		Long dbCategory1Id = categoryViewList.get(0).getCategory1Id();
		if (!category1Id.equals(dbCategory1Id)) {
			log.warn("数据异常：查询条件 category1Id =" + category1Id + "，但视图返回 category1Id = " + dbCategory1Id);
			// 根据业务决定以数据库的为准，或直接抛异常
			category1Id = dbCategory1Id;
		}


		//1.3. 封装1级JSON对象 1级分类ID跟名称
		jsonObject.putIfAbsent("categoryId", category1Id);
		jsonObject.putIfAbsent("categoryName", categoryViewList.get(0).getCategory1Name());

		//处理categoryChild
		//getCategory2Id, '2级'分类列表
		//map Category2Id，BaseCategoryViewList
		List<JSONObject> jb2List = new ArrayList<>();
		Map<Long, List<BaseCategoryView>> category2IdMap = categoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
		category2IdMap.forEach((category2Id, baseCategoryViewList2)->{
			JSONObject jb2 = new JSONObject();
			jb2.putIfAbsent("categoryId", category2Id);
			jb2.putIfAbsent("categoryName", baseCategoryViewList2.get(0).getCategory2Name());

			List<JSONObject> jb3List = new ArrayList<>();
			Map<Long, List<BaseCategoryView>> category3IdMap = baseCategoryViewList2.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
			category3IdMap.forEach((category3Id, baseCategoryViewList3)->{
				JSONObject jb3 = new JSONObject();
				jb3.putIfAbsent("categoryId", category3Id);
				jb3.putIfAbsent("categoryName", baseCategoryViewList3.get(0).getCategory3Name());
				jb3List.add(jb3);
			});
			jb2.putIfAbsent("categoryChild",jb3List);
			jb2List.add(jb2);
		});
		jsonObject.putIfAbsent("categoryChild",jb2List);
		return jsonObject;

/*

     * 查询1级分类下包含所有2,3级分类列表
     *
     * @param category1Id
     * @return 1级分类对象 包含2,3级分类列表
		@Override
		public com.alibaba.fastjson.JSONObject getBaseCategoryListByCategory1Id(Long category1Id) {
			//1.处理1级分类JSON对象
			//1.1 创建1级分类JSON对象
			com.alibaba.fastjson.JSONObject jsonObject1 = new com.alibaba.fastjson.JSONObject();
			//1.2 根据1级分类ID查询分类视图得到"1级"分类列表
			List<BaseCategoryView> category1ViewList = baseCategoryViewMapper.selectList(
					new LambdaQueryWrapper<BaseCategoryView>()
							.eq(BaseCategoryView::getCategory1Id, category1Id)
			);

			//1.3. 封装1级JSON对象 1级分类ID跟名称
			jsonObject1.put("categoryId", category1ViewList.get(0).getCategory1Id());
			jsonObject1.put("categoryName", category1ViewList.get(0).getCategory1Name());

			//2.处理2级分类
			//2.1 创建2级分类JSON集合
			ArrayList<com.alibaba.fastjson.JSONObject> jsonObject2List = new ArrayList<>();
			//2.2 对"1级"分类列表按照2级分类ID进行分组 得到 二级分类Map key="二级分类ID" value="'2级'分类列表"
			Map<Long, List<BaseCategoryView>> map2 = category1ViewList
					.stream()
					.collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
			//2.3 遍历Map 封装二级分类对象
			for (Map.Entry<Long, List<BaseCategoryView>> entry2 : map2.entrySet()) {
				com.alibaba.fastjson.JSONObject jsonObject2 = new com.alibaba.fastjson.JSONObject();
				jsonObject2.put("categoryId", entry2.getKey());
				jsonObject2.put("categoryName", entry2.getValue().get(0).getCategory2Name());
				jsonObject2List.add(jsonObject2);
				//3.处理3级分类
				//3.1 创建3级分类JSON集合
				ArrayList<com.alibaba.fastjson.JSONObject> jsonObject3List = new ArrayList<>();
				//3.2 遍历"2级分类列表"
				for (BaseCategoryView baseCategoryView : entry2.getValue()) {
					//3.3 封装三级分类JSON对象
					com.alibaba.fastjson.JSONObject jsonObject3 = new com.alibaba.fastjson.JSONObject();
					jsonObject3.put("categoryId", baseCategoryView.getCategory3Id());
					jsonObject3.put("categoryName", baseCategoryView.getCategory3Name());
					//3.4 将3级分类JSON对象加入3级分类集合中
					jsonObject3List.add(jsonObject3);
				}
				//3.5 将3级分类列表加入到2级分类对象"categoryChild"中
				jsonObject2.put("categoryChild", jsonObject3List);
			}
			//2.4 将二级列表加入一级分类对象"categoryChild"中
			jsonObject1.put("categoryChild", jsonObject2List);

			//4.响应1级分类JSON对象
			return jsonObject1;
		}
 */


	}
}



