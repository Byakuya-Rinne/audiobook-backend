package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;

    @Autowired
    private VodUploadClient vodUploadClient;

    /**
     * 音视频文件上传点播平台
     *
     * @param file
     * @return {"mediaFileId":"5145403723983411008", "mediaUrl":"http://1255727855.vod-qcloud.com/9cbe3378vodsh1255727855/a41826195145403723983411008/SfS3kjP0PEQA.mp3"}
     */
    @Override
    public Map<String, String> uploadTrack(MultipartFile file) {
        Map<String, String> map = new HashMap<String, String>();

        try {
            //1.将上传文件保存到本地得到文件路径 TODO:后续采用定时任务清理临时目录下使用完毕文件
            String localFilePath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);

            //2.构造上传请求对象:设置媒体本地上传路径
            VodUploadRequest request = new VodUploadRequest();
            request.setMediaFilePath(localFilePath);
            VodUploadResponse response = vodUploadClient.upload(vodConstantProperties.getRegion(), request);

            //3.得到文件唯一标识、文件在线地址
            if (response != null){
                String mediaFileId = response.getFileId();
                String mediaUrl = response.getMediaUrl();
                map.put("mediaFileId", mediaFileId);
                map.put("mediaUrl", mediaUrl);
                return map;
            }


        } catch (Exception e) {
            log.error("文件上传点播平台异常", e);
            throw new GuiguException(500, "文件上传点播平台异常：" + e.getMessage());
        }
        return null;
    }
}
