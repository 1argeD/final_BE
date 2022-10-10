package com.hanghae.mungnayng.controller;


import com.hanghae.mungnayng.domain.Room.RoomDetail;
import com.hanghae.mungnayng.domain.UserDetailsImpl;
import com.hanghae.mungnayng.domain.chat.dto.ChatDto;
import com.hanghae.mungnayng.domain.member.Member;
import com.hanghae.mungnayng.repository.RoomDetailRepository;
import com.hanghae.mungnayng.repository.RoomInfoRepository;
import com.hanghae.mungnayng.service.ChatService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Log4j2
@RestController
@AllArgsConstructor
public class ChatController {
    private final RoomInfoRepository roomInfoRepository;
    private final RoomDetailRepository roomDetailRepository;
    private final ChatService chatService;
    /*채팅내역 불러오기*/
    @GetMapping("/room/{itemId}")
    public ResponseEntity<?> getRoomChat(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                         @PathVariable Long itemId) {
        Member member = userDetails.getMember();
        RoomDetail roomDetail = roomDetailRepository.findByMember_MemberIdAndItem_Id(member.getMemberId(), itemId)
                .orElseThrow();/*아이템의 아이디 값과 맴버의 아이디 값이 일치 하지 않으면 채팅방의 채팅 내용을 불러오지 않음*/
    List<ChatDto> chats = chatService.getChat(roomDetail.getRoomInfo());/*채탱방 채팅내역 불러오기*/
        return ResponseEntity.ok().body(chats);
    }
}
