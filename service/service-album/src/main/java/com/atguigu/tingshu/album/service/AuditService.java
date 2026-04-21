package com.atguigu.tingshu.album.service;

public interface AuditService {

    /**
     * 对文本进行内容审核
     * @param auditText
     * @return
     */
    public String auditText(String auditText);

    /**
     * 对图片审核
     * @param auditImage Base64后图片
     * @return
     */
    public String auditImage(String auditImage);


    /**
     * 发起审核任务
     * @param mediaFileId
     * @return 任务ID
     */
    public String startReviewTask(String mediaFileId);


    /**
     * 获取审核结果
     * @param taskId 审核任务ID
     * @return 建议
     * pass：建议通过；
     * review：建议复审；
     * block：建议封禁。
     */
    public String getRevivewTaskResult(String taskId);
}
