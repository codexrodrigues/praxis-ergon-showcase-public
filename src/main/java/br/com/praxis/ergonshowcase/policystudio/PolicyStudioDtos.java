package br.com.praxis.ergonshowcase.policystudio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.praxisplatform.rules.contract.*;

final class PolicyStudioDtos {
  private PolicyStudioDtos() {}

  record RuleSetSummary(String ruleSetKey, String resourceKey, String boundedContextKey,
      String operationKey, int version, String titlePtBr, String domainGroupPtBr,
      String descriptionPtBr, List<String> availableRoots, List<String> conceptRoots,
      List<String> referencedFactPaths, Map<String, List<String>> factPathsByRoot,
      int totalSlots, long protectedGuardSlotsCount,
      long parameterizableSlotsCount, long replaceableSlotsCount, long restrictOnlySlotsCount,
      long activeCustomerOverridesCount, String activePlanDigest, String productPlanDigest) {}

  record Organization(int empCodigo, boolean canPublish) {}

  record RuleSetDetail(String ruleSetKey, String titlePtBr, String descriptionPtBr,
      List<String> availableRoots, List<String> conceptRoots, List<String> referencedFactPaths,
      Map<String, List<String>> factPathsByRoot, RuleFailPolicy failPolicy, String productPlanDigest,
      String activePlanDigest, String activationTag, int empCodigo, boolean customizedForOrgao,
      List<Slot> slots, List<DagEdge> dagEdges, List<PresetScenario> presetScenarios) {}

  record Slot(String slotKey, DecisionStage stage, SlotCardinality cardinality,
      OverridePolicy overridePolicy, DecisionAggregationPolicy aggregationPolicy,
      boolean canBypass, boolean canParameterize, boolean canAugment,
      @JsonProperty("isBypassedInOrgao") boolean isBypassedInOrgao,
      ParameterTemplate parameterTemplate, List<Binding> bindings) {}

  record ParameterTemplate(String templateKey, String templateVersion, String falseReasonCode,
      List<Parameter> parameters) {}

  record Parameter(String name, RuleParameterType type, String unit, BigDecimal minValue,
      BigDecimal maxValue, JsonNode defaultValue, JsonNode effectiveValue) {}

  record Binding(String bindingKey, DecisionSource source, CompositionPolicy compositionPolicy,
      boolean enabled, int order, List<String> dependsOn, RuleExecutorType executorType,
      String implementationKey, JsonNode expression, RuleDecision falseDecision,
      String falseReasonCode, List<String> requiredFactPaths, List<String> referencedFactPaths,
      String messagePtBr, String targetField, String plsqlOrigin, String plsqlLineage) {}

  enum EdgeKind { DEPENDS_ON, STAGE_SEQUENCE }

  record DagEdge(String fromBindingKey, String toBindingKey, String fromSlotKey,
      String toSlotKey, EdgeKind kind) {}

  record PresetScenario(String presetScenarioKey, String descriptionPtBr, JsonNode facts) {}

  enum CustomizationMode { BYPASS_SLOT, REPLACE_SLOT_EXPRESSION, UPDATE_PARAMETERS, AUGMENT_SLOT }

  @Schema(description = "Alteração de política aplicada a um slot e a um único órgão.")
  record CustomizationRequest(
      @Schema(description = "Modalidade de composição autorizada pela política do slot.") CustomizationMode mode,
      @Schema(description = "Identidade do slot de decisão que recebe a alteração.") String slotKey,
      @Schema(description = "Ativa o bypass ou restaura a composição vigente antes dele.") Boolean bypass,
      @Schema(description = "Justificativa opcional associada ao bypass.") String customReasonMessagePtBr,
      @Schema(description = "Predicado JSON Logic de conformidade para a decisão local.") JsonNode expressionJson,
      @Schema(description = "Código de razão emitido quando a expressão substituta reprova.") String falseReasonCode,
      @Schema(description = "Valores escalares declarados para a URN e versão do template do slot.") Map<String, JsonNode> parameterValues,
      @Schema(description = "Código CERG da restrição adicional do órgão.") String ruleCode,
      @Schema(description = "Campo do contrato a corrigir quando a restrição é violada.") String targetField,
      @Schema(description = "Mensagem de negócio exibida ao operador que precisa corrigir a violação.") String messagePtBr,
      @Schema(description = "Referência ao ato ou à política local que fundamenta a restrição.") String origin,
      @Schema(description = "Controla se a restrição adicional participa do plano ativo.") Boolean enabled) {}

  @Schema(description = "Compara a decisão dos dois órgãos com a mesma árvore de fatos.")
  record SimulationRequest(
      @Schema(description = "Identidade do conjunto governado a simular.") String ruleSetKey,
      @Schema(description = "Órgão cujo plano ativo serve de referência.", defaultValue = "1") Integer baselineEmpCodigo,
      @Schema(description = "Órgão cuja política será comparada à referência.", defaultValue = "2") Integer targetEmpCodigo,
      @Schema(description = "Árvore de fatos com as raízes declaradas pelo conjunto.") JsonNode facts,
      @Schema(description = "Identidade do cenário do catálogo usado no lugar dos fatos explícitos.") String presetScenarioKey) {
    int baselineOrgao() {
      return baselineEmpCodigo == null ? 1 : baselineEmpCodigo;
    }

    int targetOrgao() {
      return targetEmpCodigo == null ? 2 : targetEmpCodigo;
    }
  }

  @Schema(description = "Compara o plano ativo e uma proposta ainda não publicada para o mesmo órgão.")
  record CandidateRequest(
      @Schema(description = "Alteração que será compilada e avaliada sem ativação.") CustomizationRequest customization,
      @Schema(description = "Fatos compartilhados pela avaliação dos dois planos.") JsonNode facts,
      @Schema(description = "Cenário do catálogo utilizado no lugar dos fatos explícitos.") String presetScenarioKey) {}

  record SimulationResponse(SimulationResult baseline, SimulationResult target,
      List<String> divergentSlots, String nowUtc) {}

  record SimulationResult(Integer empCodigo, String planDigest, String factsDigest, RuleDecision decision,
      List<String> reasonCodes, List<Violation> violations, List<SlotTrace> slotTraces) {}

  record Violation(String code, String slotKey, String bindingKey, DecisionSource source,
      DecisionStage stage, RuleDecision decision, String targetField, String messagePtBr,
      String plsqlOrigin, String plsqlLineage) {}

  record SlotTrace(String slotKey, String bindingKey, DecisionStage stage, DecisionSource source,
      CompositionPolicy compositionPolicy, RuleDecision decision, String reasonCode, boolean bypassed) {}
}
