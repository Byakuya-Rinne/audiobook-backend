package com.atguigu.tingshu.album;
import com.atguigu.tingshu.album.impl.AlbumDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 专辑模块远程调用Feign接口,底层产生代理对象发起Http请求调用
 * 服务提供方专辑服务中提供接口地址：
 *  http://localhost:8501/api/album/albumInfo/getAlbumInfo/{id}
 * 服务调用方使用确定请求地址：将目标服务实例列表缓存到本地
 *  1.从Feign接口中得到：http://service-album/api/album/albumInfo/getAlbumInfo/{id}
 *  2.从Nacos得到专辑服务service-album对应两个实例信息
 *      实例1：http://localhost:8501
 *      实例2：http://localhost:8401
 *  3.OpenFeign底层集成负载均衡器组件：LoadBalancer 默认轮询
 *  4.发请求会将URL路径中域名改为具体IP跟端口
 *    第一次：http://localhost:8501/api/album/albumInfo/getAlbumInfo/{id}
 *    第二次：http://localhost:8401/api/album/albumInfo/getAlbumInfo/{id}
 * 专辑模块远程调用Feign接口
 */
//OpenFeign拿value去Nacos注册中心里找
@FeignClient(value = "service-album", fallback = AlbumDegradeFeignClient.class)
public interface AlbumFeignClient {
    //baseUrl=http://service-album = http://192.168.31.21:8501
    // 客户端从Nacos拉取到目标服务可用实例列表进行负载均衡（默认策略轮询）

    /**
     * 根据专辑ID查询专辑信息（包含标签列表）
     *
     * @param id
     * @return
     */
    @GetMapping("/albumInfo/getAlbumInfo/{id}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id);



    /**
     * 根据3级分类ID查询分类视图对象
     * @param category3Id
     * @return
     */
    @GetMapping("/category/getCategoryView/{category3Id}")
    public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id);





}
