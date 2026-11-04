package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.FeriasGozo;
import br.com.praxis.ergonshowcase.domain.Frequencia;
import br.com.praxis.ergonshowcase.domain.ParametroContagemFerias;
import br.com.praxis.ergonshowcase.domain.PeriodoAquisitivoFerias;
import br.com.praxis.ergonshowcase.rules.AfastamentoConcessaoRuleSet;
import br.com.praxis.ergonshowcase.rules.DependenteElegibilidadeRuleSet;
import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.rules.FeriasGozoRuleSet;
import br.com.praxis.ergonshowcase.rules.FrequenciaLancamentoRuleSet;
import br.com.praxis.ergonshowcase.rules.ParametroContagemFeriasRuleSet;
import br.com.praxis.ergonshowcase.rules.PeriodoAquisitivoFeriasRuleSet;
import br.com.praxis.ergonshowcase.rules.Rn013RegrasFrequenciaRuleSet;
import br.com.praxis.ergonshowcase.rules.VinculoProvimentoRuleSet;
import br.com.praxis.ergonshowcase.service.FeriasGozoService;
import br.com.praxis.ergonshowcase.service.FrequenciaService;
import br.com.praxis.ergonshowcase.service.ParametroContagemFeriasService;
import br.com.praxis.ergonshowcase.service.PeriodoAquisitivoFeriasService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.praxisplatform.rules.contract.DecisionBinding;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gate de contrato determinístico para garantir:
 * <ol>
 *   <li>Paridade 1:1 ({@code KEEP_SEPARATE}) entre {@link ErgonRuleErrorCatalog} e os bindings dos
 *       8 {@link RuleSetDefinition}s (produto + extensões por órgão), sem códigos órfãos e sem
 *       aglutinação de múltiplos códigos {@code ERG-*} numa mesma entrada;
 *   <li>Ausência de booleanos de fachada no {@link VinculoProvimentoRuleSet}, garantindo que a
 *       comparação relacional viva na árvore JSON Logic sobre fatos primitivos;
 *   <li>Execução das guardas de ciclo de vida {@code beforeDelete} e recálculo {@code afterDelete}.
 * </ol>
 */
class RuleCatalogExhaustivenessContractTest {

  private static final Pattern ERG_CODE_PATTERN = Pattern.compile("ERG-\\d{4,5}");

  @Test
  void allCataloguedRulesAreRejectedByAtLeastOneDomainFixture() {
    RuleCatalogExecution.verify();
  }

  @Test
  void everyGovernedFamilyHasASeparateCompiledCustomerPlan() {
    var rules = new br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade();
    var families = ErgonCrossSliceCustomizationMatrixTest.families().toList();
    assertThat(families).hasSize(18);
    assertThat(families.stream().map(f -> f.product().get().ref()).toList()).doesNotHaveDuplicates();
    for (var family : families) {
      var product = rules.compilePlan(family.product().get());
      var customized = rules.compilePlan(family.customized().get());
      assertThat(customized.definition().ref()).isEqualTo(product.definition().ref());
      assertThat(customized.planDigest()).isNotEqualTo(product.planDigest());
      assertThat(customized.orderedBindings())
          .anyMatch(binding -> binding.source() == org.praxisplatform.rules.contract.DecisionSource.CUSTOMER);
    }
  }

  @Test
  void everyCatalogEntryIsAtomicAndReferencedByAtLeastOneRuleSetBinding() throws IOException {
    List<RuleSetDefinition> allRuleSets =
        List.of(
            br.com.praxis.ergonshowcase.rules.AverbacaoTempoServicoRuleSet.productRuleSet(),
            br.com.praxis.ergonshowcase.rules.AverbacaoTempoServicoRuleSet.orgao2CustomizedRuleSet(),
            br.com.praxis.ergonshowcase.rules.PeriodoAquisitivoLicencaEspecialRuleSet.productRuleSet(),
            br.com.praxis.ergonshowcase.rules.PeriodoAquisitivoLicencaEspecialRuleSet.orgao2CustomizedRuleSet(),
            br.com.praxis.ergonshowcase.rules.LicencaEspecialGozoRuleSet.productRuleSet(),
            br.com.praxis.ergonshowcase.rules.LicencaEspecialGozoRuleSet.orgao2CustomizedRuleSet(),
            br.com.praxis.ergonshowcase.rules.VendaLicencaEspecialRuleSet.productRuleSet(),
            br.com.praxis.ergonshowcase.rules.VendaLicencaEspecialRuleSet.orgao2CustomizedRuleSet(),
            br.com.praxis.ergonshowcase.rules.CargoQuadroRuleSet.productRuleSet(),
            br.com.praxis.ergonshowcase.rules.CargoQuadroRuleSet.orgao2CustomizedRuleSet(),
            br.com.praxis.ergonshowcase.rules.ServidorPessoaCadastroRuleSet.productRuleSet(),
            br.com.praxis.ergonshowcase.rules.ServidorPessoaCadastroRuleSet.orgao2CustomizedRuleSet(),
            Rn013RegrasFrequenciaRuleSet.productRuleSet(),
            Rn013RegrasFrequenciaRuleSet.orgao2CustomizedRuleSet(),
            DependenteElegibilidadeRuleSet.productRuleSet(),
            DependenteElegibilidadeRuleSet.orgao2CustomizedRuleSet(),
            VinculoProvimentoRuleSet.productRuleSet(),
            VinculoProvimentoRuleSet.orgao2CustomizedRuleSet(),
            FrequenciaLancamentoRuleSet.productRuleSet(),
            FrequenciaLancamentoRuleSet.orgao2CustomizedRuleSet(),
            AfastamentoConcessaoRuleSet.productRuleSet(),
            AfastamentoConcessaoRuleSet.orgao2CustomizedRuleSet(),
            ParametroContagemFeriasRuleSet.productRuleSet(),
            ParametroContagemFeriasRuleSet.orgao2CustomizedRuleSet(),
            PeriodoAquisitivoFeriasRuleSet.productRuleSet(),
            PeriodoAquisitivoFeriasRuleSet.orgao2CustomizedRuleSet(),
            FeriasGozoRuleSet.productRuleSet(),
            FeriasGozoRuleSet.orgao2CustomizedRuleSet(),
            br.com.praxis.ergonshowcase.rules.DesignacaoFuncaoRuleSet.productRuleSet(),
            br.com.praxis.ergonshowcase.rules.SubstituicaoFuncaoRuleSet.productRuleSet(),
            br.com.praxis.ergonshowcase.rules.DesignacaoFuncaoRuleSet.orgao2CustomizedRuleSet(),
            br.com.praxis.ergonshowcase.rules.SubstituicaoFuncaoRuleSet.orgao2CustomizedRuleSet(),
            br.com.praxis.ergonshowcase.rules.FuncaoConfiancaRuleSet.productRuleSet(),
            br.com.praxis.ergonshowcase.rules.FuncaoConfiancaRuleSet.orgao2CustomizedRuleSet(),
            br.com.praxis.ergonshowcase.rules.VagaFuncaoRuleSet.productRuleSet(),
            br.com.praxis.ergonshowcase.rules.VagaFuncaoRuleSet.orgao2CustomizedRuleSet());

    Set<String> boundReasonCodes =
        java.util.stream.Stream.concat(allRuleSets.stream(),
                java.util.Arrays.stream(br.com.praxis.ergonshowcase.rules.OperationalRuleSet.values())
                    .map(rule -> rule.productRuleSet(new com.fasterxml.jackson.databind.ObjectMapper())))
            .flatMap(rs -> rs.bindings().stream())
            .map(DecisionBinding::falseReasonCode)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));

    Path facadeFile =
        Path.of("src/main/java/br/com/praxis/ergonshowcase/rules/ErgonRuleEngineFacade.java");
    if (Files.isRegularFile(facadeFile)) {
      String facadeSrc = Files.readString(facadeFile);
      for (String code : ErgonRuleErrorCatalog.all().keySet()) {
        String constName = code.replace('-', '_');
        if (facadeSrc.contains("\"" + code + "\"") || facadeSrc.contains(constName)) {
          boundReasonCodes.add(code);
        }
      }
    }

    assertThat(ErgonRuleErrorCatalog.all()).isNotEmpty();

    for (ErgonRuleErrorCatalog.RuleErrorDescriptor descriptor :
        ErgonRuleErrorCatalog.all().values()) {
      assertThat(boundReasonCodes)
          .as("Código %s do ErgonRuleErrorCatalog deve estar vinculado em pelo menos um RuleSet",
              descriptor.code())
          .contains(descriptor.code());

      Set<String> lineageCodes = new HashSet<>();
      Matcher matcher = ERG_CODE_PATTERN.matcher(descriptor.plsqlOrigin());
      while (matcher.find()) {
        lineageCodes.add(matcher.group());
      }
      if (descriptor.code().startsWith("ERG-")) {
        assertThat(lineageCodes)
            .as("Entrada %s não pode aglutinar múltiplos códigos ERG-* em plsqlOrigin (1:1 KEEP_SEPARATE)",
                descriptor.code())
            .isSubsetOf(descriptor.code());
      }
    }
  }

  @Test
  void vinculoProvimentoRuleSetUsesExplicitJsonLogicWithoutBooleanFacadeShortcuts() {
    String serializedBindings =
        VinculoProvimentoRuleSet.productRuleSet().bindings().stream()
            .map(DecisionBinding::executor)
            .filter(Objects::nonNull)
            .map(ref -> ref.expression())
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.joining("\n"));

    assertThat(serializedBindings)
        .doesNotContain(
            "cargo.categoriaCompativel",
            "cargoComissao.tipoComissaoValido",
            "cargo.hasVacancyBalance",
            "cargoComissao.hasVacancyBalance")
        .contains(
            "cargo.tipoCargo",
            "cargo.controleVaga",
            "cargo.vagasAutorizadas",
            "cargo.ocupadas",
            "cargoComissao.tipoCargo",
            "cargoComissao.controleVaga",
            "cargoComissao.vagasAutorizadas",
            "cargoComissao.ocupadas");
  }

  @Test
  void chefiaSlice7RuleSetsUseOntologicalRootsAndPrimitiveImpedimentFacts() throws Exception {
    assertThat(br.com.praxis.ergonshowcase.rules.FuncaoConfiancaRuleSet.productRuleSet().availableRoots())
        .containsExactlyInAnyOrder("draft");
    assertThat(br.com.praxis.ergonshowcase.rules.VagaFuncaoRuleSet.productRuleSet().availableRoots())
        .containsExactlyInAnyOrder("draft", "current", "funcao");
    assertThat(br.com.praxis.ergonshowcase.rules.DesignacaoFuncaoRuleSet.productRuleSet().availableRoots())
        .containsExactlyInAnyOrder("draft", "neighborhood", "current", "vaga");
    var substituicaoRuleSet = br.com.praxis.ergonshowcase.rules.SubstituicaoFuncaoRuleSet.productRuleSet();
    assertThat(substituicaoRuleSet.availableRoots())
        .containsExactlyInAnyOrder("draft", "designacao", "substituto", "impedimento", "funcao", "neighborhood");

    var byCode = substituicaoRuleSet.bindings().stream()
        .collect(Collectors.toMap(DecisionBinding::falseReasonCode, DecisionBinding::requiredFactPaths));
    assertThat(byCode.get("ERG-01300")).containsExactlyInAnyOrder("draft");
    assertThat(byCode.get("ERG-01311")).containsExactlyInAnyOrder("impedimento", "draft");
    assertThat(byCode.get("ERG-01315")).containsExactlyInAnyOrder("funcao");
    assertThat(byCode.get("ERG-01324")).containsExactlyInAnyOrder("neighborhood");
    assertThat(substituicaoRuleSet.slots().stream()
        .filter(slot -> "chefia.erg-01311".equals(slot.slotKey()))
        .findFirst().orElseThrow().overridePolicy())
        .isEqualTo(org.praxisplatform.rules.contract.OverridePolicy.FORBIDDEN);

    var scopedAst = new com.fasterxml.jackson.databind.ObjectMapper().readTree("""
        {"none":[{"var":"neighborhood.substituicoes"},{"===":[{"var":"situacao"},"EM_EXERCICIO"]}]}
        """);
    assertThat(br.com.praxis.ergonshowcase.rules.ChefiaRuleSetSupport.roots(scopedAst))
        .containsExactly("neighborhood");

    String substituicaoBindings =
        substituicaoRuleSet.bindings().stream()
            .map(DecisionBinding::executor)
            .filter(Objects::nonNull)
            .map(ref -> ref.expression())
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.joining("\n"));

    assertThat(substituicaoBindings)
        .doesNotContain("facts.", "impedimentosTitular")
        .contains(
            "impedimento.tipo",
            "impedimento.situacao",
            "impedimento.inicio",
            "impedimento.fim",
            "impedimento.interrupcao",
            "designacao.titular",
            "substituto.exercicio",
            "funcao.permiteSubstituicao",
            "neighborhood.feriasSubstituto",
            "neighborhood.afastamentosSubstituto",
            "neighborhood.outrasSubstituicoesTitular");
    assertThat(substituicaoRuleSet.referencedFactPaths())
        .contains(
            "draft.inicio",
            "draft.fim",
            "designacao.titular",
            "substituto.exercicio",
            "impedimento.tipo",
            "funcao.permiteSubstituicao",
            "neighborhood.feriasSubstituto",
            "neighborhood.afastamentosSubstituto",
            "neighborhood.outrasSubstituicoesSubstituto",
            "neighborhood.outrasSubstituicoesTitular");
  }

  @Test
  void tempoLicencaSlice6RuleSetsUseOntologicalRootsWithoutFlatFactsBag() {
    var averbacao = br.com.praxis.ergonshowcase.rules.AverbacaoTempoServicoRuleSet.productRuleSet();
    assertThat(averbacao.availableRoots())
        .containsExactlyInAnyOrder("draft", "vinculo", "neighborhood");
    assertThat(averbacao.referencedFactPaths())
        .doesNotContain("facts")
        .contains("draft.inicio", "draft.fim", "vinculo.exercicio", "neighborhood.sobreposicoes", "neighborhood.consumoPeriodosGerados");

    var periodo = br.com.praxis.ergonshowcase.rules.PeriodoAquisitivoLicencaEspecialRuleSet.productRuleSet();
    assertThat(periodo.availableRoots())
        .containsExactlyInAnyOrder("draft", "current", "vinculo", "politica", "neighborhood");
    assertThat(periodo.referencedFactPaths())
        .doesNotContain("facts")
        .contains("draft.diasDireito", "current.edicaoManual", "vinculo.exercicio", "politica.temLicEsp", "neighborhood.gozoAtivo");

    var gozo = br.com.praxis.ergonshowcase.rules.LicencaEspecialGozoRuleSet.productRuleSet();
    assertThat(gozo.availableRoots())
        .containsExactlyInAnyOrder("draft", "vinculo", "periodoAquisitivo", "politica", "venda", "neighborhood");
    assertThat(gozo.referencedFactPaths())
        .doesNotContain("facts")
        .contains("draft.diasGozo", "vinculo.vacancia", "periodoAquisitivo.saldoDisponivel", "politica.baseAbono", "venda.estado", "neighborhood.consumoAcumulado");

    var venda = br.com.praxis.ergonshowcase.rules.VendaLicencaEspecialRuleSet.productRuleSet();
    assertThat(venda.availableRoots())
        .containsExactlyInAnyOrder("draft", "periodoAquisitivo", "politica", "neighborhood");
    assertThat(venda.referencedFactPaths())
        .doesNotContain("facts")
        .contains("draft.diasVendidos", "periodoAquisitivo.saldoDisponivel", "politica.permiteVenda", "neighborhood.pecuniaAcumulada");

    var cargo = br.com.praxis.ergonshowcase.rules.CargoQuadroRuleSet.productRuleSet();
    assertThat(cargo.availableRoots())
        .containsExactlyInAnyOrder("draft", "cargoAglutinador", "neighborhood");
    assertThat(cargo.referencedFactPaths())
        .doesNotContain("facts", "facts.paiHabilitado")
        .contains("draft.controle", "draft.autorizadas", "cargoAglutinador.aglutinador", "neighborhood.ocupadas", "neighborhood.dependentes");

    var servidor = br.com.praxis.ergonshowcase.rules.ServidorPessoaCadastroRuleSet.productRuleSet();
    assertThat(servidor.availableRoots())
        .containsExactlyInAnyOrder("draft", "neighborhood", "context");
    assertThat(servidor.referencedFactPaths())
        .doesNotContain("facts")
        .contains("draft.nascimento", "draft.anosCompletosHoje", "neighborhood.outrosComCpf", "context.hoje", "context.anoAtual");

    for (var family : ErgonCrossSliceCustomizationMatrixTest.families().toList()) {
      var productDef = family.product().get();
      var customizedDef = family.customized().get();
      for (String conceptRoot : productDef.conceptRoots()) {
        assertThat(
                !productDef.factPathsByRoot().getOrDefault(conceptRoot, List.of()).isEmpty()
                    || !customizedDef.factPathsByRoot().getOrDefault(conceptRoot, List.of()).isEmpty())
            .as("Conceito 1-hop '%s' do RuleSet %s deve expor os atributos referenciados nas regras de produto ou customizadas",
                conceptRoot, productDef.ref().ruleSetKey())
            .isTrue();
      }
      for (var def : List.of(productDef, customizedDef)) {
        assertThat(def.availableRoots())
            .as("RuleSet %s (%s) não pode usar saco escalar achatado 'facts'", def.ref().ruleSetKey(), def.ref().operationKey())
            .doesNotContain("facts");
        assertThat(def.referencedFactPaths())
            .as("RuleSet %s (%s) não pode referenciar caminhos sob 'facts.*'", def.ref().ruleSetKey(), def.ref().operationKey())
            .noneMatch(path -> path.equals("facts") || path.startsWith("facts."));
        assertThat(def.factPathsByRoot().keySet())
            .as("RuleSet %s deve expor dicionário de fatos para todas as raízes em availableRoots", def.ref().ruleSetKey())
            .containsExactlyElementsOf(def.availableRoots());
        assertThat(def.factPathsByRoot().get("draft"))
            .as("RuleSet %s deve expor atributos ontológicos da raiz 'draft'", def.ref().ruleSetKey())
            .isNotEmpty();
        for (var slot : def.slots()) {
          if (slot.parameterTemplate() != null
              && slot.parameterTemplate().executor() != null
              && slot.parameterTemplate().executor().expression() != null) {
            var templateRoots =
                br.com.praxis.ergonshowcase.rules.ErgonOntologyRuleSetSupport.roots(
                    slot.parameterTemplate().executor().expression());
            assertThat(def.availableRoots())
                .as("Template %s do slot %s deve ter todas as raízes em availableRoots",
                    slot.parameterTemplate().templateKey(), slot.slotKey())
                .containsAll(templateRoots);
            for (String root : templateRoots) {
              assertThat(slot.parameterTemplate().requiredFactPaths())
                  .as("Template %s do slot %s deve declarar cobertura da raiz '%s' em requiredFactPaths",
                      slot.parameterTemplate().templateKey(), slot.slotKey(), root)
                  .anyMatch(path -> path.equals(root) || path.startsWith(root + "."));
            }
          }
        }
        for (var binding : def.bindings()) {
          if (binding.enabled()
              && binding.executor() != null
              && binding.executor().type() == org.praxisplatform.rules.contract.RuleExecutorType.JSON_LOGIC
              && binding.executor().expression() != null) {
            var bindingRoots =
                br.com.praxis.ergonshowcase.rules.ErgonOntologyRuleSetSupport.roots(
                    binding.executor().expression());
            assertThat(def.availableRoots())
                .as("Binding %s (%s) deve ter todas as raízes em availableRoots",
                    binding.bindingKey(), binding.source())
                .containsAll(bindingRoots);
            for (String root : bindingRoots) {
              assertThat(binding.requiredFactPaths())
                  .as("Binding %s (%s) deve declarar cobertura da raiz '%s' em requiredFactPaths",
                      binding.bindingKey(), binding.source(), root)
                  .anyMatch(path -> path.equals(root) || path.startsWith(root + "."));
            }
          }
        }
      }
    }

    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    for (var opRule : br.com.praxis.ergonshowcase.rules.OperationalRuleSet.values()) {
      var def = opRule.productRuleSet(mapper);
      assertThat(def.availableRoots())
          .as("OperationalRuleSet %s não pode usar saco escalar achatado 'facts'", opRule.name())
          .doesNotContain("facts");
      assertThat(def.referencedFactPaths())
          .as("OperationalRuleSet %s não pode referenciar caminhos sob 'facts.*'", opRule.name())
          .noneMatch(path -> path.equals("facts") || path.startsWith("facts."));
    }

    assertThatThrownBy(() ->
        br.com.praxis.ergonshowcase.rules.ErgonOntologyRuleSetSupport.binding(
            "invalid.legacy", "slot", org.praxisplatform.rules.contract.DecisionSource.PRODUCT,
            null, "{\"==\":[{\"var\":\"facts.foo\"},1]}", "CERG-99999", 1))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("facts");
  }

  @Test
  void previewBindingsAndDomainCatalogPreserveOntologicalSymmetryAndConceptRoots() {
    var rules = new br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade();
    rules.enforceAfastamentoPreviewDates(3, java.time.LocalDate.of(2026, 1, 10), java.time.LocalDate.of(2026, 1, 20));
    assertThatThrownBy(() ->
        rules.enforceCodigoAfastamentoPreview(3, "LICENCA", 10, false))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            ex -> assertThat(ex.getReasonCodes()).containsExactly("ERG-04306"));
    assertThatThrownBy(() ->
        rules.enforceAfastamentoPreviewDates(3, java.time.LocalDate.of(2026, 1, 20), java.time.LocalDate.of(2026, 1, 10)))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            ex -> assertThat(ex.getReasonCodes()).containsExactly("ERG-04313"));
    assertThatThrownBy(() ->
        rules.enforceCargoAglutinadorPreview(2, 10, false))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            ex -> assertThat(ex.getReasonCodes()).containsExactly("CERG-02105"));

    var catalogProvider = new br.com.praxis.ergonshowcase.policystudio.ErgonDomainRuleSetCatalogProvider(
        rules, new com.fasterxml.jackson.databind.ObjectMapper());
    var domainCatalog = new org.praxisplatform.uischema.domain.SemanticDomainCatalogService(
        new org.praxisplatform.uischema.action.ActionDefinitionRegistry() {
          @Override public List<org.praxisplatform.uischema.action.ActionDefinition> findByResourceKey(String rk) { return List.of(); }
          @Override public List<org.praxisplatform.uischema.action.ActionDefinition> findByGroup(String g) { return List.of(); }
        },
        new org.praxisplatform.uischema.surface.SurfaceDefinitionRegistry() {
          @Override public List<org.praxisplatform.uischema.surface.SurfaceDefinition> findByResourceKey(String rk) { return List.of(); }
          @Override public List<org.praxisplatform.uischema.surface.SurfaceDefinition> findByGroup(String g) { return List.of(); }
        },
        org.praxisplatform.uischema.options.OptionSourceRegistry.empty(),
        null,
        null,
        java.time.Clock.systemUTC(),
        "praxis-ergon-showcase",
        "Praxis Ergon Showcase",
        "1.0.0",
        null,
        null,
        () -> java.util.Optional.of("1"),
        catalogProvider);
    var cargoCatalog = domainCatalog.findByResourceKey("administracao-pessoal.cargos");
    var cargoPolicyNode = cargoCatalog.nodes().stream()
        .filter(node -> "policy_hint".equals(node.nodeType()))
        .findFirst().orElseThrow();
    assertThat(cargoPolicyNode.metadata().get("conceptRoots"))
        .isEqualTo(List.of("cargoAglutinador"));

    var averbacaoProduct = br.com.praxis.ergonshowcase.rules.AverbacaoTempoServicoRuleSet.productRuleSet();
    var invalidCustomerBinding = new DecisionBinding(
        "customer.orgao3.augment.cerg-06111",
        "administracao-pessoal.averbacoes-tempo-servico.cerg-06111",
        org.praxisplatform.rules.contract.DecisionSource.CUSTOMER,
        org.praxisplatform.rules.contract.CompositionPolicy.AUGMENT,
        org.praxisplatform.rules.contract.RuleExecutorRef.jsonLogic(
            new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(
                java.util.Map.of("==", List.of(java.util.Map.of("var", "draft.finalidade"), "APOSENTADORIA")))),
        List.of(),
        99,
        true,
        org.praxisplatform.rules.contract.RuleDecision.DENY,
        "CERG-99901",
        List.of(),
        null);
    var invalidCandidateBindings = new java.util.ArrayList<>(averbacaoProduct.bindings());
    invalidCandidateBindings.add(invalidCustomerBinding);
    var invalidCandidateDef = new org.praxisplatform.rules.contract.RuleSetDefinition(
        averbacaoProduct.ref(), averbacaoProduct.availableRoots(), averbacaoProduct.slots(),
        invalidCandidateBindings, averbacaoProduct.compatibility(), averbacaoProduct.failPolicy());
    assertThatThrownBy(() -> rules.registerPlan(
        averbacaoProduct.ref().ruleSetKey(), 3, invalidCandidateDef))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requiredFactPaths");
  }

  @Test
  void deleteLifecycleHooksEnforceStateGuardsOnMutableServices() {
    TenantContext tenant =
        new TenantContext() {
          @Override
          public int orgao() {
            return 1;
          }

          @Override
          public OptionalInt explicitOrgao() {
            return OptionalInt.of(1);
          }
        };

    FrequenciaService frequenciaService = new FrequenciaService(null, null, tenant);
    Frequencia freqHomologada = new Frequencia();
    freqHomologada.setEmpCodigo(1);
    freqHomologada.setSituacao("HOMOLOGADO");
    assertThatThrownBy(
            () ->
                ReflectionTestUtils.invokeMethod(
                    frequenciaService, "beforeDelete", 10L, freqHomologada))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

    ParametroContagemFeriasService paramService =
        new ParametroContagemFeriasService(null, null, tenant);
    ParametroContagemFerias activeParam = new ParametroContagemFerias();
    activeParam.setEmpCodigo(1);
    activeParam.setActive(true);
    assertThatThrownBy(
            () ->
                ReflectionTestUtils.invokeMethod(paramService, "beforeDelete", 10L, activeParam))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

    FeriasGozoService feriasGozoService = new FeriasGozoService(null, null, tenant);
    FeriasGozo gozada = new FeriasGozo();
    gozada.setEmpCodigo(1);
    gozada.setSituacao("GOZADA");
    assertThatThrownBy(
            () -> ReflectionTestUtils.invokeMethod(feriasGozoService, "beforeDelete", 10L, gozada))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

    PeriodoAquisitivoFeriasService paService =
        new PeriodoAquisitivoFeriasService(null, null, tenant);
    PeriodoAquisitivoFerias pa = new PeriodoAquisitivoFerias();
    pa.setEmpCodigo(2);
    assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(paService, "beforeDelete", 10L, pa))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
  }

  @Test
  void ergonRuleViolationExceptionSupportsBoth422And409AndInvariantsHasNoSplitBrain()
      throws IOException {
    ErgonRuleViolationException ex422 =
        new ErgonRuleViolationException(
            "ERG-01850",
            ParametroContagemFeriasRuleSet.SLOT_CONCOMITANCIA_VIGENCIA_ESCOPO,
            "product.parametros-conta.erg-01850",
            org.praxisplatform.rules.contract.DecisionSource.PRODUCT,
            org.praxisplatform.rules.contract.DecisionStage.POST_DECISION,
            "dtini",
            "Conflito de vigência ativa");
    assertThat(ex422.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

    var conflict = ErgonRuleErrorCatalog.find("ERG-08393").orElseThrow();
    ErgonRuleViolationException ex409 = new ErgonRuleViolationException(conflict.code(), conflict.slotKey(),
        "product.conflict", org.praxisplatform.rules.contract.DecisionSource.PRODUCT,
        org.praxisplatform.rules.contract.DecisionStage.DOMAIN_DECISION, conflict.targetField(), conflict.messagePtBr());
    assertThat(ex409.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(ex409.getReasonCodes()).containsExactly("ERG-08393");

    Path invariantsFile =
        Path.of("src/main/java/br/com/praxis/ergonshowcase/service/ErgonInvariants.java");
    if (Files.isRegularFile(invariantsFile)) {
      String invariantsSrc = Files.readString(invariantsFile);
      assertThat(ERG_CODE_PATTERN.matcher(invariantsSrc).find())
          .as("ErgonInvariants não pode conter códigos ERG-* hardcoded em Split-Brain com os RuleSets")
          .isFalse();
    }
  }
}
