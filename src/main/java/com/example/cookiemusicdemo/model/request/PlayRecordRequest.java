package com.example.cookiemusicdemo.model.request;

import lombok.Data;

@Data
public class PlayRecordRequest {
    private Integer consumerId;
    private Integer songId;
    private Integer playSeconds;
    private Byte source;
}

