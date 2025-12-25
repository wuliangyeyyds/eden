package com.ssrms.controller;

import com.ssrms.common.Result;
import com.ssrms.controller.dto.AiChatRequestDTO;
import com.ssrms.controller.vo.AiChatVO;
import com.ssrms.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@CrossOrigin
public class AiController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public Result chat(@RequestBody AiChatRequestDTO req) {
        try {
            AiChatVO vo = aiChatService.chat(req);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
}
