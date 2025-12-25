package com.ssrms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssrms.config.CozeProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Component
@RequiredArgsConstructor
public class CozeClient {

    private final CozeProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    @Data
    @AllArgsConstructor
    public static class CozeChatResult {
        private String reply;
        private String conversationId;
    }

    public CozeChatResult chatStream(String userId, String conversationId, String content) {
        if (!StringUtils.hasText(props.getBotId())) {
            throw new IllegalStateException("coze.bot-id 未配置");
        }
        if (!StringUtils.hasText(props.getToken())) {
            throw new IllegalStateException("coze.token 未配置");
        }
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId 不能为空");
        }

        String base = props.getBaseUrl();
        if (!StringUtils.hasText(base)) base = "https://api.coze.cn";

        String url = base.replaceAll("/+$", "") + "/v3/chat";
        if (StringUtils.hasText(conversationId)) {
            url = url + "?conversation_id=" + URLEncoder.encode(conversationId, StandardCharsets.UTF_8);
        }

        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "user");
        msg.put("content", content);
        msg.put("content_type", "text");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bot_id", props.getBotId());
        body.put("user_id", userId);
        body.put("stream", true);
        body.put("additional_messages", List.of(msg));

        try {
            String json = mapper.writeValueAsString(body);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                    .build();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(props.getReadTimeoutMs()))
                    .header("Authorization", "Bearer " + props.getToken())
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() / 100 != 2) {
                String err = new String(resp.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new RuntimeException("Coze API HTTP " + resp.statusCode() + ": " + err);
            }

            return parseSse(resp.body());
        } catch (Exception e) {
            throw new RuntimeException("调用 Coze 失败：" + e.getMessage(), e);
        }
    }

    private CozeChatResult parseSse(InputStream in) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;

        String currentEvent = null;
        StringBuilder dataBuf = new StringBuilder();

        StringBuilder answer = new StringBuilder();
        String conversationId = null;

        while ((line = br.readLine()) != null) {
            if (line.isEmpty()) {
                // end of one SSE block
                if (currentEvent != null && dataBuf.length() > 0) {
                    String dataStr = dataBuf.toString().trim();
                    // reset for next
                    String ev = currentEvent.trim();
                    currentEvent = null;
                    dataBuf.setLength(0);

                    if ("done".equals(ev)) {
                        break;
                    }

                    if (dataStr.equals("[DONE]")) {
                        break;
                    }

                    // Some done events may return json, ignore safely
                    if (dataStr.startsWith("{")) {
                        JsonNode node = mapper.readTree(dataStr);

                        // capture conversation_id whenever it appears
                        if (conversationId == null) {
                            JsonNode cid = node.get("conversation_id");
                            if (cid != null && cid.isTextual()) conversationId = cid.asText();
                        }

                        if ("conversation.message.delta".equals(ev) || "conversation.message.completed".equals(ev)) {
                            String role = node.path("role").asText("");
                            String type = node.path("type").asText("");
                            String content = node.path("content").asText("");

                            if (conversationId == null) {
                                String cid2 = node.path("conversation_id").asText(null);
                                if (cid2 != null && !cid2.isEmpty()) conversationId = cid2;
                            }

                            // 只拼接真正的回答内容
                            if ("assistant".equals(role) && "answer".equals(type) && content != null) {
                                // delta 会一小段一小段返回；completed 是整段（可能重复）
                                if ("conversation.message.delta".equals(ev)) {
                                    answer.append(content);
                                }
                            }
                        } else if ("conversation.chat.created".equals(ev) || "conversation.chat.in_progress".equals(ev) || "conversation.chat.completed".equals(ev)) {
                            if (conversationId == null) {
                                String cid = node.path("conversation_id").asText(null);
                                if (cid != null && !cid.isEmpty()) conversationId = cid;
                            }
                        } else if ("conversation.chat.failed".equals(ev) || "error".equals(ev)) {
                            String msg = node.path("msg").asText("Coze 返回错误");
                            throw new RuntimeException(msg);
                        }
                    }
                }
                continue;
            }

            if (line.startsWith("event:")) {
                currentEvent = line.substring("event:".length()).trim();
            } else if (line.startsWith("data:")) {
                String part = line.substring("data:".length());
                dataBuf.append(part);
            } else {
                // ignore comments or unknown fields
            }
        }

        String reply = answer.toString().trim();
        if (reply.isEmpty()) {
            reply = "（AI 没有返回可用内容）";
        }
        return new CozeChatResult(reply, conversationId);
    }
}
