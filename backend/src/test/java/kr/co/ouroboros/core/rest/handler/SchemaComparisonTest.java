package kr.co.ouroboros.core.rest.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import org.junit.jupiter.api.Test;

//@SpringBootTest
public class SchemaComparisonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSchemaComparison() throws Exception {
        // 스캔된 스펙 (실제 제공된 JSON)
        String scannedJson = """
                {
                  "openapi": "3.1.0",
                  "info": {
                    "title": "OpenAPI definition",
                    "version": "v0"
                  },
                  "components": {
                    "schemas": {
                      "Address": {
                        "type": "object",
                        "properties": {
                          "roadname": {
                            "type": "string"
                          },
                          "dong": {
                            "type": "string"
                          },
                          "gu": {
                            "type": "string"
                          }
                        }
                      },
                      "User": {
                        "type": "object",
                        "properties": {
                          "name": {
                            "type": "string"
                          },
                          "age": {
                            "type": "integer",
                            "format": "int32"
                          },
                          "height": {
                            "type": "number",
                            "format": "double"
                          },
                          "address": {
                            "$ref": "#/components/schemas/Address"
                          }
                        }
                      }
                    }
                  }
                }
                """;

        // 파일 스펙 (일부 다른 내용)
        String fileJson = """
                {
                  "openapi": "3.1.0",
                  "info": {
                    "title": "OpenAPI definition",
                    "version": "v0"
                  },
                  "components": {
                    "schemas": {
                      "Address": {
                        "type": "object",
                        "properties": {
                          "roadname": {
                            "type": "string"
                          },
                          "dong": {
                            "type": "string"
                          },
                          "gu": {
                            "type": "string"
                          }
                        }
                      },
                      "User": {
                        "type": "object",
                        "properties": {
                          "name": {
                            "type": "string"
                          },
                          "age": {
                            "type": "integer",
                            "format": "int32"
                          },
                          "height": {
                            "type": "number",
                            "format": "double"
                          },
                          "address": {
                            "$ref": "#/components/schemas/Address"
                          }
                        }
                      },
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

        SchemaComparator schemaComparator = new SchemaComparator();
        Map<String, Boolean> results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        System.out.println("=== 스키마 비교 결과 ===");
        results.forEach((schemaName, isMatch) ->
                System.out.println(schemaName + ": " + (isMatch ? "일치" : "불일치"))
        );

        // 검증
        assertTrue(results.get("Address")); // Address는 일치해야 함
        assertTrue(results.get("User"));    // User는 일치해야 함
        assertFalse(results.get("Book"));   // Book은 스캔된 스펙에 없으므로 false
    }
}
