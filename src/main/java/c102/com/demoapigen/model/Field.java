package c102.com.demoapigen.model;

import lombok.Data;
import java.util.List;

@Data
public class Field {
    private String name;
    private String type; // "string", "number", "boolean", "object", "array", "faker", "file"
    private String fakerType; // e.g., "name.fullName", "address.city", "internet.email"
    private Boolean required; // 필수 여부 (true/false/null)
    private String defaultValue; // 기본값
    private List<Field> fields; // for nested objects
    private Field arrayItemType; // for array type fields
}
