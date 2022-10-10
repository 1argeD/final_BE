package com.hanghae.mungnayng.repository;

import com.hanghae.mungnayng.Redis.RedisSub;
import com.hanghae.mungnayng.domain.Room.RoomInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Repository
public class RedisRepository {
    private final RedisSub redisSub;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final RoomInfoRepository roomInfoRepository;
    private Map<String, ChannelTopic> topicMap;

    @PostConstruct
    private void init() {
        topicMap = new HashMap<>();
        List<RoomInfo> roomList = roomInfoRepository.findAll();
        for (RoomInfo c : roomList) {
            ChannelTopic topic = ChannelTopic.of(c.getId().toString());
            redisMessageListenerContainer.addMessageListener(redisSub, topic);
            topicMap.put(c.getId().toString(), topic);
        }
    }
/*roomService에서  전달받은 roomId를 업데이트 해줌*/
    public void subscribe(String roomId) {
        ChannelTopic topic = topicMap.get(roomId);
        if (topic == null) {
            topic = ChannelTopic.of(roomId);
            redisMessageListenerContainer.addMessageListener(redisSub, topic);
            topicMap.put(roomId, topic);
        }
    }
/*실질적으로 메세지를 subscribe할 roomId를 불러옴*/
    public ChannelTopic getTopic(String roomId) {
        return topicMap.get(roomId);
    }
}
