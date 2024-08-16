package com.example.livealone.chat.service;

import com.example.livealone.chat.dto.ChatInitDto;
import com.example.livealone.chat.entity.ChatErrorLog;
import com.example.livealone.chat.entity.ChatMessage;
import com.example.livealone.chat.entity.ChatSessionLog;
import com.example.livealone.chat.repository.ChatErrorLogRepository;
import com.example.livealone.chat.repository.ChatMessageRepository;
import com.example.livealone.chat.repository.ChatSessionLogRepository;
import com.example.livealone.global.dto.SocketMessageDto;
import com.example.livealone.global.entity.SocketMessageType;
import com.example.livealone.global.security.JwtService;
import com.example.livealone.user.dto.ReissueRequestDto;
import com.example.livealone.user.dto.TokenResponseDto;
import com.example.livealone.user.service.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.example.livealone.global.entity.SocketMessageType.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatErrorLogRepository chatErrorLogRepository;
    private final ChatSessionLogRepository chatSessionLogRepository;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final AuthService authService;
    private final Random random = new Random();

    private final ConcurrentLinkedQueue<ChatMessage> messageBuffer = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ChatErrorLog> errorLogsBuffer = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ChatSessionLog> sessionLogsBuffer = new ConcurrentLinkedQueue<>();

    private static final String[] COLORS = {
            "#FF33F0", "#4566BC", "#E4E669", "#d071b6", "#a471d0", "#b7d071", "#d09d71", "#7174d0"
    };
    private static final String[] ADMIN_COLORS = {
            "#FF0000"
    };

    private static final int batchSize = 100;

    public String createSessionReply(SocketMessageDto socketMessageDto) throws JsonProcessingException {
        SocketMessageDto messageDto = null;

        SocketMessageType type = socketMessageDto.getType();
        switch (type) {
            case REQUEST_AUTH -> {
                String token = socketMessageDto.getMessage();
                if (token == null || !token.startsWith("Bearer ")) {
                    messageDto = new SocketMessageDto(INVALID_TOKEN, "back-server", token);
                    break;
                }
                String replaceToken = token.replace("Bearer ", "");
                String isValidToken = jwtService.isValidToken(replaceToken);
                if (!Objects.equals(isValidToken, "Valid")) {
                    messageDto = new SocketMessageDto(INVALID_TOKEN, "back-server", isValidToken);
                    break;
                }
                Claims claims = jwtService.getClaims(replaceToken);
                String nickname = claims.get("nickname", String.class);

                String color;
                String role = claims.get("role", String.class);

                if (role != null && Objects.equals(role, "ADMIN")) {

                    int randomIndex = random.nextInt(ADMIN_COLORS.length);
                    color = ADMIN_COLORS[randomIndex];
                } else {
                    int randomIndex = random.nextInt(COLORS.length);
                    color = COLORS[randomIndex];
                }

                messageDto = new SocketMessageDto(RESPONSE_AUTH, nickname, color);
            }

            case REQUEST_REFRESH -> {
                try {
                    String refreshToken = socketMessageDto.getMessage() == null ? "invalid RefreshToken" : socketMessageDto.getMessage();
                    TokenResponseDto tokenResponseDto = authService.reissueAccessToken(new ReissueRequestDto(refreshToken));

                    messageDto = new SocketMessageDto(SocketMessageType.RESPONSE_REFRESH, "back-server", objectMapper.writeValueAsString(tokenResponseDto));
                } catch (Exception e) {
                    messageDto = new SocketMessageDto((ANONYMOUS_USER), "back-server", e.getMessage());
                }
            }
            case REQUEST_CHAT_INIT -> {
                messageDto = writeInitMessages();
            }
        }

        return objectMapper.writeValueAsString(messageDto);
    }

    public SocketMessageDto write(String message) {
        try {
            return objectMapper.readValue(message, SocketMessageDto.class);
        } catch (JsonProcessingException e) {
            return new SocketMessageDto(ERROR, "back-server", "메시지 전송 실패");
        }
    }

    public String createKafkaMessage(SocketMessageDto chat) throws JsonProcessingException {
        try {
            saveMessage(chat);
            return objectMapper.writeValueAsString(chat);
        } catch (JsonProcessingException e) {
            SocketMessageDto dto = new SocketMessageDto(ERROR, "back-server", "JSON데이터 생성 실패");

            return objectMapper.writeValueAsString(dto);
        }
    }
    private void saveMessage(SocketMessageDto socketMessageDto) {

        try {
            switch (socketMessageDto.getType()) {
                case REQUEST_AUTH -> {
                    ChatSessionLog chatSessionLog = new ChatSessionLog(socketMessageDto.getMessenger(), socketMessageDto.getMessage());
                    sessionLogsBuffer.add(chatSessionLog);
                    if (sessionLogsBuffer.size() > batchSize) {
                        saveSessionLogs();
                    }
                }
                case CHAT_MESSAGE -> {
                    chatMessageRepository.save(new ChatMessage(socketMessageDto.getMessenger(), socketMessageDto.getMessage()));
                    if (messageBuffer.size() > batchSize) {
                        saveChatMessages();
                    }
                }
                case ERROR-> {
                    ChatErrorLog chatErrorLog = new ChatErrorLog(socketMessageDto.getMessage());
                    errorLogsBuffer.add(chatErrorLog);
                    if (errorLogsBuffer.size() > batchSize) {
                        saveErrorLogs();
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            addErrorLogs(e.getMessage());
        }
    }

    private void addErrorLogs(String message) {
        ChatErrorLog chatErrorLog = new ChatErrorLog((message));
        errorLogsBuffer.add(chatErrorLog);
        if (errorLogsBuffer.size() > batchSize) {
            saveErrorLogs();
        }
    }

    private synchronized void saveChatMessages() {
        if (!messageBuffer.isEmpty()) {
            chatMessageRepository.saveAll(new ArrayList<>(messageBuffer));
            messageBuffer.clear();
        }
    }

    private synchronized void saveErrorLogs() {
        if (!errorLogsBuffer.isEmpty()) {
            chatErrorLogRepository.saveAll(new ArrayList<>(errorLogsBuffer));
            errorLogsBuffer.clear();
        }
    }

    private synchronized void saveSessionLogs() {
        if (!sessionLogsBuffer.isEmpty()) {
            chatSessionLogRepository.saveAll(new ArrayList<>(sessionLogsBuffer));
            sessionLogsBuffer.clear();
        }
    }


    public void flush() {
        log.debug("서버 종료 전 버퍼에 있는 데이터 저장");
        saveChatMessages();
        saveErrorLogs();
        saveSessionLogs();
    }

    private SocketMessageDto writeInitMessages() {
        try {
            List<ChatMessage> chatList = chatMessageRepository.findTop30ByOrderByIdDesc();

            List<ChatInitDto> initData = new ArrayList<>();
            for (ChatMessage chat : chatList) {
                String initNickname = chat.getNickname();
                String initText = chat.getMessage();
                ChatInitDto init = new ChatInitDto(initNickname, initText);
                initData.add(init);
            }
            Collections.reverse(initData);
            String messageJSON = objectMapper.writeValueAsString(initData);
            return new SocketMessageDto(RESPONSE_CHAT_INIT, "back-server", messageJSON);

        } catch (IOException e) {
            log.debug(e.getMessage());
            addErrorLogs(e.getMessage());
            return new SocketMessageDto(ERROR, "back-server", "메시지 초기화 실패");
        }
    }
}
