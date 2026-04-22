package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.codec.Base64;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.ims.v20201229.ImsClient;
import com.tencentcloudapi.ims.v20201229.models.ImageModerationRequest;
import com.tencentcloudapi.ims.v20201229.models.ImageModerationResponse;
import com.tencentcloudapi.tms.v20201229.TmsClient;
import com.tencentcloudapi.tms.v20201229.models.TextModerationRequest;
import com.tencentcloudapi.tms.v20201229.models.TextModerationResponse;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuditServiceImpl implements AuditService {
    @Autowired
    private TmsClient tmsClient;

    @Autowired
    private ImsClient imsClient;

    @Autowired
    private VodClient vodClient;


    @Override
    public String auditText(String auditText) {
        try {
            //1.实例化一个请求对象,每个接口都会对应一个request对象
            TextModerationRequest req = new TextModerationRequest();
            req.setContent(Base64.encode(auditText));
            //2.返回的resp是一个TextModerationResponse的实例，与请求对象对应
            TextModerationResponse resp = tmsClient.TextModeration(req);
            //3.解析结果，获取建议
            if (resp != null) {
                String suggestion = resp.getSuggestion();
                return suggestion.toLowerCase();
            }
        } catch (TencentCloudSDKException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public String auditImage(String auditImage) {
        try {
            //1. 实例化一个请求对象
            ImageModerationRequest req = new ImageModerationRequest();
            req.setFileContent(auditImage);
            //2. 返回的resp是一个ImageModerationResponse的实例，与请求对象对应
            ImageModerationResponse resp = imsClient.ImageModeration(req);
            if (resp != null) {
                String suggestion = resp.getSuggestion();
                return suggestion.toLowerCase();
            }
        } catch (TencentCloudSDKException e) {
            log.info("图片内容审核失败：", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public String startReviewTask(String mediaFileId) {
        try {
            // 实例化一个请求对象,每个接口都会对应一个request对象
            ReviewAudioVideoRequest req = new ReviewAudioVideoRequest();
            req.setFileId(mediaFileId);
            // 返回的resp是一个ReviewAudioVideoResponse的实例，与请求对象对应
            ReviewAudioVideoResponse resp = vodClient.ReviewAudioVideo(req);
            // 3.解析结果
            if (resp != null) {
                return resp.getTaskId();
            }
        } catch (TencentCloudSDKException e) {
            log.error("发起音频审核任务异常：", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public String getReviewTaskResult(String taskId) {

        try {
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeTaskDetailRequest req = new DescribeTaskDetailRequest();
            req.setTaskId(taskId);
            // 返回的resp是一个DescribeTaskDetailResponse的实例，与请求对象对应
            DescribeTaskDetailResponse resp = vodClient.DescribeTaskDetail(req);

            if (resp != null) {
                //3.1 确保任务类型是：音视频审核任务
                if ("ReviewAudioVideo".equals(resp.getTaskType())) {
                    //3.2 任务完成情况下 ，获取音视频审核任务信息
                    if("FINISH".equals(resp.getStatus())){
                        ReviewAudioVideoTask reviewAudioVideoTask = resp.getReviewAudioVideoTask();
                        if("FINISH".equals(reviewAudioVideoTask.getStatus())){
                            //音视频审核任务的输出。
                            ReviewAudioVideoTaskOutput output = reviewAudioVideoTask.getOutput();
                            String suggestion = output.getSuggestion();
                            return suggestion.toLowerCase();
                        }
                    }
                }
            }
        } catch (TencentCloudSDKException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
