package br.com.praxis.ergonshowcase;

import br.com.praxis.ergonshowcase.config.CamposCustomizadosProvider;
import br.com.praxis.ergonshowcase.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.extension.CustomOpenApiResolver;
import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the actual schema resolver, rather than only reading the Java annotations. */
class P4MetadataProjectionTest {
  @Test void titleInspectionUsesOnlyPersistedResponseIdentityAndRetainsEditorialMetadata() {
    var json = new ObjectMapper().findAndRegisterModules();
    var converters = new ModelConverters();
    converters.addConverter(new CustomOpenApiResolver(json));
    for (var pair : List.of(
        Map.entry(CargoResponseDTO.class, CreateCargoDTO.class),
        Map.entry(OrgaoResponseDTO.class, CreateOrgaoDTO.class),
        Map.entry(ServidorPessoaResponseDTO.class, CreateServidorPessoaDTO.class),
        Map.entry(DependenteResponseDTO.class, CreateDependenteDTO.class))) {
      var response = converters.resolveAsResolvedSchema(new AnnotatedType(pair.getKey()).resolveAsRef(false)).schema;
      var create = converters.resolveAsResolvedSchema(new AnnotatedType(pair.getValue()).resolveAsRef(false)).schema;
      var responseTitle = io.swagger.v3.core.util.Json.mapper().valueToTree(response.getProperties().get("nome"));
      var createTitle = io.swagger.v3.core.util.Json.mapper().valueToTree(create.getProperties().get("nome"));
      assertThat(responseTitle.at("/x-ui/resourceReference/valueField").asText())
          .as("%s title schema: %s", pair.getKey().getSimpleName(), responseTitle).isEqualTo("id");
      assertThat(responseTitle.at("/x-ui/resourceReference/previewSurfaceId").asText()).isNotBlank();
      assertThat(responseTitle.at("/x-ui/label").asText()).isNotBlank();
      assertThat(responseTitle.path("description").asText()).isNotBlank();
      assertThat(createTitle.at("/x-ui/resourceReference").isMissingNode()).isTrue();
      assertThat(createTitle.at("/x-ui/label")).isEqualTo(responseTitle.at("/x-ui/label"));
      assertThat(createTitle.path("readOnly").asBoolean()).isFalse();
    }
  }

  @Test void persistedCustomFieldRulesPublishOneNativeExpressionForEveryAdmittedStorageShape() {
    var expression = Map.of("<=", List.of(Map.of("var", "value"), Map.of("var", "draft.jornadaSemanal")));
    for (var stored : List.of(expression, Map.of("jsonLogic", expression), Map.of("rule", expression))) {
      var projected = CamposCustomizadosProvider.jsonLogicRules((Map) stored);
      assertThat(projected.get("jsonLogic")).isEqualTo(expression);
      assertThat(projected.get("message")).asString().isNotBlank();
    }
    assertThat(CamposCustomizadosProvider.jsonLogicRules(Map.of("min", 1, "max", 44))).isEmpty();
  }
}
