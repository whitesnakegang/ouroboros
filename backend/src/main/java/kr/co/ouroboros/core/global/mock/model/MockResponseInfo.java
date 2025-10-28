package kr.co.ouroboros.core.global.mock.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class MockResponseInfo {
    private int status;
    private String body;
}
