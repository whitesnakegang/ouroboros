package c102.com.demoapigen.model;

import lombok.Data;

@Data
public class StatusResponse {
    private Integer statusCode;  // HTTP status code (200, 400, 404, etc.)
    private Response response;   // Response structure for this status code
}
