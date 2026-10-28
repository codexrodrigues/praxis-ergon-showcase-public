package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;

import br.com.praxis.ergonshowcase.config.OptionSourcesConfig;
import br.com.praxis.ergonshowcase.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.authoring.*;
import org.praxisplatform.uischema.extension.CustomOpenApiResolver;
import org.praxisplatform.uischema.options.diagnostics.*;
import org.praxisplatform.uischema.validation.PraxisExpressionStaticValidator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

/** Gates nativos do JAR: executáveis mesmo sem rede, sem substituir a prova HTTP/Neon. */
class PraxisMetadataCompletenessTest {
  private List<Class<?>> dtos() throws Exception {
    var scanner = new ClassPathScanningCandidateComponentProvider(false) {
      @Override protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition bean) {
        return bean.getMetadata().isIndependent() && !bean.getMetadata().isInterface();
      }
    };
    scanner.addIncludeFilter(new RegexPatternTypeFilter(java.util.regex.Pattern.compile(".*")));
    var classes = new ArrayList<Class<?>>();
    for (var bean : scanner.findCandidateComponents("br.com.praxis.ergonshowcase.dto"))
      classes.add(Class.forName(bean.getBeanClassName()));
    assertThat(classes).hasSizeGreaterThanOrEqualTo(43)
        .contains(CargoDados.class, OrgaoDados.class, ServidorPessoaDados.class, VinculoDados.class,
            CreateCargoDTO.class, UpdateCargoDTO.class, CargoFilterDTO.class,
            CreateOrgaoDTO.class, UpdateOrgaoDTO.class, OrgaoFilterDTO.class,
            CreateServidorPessoaDTO.class, UpdateServidorPessoaDTO.class, ServidorPessoaFilterDTO.class,
            CreateVinculoDTO.class, UpdateVinculoDTO.class, VinculoFilterDTO.class)
        .contains(CargoResponseDTO.class, ServidorPessoaResponseDTO.class, VinculoResponseDTO.class,
            OrgaoResponseDTO.class, DocumentoLegalResponseDTO.class, SetorResponseDTO.class);
    return classes;
  }

  @Test
  void allDomainDtosPassSemanticReview() throws Exception {
    var reviewer = new SemanticMetadataReviewer();
    var errors = new ArrayList<SemanticMetadataReviewIssue>();
    for (var dto : dtos()) errors.addAll(reviewer.review(dto).issues().stream()
        .filter(i -> i.severity() == SemanticMetadataIssueSeverity.ERROR).toList());
    assertThat(errors).isEmpty();
  }

  @Test
  void actualResolvedMicrovisualizationsHaveValidExpressions() {
    var converters = new ModelConverters();
    converters.addConverter(new CustomOpenApiResolver(new ObjectMapper().findAndRegisterModules()));
    var validator = new PraxisExpressionStaticValidator();
    for (var dto : List.of(CargoResponseDTO.class, ServidorPessoaResponseDTO.class, VinculoResponseDTO.class)) {
      var resolved = converters.resolveAsResolvedSchema(new AnnotatedType(dto).resolveAsRef(false));
      var schema = resolved.schema;
      assertThat(schema.getProperties()).as(dto.getSimpleName()).isNotEmpty();
      assertThat(validator.validateSchemaExpressions(dto.getSimpleName(), schema, false)).isEmpty();
      var json = new ObjectMapper().valueToTree(schema);
      assertThat(json.toString()).as("resolver must actually emit microVisualization for " + dto)
          .contains("microVisualization", "thresholds", "fallbackText");
    }
    assertThat(validator.validateExpression("=campoInexistente + 1", Set.of("id"), "negative").valid()).isFalse();
  }

  @Test
  void expectedLookupInventoryIsPublishedAndRich() {
    var registry = new OptionSourcesConfig().options();
    // Inventário independente do registry: remover ou mover uma fonte deve falhar.
    OptionSourcePublicationInventory inventory = OptionSourcePublicationInventory.of(
        new OptionSourcePublicationCandidate("orgao", "/api/ergon/orgaos", "ergon"),
        new OptionSourcePublicationCandidate("cargo", "/api/ergon/cargos", "ergon"),
        new OptionSourcePublicationCandidate("pessoa", "/api/ergon/servidor-pessoas", "ergon"),
        new OptionSourcePublicationCandidate("vinculo", "/api/ergon/vinculos", "ergon"),
        new OptionSourcePublicationCandidate("setor", "/api/ergon/setores", "ergon"),
        new OptionSourcePublicationCandidate("documento", "/api/ergon/documentos-legais", "ergon"));
    var diagnostics = new OptionSourcePublicationDiagnostics(registry, List.of(inventory));
    assertThat(diagnostics.diagnose()).hasSize(6);
    assertThat(diagnostics.unpublishedOrMismatched()).isEmpty();
    for (var descriptor : registry.descriptors()) {
      var lookup = descriptor.entityLookup();
      assertThat(lookup.selectionPolicy().allowRetainInvalidExistingValue()).isTrue();
      assertThat(lookup.detail().presentation()).isEqualTo("drawer");
      assertThat(lookup.detail().previewSurfaceId()).isNotBlank();
      assertThat(lookup.display().preset()).isEqualTo("directory");
      assertThat(lookup.display().fields()).isNotEmpty();
      assertThat(lookup.filtering().sortOptions()).hasSize(2);
      assertThat(descriptor.dependsOn()).containsAll(descriptor.dependencyFilterMap().keySet());
    }
    var missing = new OptionSourcePublicationDiagnostics(org.praxisplatform.uischema.options.OptionSourceRegistry.empty(), List.of(inventory));
    assertThat(missing.unpublishedOrMismatched()).hasSize(6);
  }

  @Test
  void derivedIndicatorsPreserveUnknownAndDoNotInventValidity() {
    var cargo = new CargoResponseDTO();
    assertThat(cargo.getVagasOcupadas()).isNull();
    cargo.setVagasAutorizadas(10);
    assertThat(cargo.getVagasOcupadas()).isNull();
    cargo.setSaldoVagasDisponiveis(-2L);
    assertThat(cargo.getVagasOcupadas()).isEqualTo(12L);
    var pessoa = new ServidorPessoaResponseDTO();
    assertThat(pessoa.getCanaisContatoPreenchidos()).isZero();
    pessoa.setTelefone("   ");
    pessoa.setNumtelCelular("99999");
    pessoa.setEMail("texto ainda não validado");
    assertThat(pessoa.getCanaisContatoPreenchidos()).isEqualTo(2);
  }
}
