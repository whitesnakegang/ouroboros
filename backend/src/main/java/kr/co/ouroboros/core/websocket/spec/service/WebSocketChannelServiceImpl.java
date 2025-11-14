package kr.co.ouroboros.core.websocket.spec.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.manager.OuroApiSpecManager;
import kr.co.ouroboros.core.websocket.common.dto.Channel;
import kr.co.ouroboros.core.websocket.common.dto.MessageReference;
import kr.co.ouroboros.core.websocket.common.yaml.WebSocketYamlParser;
import kr.co.ouroboros.core.websocket.spec.util.ReferenceConverter;
import kr.co.ouroboros.ui.websocket.spec.dto.ChannelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private final OuroApiSpecManager specManager;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public List<ChannelResponse> getAllChannels() throws Exception {
        lock.readLock().lock();
        try {
            // Read from cache
            Map<String, Object> asyncApiDoc = specManager.convertSpecToMap(specManager.getApiSpec(Protocol.WEB_SOCKET));
            if (asyncApiDoc == null) {
                return new ArrayList<>();
            }

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
            // Read from cache
            Map<String, Object> asyncApiDoc = specManager.convertSpecToMap(specManager.getApiSpec(Protocol.WEB_SOCKET));
            if (asyncApiDoc == null) {
                throw new IllegalArgumentException("No channels found. The specification file does not exist.");
            }

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
     * <p>
     * Handles conversion between YAML format ($ref) and JSON format (ref).
     *
     * @param channelMap channel definition map from YAML
     * @return Channel DTO
     */
    private Channel convertMapToChannel(Map<String, Object> channelMap) {
        try {
            // Convert $ref to ref in messages for JSON API using ReferenceConverter
            @SuppressWarnings("unchecked")
            Map<String, Object> messages = (Map<String, Object>) channelMap.get("messages");
            if (messages != null) {
                Map<String, Object> convertedMessages = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : messages.entrySet()) {
                    Object messageObj = entry.getValue();
                    if (messageObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> messageMap = (Map<String, Object>) messageObj;
                        // Convert $ref to ref using ReferenceConverter
                        Map<String, Object> convertedMessage = ReferenceConverter.convertDollarRefToRef(messageMap);
                        convertedMessages.put(entry.getKey(), convertedMessage);
                    } else {
                        convertedMessages.put(entry.getKey(), messageObj);
                    }
                }
                channelMap = new LinkedHashMap<>(channelMap);
                channelMap.put("messages", convertedMessages);
            }
            
            return objectMapper.convertValue(channelMap, Channel.class);
        } catch (Exception e) {
            log.error("Failed to convert channel map to Channel DTO", e);
            // Fallback: manual conversion
            Channel channel = new Channel();
            channel.setAddress((String) channelMap.get("address"));

            @SuppressWarnings("unchecked")
            Map<String, Object> messages = (Map<String, Object>) channelMap.get("messages");
            if (messages != null) {
                // Convert $ref to ref in messages using ReferenceConverter
                Map<String, MessageReference> convertedMessages = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : messages.entrySet()) {
                    Object messageObj = entry.getValue();
                    if (messageObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> messageMap = (Map<String, Object>) messageObj;
                        // Convert using ReferenceConverter
                        Map<String, Object> convertedMap = ReferenceConverter.convertDollarRefToRef(messageMap);
                        MessageReference msgRef = new MessageReference();
                        // Handle both $ref (YAML) and ref (JSON)
                        String ref = (String) convertedMap.get("ref");
                        if (ref == null) {
                            ref = (String) messageMap.get("$ref");
                        }
                        msgRef.setRef(ref);
                        convertedMessages.put(entry.getKey(), msgRef);
                    }
                }
                channel.setMessages(convertedMessages);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> bindings = (Map<String, Object>) channelMap.get("bindings");
            channel.setBindings(bindings);

            return channel;
        }
    }
}
