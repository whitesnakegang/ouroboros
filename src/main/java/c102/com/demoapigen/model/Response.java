package c102.com.demoapigen.model;

import lombok.Data;
import java.util.List;

@Data
public class Response {
    private String type; // "object", "array", "string", etc.
    private List<Field> fields;
    private Field arrayItemType; // for array type responses
}
