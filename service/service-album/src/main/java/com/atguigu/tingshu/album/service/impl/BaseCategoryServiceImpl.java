package com.atguigu.tingshu.album.service.impl;

import cn.hutool.json.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
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
}



