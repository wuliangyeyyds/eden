package com.ssrms.service;

import com.ssrms.controller.dto.AiChatRequestDTO;
import com.ssrms.controller.vo.AiChatVO;

public interface AiChatService {
    AiChatVO chat(AiChatRequestDTO req);
}
