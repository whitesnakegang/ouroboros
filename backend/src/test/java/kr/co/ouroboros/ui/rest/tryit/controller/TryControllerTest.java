package kr.co.ouroboros.ui.rest.tryit.controller;

import kr.co.ouroboros.ui.rest.tryit.dto.TryIssuesResponse;
import kr.co.ouroboros.ui.rest.tryit.dto.TryMethodListResponse;
import kr.co.ouroboros.ui.rest.tryit.dto.TrySummaryResponse;
import kr.co.ouroboros.ui.rest.tryit.dto.TryTraceResponse;
import kr.co.ouroboros.core.rest.tryit.service.TryIssuesService;
import kr.co.ouroboros.core.rest.tryit.service.TryMethodListService;
import kr.co.ouroboros.core.rest.tryit.service.TrySummaryService;
import kr.co.ouroboros.core.rest.tryit.service.TryTraceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TryController 테스트")
class TryControllerTest {

    @Mock
    private TryMethodListService tryMethodListService;

    @Mock
    private TryTraceService tryTraceService;

    @Mock
    private TryIssuesService tryIssuesService;

    @Mock
    private TrySummaryService trySummaryService;

    @InjectMocks
    private TryController tryController;

    private MockMvc mockMvc;
    private String validTryId;

    @BeforeEach
    void setUp() {
        // Bean Validation을 위한 설정
        LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.afterPropertiesSet();
        
        // MethodValidationInterceptor를 사용하여 AOP 프록시 생성
        MethodValidationInterceptor interceptor = new MethodValidationInterceptor(validatorFactory.getValidator());
        
        // 컨트롤러를 AOP 프록시로 감싸기
        ProxyFactory proxyFactory = new ProxyFactory(tryController);
        proxyFactory.addAdvice(interceptor);
        TryController proxiedController = (TryController) proxyFactory.getProxy();
        
        mockMvc = MockMvcBuilders.standaloneSetup(proxiedController)
                .setControllerAdvice(
                        new kr.co.ouroboros.core.rest.tryit.exception.TryExceptionHandler(),
                        new kr.co.ouroboros.core.global.exception.GlobalExceptionHandler()
                )
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter())
                .build();
        validTryId = UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("유효한 tryId로 summary 조회 성공")
    void getSummary_Success() throws Exception {
        // given
        TrySummaryResponse response = TrySummaryResponse.builder()
                .tryId(validTryId)
                .traceId("trace123")
                .status(kr.co.ouroboros.core.rest.tryit.trace.dto.AnalysisStatus.COMPLETED)
                .statusCode(200)
                .totalDurationMs(100L)
                .spanCount(5)
                .issueCount(2)
                .build();

        when(trySummaryService.getSummary(validTryId)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/ouro/tries/{tryId}", validTryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tryId").value(validTryId))
                .andExpect(jsonPath("$.data.traceId").value("trace123"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.statusCode").value(200))
                .andExpect(jsonPath("$.data.totalDurationMs").value(100))
                .andExpect(jsonPath("$.data.spanCount").value(5))
                .andExpect(jsonPath("$.data.issueCount").value(2));
    }

    @Test
    @DisplayName("유효하지 않은 tryId 형식으로 summary 조회 시 400 에러")
    void getSummary_InvalidTryId_Returns400() throws Exception {
        // given
        String invalidTryId = "invalid-uuid";

        // when & then
        mockMvc.perform(get("/ouro/tries/{tryId}", invalidTryId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효한 tryId로 methods 조회 성공")
    void getMethods_Success() throws Exception {
        // given
        TryMethodListResponse.MethodInfo methodInfo = TryMethodListResponse.MethodInfo.builder()
                .spanId("span1")
                .name("OrderController.getOrder")
                .methodName("getOrder")
                .className("OrderController")
                .selfDurationMs(50L)
                .selfPercentage(50.0)
                .parameters(List.of())
                .build();

        TryMethodListResponse response = TryMethodListResponse.builder()
                .tryId(validTryId)
                .traceId("trace123")
                .totalDurationMs(100L)
                .totalCount(1)
                .page(0)
                .size(5)
                .hasMore(false)
                .methods(List.of(methodInfo))
                .build();

        when(tryMethodListService.getMethodList(validTryId, 0, 5)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/ouro/tries/{tryId}/methods", validTryId)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tryId").value(validTryId))
                .andExpect(jsonPath("$.data.traceId").value("trace123"))
                .andExpect(jsonPath("$.data.totalDurationMs").value(100))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.hasMore").value(false))
                .andExpect(jsonPath("$.data.methods[0].spanId").value("span1"))
                .andExpect(jsonPath("$.data.methods[0].name").value("OrderController.getOrder"))
                .andExpect(jsonPath("$.data.methods[0].methodName").value("getOrder"))
                .andExpect(jsonPath("$.data.methods[0].className").value("OrderController"));
    }

    @Test
    @DisplayName("methods 조회 시 기본 페이지 파라미터 사용")
    void getMethods_DefaultParameters() throws Exception {
        // given
        TryMethodListResponse response = TryMethodListResponse.builder()
                .tryId(validTryId)
                .traceId("trace123")
                .totalDurationMs(100L)
                .totalCount(0)
                .page(0)
                .size(5)
                .hasMore(false)
                .methods(List.of())
                .build();

        when(tryMethodListService.getMethodList(validTryId, 0, 5)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/ouro/tries/{tryId}/methods", validTryId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("유효한 tryId로 trace 조회 성공")
    void getTrace_Success() throws Exception {
        // given
        TryTraceResponse response = TryTraceResponse.builder()
                .tryId(validTryId)
                .traceId("trace123")
                .totalDurationMs(100L)
                .spans(List.of())
                .build();

        when(tryTraceService.getTrace(validTryId)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/ouro/tries/{tryId}/trace", validTryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tryId").value(validTryId))
                .andExpect(jsonPath("$.data.traceId").value("trace123"))
                .andExpect(jsonPath("$.data.totalDurationMs").value(100));
    }

    @Test
    @DisplayName("유효한 tryId로 issues 조회 성공")
    void getIssues_Success() throws Exception {
        // given
        TryIssuesResponse response = TryIssuesResponse.builder()
                .tryId(validTryId)
                .issues(List.of())
                .build();

        when(tryIssuesService.getIssues(validTryId)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/ouro/tries/{tryId}/issues", validTryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tryId").value(validTryId))
                .andExpect(jsonPath("$.data.issues").isArray());
    }

    @Test
    @DisplayName("유효하지 않은 tryId 형식으로 methods 조회 시 400 에러")
    void getMethods_InvalidTryId_Returns400() throws Exception {
        // given
        String invalidTryId = "invalid-uuid";

        // when & then
        mockMvc.perform(get("/ouro/tries/{tryId}/methods", invalidTryId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효하지 않은 tryId 형식으로 trace 조회 시 400 에러")
    void getTrace_InvalidTryId_Returns400() throws Exception {
        // given
        String invalidTryId = "invalid-uuid";

        // when & then
        mockMvc.perform(get("/ouro/tries/{tryId}/trace", invalidTryId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효하지 않은 tryId 형식으로 issues 조회 시 400 에러")
    void getIssues_InvalidTryId_Returns400() throws Exception {
        // given
        String invalidTryId = "invalid-uuid";

        // when & then
        mockMvc.perform(get("/ouro/tries/{tryId}/issues", invalidTryId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("음수 page 값으로 methods 조회 시 400 에러")
    void getMethods_NegativePage_Returns400() throws Exception {
        // given
        int negativePage = -1;

        // when & then
        mockMvc.perform(get("/ouro/tries/{tryId}/methods", validTryId)
                        .param("page", String.valueOf(negativePage))
                        .param("size", "5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("size가 0인 경우 methods 조회 시 400 에러")
    void getMethods_ZeroSize_Returns400() throws Exception {
        // given
        int zeroSize = 0;

        // when & then
        mockMvc.perform(get("/ouro/tries/{tryId}/methods", validTryId)
                        .param("page", "0")
                        .param("size", String.valueOf(zeroSize)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("음수 size 값으로 methods 조회 시 400 에러")
    void getMethods_NegativeSize_Returns400() throws Exception {
        // given
        int negativeSize = -1;

        // when & then
        mockMvc.perform(get("/ouro/tries/{tryId}/methods", validTryId)
                        .param("page", "0")
                        .param("size", String.valueOf(negativeSize)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("size가 100 초과인 경우 methods 조회 시 400 에러")
    void getMethods_SizeExceedsMax_Returns400() throws Exception {
        // given
        int tooLargeSize = 101;

        // when & then
        mockMvc.perform(get("/ouro/tries/{tryId}/methods", validTryId)
                        .param("page", "0")
                        .param("size", String.valueOf(tooLargeSize)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효한 범위 내의 page와 size로 methods 조회 성공")
    void getMethods_ValidPagination_Success() throws Exception {
        // given
        TryMethodListResponse response = TryMethodListResponse.builder()
                .tryId(validTryId)
                .traceId("trace123")
                .totalDurationMs(100L)
                .totalCount(0)
                .page(10)
                .size(50)
                .hasMore(false)
                .methods(List.of())
                .build();

        when(tryMethodListService.getMethodList(validTryId, 10, 50)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/ouro/tries/{tryId}/methods", validTryId)
                        .param("page", "10")
                        .param("size", "50"))
                .andExpect(status().isOk());
    }
}

