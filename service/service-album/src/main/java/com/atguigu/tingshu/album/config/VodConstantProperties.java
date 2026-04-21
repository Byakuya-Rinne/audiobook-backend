package com.atguigu.tingshu.album.config;


import com.qcloud.vod.VodUploadClient;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.ims.v20201229.ImsClient;
import com.tencentcloudapi.tms.v20201229.TmsClient;
import com.tencentcloudapi.vod.v20180717.VodClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="vod") //读取节点
@Data
public class VodConstantProperties {

    private Integer appId;
    private String secretId;
    private String secretKey;
    //https://cloud.tencent.com/document/api/266/31756#.E5.9C.B0.E5.9F.9F.E5.88.97.E8.A1.A8
    private String region;
    private String procedure;
    private String tempPath;
    private String playKey;

    @Bean
    public Credential credential() {
        return new Credential(secretId, secretKey);
    }

    @Bean
    public TmsClient tmsClient(){
        return new TmsClient(credential(), region);
    }

    @Bean
    public ImsClient imsClient() {
        return new ImsClient(credential(), region);
    }

    @Bean
    public VodClient vodClient() {
        return new VodClient(credential(), region);
    }

    @Bean
    public VodUploadClient uploadClient() {
        return new VodUploadClient(secretId, secretKey);
    }

}
