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
    public ResponseEntity<GlobalApiResponse<List<ChannelResponse>>> getAllChannels() throws Exception {
        List<ChannelResponse> channels = channelService.getAllChannels();
        return ResponseEntity.ok(GlobalApiResponse.success(channels, "Channels retrieved successfully"));
    }


    /**
     * Retrieves a single channel by name.
     *
     * @param channelName the name of the channel to retrieve
     * @return response containing the channel
     */
    @GetMapping("/{channelName}")
    public ResponseEntity<GlobalApiResponse<ChannelResponse>> getChannel(
            @PathVariable String channelName) throws Exception {
        ChannelResponse channel = channelService.getChannel(channelName);
        return ResponseEntity.ok(GlobalApiResponse.success(channel, "Channel retrieved successfully"));
    }
}
