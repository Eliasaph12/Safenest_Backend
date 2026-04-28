package com.safenest.controller;

import com.safenest.service.PlatformService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final PlatformService platformService;

    public ChatController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/agents")
    public List<Map<String, Object>> getAuthorizedAgents() {
        return platformService.getAuthorizedSupportAgents();
    }

    @GetMapping("/agent")
    public ResponseEntity<Map<String, Object>> getPrimaryAgent() {
        Map<String, Object> agent = platformService.getPrimarySupportAgent();
        if (agent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(agent);
    }

    @GetMapping("/messages")
    public List<Map<String, Object>> getMessages(
        @RequestParam Integer senderId,
        @RequestParam Integer receiverId,
        @RequestParam(required = false) Long sessionId
    ) {
        return platformService.getChatMessages(senderId, receiverId, sessionId);
    }

    @GetMapping("/threads")
    public List<Map<String, Object>> getAgentThreads(@RequestParam Integer agentId) {
        return platformService.getAgentChatThreads(agentId);
    }

    @GetMapping("/sessions")
    public List<Map<String, Object>> getVictimSessions(@RequestParam Integer victimId) {
        return platformService.getVictimChatSessions(victimId);
    }

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createSession(@RequestBody Map<String, Object> request) {
        Integer victimId = Integer.valueOf(String.valueOf(request.get("victimId")));
        Integer agentId = Integer.valueOf(String.valueOf(request.get("agentId")));
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createChatSession(victimId, agentId));
    }

    @PostMapping("/sessions/close")
    public ResponseEntity<Map<String, Object>> closeSession(@RequestBody Map<String, Object> request) {
        Long sessionId = Long.valueOf(String.valueOf(request.get("sessionId")));
        Map<String, Object> response = platformService.closeChatSession(sessionId);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/delete")
    public ResponseEntity<Map<String, Object>> deleteSession(@RequestBody Map<String, Object> request) {
        Long sessionId = Long.valueOf(String.valueOf(request.get("sessionId")));
        Map<String, Object> response = platformService.deleteChatSession(sessionId);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/messages")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.sendChatMessage(request));
    }
}
