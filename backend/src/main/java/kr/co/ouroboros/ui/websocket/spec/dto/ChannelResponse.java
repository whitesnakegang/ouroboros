package kr.co.ouroboros.ui.websocket.spec.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import kr.co.ouroboros.core.websocket.common.dto.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for AsyncAPI channel operations.
 * <p>
 * Wraps the Channel common DTO with channel name for identification.
 *
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChannelResponse {

    /**
     * Channel name (identifier in channels section)
     */
    private String channelName;

    /**
     * Channel definition (from common DTO)
     */
    private Channel channel;
}
