package kr.co.ouroboros.core.rest.spec.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRestApiResponse {
    private boolean success;
    private String message;
    private String filePath;
}
