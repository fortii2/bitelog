package me.forty2.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisDataWithExpireTime {
    private Object data;
    private LocalDateTime expireTime;
}
