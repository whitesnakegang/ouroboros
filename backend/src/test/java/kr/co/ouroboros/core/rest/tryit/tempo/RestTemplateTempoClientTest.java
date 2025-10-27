package kr.co.ouroboros.core.rest.tryit.tempo;

import kr.co.ouroboros.core.rest.tryit.config.TempoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for RestTemplateTempoClient.
 * Uses MockRestServiceServer to simulate Tempo API responses.
 */
@RestClientTest
class RestTemplateTempoClientTest {
    
    private RestTemplateTempoClient client;
    private MockRestServiceServer mockServer;
    private TempoProperties properties;
    
    @BeforeEach
    void setUp() {
        properties = new TempoProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://localhost:3200");
        properties.setMaxPollAttempts(3);
        properties.setPollIntervalMillis(100);
        
        RestTemplate restTemplate = new RestTemplate();
        client = new RestTemplateTempoClient(properties, new RestTemplateBuilder());
        // Access restTemplate via reflection for testing
        try {
            java.lang.reflect.Field field = RestTemplateTempoClient.class.getDeclaredField("restTemplate");
            field.setAccessible(true);
            field.set(client, restTemplate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }
    
    @Test
    void testSearchTraces_Success() {
        // Given
        String searchResponse = """
                {
                  "traces": [
                    {"traceID": "trace-1"},
                    {"traceID": "trace-2"}
                  ]
                }
                """;
        
        mockServer.expect(requestTo("http://localhost:3200/api/search?q=service.name%3Dtest"))
                .andRespond(withSuccess(searchResponse, MediaType.APPLICATION_JSON));
        
        // When
        List<String> traces = client.searchTraces("service.name=test");
        
        // Then
        assertThat(traces).hasSize(2);
        assertThat(traces).containsExactly("trace-1", "trace-2");
        mockServer.verify();
    }
    
    @Test
    void testSearchTraces_EmptyResult() {
        // Given
        String searchResponse = """
                {
                  "traces": []
                }
                """;
        
        mockServer.expect(requestTo("http://localhost:3200/api/search?q=service.name%3Dtest"))
                .andRespond(withSuccess(searchResponse, MediaType.APPLICATION_JSON));
        
        // When
        List<String> traces = client.searchTraces("service.name=test");
        
        // Then
        assertThat(traces).isEmpty();
        mockServer.verify();
    }
    
    @Test
    void testSearchTraces_WhenDisabled() {
        // Given
        properties.setEnabled(false);
        
        // When
        List<String> traces = client.searchTraces("service.name=test");
        
        // Then
        assertThat(traces).isEmpty();
        mockServer.verify();
    }
    
    @Test
    void testGetTrace_Success() {
        // Given
        String traceData = """
                {
                  "batches": [
                    {"resource": {"attributes": []}}
                  ]
                }
                """;
        
        mockServer.expect(requestTo("http://localhost:3200/api/traces/trace-1"))
                .andRespond(withSuccess(traceData, MediaType.APPLICATION_JSON));
        
        // When
        String result = client.getTrace("trace-1");
        
        // Then
        assertThat(result).isEqualTo(traceData);
        mockServer.verify();
    }
    
    @Test
    void testGetTrace_WhenDisabled() {
        // Given
        properties.setEnabled(false);
        
        // When
        String result = client.getTrace("trace-1");
        
        // Then
        assertThat(result).isNull();
    }
    
    @Test
    void testPollForTrace_FoundOnFirstAttempt() throws InterruptedException {
        // Given
        String searchResponse = """
                {
                  "traces": [{"traceID": "trace-1"}]
                }
                """;
        
        mockServer.expect(requestTo("http://localhost:3200/api/search?q=service.name%3Dtest"))
                .andRespond(withSuccess(searchResponse, MediaType.APPLICATION_JSON));
        
        // When
        String traceId = client.pollForTrace("service.name=test");
        
        // Then
        assertThat(traceId).isEqualTo("trace-1");
        mockServer.verify();
    }
    
    @Test
    void testPollForTrace_FoundOnSecondAttempt() throws InterruptedException {
        // Given
        mockServer.expect(requestTo("http://localhost:3200/api/search?q=service.name%3Dtest"))
                .andRespond(withSuccess("{\"traces\": []}", MediaType.APPLICATION_JSON));
        
        String searchResponse = """
                {
                  "traces": [{"traceID": "trace-1"}]
                }
                """;
        
        mockServer.expect(requestTo("http://localhost:3200/api/search?q=service.name%3Dtest"))
                .andRespond(withSuccess(searchResponse, MediaType.APPLICATION_JSON));
        
        // When
        String traceId = client.pollForTrace("service.name=test");
        
        // Then
        assertThat(traceId).isEqualTo("trace-1");
        mockServer.verify();
    }
    
    @Test
    void testPollForTrace_Timeout() throws InterruptedException {
        // Given
        mockServer.expect(requestTo("http://localhost:3200/api/search?q=service.name%3Dtest"))
                .andRespond(withSuccess("{\"traces\": []}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://localhost:3200/api/search?q=service.name%3Dtest"))
                .andRespond(withSuccess("{\"traces\": []}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://localhost:3200/api/search?q=service.name%3Dtest"))
                .andRespond(withSuccess("{\"traces\": []}", MediaType.APPLICATION_JSON));
        
        // When
        String traceId = client.pollForTrace("service.name=test");
        
        // Then
        assertThat(traceId).isNull();
        mockServer.verify();
    }
    
    @Test
    void testIsEnabled_WhenTrue() {
        // Given
        properties.setEnabled(true);
        
        // When/Then
        assertThat(client.isEnabled()).isTrue();
    }
    
    @Test
    void testIsEnabled_WhenFalse() {
        // Given
        properties.setEnabled(false);
        
        // When/Then
        assertThat(client.isEnabled()).isFalse();
    }
}
