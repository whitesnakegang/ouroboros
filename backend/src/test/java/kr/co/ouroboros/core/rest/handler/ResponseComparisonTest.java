package kr.co.ouroboros.core.rest.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import kr.co.ouroboros.core.rest.common.dto.MediaType;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.Response;
import kr.co.ouroboros.core.rest.handler.RequestDiffHelper.HttpMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

//@SpringBootTest
public class ResponseComparisonTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private ResponseComparator responseComparator;

    @BeforeEach
    public void setUp() {
        responseComparator = new ResponseComparator();
    }

    @Test
    public void testOO_AllMatch_thenProgressCompleted_andNoDiff() throws Exception {
        // 스캔된 스펙 (User 스키마 참조)
        String scannedJson = """
                {
                  "openapi": "3.1.0",
                  "info": {
                    "title": "OpenAPI definition",
                    "version": "v0"
                  },
                  "paths": {
                    "/api/test/users/{id}": {
                      "put": {
                        "x-ouroboros-id": "a886a792-b09c-40f2-b4ae-62f74c3c2009",
                        "x-ouroboros-progress": "mock",
                        "x-ouroboros-tag": "none",
                        "x-ouroboros-diff": "none",
                        "responses": {
                          "200": {
                            "description": "정상적으로 조회됨",
                            "content": {
                              "application/json": {
                                "schema": {
                                  "$ref": "#/components/schemas/User"
                                }
                              }
                            }
                          },
                          "400": {
                            "description": "잘못된 요청 파라미터",
                            "content": {
                              "*/*": {
                                "schema": {
                                  "type": "object",
                                  "additionalProperties": {}
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  },
                  "components": {
                    "schemas": {
                      "User": {
                        "type": "object",
                        "properties": {
                          "name": {
                            "type": "string"
                          },
                          "age": {
                            "type": "integer",
                            "format": "int32"
                          }
                        }
                      }
                    }
                  }
                }
                """;

        // 파일 스펙 (동일한 User 스키마 참조)
        String fileJson = """
                {
                  "openapi": "3.1.0",
                  "info": {
                    "title": "OpenAPI definition",
                    "version": "v0"
                  },
                  "paths": {
                    "/api/test/users/{id}": {
                      "put": {
                        "x-ouroboros-id": "a886a792-b09c-40f2-b4ae-62f74c3c2009",
                        "x-ouroboros-progress": "mock",
                        "x-ouroboros-tag": "none",
                        "x-ouroboros-diff": "none",
                        "responses": {
                          "200": {
                            "description": "정상적으로 조회됨",
                            "content": {
                              "application/json": {
                                "schema": {
                                  "$ref": "#/components/schemas/User"
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  },
                  "components": {
                    "schemas": {
                      "User": {
                        "type": "object",
                        "properties": {
                          "name": {
                            "type": "string"
                          },
                          "age": {
                            "type": "integer",
                            "format": "int32"
                          }
                        }
                      }
                    }
                  }
                }
                """;

        OuroRestApiSpec scannedSpec = mapper.readValue(scannedJson, OuroRestApiSpec.class);
        OuroRestApiSpec fileSpec = mapper.readValue(fileJson, OuroRestApiSpec.class);

        // 스키마 매칭 결과 (User 스키마가 일치)
        Map<String, Boolean> schemaMatchResults = new HashMap<>();
        schemaMatchResults.put("User", true);

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/api/test/users/{id}")
                .getPut();
        Operation fileOperation = fileSpec.getPaths()
                .get("/api/test/users/{id}")
                .getPut();

        // 응답 비교 실행
        // 파일 스펙의 초기 상태: diff 없음("none"), progress 없음(null)
        responseComparator.compareResponsesForMethod("/api/test/users/{id}", HttpMethod.PUT, scannedOperation, fileOperation, schemaMatchResults);

        // 검증: 모든 응답 일치이므로 diff는 none 유지, progress=completed 설정
        assertEquals("none", fileOperation.getXOuroborosDiff(), "일치하는 응답이면 diff가 none이어야 합니다.");
        assertEquals("completed", fileOperation.getXOuroborosProgress(), "응답이 모두 일치하고 diff가 none이면 progress는 completed여야 합니다.");
        // 검증: 스캔에만 있던 400 응답은 파일 스펙에 추가됨(X/O 규칙)
        assertNotNull(fileOperation.getResponses()
                .get("400"), "누락된 400 응답이 파일 스펙에 추가되어야 합니다.");
    }

    @Test
    public void testOO_Mismatch_whenDiffNone_thenSetDiffResponse_andProgressMockIfCompleted() throws Exception {
        // 스캔된 스펙 (User 스키마 참조)
        String scannedJson = """
                {
                  "openapi": "3.1.0",
                  "info": {
                    "title": "OpenAPI definition",
                    "version": "v0"
                  },
                  "paths": {
                    "/api/test/users": {
                      "get": {
                        "x-ouroboros-id": "11111111-1111-1111-1111-111111111111",
                        "x-ouroboros-progress": "completed",
                        "x-ouroboros-tag": "none",
                        "x-ouroboros-diff": "none",
                        "responses": {
                          "200": {
                            "description": "OK",
                            "content": {
                              "application/json": {
                                "schema": {
                                  "$ref": "#/components/schemas/User"
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  },
                  "components": {
                    "schemas": {
                      "User": {
                        "type": "object",
                        "properties": {
                          "name": {
                            "type": "string"
                          },
                          "age": {
                            "type": "integer",
                            "format": "int32"
                          }
                        }
                      }
                    }
                  }
                }
                """;

        // 파일 스펙 (다른 스키마 참조)
        String fileJson = """
                {
                  "openapi": "3.1.0",
                  "info": {
                    "title": "OpenAPI definition",
                    "version": "v0"
                  },
                  "paths": {
                    "/api/test/users": {
                      "get": {
                        "x-ouroboros-id": "11111111-1111-1111-1111-111111111111",
                        "x-ouroboros-progress": "mock",
                        "x-ouroboros-tag": "none",
                        "x-ouroboros-diff": "none",
                        "responses": {
                          "200": {
                            "description": "OK",
                            "content": {
                              "application/json": {
                                "schema": {
                                  "$ref": "#/components/schemas/Book"
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  },
                  "components": {
                    "schemas": {
                      "Book": {
                        "type": "object",
                        "properties": {
                          "title": {
                            "type": "string"
                          }
                        }
                      }
                    }
                  }
                }
                """;

        OuroRestApiSpec scannedSpec = mapper.readValue(scannedJson, OuroRestApiSpec.class);
        OuroRestApiSpec fileSpec = mapper.readValue(fileJson, OuroRestApiSpec.class);

        // 스키마 매칭 결과 (User 스키마가 일치하지 않음)
        Map<String, Boolean> schemaMatchResults = new HashMap<>();
        schemaMatchResults.put("User", false);

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/api/test/users")
                .getGet();
        Operation fileOperation = fileSpec.getPaths()
                .get("/api/test/users")
                .getGet();

        // 파일 스펙의 초기 상태는 JSON에 포함됨: diff=none, progress=completed

        // 응답 비교 실행
        responseComparator.compareResponsesForMethod("/api/test/users", HttpMethod.GET, scannedOperation, fileOperation, schemaMatchResults);

        // 검증: 불일치 → diff: none -> response, progress: completed -> mock
        assertEquals("response", fileOperation.getXOuroborosDiff(), "$ref가 다르면 diff는 'response'가 되어야 합니다.");
        assertEquals("mock", fileOperation.getXOuroborosProgress(), "불일치면 completed였던 progress는 mock으로 바뀌어야 합니다.");
    }

    @Test
    public void testOO_Mismatch_whenDiffRequest_thenSetDiffBoth_andProgressMockIfCompleted() throws Exception {
        // 스캔된 스펙 (string 타입)
        String scannedJson = """
                {
                  "openapi": "3.1.0",
                  "info": {
                    "title": "OpenAPI definition",
                    "version": "v0"
                  },
                  "paths": {
                    "/api/test/response": {
                      "get": {
                        "x-ouroboros-id": "a886a792-b09c-40f2-b4ae-62f74c3c2009",
                        "x-ouroboros-progress": "completed",
                        "x-ouroboros-tag": "none",
                        "x-ouroboros-diff": "request",
                        "responses": {
                          "200": {
                            "description": "OK",
                            "content": {
                              "*/*": {
                                "schema": {
                                  "type": "string"
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        // 파일 스펙 (integer 타입)
        String fileJson = """
                {
                  "openapi": "3.1.0",
                  "info": {
                    "title": "OpenAPI definition",
                    "version": "v0"
                  },
                  "paths": {
                    "/api/test/response": {
                      "get": {
                        "x-ouroboros-id": "a886a792-b09c-40f2-b4ae-62f74c3c2009",
                        "x-ouroboros-progress": "mock",
                        "x-ouroboros-tag": "none",
                        "x-ouroboros-diff": "request",
                        "responses": {
                          "200": {
                            "description": "OK",
                            "content": {
                              "*/*": {
                                "schema": {
                                  "type": "integer"
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        OuroRestApiSpec scannedSpec = mapper.readValue(scannedJson, OuroRestApiSpec.class);
        OuroRestApiSpec fileSpec = mapper.readValue(fileJson, OuroRestApiSpec.class);

        Map<String, Boolean> schemaMatchResults = new HashMap<>();

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/api/test/response")
                .getGet();
        Operation fileOperation = fileSpec.getPaths()
                .get("/api/test/response")
                .getGet();

        // 파일 스펙의 초기 상태는 JSON에 포함됨: diff=request, progress=completed

        // 응답 비교 실행 (경로/메서드 일치하게 정정)
        responseComparator.compareResponsesForMethod("/api/test/response", HttpMethod.GET, scannedOperation, fileOperation, schemaMatchResults);

        // 검증: 불일치 → diff: request -> both, progress: completed -> mock
        assertEquals("both", fileOperation.getXOuroborosDiff(), "기존 diff가 request이면 both로 변경되어야 합니다.");
        assertEquals("mock", fileOperation.getXOuroborosProgress(), "불일치면 completed였던 progress는 mock으로 바뀌어야 합니다.");
    }

    @Test
    public void testResponseComparisonWithWildcardContentType() throws Exception {
        // 스캔된 스펙 (*/* content-type)
        String scannedJson = """
                {
                  "openapi": "3.1.0",
                  "info": {
                    "title": "OpenAPI definition",
                    "version": "v0"
                  },
                  "paths": {
                    "/api/test/wildcard": {
                      "get": {
                        "x-ouroboros-id": "22222222-2222-2222-2222-222222222222",
                        "x-ouroboros-progress": "mock",
                        "x-ouroboros-tag": "none",
                        "x-ouroboros-diff": "none",
                        "responses": {
                          "200": {
                            "description": "OK",
                            "content": {
                              "*/*": {
                                "schema": {
                                  "type": "string"
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        // 파일 스펙 (application/json content-type)
        String fileJson = """
                {
                  "openapi": "3.1.0",
                  "info": {
                    "title": "OpenAPI definition",
                    "version": "v0"
                  },
                  "paths": {
                    "/api/test/wildcard": {
                      "get": {
                        "x-ouroboros-id": "22222222-2222-2222-2222-222222222222",
                        "x-ouroboros-progress": "mock",
                        "x-ouroboros-tag": "none",
                        "x-ouroboros-diff": "none",
                        "responses": {
                          "200": {
                            "description": "OK",
                            "content": {
                              "application/json": {
                                "schema": {
                                  "type": "string"
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        OuroRestApiSpec scannedSpec = mapper.readValue(scannedJson, OuroRestApiSpec.class);
        OuroRestApiSpec fileSpec = mapper.readValue(fileJson, OuroRestApiSpec.class);

        Map<String, Boolean> schemaMatchResults = new HashMap<>();

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/api/test/wildcard")
                .getGet();
        Operation fileOperation = fileSpec.getPaths()
                .get("/api/test/wildcard")
                .getGet();

        // 응답 비교 실행
        responseComparator.compareResponsesForMethod("/api/test/wildcard", HttpMethod.GET, scannedOperation, fileOperation, schemaMatchResults);

        // 검증: */*는 모든 content-type과 일치하므로 diff는 none 유지
        assertEquals("none", fileOperation.getXOuroborosDiff(), "*/* content-type은 모든 타입과 일치하므로 diff는 none이어야 합니다.");
    }

    @Test
    public void testNullOperations_OX_Skip_withoutChanges() throws Exception {
        Map<String, Boolean> schemaMatchResults = new HashMap<>();

        // O/X: 파일만 있고 스캔에는 없는 경우 → 검사 스킵, 변경 없음
        Operation scannedOperation = new Operation();
        scannedOperation.setResponses(null); // 스캔에 응답 없음
        Operation fileOperation = new Operation();
        Map<String, Response> fileResponses = new HashMap<>();
        Response fileResponse = new Response();
        Map<String, MediaType> fileContent = new HashMap<>();
        MediaType mt = new MediaType();
        fileContent.put("application/json", mt);
        fileResponse.setContent(fileContent);
        fileResponses.put("200", fileResponse);
        fileOperation.setResponses(fileResponses);
        fileOperation.setXOuroborosDiff("none");
        fileOperation.setXOuroborosProgress("completed");

        responseComparator.compareResponsesForMethod("/api/test/null", HttpMethod.GET, scannedOperation, fileOperation, schemaMatchResults);

        // 변경 없음 확인
        assertEquals("none", fileOperation.getXOuroborosDiff());
        assertEquals("completed", fileOperation.getXOuroborosProgress());
    }

    @Test
    public void testXO_MissingInFile_thenInsertResponse_noDiffNoProgressChange() throws Exception {
        // 스캔(코드)에만 있는 상태코드가 파일에 삽입되어야 함 (X/O)
        String scannedJson = """
                {
                  "openapi": "3.1.0",
                  "info": {
                    "title": "t",
                    "version": "v0"
                  },
                  "paths": {
                    "/p": {
                      "get": {
                        "x-ouroboros-diff": "none",
                        "x-ouroboros-progress": "mock",
                        "responses": {
                          "200": {
                            "description": "ok",
                            "content": {
                              "application/json": {
                                "schema": {
                                  "type": "string"
                                }
                              }
                            }
                          },
                          "201": {
                            "description": "created",
                            "content": {
                              "application/json": {
                                "schema": {
                                  "type": "string"
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        String fileJson = """
                {
                  "openapi": "3.1.0",
                  "info": {
                    "title": "t",
                    "version": "v0"
                  },
                  "paths": {
                    "/p": {
                      "get": {
                        "x-ouroboros-diff": "none",
                        "x-ouroboros-progress": "mock",
                        "responses": {
                          "200": {
                            "description": "ok",
                            "content": {
                              "application/json": {
                                "schema": {
                                  "type": "string"
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        OuroRestApiSpec scannedSpec = mapper.readValue(scannedJson, OuroRestApiSpec.class);
        OuroRestApiSpec fileSpec = mapper.readValue(fileJson, OuroRestApiSpec.class);

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/p")
                .getGet();
        Operation fileOperation = fileSpec.getPaths()
                .get("/p")
                .getGet();

        responseComparator.compareResponsesForMethod("/p", HttpMethod.GET, scannedOperation, fileOperation, new HashMap<>());

        // 201이 추가되고 diff/progress는 변하지 않음
        assertNotNull(fileOperation.getResponses()
                .get("201"));
        assertEquals("none", fileOperation.getXOuroborosDiff());
        assertEquals("completed", fileOperation.getXOuroborosProgress());
    }

    @Test
    public void testOO_FileOnlyStatus_whenDiffNone_setsResponseAndMock() throws Exception {
        // 파일에만 있는 상태코드(O/X) → 엔드포인트 불일치로 간주: diff none -> response, progress completed -> mock
        String scannedJson = """
                    {
                      "openapi": "3.1.0",
                      "info": {"title": "t","version": "v0"},
                      "paths": {"/e": {"get": {
                        "x-ouroboros-id": "33333333-3333-3333-3333-333333333333",
                        "x-ouroboros-progress": "completed",
                        "x-ouroboros-tag": "none",
                        "x-ouroboros-diff": "none",
                        "responses": {"201": {"description": "created","content": {"application/json": {"schema": {"type": "string"}}}}}
                      }}}
                    }
                """;
        String fileJson = """
                    {
                      "openapi": "3.1.0",
                      "info": {"title": "t","version": "v0"},
                      "paths": {"/e": {"get": {
                        "x-ouroboros-id": "33333333-3333-3333-3333-333333333333",
                        "x-ouroboros-progress": "completed",
                        "x-ouroboros-tag": "none",
                        "x-ouroboros-diff": "none",
                        "responses": {"200": {"description": "ok","content": {"application/json": {"schema": {"type": "string"}}}}}
                      }}}
                    }
                """;

        OuroRestApiSpec scannedSpec = mapper.readValue(scannedJson, OuroRestApiSpec.class);
        OuroRestApiSpec fileSpec = mapper.readValue(fileJson, OuroRestApiSpec.class);

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/e")
                .getGet();
        Operation fileOperation = fileSpec.getPaths()
                .get("/e")
                .getGet();

        responseComparator.compareResponsesForMethod("/e", HttpMethod.GET, scannedOperation, fileOperation, new HashMap<>());

        assertEquals("response", fileOperation.getXOuroborosDiff());
        assertEquals("mock", fileOperation.getXOuroborosProgress());
    }

    @Test
    public void testOO_FileOnlyStatus_whenDiffRequest_setsBothAndMock() throws Exception {
        // 파일에만 있는 상태코드(O/X) + 기존 diff=request → both, progress completed -> mock
        String scannedJson = """
                    {
                      "openapi": "3.1.0",
                      "info": {"title": "t","version": "v0"},
                      "paths": {"/e2": {"get": {
                        "x-ouroboros-id": "44444444-4444-4444-4444-444444444444",
                        "x-ouroboros-progress": "completed",
                        "x-ouroboros-tag": "none",
                        "x-ouroboros-diff": "request",
                        "responses": {"201": {"description": "created","content": {"application/json": {"schema": {"type": "string"}}}}}
                      }}}
                    }
                """;
        String fileJson = """
                    {
                      "openapi": "3.1.0",
                      "info": {"title": "t","version": "v0"},
                      "paths": {"/e2": {"get": {
                        "x-ouroboros-id": "44444444-4444-4444-4444-444444444444",
                        "x-ouroboros-progress": "completed",
                        "x-ouroboros-tag": "none",
                        "x-ouroboros-diff": "request",
                        "responses": {"200": {"description": "ok","content": {"application/json": {"schema": {"type": "string"}}}}}
                      }}}
                    }
                """;

        OuroRestApiSpec scannedSpec = mapper.readValue(scannedJson, OuroRestApiSpec.class);
        OuroRestApiSpec fileSpec = mapper.readValue(fileJson, OuroRestApiSpec.class);

        Operation scannedOperation = scannedSpec.getPaths()
                .get("/e2")
                .getGet();
        Operation fileOperation = fileSpec.getPaths()
                .get("/e2")
                .getGet();

        responseComparator.compareResponsesForMethod("/e2", HttpMethod.GET, scannedOperation, fileOperation, new HashMap<>());

        assertEquals("both", fileOperation.getXOuroborosDiff());
        assertEquals("mock", fileOperation.getXOuroborosProgress());
    }
}
