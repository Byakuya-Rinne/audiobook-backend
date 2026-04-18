package com.atguigu.tingshu.album.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    String fileUpload(MultipartFile file);
}
