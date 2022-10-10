package com.hanghae.mungnayng.service;

import com.hanghae.mungnayng.domain.Room.Dto.RoomInfoRequestDto;
import com.hanghae.mungnayng.domain.Room.Dto.RoomInfoResponseDto;
import com.hanghae.mungnayng.domain.Room.Dto.RoomInviteDto;
import com.hanghae.mungnayng.domain.Room.RoomDetail;
import com.hanghae.mungnayng.domain.Room.RoomInfo;
import com.hanghae.mungnayng.domain.chat.Chat;
import com.hanghae.mungnayng.domain.item.Item;
import com.hanghae.mungnayng.domain.member.Member;
import com.hanghae.mungnayng.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomInfoRepository roomInfoRepository;
    private final MemberRepository memberRepository;
    private final RoomDetailRepository roomDetailsRepository;
    private final ChatRepository chatRepository;
    private final RedisRepository redisRepository;

    private final ItemRepository itemRepository;

    @Transactional
    public RoomInfoResponseDto createRoom(String nickname, Long me, Long memberId, Long itemId, String title) {
        Member member = memberRepository.findById(me).orElseThrow();
//        if(me.equals( memberId)) throw new IllegalArgumentException( "자신의 게시글 입니다.");

        RoomInfoRequestDto RequestDto = new RoomInfoRequestDto(nickname, member.getMemberId(), memberId, itemId, title);
        return createRoom(member, RequestDto);
    }

    @Transactional
    public RoomInfoResponseDto createRoom(Member member, RoomInfoRequestDto requestDto) {
        Item item = itemRepository.findById(requestDto.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("해당하는 게시글이 없습니다."));
        /*roomDetail을 통해서 맴버의 아이디 값과, 아이템의 아이디값을 조회에 있는 채팅 방일 경우 기존의 채팅 방을 아닐경우 새로운 채팅방을 생성함*/
        RoomDetail room = roomDetailsRepository.findByMember_MemberIdAndItem_Id(member.getMemberId(), requestDto.getItemId())/*맴버와 아이템 아이디 값이 없으면 빌드실행*/
                .orElseGet(() -> {
                    RoomInfo roomInfo = RoomInfo.builder()
                            .member(member)
                            .item(item)
                            .nickname(requestDto.getNickname())
                            .title(requestDto.getTitle())
                            .roomDetail(new ArrayList<>())
                            .build();
                    RoomDetail roomDetail = RoomDetail.builder()
                            .item(item)
                            .member(member)
                            .roomInfo(roomInfo)
                            .build();

                    roomInfo.getRoomDetail().add(roomDetail);
                    roomInfoRepository.save(roomInfo);
                    redisRepository.subscribe(roomInfo.getId().toString());/*redis레파지토리에서 subscribe할 roomId값을 전달함*/
                    return roomDetail;
                });
        return RoomInfoResponseDto.Info(room);
    }

/*최근에 읽은 채팅 내역 업데이트*/
    @Transactional
    public void updateLastReadChat(Long roomId, Long memberId, Long itemId) {
        RoomDetail detail = roomDetailsRepository.findByRoomInfo_IdAndMember_MemberIdAndItem_Id(roomId, memberId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방에 속해있지 않은 회원입니다."));

        Chat chat = chatRepository.findFirstByRoomDetail_RoomInfo_IdOrderByCreatedAtDesc(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅 내역이 존재하지 않습니다."));

        detail.updateChatId(chat.getId());
    }


    @Transactional(readOnly = true)
    public List<RoomInfoResponseDto> getRoomInfo(String memberId) {
        Member member = memberRepository.findById(Long.parseLong(memberId)).orElseThrow();
        return getRoomInfo(member);
    }
    /*방 리스트 전체 전달*/
    @Transactional(readOnly = true)
    public List<RoomInfoResponseDto> getRoomInfo(Member member) {
        List<RoomDetail> allByOrderByModifiedAtDesc = roomDetailsRepository.findAllByMemberOrderByModifiedAtDesc(member);
        return allByOrderByModifiedAtDesc.stream()
                .map(RoomInfoResponseDto::Info)
                .collect(Collectors.toList());
    }
/*방나가기*/
    @Transactional
    public void deleteRoomInfo(Member member, Long roomInfoId) {
        RoomInfo roomInfo = roomInfoRepository.findById(roomInfoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅창입니다."));
        if (!member.getMemberId().equals(roomInfo.getMember().getMemberId()))
            throw new IllegalArgumentException("채팅방에 존재하지 않는 유저입니다.");
        roomInfoRepository.delete(roomInfo);
    }

    @Transactional
    public void inviteRoom(Long memberId, Long roomId, RoomInviteDto inviteDto) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        inviteRoom(member, roomId, inviteDto);
    }
/*같이 방을 생성할 대상 초대 */
    public void inviteRoom(Member me, Long roomInfoId, RoomInviteDto inviteDto) {
        RoomInfo roomInfo = roomInfoRepository.findById(roomInfoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅창 입니다."));
        Member member = memberRepository.findById(inviteDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("초대 대상이 올바르지 않습니다."));
        if (me.getNickname().equals(roomInfo.getNickname()))
            throw new IllegalArgumentException("자신의 게시글 입니다.");
        Item item = itemRepository.findById(inviteDto.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));
        log.info(item.toString());
        RoomDetail roomDetail = roomDetailsRepository.findByRoomInfo_IdAndMember_MemberIdAndItem_Id(roomInfoId, inviteDto.getMemberId(), inviteDto.getItemId())
                .orElse(new RoomDetail(roomInfo, member, item));/*roomDetail의 정보 업데이트*/

        roomDetailsRepository.save(roomDetail);

    }
}
