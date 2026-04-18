package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.FileUploadService;
import com.atguigu.tingshu.common.execption.GuiguException;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class FileUploadServiceImpl implements FileUploadService{

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConstantProperties minioConstantProperties;

    /**
     * 图片（封面、头像）文件上传
     * 前端提交文件参数名：file
     *
     * @param file
     * @return
     */
    @Override
    public String fileUpload(MultipartFile file) {

        try {
            //1.校验文件是否合法（图片格式校验、图片尺寸校验）
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (bufferedImage == null) {
                throw new GuiguException(400, "文件格式错误");
            }
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            if(width > 900 || height > 900){
                throw new GuiguException(400, "文件尺寸过大！");
            }
        } catch (Exception e) {
            throw new GuiguException(500, "文件校验处理失败");
        }

        //2.存储到minIO
        try {
            //2.1 生成路径文件夹
            String folder = "/" + DateUtil.today();
            String fileName = IdUtil.randomUUID().substring(0, 10);
            String extName = FileNameUtil.extName(file.getOriginalFilename());
            String objName = folder + "/" + fileName + "." + extName;

            //2.2 上传MinIO
            String bucketName = minioConstantProperties.getBucketName();
            minioClient.putObject(
                    PutObjectArgs.builder()                     // 1. 获取构建器实例
                            .bucket(bucketName)                     // 2. 指定存储桶名称
                            .object(objName)                        // 3. 指定对象在桶内的完整路径（含文件名）
                            .stream(                                // 4. 设置文件输入流及相关参数
                                    file.getInputStream(),     //    文件内容流
                                    file.getSize(),            //    文件总大小（字节）
                                    -1                                  //    分片大小（-1 表示自动分片）
                            )
                            .contentType(file.getContentType()) // 5. 设置 MIME 类型
                            .build()                               // 6. 最终构建出 PutObjectArgs 对象
            );
            return minioConstantProperties.getEndpointUrl() + "/" + bucketName + objName;
        } catch (Exception e) {
            throw new GuiguException(500, "图片上传失败");
        }




    }



}
