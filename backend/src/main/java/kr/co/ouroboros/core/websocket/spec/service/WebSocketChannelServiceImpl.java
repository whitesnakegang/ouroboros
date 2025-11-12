package kr.co.ouroboros.core.websocket.spec.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.websocket.common.dto.Channel;
import kr.co.ouroboros.core.websocket.common.yaml.WebSocketYamlParser;
import kr.co.ouroboros.ui.websocket.spec.dto.ChannelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of {@link WebSocketChannelService}.
 * <p>
 * Provides read-only access to channels in the AsyncAPI specification.
 * Channels are automatically created and managed by operations.
 *
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketChannelServiceImpl implements WebSocketChannelService {

    private final WebSocketYamlParser yamlParser;
    private final ObjectMapper objectMapper;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public List<ChannelResponse> getAllChannels() throws Exception {
        lock.readLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                return new ArrayList<>();
            }

            Map<String, Object> asyncApiDoc = yamlParser.readDocument();
            Map<String, Object> channels = yamlParser.getChannels(asyncApiDoc);

            if (channels == null || channels.isEmpty()) {
                return new ArrayList<>();
            }

            List<ChannelResponse> responses = new ArrayList<>();
            for (Map.Entry<String, Object> entry : channels.entrySet()) {
                String channelName = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> channelDefinition = (Map<String, Object>) entry.getValue();

                Channel channel = convertMapToChannel(channelDefinition);

                responses.add(ChannelResponse.builder()
                        .channelName(channelName)
                        .channel(channel)
                        .build());
            }

            return responses;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public ChannelResponse getChannel(String channelName) throws Exception {
        lock.readLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No channels found. The specification file does not exist.");
            }

            Map<String, Object> asyncApiDoc = yamlParser.readDocument();
            Map<String, Object> channelDefinition = yamlParser.getChannel(asyncApiDoc, channelName);

            if (channelDefinition == null) {
                throw new IllegalArgumentException("Channel '" + channelName + "' not found");
            }

            Channel channel = convertMapToChannel(channelDefinition);

            return ChannelResponse.builder()
                    .channelName(channelName)
                    .channel(channel)
                    .build();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Converts a Map to a Channel DTO.
     *
     * @param channelMap channel definition map
     * @return Channel DTO
     */
    private Channel convertMapToChannel(Map<String, Object> channelMap) {
        try {
            return objectMapper.convertValue(channelMap, Channel.class);
        } catch (Exception e) {
            log.error("Failed to convert channel map to Channel DTO", e);
            // Fallback: manual conversion
            Channel channel = new Channel();
            channel.setAddress((String) channelMap.get("address"));

            @SuppressWarnings("unchecked")
            Map<String, Object> messages = (Map<String, Object>) channelMap.get("messages");
            if (messages != null) {
                // Messages will be automatically converted by Jackson
                channel.setMessages((Map) messages);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> bindings = (Map<String, Object>) channelMap.get("bindings");
            channel.setBindings(bindings);

            return channel;
        }
    }
}
