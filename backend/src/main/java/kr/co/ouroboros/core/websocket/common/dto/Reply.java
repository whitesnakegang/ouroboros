package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AsyncAPI Reply Object.
 * <p>
 * Represents a reply configuration for an operation.
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Reply {

    private ChannelReference channel;
    private List<MessageReference> messages;
}

