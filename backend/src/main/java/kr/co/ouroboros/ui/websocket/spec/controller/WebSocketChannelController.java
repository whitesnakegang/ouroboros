package kr.co.ouroboros.ui.websocket.spec.controller;

import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import kr.co.ouroboros.core.websocket.spec.service.WebSocketChannelService;
import kr.co.ouroboros.ui.websocket.spec.dto.ChannelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing AsyncAPI channels.
 * <p>
 * Provides read-only endpoints for channels that are automatically created by operations.
 *
 * @since 0.1.0
 */
@Slf4j
@RestController
@RequestMapping("/ouro/websocket-specs/channels")
@RequiredArgsConstructor
public class WebSocketChannelController {

    private final WebSocketChannelService channelService;

    /**
     * Retrieves all channels.
     *
     * @return response containing list of all channels
     */
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<ChannelResponse>>> getAllChannels() {
        try {
            List<ChannelResponse> channels = channelService.getAllChannels();
            return ResponseEntity.ok(GlobalApiResponse.success(channels, "Channels retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve channels", e);
            return ResponseEntity.status(500).body(
                    GlobalApiResponse.error(500, "Failed to retrieve channels", "CHANNEL_RETRIEVAL_ERROR", "An internal error occurred")
            );
        }
    }

    /**
     * Retrieves a single channel by name.
     *
     * @param channelName the name of the channel to retrieve
     * @return response containing the channel
     */
    @GetMapping("/{channelName}")
    public ResponseEntity<GlobalApiResponse<ChannelResponse>> getChannel(
            @PathVariable String channelName) {
        try {
            ChannelResponse channel = channelService.getChannel(channelName);
            return ResponseEntity.ok(GlobalApiResponse.success(channel, "Channel retrieved successfully"));
        } catch (IllegalArgumentException e) {
            log.warn("Channel not found: {}", channelName);
            return ResponseEntity.status(404).body(
                    GlobalApiResponse.error(404, "Channel not found", "CHANNEL_NOT_FOUND", e.getMessage())
            );
        } catch (Exception e) {
            log.error("Failed to retrieve channel: {}", channelName, e);
            return ResponseEntity.status(500).body(
                    GlobalApiResponse.error(500, "Failed to retrieve channel", "CHANNEL_RETRIEVAL_ERROR", "An internal error occurred")
            );
        }
    }
}
