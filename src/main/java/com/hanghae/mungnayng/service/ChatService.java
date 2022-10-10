package com.hanghae.mungnayng.service;

import com.hanghae.mungnayng.domain.Room.RoomDetail;
import com.hanghae.mungnayng.domain.Room.RoomInfo;
import com.hanghae.mungnayng.domain.chat.Chat;
import com.hanghae.mungnayng.domain.chat.dto.ChatDto;
import com.hanghae.mungnayng.repository.ChatRepository;
import com.hanghae.mungnayng.repository.RoomDetailRepository;
import com.hanghae.mungnayng.repository.RoomInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final RoomDetailRepository roomDetailsRepository;
    private final RoomInfoRepository roomInfoRepository;
    private final ChatRepository chatRepository;

    @Transactional
    public ChatDto saveChat(Long roomId, ChatDto message) {
        RoomDetail roomDetail = roomDetailsRepository.findByRoomInfo_IdAndMember_MemberId(roomId, message.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방에 관한 정보가 없습니다."));/*roomDetail에서 채팅방과 맴버아이디를 통해 해당 채팅방에 속한 사람이 맞는지 검사*/
        roomDetail.getRoomInfo().updateRecentChat(message.getContent());/*해당 채팅방에 있는 사람이 맞을 시 방 아이디의 최근에 읽은 채팅 내역 갱신*/
        RoomInfo roomInfo = roomInfoRepository.findById(roomId)/*해당 채팅방이 존재하는 지 조회*/
                .orElseThrow(()-> new IllegalArgumentException("채팅방이 존재하지 않습니다."));
        Chat chat = Chat.builder()/*채팅 빌드*/
                .id(message.getChaId())
                .roomInfoId(roomInfo.getId())
                .roomDetail(roomDetail)
                .message(message.getContent())
                .build();

        ChatDto chatDto = new ChatDto(chatRepository.save(chat));
        return chatDto;
    }


    /*채팅 보내기*/
    @Transactional
    public List<ChatDto> getChat(RoomInfo roomInfo) {
        List<Chat> chats = chatRepository.findByRoomDetail_RoomInfo_IdOrderByCreatedAtDesc(roomInfo.getId());/*채팅시간을 만들어진 순서대로 나열*/
        return chats.stream()
                .map(ChatDto::new)
                .collect(Collectors.toList());
    }
}
