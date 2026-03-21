package com.example.cookiemusicdemo.model.request;

import lombok.Data;

@Data
public class AdminPostAuditRequest {
    private Long postId;
    /**
     * 1=通过, 2=驳回, 3=下架
     */
    private Integer status;
    /**
     * 预留字段：当前版本不落库，仅用于前端提示/日志扩展
     */
    private String reason;
}

