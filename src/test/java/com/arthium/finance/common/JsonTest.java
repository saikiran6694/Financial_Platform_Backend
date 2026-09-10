package com.arthium.finance.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonTest {

    private Json json;

    @BeforeEach
    void setUp() {
        json = new Json(new ObjectMapper());
    }

    @Test
    void write_serialisesMapToJsonString() {
        String result = json.write(Map.of("key", "value"));

        assertThat(result).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void readMap_validJsonObject_returnsMap() {
        Map<String, Object> result = json.readMap("{\"amount\":12.5,\"title\":\"Coffee\"}");

        assertThat(result).containsEntry("title", "Coffee");
        assertThat(result).containsEntry("amount", 12.5);
    }

    @Test
    void readMap_jsonArray_returnsNull() {
        assertThat(json.readMap("[1,2,3]")).isNull();
    }

    @Test
    void readMap_malformedJson_returnsNull() {
        assertThat(json.readMap("not json")).isNull();
    }

    @Test
    void readList_validJsonArray_returnsList() {
        List<Object> result = json.readList("[1,2,3]");

        assertThat(result).containsExactly(1, 2, 3);
    }

    @Test
    void readList_jsonObject_returnsNull() {
        assertThat(json.readList("{\"a\":1}")).isNull();
    }

    @Test
    void readList_malformedJson_returnsNull() {
        assertThat(json.readList("not json")).isNull();
    }

    @Test
    void stripCodeFences_removesJsonFenceMarkers() {
        String raw = "```json\n{\"a\":1}\n```";

        assertThat(Json.stripCodeFences(raw)).isEqualTo("{\"a\":1}");
    }

    @Test
    void stripCodeFences_removesPlainFenceMarkers() {
        String raw = "```\n{\"a\":1}\n```";

        assertThat(Json.stripCodeFences(raw)).isEqualTo("{\"a\":1}");
    }

    @Test
    void stripCodeFences_noFences_trimsOnly() {
        assertThat(Json.stripCodeFences("  {\"a\":1}  ")).isEqualTo("{\"a\":1}");
    }

    @Test
    void stripCodeFences_nullInput_returnsEmptyString() {
        assertThat(Json.stripCodeFences(null)).isEqualTo("");
    }

    @Test
    void asMap_mapValue_returnsCastMap() {
        Object value = Map.of("a", 1);

        assertThat(Json.asMap(value)).isEqualTo(Map.of("a", 1));
    }

    @Test
    void asMap_nonMapValue_returnsNull() {
        assertThat(Json.asMap("not a map")).isNull();
    }

    @Test
    void asList_listValue_returnsCastList() {
        Object value = List.of(1, 2);

        assertThat(Json.asList(value)).isEqualTo(List.of(1, 2));
    }

    @Test
    void asList_nonListValue_returnsNull() {
        assertThat(Json.asList("not a list")).isNull();
    }

    @Test
    void get_nullSource_returnsNull() {
        assertThat(Json.get(null, "key")).isNull();
    }

    @Test
    void get_missingKey_returnsNull() {
        assertThat(Json.get(Map.of("a", 1), "b")).isNull();
    }

    @Test
    void str_numericValue_returnsStringRepresentation() {
        assertThat(Json.str(Map.of("amount", 5), "amount")).isEqualTo("5");
    }

    @Test
    void str_nullSource_returnsNull() {
        assertThat(Json.str(null, "amount")).isNull();
    }

    @Test
    void dbl_numberValue_returnsDouble() {
        assertThat(Json.dbl(Map.of("amount", 5), "amount")).isEqualTo(5.0);
    }

    @Test
    void dbl_numericStringValue_parsesToDouble() {
        assertThat(Json.dbl(Map.of("amount", "12.5"), "amount")).isEqualTo(12.5);
    }

    @Test
    void dbl_blankStringValue_returnsNull() {
        assertThat(Json.dbl(Map.of("amount", ""), "amount")).isNull();
    }

    @Test
    void dbl_nonParsableStringValue_returnsNull() {
        assertThat(Json.dbl(Map.of("amount", "abc"), "amount")).isNull();
    }

    @Test
    void dbl_missingKey_returnsNull() {
        assertThat(Json.dbl(Map.of(), "amount")).isNull();
    }
}
