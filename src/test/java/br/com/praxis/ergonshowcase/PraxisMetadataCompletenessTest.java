package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;

import br.com.praxis.ergonshowcase.config.OptionSourcesConfig;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.action.ActionDefinition;
import org.praxisplatform.uischema.action.ActionDefinitionRegistry;
import org.praxisplatform.uischema.authoring.*;
import org.praxisplatform.uischema.domain.DomainCatalogResponse;
import org.praxisplatform.uischema.domain.SemanticDomainCatalogService;
import org.praxisplatform.uischema.extension.CustomOpenApiResolver;
import org.praxisplatform.uischema.openapi.CanonicalOperationRef;
import org.praxisplatform.uischema.openapi.OpenApiDocumentService;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.diagnostics.*;
import org.praxisplatform.uischema.schema.CanonicalSchemaRef;
import org.praxisplatform.uischema.surface.RelatedResourceSurface;
import org.praxisplatform.uischema.surface.SurfaceDefinition;
import org.praxisplatform.uischema.surface.SurfaceDefinitionRegistry;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceResponseCardinality;
import org.praxisplatform.uischema.surface.SurfaceScope;
import org.praxisplatform.uischema.validation.PraxisExpressionStaticValidator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

/** Gates nativos do JAR: executáveis mesmo sem rede, sem substituir a prova HTTP/Neon. */
class PraxisMetadataCompletenessTest {
  @Test
  void slice7PublishesVisibleLookupColumnsAndFiltersTheOptionalRemunerationDate() {
    var mapper = new ObjectMapper().findAndRegisterModules();
    var converters = new ModelConverters();
    converters.addConverter(new CustomOpenApiResolver(mapper));
    var fieldsByResource = Map.of(
        VagaFuncaoResponseDTO.class, List.of("funcaoId", "setorId"),
        DesignacaoFuncaoResponseDTO.class, List.of("funcaoId", "setorId"),
        SubstituicaoFuncaoResponseDTO.class, List.of("designacaoId"),
        RadarChefiaSubstituicaoResponseDTO.class, List.of("setorId"));
    fieldsByResource.forEach((type, fields) -> {
      var schema = converters.resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(false)).schema;
      for (String field : fields) {
        var property = (io.swagger.v3.oas.models.media.Schema<?>) schema.getProperties().get(field);
        var ui = mapper.valueToTree(property.getExtensions().get("x-ui"));
        assertThat(ui.path("tableHidden").asBoolean()).as("%s.%s", type.getSimpleName(), field).isFalse();
        assertThat(ui.path("controlType").asText()).isEqualTo("entityLookup");
        assertThat(ui.path("endpoint").asText()).isNotBlank();
      }
    });
    var schema = converters.resolveAsResolvedSchema(
        new AnnotatedType(SubstituicaoFuncaoFilterDTO.class).resolveAsRef(false)).schema;
    assertThat(schema.getProperties().keySet())
        .contains("semInicioRemunerado", "comInicioRemunerado")
        .doesNotContain("semTermino", "comTermino");
  }

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
    assertThat(classes).hasSizeGreaterThanOrEqualTo(125)
        .contains(CargoDados.class, OrgaoDados.class, ServidorPessoaDados.class, VinculoDados.class, DependenteDados.class, AfastamentoDados.class, FrequenciaDados.class, DefinicaoCampoCustomizadoDados.class,
            ParametroContagemFeriasDados.class, PeriodoAquisitivoFeriasDados.class, FeriasGozoDados.class,
            CreateCargoDTO.class, UpdateCargoDTO.class, CargoFilterDTO.class,
            CreateOrgaoDTO.class, UpdateOrgaoDTO.class, OrgaoFilterDTO.class,
            CreateServidorPessoaDTO.class, UpdateServidorPessoaDTO.class, ServidorPessoaFilterDTO.class,
            CreateVinculoDTO.class, UpdateVinculoDTO.class, VinculoFilterDTO.class,
            CreateDependenteDTO.class, UpdateDependenteDTO.class, DependenteFilterDTO.class,
            TipoDependenteFilterDTO.class,
            TipoFrequenciaFilterDTO.class, CodigoFrequenciaFilterDTO.class,
            CreateAfastamentoDTO.class, UpdateAfastamentoDTO.class, AfastamentoFilterDTO.class,
            RegraFrequenciaEstatutariaFilterDTO.class,
            CreateFrequenciaDTO.class, UpdateFrequenciaDTO.class, FrequenciaFilterDTO.class,
            TabelaFilterDTO.class, ItemTabelaFilterDTO.class,
            CreateDefinicaoCampoCustomizadoDTO.class, UpdateDefinicaoCampoCustomizadoDTO.class, DefinicaoCampoCustomizadoFilterDTO.class,
            CreateParametroContagemFeriasDTO.class, UpdateParametroContagemFeriasDTO.class, ParametroContagemFeriasFilterDTO.class,
            CreatePeriodoAquisitivoFeriasDTO.class, UpdatePeriodoAquisitivoFeriasDTO.class, PeriodoAquisitivoFeriasFilterDTO.class,
            CreateFeriasGozoDTO.class, UpdateFeriasGozoDTO.class, FeriasGozoFilterDTO.class,
            RadarFeriasPrescricaoFilterDTO.class,
            CreateAverbacaoTempoServicoDTO.class, UpdateAverbacaoTempoServicoDTO.class, AverbacaoTempoServicoFilterDTO.class,
            CreatePeriodoAquisitivoLicencaEspecialDTO.class, UpdatePeriodoAquisitivoLicencaEspecialDTO.class, PeriodoAquisitivoLicencaEspecialFilterDTO.class,
            CreateLicencaEspecialGozoDTO.class, UpdateLicencaEspecialGozoDTO.class, LicencaEspecialGozoFilterDTO.class,
            CreateVendaLicencaEspecialDTO.class, UpdateVendaLicencaEspecialDTO.class, VendaLicencaEspecialFilterDTO.class,
            RadarTempoServicoLicencaEspecialFilterDTO.class)
        .contains(CargoResponseDTO.class, ServidorPessoaResponseDTO.class, VinculoResponseDTO.class,
            OrgaoResponseDTO.class, DocumentoLegalResponseDTO.class, SetorResponseDTO.class,
            DependenteResponseDTO.class, TipoDependenteResponseDTO.class,
            TipoFrequenciaResponseDTO.class, CodigoFrequenciaResponseDTO.class, AfastamentoResponseDTO.class,
            RegraFrequenciaEstatutariaResponseDTO.class, FrequenciaResponseDTO.class,
            TabelaResponseDTO.class, ItemTabelaResponseDTO.class, DefinicaoCampoCustomizadoResponseDTO.class,
            ParametroContagemFeriasResponseDTO.class, PeriodoAquisitivoFeriasResponseDTO.class, FeriasGozoResponseDTO.class,
            RadarFeriasPrescricaoResponseDTO.class,
            AverbacaoTempoServicoResponseDTO.class, PeriodoAquisitivoLicencaEspecialResponseDTO.class,
            LicencaEspecialGozoResponseDTO.class, VendaLicencaEspecialResponseDTO.class,
            RadarTempoServicoLicencaEspecialResponseDTO.class);
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
  void allControllerResourceFactsPassSemanticReview() throws Exception {
    var scanner = new ClassPathScanningCandidateComponentProvider(false) {
      @Override protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition bean) {
        return bean.getMetadata().isIndependent() && !bean.getMetadata().isInterface();
      }
    };
    scanner.addIncludeFilter(new RegexPatternTypeFilter(java.util.regex.Pattern.compile(".*Controller")));
    var reviewer = new SemanticMetadataReviewer();
    var errors = new ArrayList<SemanticMetadataReviewIssue>();
    var controllers = new ArrayList<Class<?>>();
    var sources = new br.com.praxis.ergonshowcase.config.OptionSourcesConfig().options().descriptors();
    for (var bean : scanner.findCandidateComponents("br.com.praxis.ergonshowcase.controller")) {
      var controller = Class.forName(bean.getBeanClassName());
      controllers.add(controller);
      errors.addAll(reviewer.reviewResourceFacts(controller).issues().stream()
          .filter(i -> i.severity() == SemanticMetadataIssueSeverity.ERROR).toList());
      var resource = controller.getAnnotation(org.praxisplatform.uischema.annotation.ApiResource.class);
      if (resource != null) {
        var ownedSources = sources.stream().filter(source ->
            java.util.Arrays.asList(resource.value()).contains(source.resourcePath())
                || java.util.Arrays.asList(resource.path()).contains(source.resourcePath())).toList();
        errors.addAll(reviewer.review(controller, ownedSources, null).issues().stream()
            .filter(i -> i.code().equals("option-source-ineligible")).toList());
      }
    }
    assertThat(controllers.stream().filter(c -> c.isAnnotationPresent(org.praxisplatform.uischema.annotation.ApiResource.class))).hasSize(36);
    assertThat(errors).isEmpty();
  }

  @Test
  void actualResolvedMicrovisualizationsHaveValidExpressions() {
    var converters = new ModelConverters();
    converters.addConverter(new CustomOpenApiResolver(new ObjectMapper().findAndRegisterModules()));
    var validator = new PraxisExpressionStaticValidator();
    for (var dto : List.of(CargoResponseDTO.class, ServidorPessoaResponseDTO.class,
        VinculoResponseDTO.class, DependenteResponseDTO.class, AfastamentoResponseDTO.class,
        RegraFrequenciaEstatutariaResponseDTO.class, FrequenciaResponseDTO.class,
        DefinicaoCampoCustomizadoResponseDTO.class, ParametroContagemFeriasResponseDTO.class,
        PeriodoAquisitivoFeriasResponseDTO.class, FeriasGozoResponseDTO.class,
        RadarFeriasPrescricaoResponseDTO.class,
        AverbacaoTempoServicoResponseDTO.class, PeriodoAquisitivoLicencaEspecialResponseDTO.class,
        LicencaEspecialGozoResponseDTO.class, VendaLicencaEspecialResponseDTO.class,
        RadarTempoServicoLicencaEspecialResponseDTO.class, SubstituicaoFuncaoResponseDTO.class,
        RadarChefiaSubstituicaoResponseDTO.class, FuncaoConfiancaResponseDTO.class, VagaFuncaoResponseDTO.class, SetorResponseDTO.class, HistoricoSetorResponseDTO.class, SetorParametrizacaoErgonResponseDTO.class, CargoSetorResponseDTO.class, SubempresaLocalidadeResponseDTO.class, GrupoSetorResponseDTO.class, RadarOrganogramaChefiaOcupacaoResponseDTO.class)) {
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
        new OptionSourcePublicationCandidate("documento", "/api/ergon/documentos-legais", "ergon"),
        new OptionSourcePublicationCandidate("tipoDependente", "/api/ergon/tipos-dependente", "ergon"),
        new OptionSourcePublicationCandidate("dependente", "/api/ergon/dependentes", "ergon"),
        new OptionSourcePublicationCandidate("tipoFrequencia", "/api/ergon/tipos-frequencia", "ergon"),
        new OptionSourcePublicationCandidate("codigoFrequencia", "/api/ergon/codigos-frequencia", "ergon"),
        new OptionSourcePublicationCandidate("afastamento", "/api/ergon/afastamentos", "ergon"),
        new OptionSourcePublicationCandidate("regraFrequenciaEstatutaria", "/api/ergon/regras-frequencia-estatutaria", "ergon"),
        new OptionSourcePublicationCandidate("frequencia", "/api/ergon/frequencias", "ergon"),
        new OptionSourcePublicationCandidate("tabela", "/api/ergon/tabelas", "ergon"),
        new OptionSourcePublicationCandidate("itemTabela", "/api/ergon/itens-tabela", "ergon"),
        new OptionSourcePublicationCandidate("campoCustomizado", "/api/ergon/campos-customizados", "ergon"),
        new OptionSourcePublicationCandidate("parametroContagemFerias", "/api/ergon/parametros-contagem-ferias", "ergon"),
        new OptionSourcePublicationCandidate("periodoAquisitivoFerias", "/api/ergon/periodos-aquisitivos-ferias", "ergon"),
        new OptionSourcePublicationCandidate("feriasGozo", "/api/ergon/ferias-gozo", "ergon"),
        new OptionSourcePublicationCandidate("averbacaoTempoServico", "/api/ergon/averbacoes-tempo-servico", "ergon"),
        new OptionSourcePublicationCandidate("periodoAquisitivoLicencaEspecial", "/api/ergon/periodos-aquisitivos-licenca-especial", "ergon"),
        new OptionSourcePublicationCandidate("licencaEspecialGozo", "/api/ergon/licencas-especiais-gozo", "ergon"),
        new OptionSourcePublicationCandidate("vendaLicencaEspecial", "/api/ergon/vendas-licenca-especial", "ergon"),
        new OptionSourcePublicationCandidate("funcao", "/api/ergon/funcoes", "ergon"),
        new OptionSourcePublicationCandidate("vagaFuncao", "/api/ergon/vagas-funcao", "ergon"),
        new OptionSourcePublicationCandidate("designacao", "/api/ergon/designacoes", "ergon"),
        new OptionSourcePublicationCandidate("substituicao", "/api/ergon/substituicoes", "ergon"),
        new OptionSourcePublicationCandidate("historicoSetor", "/api/ergon/historico-setores", "ergon"),
        new OptionSourcePublicationCandidate("setorErgon", "/api/ergon/setores-ergon", "ergon"),
        new OptionSourcePublicationCandidate("cargoSetor", "/api/ergon/cargos-por-setor", "ergon"),
        new OptionSourcePublicationCandidate("subempresaLocalidade", "/api/ergon/subempresas-localidades", "ergon"),
        new OptionSourcePublicationCandidate("grupoSetor", "/api/ergon/grupos-setores", "ergon"));
    var diagnostics = new OptionSourcePublicationDiagnostics(registry, List.of(inventory));
    assertThat(diagnostics.diagnose()).hasSize(32);
    assertThat(diagnostics.unpublishedOrMismatched()).isEmpty();
    assertThat(registry.descriptors()).filteredOn(source -> source.key().equals("vagaFuncao"))
        .singleElement().satisfies(source -> assertThat(source.dependencyFilterMap())
            .containsEntry("empCodigo", "empCodigo").containsEntry("funcaoId", "funcaoId"));
    assertThat(registry.descriptors()).filteredOn(source -> source.key().equals("substituicao"))
        .singleElement().satisfies(source -> assertThat(source.dependencyFilterMap())
            .containsEntry("empCodigo", "empCodigo").containsEntry("designacaoId", "designacaoId"));
    for (var descriptor : registry.descriptors()) {
      var lookup = descriptor.entityLookup();
      assertThat(lookup.selectionPolicy().allowRetainInvalidExistingValue()).isTrue();
      assertThat(lookup.detail().presentation()).isEqualTo("drawer");
      assertThat(lookup.detail().previewSurfaceId()).isNotBlank();
      assertThat(lookup.display().preset()).isEqualTo("directory");
      assertThat(lookup.display().fields()).isNotEmpty();
      assertThat(lookup.filtering().sortOptions()).extracting(org.praxisplatform.uischema.options.LookupSortOption::key)
          .contains("code", "description");
      assertThat(lookup.filtering().defaultSort()).isEqualTo("description");
      assertThat(descriptor.dependsOn()).containsAll(descriptor.dependencyFilterMap().keySet());
    }
    var missing = new OptionSourcePublicationDiagnostics(org.praxisplatform.uischema.options.OptionSourceRegistry.empty(), List.of(inventory));
    assertThat(missing.unpublishedOrMismatched()).hasSize(32);
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

  @Test
  void semanticDomainCatalogSynthesizesErgonKnowledgeGraphAndDataProductsAutomatically() {
    var mapper = new ObjectMapper().findAndRegisterModules();
    var converters = new ModelConverters();
    converters.addConverter(new CustomOpenApiResolver(mapper));

    var openApiMapper = io.swagger.v3.core.util.Json.mapper();
    var vinculoSchema = openApiMapper.valueToTree(
        converters.resolveAsResolvedSchema(new AnnotatedType(VinculoResponseDTO.class).resolveAsRef(false)).schema);
    var radarSchema = openApiMapper.valueToTree(
        converters.resolveAsResolvedSchema(new AnnotatedType(RadarFeriasPrescricaoResponseDTO.class).resolveAsRef(false)).schema);

    ActionDefinitionRegistry actions = new ActionDefinitionRegistry() {
      @Override public List<ActionDefinition> findByResourceKey(String resourceKey) { return List.of(); }
      @Override public List<ActionDefinition> findByGroup(String group) { return List.of(); }
    };
    SurfaceDefinitionRegistry surfaces = new SurfaceDefinitionRegistry() {
      @Override public List<SurfaceDefinition> findByResourceKey(String resourceKey) { return List.of(); }
      @Override public List<SurfaceDefinition> findByGroup(String group) {
        return List.of(
            new SurfaceDefinition(
                "detail", "administracao-pessoal.vinculos", "/api/ergon/vinculos", "ergon",
                SurfaceKind.READ_PROJECTION, SurfaceScope.ITEM, "Visão 360° do Vínculo",
                "Inspeção 360° de vínculo funcional.", null, "response",
                SurfaceResponseCardinality.VOID,
                new CanonicalOperationRef("ergon", "findVinculoById", "/api/ergon/vinculos/{id}", "GET"),
                new CanonicalSchemaRef("VinculoResponseDTO", "response", "/schemas/filtered?schemaType=response"),
                10, List.of(), List.of(), List.of("vinculo"),
                new RelatedResourceSurface("administracao-pessoal.vinculos", "id", "administracao-pessoal.periodos-aquisitivos-ferias", "/api/ergon/periodos-aquisitivos-ferias", "vinculoId", false, "id", List.of())),
            new SurfaceDefinition(
                "detail", "administracao-pessoal.radar-ferias-prescricao", "/api/ergon/radar-ferias-prescricao", "ergon",
                SurfaceKind.READ_PROJECTION, SurfaceScope.ITEM, "Inspeção 360° de Prescrição de Férias",
                "Radar analítico de prescrição de férias.", null, "response",
                SurfaceResponseCardinality.VOID,
                new CanonicalOperationRef("ergon", "findRadarFeriasById", "/api/ergon/radar-ferias-prescricao/{id}", "GET"),
                new CanonicalSchemaRef("RadarFeriasPrescricaoResponseDTO", "response", "/schemas/filtered?schemaType=response"),
                10, List.of(), List.of(), List.of("radar", "ferias")));
      }
    };
    OpenApiDocumentService openApi = new OpenApiDocumentService() {
      @Override public String resolveGroupFromPath(String path) { return "ergon"; }
      @Override public JsonNode getDocumentForGroup(String groupName) {
        return mapper.valueToTree(Map.of(
            "paths", Map.of(
                "/api/ergon/vinculos", Map.of(
                    "get", Map.of("responses", Map.of("200", Map.of())),
                    "post", Map.of("responses", Map.of("201", Map.of()))),
                "/api/ergon/radar-ferias-prescricao", Map.of(
                    "get", Map.of("responses", Map.of("200", Map.of()))),
                "/api/ergon/radar-ferias-prescricao/stats/group-by", Map.of(
                    "post", Map.of(
                        "operationId", "radarFeriasGroupBy",
                        "responses", Map.of("200", Map.of())))),
            "components", Map.of(
                "schemas", Map.of(
                    "VinculoResponseDTO", vinculoSchema,
                    "RadarFeriasPrescricaoResponseDTO", radarSchema))));
      }
      @Override public String getOrComputeSchemaHash(String schemaId, Supplier<JsonNode> payloadSupplier) {
        return "hash:" + schemaId;
      }
      @Override public void clearCaches() {}
    };

    var service = new SemanticDomainCatalogService(
        actions, surfaces, OptionSourceRegistry.empty(), openApi,
        Clock.fixed(Instant.parse("2026-04-06T12:00:00Z"), ZoneOffset.UTC),
        "praxis-ergon-showcase", "Ergon Showcase", "0.1.0");

    DomainCatalogResponse catalog = service.findByGroup("ergon");

    var radarNode = catalog.nodes().stream()
        .filter(n -> "administracao-pessoal.radar-ferias-prescricao".equals(n.nodeKey()))
        .findFirst().orElseThrow();
    assertThat(radarNode.metadata())
        .containsEntry("semanticRole", "analytical_data_product")
        .containsEntry("dataProduct", true);
    @SuppressWarnings("unchecked")
    var analyzes = (List<String>) radarNode.metadata().get("analyzesConcepts");
    assertThat(analyzes)
        .contains(
            "administracao-pessoal.vinculos",
            "administracao-pessoal.periodos-aquisitivos-ferias",
            "administracao-pessoal.funcionarios",
            "administracao-pessoal.setores",
            "administracao-pessoal.cargos",
            "administracao-pessoal.empresas")
        .doesNotContain("administracao-pessoal.radar-ferias-prescricao");

    var vinculoNode = catalog.nodes().stream()
        .filter(n -> "administracao-pessoal.vinculos".equals(n.nodeKey()))
        .findFirst().orElseThrow();
    assertThat(vinculoNode.metadata())
        .containsEntry("semanticRole", "operational_resource")
        .containsEntry("childConcepts", List.of("administracao-pessoal.periodos-aquisitivos-ferias"))
        .containsEntry("analyzedByDataProducts", List.of("administracao-pessoal.radar-ferias-prescricao"));
    @SuppressWarnings("unchecked")
    var referenced = (List<String>) vinculoNode.metadata().get("referencedConcepts");
    assertThat(referenced).contains(
        "administracao-pessoal.funcionarios",
        "administracao-pessoal.cargos",
        "administracao-pessoal.setores",
        "administracao-pessoal.empresas",
        "administracao-pessoal.periodos-aquisitivos-ferias");

    var feriasNode = catalog.nodes().stream()
        .filter(n -> "administracao-pessoal.periodos-aquisitivos-ferias".equals(n.nodeKey()))
        .findFirst().orElseThrow();
    assertThat(feriasNode.metadata())
        .containsEntry("parentConcepts", List.of("administracao-pessoal.vinculos"))
        .containsEntry("analyzedByDataProducts", List.of("administracao-pessoal.radar-ferias-prescricao"));
  }

  @Test
  void synthesizesMultiRadarDataProductsAnalyticsProjectionsAndOptionSourceReferencesAcrossDomains() {
    var mapper = new ObjectMapper().findAndRegisterModules();
    var converters = new ModelConverters();
    converters.addConverter(new CustomOpenApiResolver(mapper));
    var openApiMapper = io.swagger.v3.core.util.Json.mapper();

    var radarChefiaSchema = openApiMapper.valueToTree(
        converters.resolveAsResolvedSchema(new AnnotatedType(RadarChefiaSubstituicaoResponseDTO.class).resolveAsRef(false)).schema);
    var radarTempoSchema = openApiMapper.valueToTree(
        converters.resolveAsResolvedSchema(new AnnotatedType(RadarTempoServicoLicencaEspecialResponseDTO.class).resolveAsRef(false)).schema);

    ActionDefinitionRegistry actions = new ActionDefinitionRegistry() {
      @Override public List<ActionDefinition> findByResourceKey(String resourceKey) { return List.of(); }
      @Override public List<ActionDefinition> findByGroup(String group) { return List.of(); }
    };
    SurfaceDefinitionRegistry surfaces = new SurfaceDefinitionRegistry() {
      @Override public List<SurfaceDefinition> findByResourceKey(String resourceKey) { return List.of(); }
      @Override public List<SurfaceDefinition> findByGroup(String group) {
        return List.of(
            new SurfaceDefinition(
                "detail", ApiPaths.KEY_RADAR_CHEFIAS_SUBSTITUICOES, ApiPaths.RADAR_CHEFIAS_SUBSTITUICOES, "ergon",
                SurfaceKind.READ_PROJECTION, SurfaceScope.ITEM, "Radar de chefias",
                "Posição de cobertura da chefia na data de referência.", null, "response",
                SurfaceResponseCardinality.VOID,
                new CanonicalOperationRef("ergon", "detalharRadarChefiaSubstituicao", ApiPaths.RADAR_CHEFIAS_SUBSTITUICOES + "/{id}/detail", "GET"),
                new CanonicalSchemaRef("RadarChefiaSubstituicaoResponseDTO", "response", "/schemas/filtered?schemaType=response"),
                10, List.of(), List.of(), List.of("radar", "chefias")),
            new SurfaceDefinition(
                "detail", "administracao-pessoal.radar-tempo-servico-licenca-especial", ApiPaths.RADAR_TEMPO_SERVICO_LICENCA_ESPECIAL, "ergon",
                SurfaceKind.READ_PROJECTION, SurfaceScope.ITEM, "Radar de tempo e licença",
                "Saldo, tempo reconhecido, programação e prescrição do ciclo monitorado.", null, "response",
                SurfaceResponseCardinality.VOID,
                new CanonicalOperationRef("ergon", "detalharRadarTempoServico", ApiPaths.RADAR_TEMPO_SERVICO_LICENCA_ESPECIAL + "/{id}/detail", "GET"),
                new CanonicalSchemaRef("RadarTempoServicoLicencaEspecialResponseDTO", "response", "/schemas/filtered?schemaType=response"),
                10, List.of(), List.of(), List.of("radar", "licenca")));
      }
    };
    OpenApiDocumentService openApi = new OpenApiDocumentService() {
      @Override public String resolveGroupFromPath(String path) { return "ergon"; }
      @Override public JsonNode getDocumentForGroup(String groupName) {
        return mapper.valueToTree(Map.of(
            "paths", Map.of(
                ApiPaths.RADAR_CHEFIAS_SUBSTITUICOES, Map.of(
                    "get", Map.of("responses", Map.of("200", Map.of()))),
                ApiPaths.RADAR_CHEFIAS_SUBSTITUICOES + "/stats/group-by", Map.of(
                    "post", Map.of(
                        "operationId", "statsRadarChefiaSubstituicao",
                        "x-ui", Map.of(
                            "analytics", Map.of(
                                "projections", List.of(
                                    Map.of(
                                        "id", "ranking-pendencias-folha",
                                        "label", "Ranking de Pendências de Folha",
                                        "intent", "ranking",
                                        "source", Map.of("kind", "praxis.stats", "operation", "group-by", "path", ApiPaths.RADAR_CHEFIAS_SUBSTITUICOES + "/stats/group-by"),
                                        "semantics", Map.of("primaryDimension", Map.of("field", "id"), "primaryMetrics", List.of(Map.of("field", "pendenciasFolha", "aggregation", "sum"))),
                                        "interactions", Map.of("recordOpen", Map.of("target", Map.of("resourceKey", ApiPaths.KEY_RADAR_CHEFIAS_SUBSTITUICOES, "surfaceId", "detail"))))))),
                        "responses", Map.of("200", Map.of()))),
                ApiPaths.RADAR_TEMPO_SERVICO_LICENCA_ESPECIAL, Map.of(
                    "get", Map.of("responses", Map.of("200", Map.of()))),
                ApiPaths.RADAR_TEMPO_SERVICO_LICENCA_ESPECIAL + "/stats/group-by", Map.of(
                    "post", Map.of(
                        "operationId", "statsRadarTempoServicoLicencaEspecial",
                        "responses", Map.of("200", Map.of())))),
            "components", Map.of(
                "schemas", Map.of(
                    "RadarChefiaSubstituicaoResponseDTO", radarChefiaSchema,
                    "RadarTempoServicoLicencaEspecialResponseDTO", radarTempoSchema))));
      }
      @Override public String getOrComputeSchemaHash(String schemaId, Supplier<JsonNode> payloadSupplier) {
        return "hash:" + schemaId;
      }
      @Override public void clearCaches() {}
    };

    var service = new SemanticDomainCatalogService(
        actions, surfaces, new OptionSourcesConfig().options(), openApi,
        Clock.fixed(Instant.parse("2026-04-06T12:00:00Z"), ZoneOffset.UTC),
        "praxis-ergon-showcase", "Ergon Showcase", "0.1.0");

    DomainCatalogResponse catalog = service.findByGroup("ergon");

    var radarChefiaNode = catalog.nodes().stream()
        .filter(n -> ApiPaths.KEY_RADAR_CHEFIAS_SUBSTITUICOES.equals(n.nodeKey()))
        .findFirst().orElseThrow();
    assertThat(radarChefiaNode.metadata())
        .containsEntry("semanticRole", "analytical_data_product")
        .containsEntry("dataProduct", true);
    @SuppressWarnings("unchecked")
    var chefiaAnalyzes = (List<String>) radarChefiaNode.metadata().get("analyzesConcepts");
    assertThat(chefiaAnalyzes).contains(
        "administracao-pessoal.designacoes",
        "administracao-pessoal.substituicoes",
        "administracao-pessoal.vagas-funcao",
        "administracao-pessoal.funcoes",
        "administracao-pessoal.setores");

    var statsProjectionNode = catalog.nodes().stream()
        .filter(n -> (ApiPaths.KEY_RADAR_CHEFIAS_SUBSTITUICOES + ".stats.projection.ranking-pendencias-folha").equals(n.nodeKey()))
        .findFirst().orElseThrow();
    assertThat(statsProjectionNode.nodeType()).isEqualTo("stats");
    assertThat(statsProjectionNode.metadata())
        .containsEntry("statsKind", "analytics_projection")
        .containsEntry("projectionId", "ranking-pendencias-folha");

    var radarTempoNode = catalog.nodes().stream()
        .filter(n -> "administracao-pessoal.radar-tempo-servico-licenca-especial".equals(n.nodeKey()))
        .findFirst().orElseThrow();
    assertThat(radarTempoNode.metadata())
        .containsEntry("semanticRole", "analytical_data_product")
        .containsEntry("dataProduct", true);
    @SuppressWarnings("unchecked")
    var tempoAnalyzes = (List<String>) radarTempoNode.metadata().get("analyzesConcepts");
    assertThat(tempoAnalyzes).contains(
        "administracao-pessoal.periodos-aquisitivos-licenca-especial",
        "administracao-pessoal.vinculos");
  }
}
