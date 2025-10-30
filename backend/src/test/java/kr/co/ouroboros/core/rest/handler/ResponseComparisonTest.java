package kr.co.ouroboros.core.rest.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

//@SpringBootTest
public class ResponseComparisonTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private ResponseComparator responseComparator;

    @BeforeEach
    public void setUp() {
        responseComparator = new ResponseComparator();
    }

    @Test
    public void testResponseComparisonWithMatchingSchemas() throws Exception {
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

        Operation scannedOperation = scannedSpec.getPaths().get("/api/test/users/{id}").getPut();
        Operation fileOperation = fileSpec.getPaths().get("/api/test/users/{id}").getPut();

        // 응답 비교 실행
        responseComparator.compareResponsesForMethod("PUT", scannedOperation, fileOperation, "/api/test/users/{id}", schemaMatchResults);

        // 검증: 200 응답은 일치하므로 xOuroborosDiff가 설정되지 않아야 함
        assertNull(fileOperation.getXOuroborosDiff(), "일치하는 응답에 대해서는 xOuroborosDiff가 설정되지 않아야 합니다.");
        
        // 검증: 400 응답이 파일 스펙에 추가되었는지 확인
        assertNotNull(fileOperation.getResponses().get("400"), "누락된 400 응답이 파일 스펙에 추가되어야 합니다.");
        assertNull(fileOperation.getXOuroborosDiff(), "누락된 응답이 추가되면 xOuroborosDiff가 설정되지 않아야 합니다.");
    }

    @Test
    public void testResponseComparisonWithMismatchingSchemas() throws Exception {
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

        Operation scannedOperation = scannedSpec.getPaths().get("/api/test/users").getGet();
        Operation fileOperation = fileSpec.getPaths().get("/api/test/users").getGet();

        // 응답 비교 실행
        responseComparator.compareResponsesForMethod("GET", scannedOperation, fileOperation, "/api/test/users", schemaMatchResults);

        // 검증: $ref가 다르므로 불일치로 처리되어야 함
        assertEquals("response", fileOperation.getXOuroborosDiff(), "$ref가 다르면 xOuroborosDiff가 'response'로 설정되어야 합니다.");
    }

    @Test
    public void testResponseComparisonWithTypeMismatch() throws Exception {
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

        Operation scannedOperation = scannedSpec.getPaths().get("/api/test/response").getGet();
        Operation fileOperation = fileSpec.getPaths().get("/api/test/response").getGet();

        // 응답 비교 실행
        responseComparator.compareResponsesForMethod("GET", scannedOperation, fileOperation, "/api/test/response", schemaMatchResults);

        // 검증: type이 다르므로 불일치로 처리되어야 함
        assertEquals("response", fileOperation.getXOuroborosDiff(), "type이 다르면 xOuroborosDiff가 'response'로 설정되어야 합니다.");
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

        Operation scannedOperation = scannedSpec.getPaths().get("/api/test/wildcard").getGet();
        Operation fileOperation = fileSpec.getPaths().get("/api/test/wildcard").getGet();

        // 응답 비교 실행
        responseComparator.compareResponsesForMethod("GET", scannedOperation, fileOperation, "/api/test/wildcard", schemaMatchResults);

        // 검증: */*는 모든 content-type과 일치하므로 일치로 처리되어야 함
        assertNull(fileOperation.getXOuroborosDiff(), "*/* content-type은 모든 타입과 일치하므로 xOuroborosDiff가 설정되지 않아야 합니다.");
    }

    @Test
    public void testResponseComparisonWithNullOperations() throws Exception {
        Map<String, Boolean> schemaMatchResults = new HashMap<>();

        // null Operation 테스트
        responseComparator.compareResponsesForMethod("GET", null, null, "/api/test/null", schemaMatchResults);
        // 예외가 발생하지 않아야 함

        // 하나만 null인 경우 테스트
        Operation validOperation = new Operation();
        responseComparator.compareResponsesForMethod("GET", validOperation, null, "/api/test/null", schemaMatchResults);
        // 예외가 발생하지 않아야 함
    }
}
