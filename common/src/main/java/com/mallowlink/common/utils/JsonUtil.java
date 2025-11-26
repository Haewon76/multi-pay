package com.mallowlink.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mallowlink.common.dto.ToLongSerializer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class JsonUtil {

    private final ObjectMapper mapper;

    //  리스트 형식의 경우 TypeReference를 사용
    public <T> T fromJson(String jsonStr, TypeReference<T> typeReference) {
        T result = null;
        try {
            if (jsonStr == null) {
                log.error("fromJson():argument \"content\" is null");
                return null;
            }
            result = mapper.readValue(jsonStr, typeReference);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    //  일반 클래스 형식의 경우 Class를 사용하도록 처리
    public <T> T fromJson(String jsonStr, Class<T> clazz) {
        T result = null;
        try {
            if (jsonStr == null) {
                log.error("fromJson():argument \"content\" is null");
                return null;
            }
            result = mapper.readValue(jsonStr, clazz);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    public String toJson(Object vo) {

        String json = "";

        try {
            json = mapper.writeValueAsString(vo);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return json;
    }

    public String toJsonPretty(Object vo) {

        String json = "";

        try {
            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(vo);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return json;
    }

    public String toJsonWithNumber(Object vo) {
        String json = "";

        try {
            // 객체를 JSON 트리로 변환
            JsonNode rootNode = mapper.valueToTree(vo);

            // `@JsonSerialize(using = ToLongSerializer.class)`가 적용된 필드 목록 가져오기
            Set<String> numericFields = getAnnotatedFields(vo.getClass());

            // 특정 필드만 숫자로 변환
            convertFieldsToNumber((ObjectNode) rootNode, numericFields);

            json = mapper.writeValueAsString(rootNode);
        } catch (IOException e) {
            log.error("JSON 변환 오류: {}", e.getMessage(), e);
        }

        return json;
    }

    private Set<String> getAnnotatedFields(Class<?> clazz) {
        Set<String> fields = new HashSet<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(JsonSerialize.class)) {
                JsonSerialize annotation = field.getAnnotation(JsonSerialize.class);
                if (annotation.using() == ToLongSerializer.class) { // ✅ 특정 Serializer만 필터링
                    fields.add(field.getName());
                }
            }
        }
        return fields;
    }

    private static void convertFieldsToNumber(ObjectNode node, Set<String> numericFields) {
        for (String field : numericFields) {
            if (node.has(field) && node.get(field).isTextual()) { // ✅ 필드가 문자열일 경우만 변환
                try {
                    String value = node.get(field).asText();
                    if (value.matches("\\d+")) { // ✅ 숫자로만 이루어진 경우만 변환
                        node.put(field, new java.math.BigInteger(value)); // ✅ 숫자로 변환하여 JSON에 적용
                    }
                } catch (NumberFormatException e) {
                    log.warn("숫자로 변환 실패: {}", node.get(field).asText());
                }
            }
        }
    }

    /**
     * JSON Body를 읽어온다
     *
     * @param request
     * @return
     */
    public static String getRequestJsonBody(HttpServletRequest request) {
        try {
            // JSON 인 경우만 Body를 읽도록 필터링
            if (MediaType.APPLICATION_JSON_VALUE.equalsIgnoreCase(request.getContentType())) {
                return request.getReader()
                        .lines()
                        .map(String::trim)
                        .collect(Collectors.joining());
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return null;
    }

}
