package com.ssrms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "coze")
public class CozeProperties {

    /**
     * Coze OpenAPI Base URL:
     * - 国内常见：https://api.coze.cn
     * - 国际常见：https://api.coze.com
     */
    private String baseUrl = "https://api.coze.cn";

    /** 你的智能体 bot_id */
    private String botId;

    /** 个人访问令牌（PAT），必须后端保存，前端不要暴露 */
    private String token;

    private int connectTimeoutMs = 15000;
    private int readTimeoutMs = 60000;
}
