package kr.co.ouroboros.core.websocket.spec.service;

import kr.co.ouroboros.ui.websocket.spec.dto.ChannelResponse;

import java.util.List;

/**
 * Service interface for managing AsyncAPI channels.
 * <p>
 * Provides read-only operations for channels that are automatically created by operations.
 *
 * @since 0.1.0
 */
public interface WebSocketChannelService {

    /**
     * Retrieves all channels from the AsyncAPI specification.
     *
     * @return list of all channel responses
     * @throws Exception if reading the specification fails
     */
    List<ChannelResponse> getAllChannels() throws Exception;

    /**
     * Retrieves a single channel by name.
     *
     * @param channelName the name of the channel to retrieve
     * @return channel response
     * @throws IllegalArgumentException if the channel is not found
     * @throws Exception if reading the specification fails
     */
    ChannelResponse getChannel(String channelName) throws Exception;
}
