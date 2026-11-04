package br.com.praxis.ergonshowcase.config;

import br.com.praxis.ergonshowcase.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.customfields.*;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Projeta definições globais e específicas do órgão, dando precedência à definição específica. */
@Component
@Transactional(readOnly = true)
public class CamposCustomizadosProvider implements CustomFieldDefinitionProvider {
  @PersistenceContext private EntityManager em;

  public List<CustomFieldDescriptor> findDefinitions(
      String resourceKey, String tenantId, String schemaType) {
    Integer orgao = tenantId == null ? 1 : Integer.valueOf(tenantId);
    var rows =
        em.createQuery(
                "select d from DefinicaoCampoCustomizado d where d.active=true and d.resourceKey=:r and (d.empCodigo is null or d.empCodigo=:o) order by d.empCodigo nulls first,d.fieldOrder",
                DefinicaoCampoCustomizado.class)
            .setParameter("r", resourceKey)
            .setParameter("o", orgao)
            .getResultList();
    var optionTabs =
        rows.stream()
            .map(DefinicaoCampoCustomizado::getOptionTabela)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    Map<String, List<ItemTabela>> itemsByTab = new HashMap<>();
    if (!optionTabs.isEmpty()) {
      for (var item :
          em.createQuery(
                  "select i from ItemTabela i where i.id.tab in :tabs and i.ativo=true and (i.empCodigo is null or i.empCodigo=:o) order by i.ordem,i.descr",
                  ItemTabela.class)
              .setParameter("tabs", optionTabs)
              .setParameter("o", orgao)
              .getResultList()) {
        itemsByTab.computeIfAbsent(item.getId().tab(), k -> new ArrayList<>()).add(item);
      }
    }
    Map<String, CustomFieldDescriptor> result = new LinkedHashMap<>();
    for (var d : rows) {
      var v = d.getValidationRules();
      var g = d.getGovernancePolicy();
      List<String> enums = new ArrayList<>();
      List<Map<String, Object>> options = new ArrayList<>();
      if (d.getOptionTabela() != null)
        for (var item : itemsByTab.getOrDefault(d.getOptionTabela(), List.of())) {
          enums.add(item.getId().item());
          options.add(Map.of("value", item.getId().item(), "label", item.getDescr()));
        }
      CustomFieldDataType type =
          switch (d.getFieldType()) {
            case "NUMBER" -> CustomFieldDataType.DECIMAL;
            case "SELECT" -> CustomFieldDataType.ENUM;
            default -> CustomFieldDataType.valueOf(d.getFieldType());
          };
      FieldControlType control =
          switch (d.getControlType()) {
            case "SWITCH" -> FieldControlType.CHECKBOX;
            case "SEARCHABLE_SELECT" -> FieldControlType.SELECT;
            default -> FieldControlType.valueOf(d.getControlType());
          };
      result.put(
          d.getFieldKey(),
          new CustomFieldDescriptor(
              d.getFieldKey(),
              d.getLabel(),
              d.getHelpText(),
              type,
              control,
              d.getGroupName(),
              d.getFieldOrder(),
              d.getRequired(),
              false,
              true,
              false,
              "regime_teletrabalho".equals(d.getFieldKey()),
              null,
              decimal(v, "min"),
              decimal(v, "max"),
              integer(v, "minLength"),
              integer(v, "maxLength"),
              string(v, "pattern"),
              string(v, "customMessage"),
              "Preencha " + d.getLabel() + ".",
              enums,
              options,
              null,
              FilterOperation.EQUAL,
              g, jsonLogicRules(v)));
    }
    return List.copyOf(result.values());
  }

  /** The declared JSON Logic is published and evaluated by the same native descriptor. */
  public static Map<String, Object> jsonLogicRules(Map<String, Object> rules) {
    if (rules == null || rules.isEmpty()) return Map.of();
    Object expression;
    if (rules.containsKey("jsonLogic") || rules.containsKey("rule")) {
      expression = rules.get(rules.containsKey("jsonLogic") ? "jsonLogic" : "rule");
    } else {
      var tree = new LinkedHashMap<String,Object>();
      rules.forEach((key,value) -> {
        boolean scalar = switch (key) {
          case "min", "max", "minLength", "maxLength" -> value instanceof Number;
          case "pattern", "customMessage" -> value instanceof String;
          case "required" -> value instanceof Boolean;
          default -> false;
        };
        if (!scalar) tree.put(key,value);
      });
      if (tree.isEmpty()) return Map.of();
      expression = tree;
    }
    if (expression instanceof String text) {
      try { expression = new com.fasterxml.jackson.databind.ObjectMapper().readValue(text, Object.class); }
      catch (com.fasterxml.jackson.core.JsonProcessingException invalid) {
        throw new IllegalArgumentException("Expressão declarativa inválida.", invalid);
      }
    }
    var result = new LinkedHashMap<String,Object>();
    result.put("jsonLogic", expression);
    Object message = rules.get("customMessage");
    result.put("message", message != null && !message.toString().isBlank()
        ? message.toString() : br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog.ERG_21502.messagePtBr());
    return result;
  }

  private static String string(Map<String, Object> m, String k) {
    return m.get(k) == null ? null : m.get(k).toString();
  }

  private static BigDecimal decimal(Map<String, Object> m, String k) {
    return m.get(k) == null ? null : new BigDecimal(m.get(k).toString());
  }

  private static Integer integer(Map<String, Object> m, String k) {
    return m.get(k) == null ? null : Integer.valueOf(m.get(k).toString());
  }
}
