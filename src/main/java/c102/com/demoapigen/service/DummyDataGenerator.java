package c102.com.demoapigen.service;

import c102.com.demoapigen.model.Field;
import c102.com.demoapigen.model.Response;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.*;

@Slf4j
@Service
public class DummyDataGenerator {

    private final Faker faker;

    public DummyDataGenerator() {
        this.faker = new Faker();
    }

    public Object generateDummyData(Response response) {
        if (response == null) {
            return Collections.emptyMap();
        }

        String type = response.getType();
        if (type == null) {
            type = "object";
        }

        switch (type.toLowerCase()) {
            case "array":
                return generateArray(response.getArrayItemType());
            case "object":
                return generateObject(response.getFields());
            case "string":
                return faker.lorem().sentence();
            case "number":
                return faker.number().randomDouble(2, 0, 1000);
            case "boolean":
                return faker.bool().bool();
            default:
                return generateObject(response.getFields());
        }
    }

    private List<Object> generateArray(Field arrayItemType) {
        List<Object> array = new ArrayList<>();
        int arraySize = faker.number().numberBetween(3, 10);

        for (int i = 0; i < arraySize; i++) {
            if (arrayItemType == null) {
                array.add(faker.lorem().word());
            } else {
                array.add(generateFieldValue(arrayItemType));
            }
        }

        return array;
    }

    private Map<String, Object> generateObject(List<Field> fields) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (fields == null || fields.isEmpty()) {
            result.put("message", faker.lorem().sentence());
            return result;
        }

        for (Field field : fields) {
            result.put(field.getName(), generateFieldValue(field));
        }

        return result;
    }

    private Object generateFieldValue(Field field) {
        if (field == null) {
            return null;
        }

        // Check for default value first (fixed value for strings)
        if (field.getDefaultValue() != null && !field.getDefaultValue().isEmpty()) {
            return field.getDefaultValue();
        }

        String type = field.getType();
        if (type == null) {
            type = "string";
        }

        // Handle faker type first
        if ("faker".equalsIgnoreCase(type) && field.getFakerType() != null) {
            return generateFakerValue(field.getFakerType());
        }

        // Handle standard types
        switch (type.toLowerCase()) {
            case "string":
                // Auto-detect faker type based on field name
                return generateSmartStringValue(field.getName());
            case "number":
            case "integer":
                return faker.number().numberBetween(1, 1000);
            case "double":
            case "float":
                return faker.number().randomDouble(2, 0, 1000);
            case "boolean":
                return faker.bool().bool();
            case "array":
                return generateArray(field.getArrayItemType());
            case "object":
                return generateObject(field.getFields());
            default:
                return generateSmartStringValue(field.getName());
        }
    }

    private Object generateSmartStringValue(String fieldName) {
        if (fieldName == null) {
            return faker.lorem().word();
        }

        String lowerFieldName = fieldName.toLowerCase();

        // Name related
        if (lowerFieldName.contains("name") && !lowerFieldName.contains("user") && !lowerFieldName.contains("company")) {
            if (lowerFieldName.contains("first")) return faker.name().firstName();
            if (lowerFieldName.contains("last")) return faker.name().lastName();
            if (lowerFieldName.contains("full")) return faker.name().fullName();
            return faker.name().fullName();
        }

        // User related
        if (lowerFieldName.contains("username")) return faker.internet().username();
        if (lowerFieldName.contains("user") && lowerFieldName.contains("name")) return faker.internet().username();

        // Email
        if (lowerFieldName.contains("email")) return faker.internet().emailAddress();

        // Phone
        if (lowerFieldName.contains("phone")) return faker.phoneNumber().cellPhone();
        if (lowerFieldName.contains("mobile")) return faker.phoneNumber().cellPhone();
        if (lowerFieldName.contains("tel")) return faker.phoneNumber().phoneNumber();

        // Address related
        if (lowerFieldName.contains("address")) {
            if (lowerFieldName.contains("street")) return faker.address().streetAddress();
            return faker.address().fullAddress();
        }
        if (lowerFieldName.contains("street")) return faker.address().streetAddress();
        if (lowerFieldName.contains("city")) return faker.address().city();
        if (lowerFieldName.contains("state")) return faker.address().state();
        if (lowerFieldName.contains("country")) return faker.address().country();
        if (lowerFieldName.contains("zip")) return faker.address().zipCode();
        if (lowerFieldName.contains("postal")) return faker.address().zipCode();

        // Company related
        if (lowerFieldName.contains("company")) return faker.company().name();
        if (lowerFieldName.contains("industry")) return faker.company().industry();

        // Product/Commerce related
        if (lowerFieldName.contains("product")) return faker.commerce().productName();
        if (lowerFieldName.contains("price")) return faker.commerce().price();
        if (lowerFieldName.contains("department")) return faker.commerce().department();
        if (lowerFieldName.contains("category")) return faker.commerce().department();

        // Internet related
        if (lowerFieldName.contains("url")) return faker.internet().url();
        if (lowerFieldName.contains("website")) return faker.internet().url();
        if (lowerFieldName.contains("domain")) return faker.internet().domainName();
        if (lowerFieldName.contains("ip")) return faker.internet().ipV4Address();

        // Content related
        if (lowerFieldName.contains("title")) return faker.book().title();
        if (lowerFieldName.contains("description")) return faker.lorem().sentence();
        if (lowerFieldName.contains("comment")) return faker.lorem().sentence();
        if (lowerFieldName.contains("message")) return faker.lorem().sentence();
        if (lowerFieldName.contains("text")) return faker.lorem().paragraph();

        // ID related
        if (lowerFieldName.contains("uuid")) return faker.internet().uuid();

        // Date/Time
        if (lowerFieldName.contains("date")) return faker.date().birthday().toString();
        if (lowerFieldName.contains("time")) return faker.date().birthday().toString();

        // Default
        return faker.lorem().word();
    }

    private Object generateFakerValue(String fakerType) {
        try {
            String[] parts = fakerType.split("\\.");
            if (parts.length < 2) {
                log.warn("Invalid faker type format: {}. Expected format: 'category.method'", fakerType);
                return faker.lorem().word();
            }

            String category = parts[0];
            String method = parts[1];

            // Get the category object from Faker
            Method categoryMethod = Faker.class.getMethod(category);
            Object categoryObject = categoryMethod.invoke(faker);

            // Call the method on the category object
            Method valueMethod = categoryObject.getClass().getMethod(method);
            return valueMethod.invoke(categoryObject);

        } catch (Exception e) {
            log.warn("Failed to generate faker value for type: {}. Falling back to default.", fakerType, e);
            return faker.lorem().word();
        }
    }
}
