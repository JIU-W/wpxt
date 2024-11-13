package com.easypan.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * 返回给前端的数据类
 */
@JsonIgnoreProperties(ignoreUnknown = true)//当使用JSON库将Java对象序列化为JSON时，
//如果JSON包含Java对象中没有的属性，则通常会抛出异常。
//在这种情况下可以使用@JsonIgnoreProperties注解来告诉JSON库忽略这些未知属性，而不会抛出异常
public class UploadResultDto implements Serializable {
    private String fileId;
    private String status;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
