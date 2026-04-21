package com.atguigu.tingshu.album.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {

    Map<String, String> uploadTrack(MultipartFile file);
}
