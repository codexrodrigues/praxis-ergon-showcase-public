package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.praxis.ergonshowcase.dto.CargoDuplicateDraftDTO;
import br.com.praxis.ergonshowcase.dto.CreateCargoDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.praxisplatform.uischema.extension.CustomOpenApiResolver;

/** Prova o schema publicado; o formulário continua usando CREATE + valores do draft. */
class CargoDraftPresentationContractTest {
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  private Schema<?> schema(Class<?> dto, boolean openApi31) {
    var converters = new ModelConverters(openApi31);
    var resolver = new CustomOpenApiResolver(mapper);
    resolver.setOpenapi31(openApi31);
    converters.addConverter(resolver);
    return converters.resolveAsResolvedSchema(new AnnotatedType(dto)).schema;
  }

  private JsonNode ui(Schema<?> schema, String field) {
    return mapper.valueToTree(schema.getProperties().get(field).getExtensions().get("x-ui"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void draftReferencesPublishTheSameLookupSourcesAsCreate(boolean openApi31) {
    var draft = schema(CargoDuplicateDraftDTO.class, openApi31);
    var create = schema(CreateCargoDTO.class, openApi31);
    var expected = Map.of(
        "empCodigo", "/api/ergon/orgaos/option-sources/orgao/options/filter",
        "cargoAglutinador", "/api/ergon/cargos/option-sources/cargo/options/filter",
        "documentoLegalId", "/api/ergon/documentos-legais/option-sources/documento/options/filter");
    expected.forEach((field, endpoint) -> {
      var actual = ui(draft, field);
      assertThat(actual.path("controlType").asText()).as(field).isEqualTo("entityLookup");
      assertThat(actual.path("endpoint").asText()).as(field).isEqualTo(endpoint);
      assertThat(actual.path("valueField").asText()).isEqualTo("id");
      assertThat(actual.path("displayField").asText()).isEqualTo("label");
      for (String key : List.of("type", "controlType", "endpoint", "valueField", "displayField"))
        assertThat(actual.path(key)).as(field + "." + key).isEqualTo(ui(create, field).path(key));
    });
    assertThat(draft.getRequired()).isNullOrEmpty();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void draftFlagsPublishExplicitChoicesWithoutInventingAnAnswer(boolean openApi31) {
    var draft = schema(CargoDuplicateDraftDTO.class, openApi31);
    var create = schema(CreateCargoDTO.class, openApi31);
    for (String field : List.of("controleVaga", "porReferencia", "eAglutinador")) {
      var actual = ui(draft, field);
      assertThat(actual.path("controlType").asText()).as(field).isEqualTo("select");
      assertThat(actual.path("type").asText()).as(field).isEqualTo("boolean");
      assertThat(actual.path("options")).as(field).isEqualTo(ui(create, field).path("options"));
      assertThat(actual.path("options").size()).as(field).isEqualTo(2);
      assertThat(actual.path("options").get(0).path("value").isBoolean()).isTrue();
      assertThat(actual.path("options").get(0).path("value").booleanValue()).isTrue();
      assertThat(actual.path("options").get(1).path("value").isBoolean()).isTrue();
      assertThat(actual.path("options").get(1).path("value").booleanValue()).isFalse();
      var property = draft.getProperties().get(field);
      assertThat(property.getDefault()).as(field).isNull();
      if (openApi31) assertThat(property.getTypes()).as(field).contains("null");
      else assertThat(property.getNullable()).as(field).isTrue();
    }
    assertThat(create.getRequired()).contains("controleVaga", "porReferencia", "eAglutinador");
  }
}
