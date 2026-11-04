package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;

import br.com.praxis.ergonshowcase.dto.*;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class CargoDraftContractTest {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void draftNullabilityIsIndependentFromCreateRequirements(boolean openApi31) {
    var converters = new ModelConverters(openApi31);
    var resolver = new org.praxisplatform.uischema.extension.CustomOpenApiResolver(
        new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules());
    resolver.setOpenapi31(openApi31); // Mesmo dialeto aplicado pela auto-configuração canônica.
    converters.addConverter(resolver);
    io.swagger.v3.oas.models.media.Schema<?> draft = converters.resolveAsResolvedSchema(new AnnotatedType(CargoDuplicateDraftDTO.class)).schema;
    io.swagger.v3.oas.models.media.Schema<?> create = converters.resolveAsResolvedSchema(new AnnotatedType(CreateCargoDTO.class)).schema;
    assertThat(draft.getRequired()).isNullOrEmpty();
    assertThat(draft.getProperties()).hasSize(17)
        .doesNotContainKeys("version","id","dtExtincao","auditoria","cargoAglutinadorNomePrevisto");
    for (String field : List.of("cargo","empCodigo","controleVaga","porReferencia","eAglutinador",
        "nome","categoria","subcategoria","tipoCargo","cargoFuncao","escolaridade","cbo",
        "vagasAutorizadas","cargoAglutinador","dtInicioContrVaga","documentoLegalId")) {
      var property = draft.getProperties().get(field);
      if (openApi31) assertThat(property.getTypes()).as(field).contains("null");
      else assertThat(property.getNullable()).as(field).isTrue();
    }
    assertThat(create.getRequired()).contains("cargo","empCodigo").doesNotContain("controleVaga","porReferencia","eAglutinador");
    for (String field : List.of("tipoCargo","cbo","vagasAutorizadas","cargoAglutinador","dtInicioContrVaga","documentoLegalId")) {
      var property = create.getProperties().get(field);
      if (openApi31) assertThat(property.getTypes()).as(field).contains("null");
      else assertThat(property.getNullable()).as(field).isTrue();
    }
    for (String field : List.of("controleVaga","porReferencia","eAglutinador"))
      assertThat(create.getProperties().get(field).getDefault()).isNull();
  }

  @Test void omittedFlagsUseServerDefaultsAndExplicitChoicesArePreserved() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var create = new CreateCargoDTO();
      create.setCargo(1);
      create.setEmpCodigo(1);
      create.setNome("Cargo");
      create.setCategoria("GERAL");
      assertThat(factory.getValidator().validate(create)).isEmpty();

      var mapper = new br.com.praxis.ergonshowcase.mapper.CargoMapper();
      var cargo = mapper.newEntity(create);
      assertThat(cargo.getControleVaga()).isFalse();
      assertThat(cargo.getPorReferencia()).isFalse();
      assertThat(cargo.getEAglutinador()).isFalse();

      create.setControleVaga(true);
      assertThat(mapper.newEntity(create).getControleVaga()).isTrue();
    }
  }

  @Test void ordinaryCreateValidatesIdentityClassificationAndUntrustedPreview() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var input = new CreateCargoDTO();
      input.setCargo(1);input.setEmpCodigo(1);input.setNome("Cargo");input.setCategoria("GERAL");
      input.setControleVaga(false);input.setPorReferencia(false);input.setEAglutinador(false);
      input.setCargo(0);
      assertThat(factory.getValidator().validate(input)).extracting(v -> v.getPropertyPath().toString()).contains("cargo");
      input.setCargo(-1);
      assertThat(factory.getValidator().validate(input)).extracting(v -> v.getPropertyPath().toString()).contains("cargo");
      input.setCargo(1);input.setNome(" ");input.setCategoria("");input.setCargoFuncao("X");
      assertThat(factory.getValidator().validate(input)).extracting(v -> v.getPropertyPath().toString()).contains("nome","categoria","cargoFuncao");
      input.setNome("N".repeat(120));input.setCategoria("C".repeat(20));input.setCargoFuncao("F");
      assertThat(factory.getValidator().validate(input)).isEmpty();
      input.setNome("N".repeat(121));input.setCategoria("C".repeat(21));
      input.setCargoAglutinadorNomePrevisto("Não é entrada cadastral");
      assertThat(factory.getValidator().validate(input)).extracting(v -> v.getPropertyPath().toString())
          .contains("nome","categoria","cargoAglutinadorNomePrevisto");
    }
  }

  @Test void canonicalCustomFieldValidationRejectsReadOnlyInput() {
    var definition = org.mockito.Mockito.mock(org.praxisplatform.uischema.customfields.CustomFieldDescriptor.class);
    org.mockito.Mockito.when(definition.key()).thenReturn("readonly_fixture");
    org.mockito.Mockito.when(definition.readOnly()).thenReturn(true);
    var validator = new org.praxisplatform.uischema.customfields.CustomFieldValidator((resource,tenant,schema) -> List.of(definition));
    assertThatThrownBy(() -> validator.validateValues(java.util.Map.of("readonly_fixture",true),List.of(definition)))
        .isInstanceOf(org.praxisplatform.uischema.customfields.CustomFieldValidationException.class);
    // O provider real do host não publica readOnly=true: esta prova é do contrato, não de ACL do host.
  }
}
