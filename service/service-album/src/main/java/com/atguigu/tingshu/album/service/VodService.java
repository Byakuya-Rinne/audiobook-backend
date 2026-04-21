package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {

    Map<String, String> uploadTrack(MultipartFile file);

    /**
     * 从云点播平台获取音频文件详情
     * @param mediaFileId 唯一标识
     * @return
     */
    TrackMediaInfoVo getMediaInfo(String mediaFileId);

    /**
     * 从点播平台删除音频文件
     * @param oldMediaFileId
     */
    void deleteMedia(String oldMediaFileId);

}
