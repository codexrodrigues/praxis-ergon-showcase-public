package br.com.praxis.ergonshowcase.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.praxisplatform.rules.contract.RuleDecision;

/**
 * Fatos admitidos pela fonte interna de horários para um par de vínculos. O intervalo de
 * cobertura descreve a escala completa no período indicado, inclusive exceções e plantões.
 */
public record ProvaHorariosAcumulacao(
    LocalDate de, LocalDate ate, ZoneId fuso, Duration deslocamentoMinimo,
    List<Expediente> candidato, List<Expediente> concomitante) {

  public ProvaHorariosAcumulacao {
    candidato = List.copyOf(candidato);
    concomitante = List.copyOf(concomitante);
  }

  public record Expediente(Instant inicio, Instant fim) {
    public Expediente {
      if (!inicio.isBefore(fim)) throw new IllegalArgumentException("Expediente deve ter duração positiva.");
    }
  }

  void writeTo(ObjectNode node) {
    node.put("de", de.toString());
    node.put("ate", ate.toString());
    node.put("fuso", fuso.getId());
    node.put("deslocamentoMinimo", deslocamentoMinimo.toString());
    writeWindows(node, "candidato", candidato);
    writeWindows(node, "concomitante", concomitante);
  }

  private static void writeWindows(ObjectNode node, String field, List<Expediente> expedientes) {
    var array = node.putArray(field);
    for (var expediente : expedientes) {
      array.addObject().put("inicio", expediente.inicio().toString()).put("fim", expediente.fim().toString());
    }
  }

  private static List<Expediente> readWindows(JsonNode node) {
    if (!node.isArray()) throw new IllegalArgumentException("Escala inválida.");
    var windows = new ArrayList<Expediente>();
    for (JsonNode window : node) {
      windows.add(new Expediente(Instant.parse(window.path("inicio").asText()),
          Instant.parse(window.path("fim").asText())));
    }
    return windows;
  }

  static RuleDecision avaliar(JsonNode node, LocalDate inicio, LocalDate fim) {
    if (!node.isObject()) return RuleDecision.INCONCLUSIVE;
    try {
      return new ProvaHorariosAcumulacao(LocalDate.parse(node.path("de").asText()),
          LocalDate.parse(node.path("ate").asText()), ZoneId.of(node.path("fuso").asText()),
          Duration.parse(node.path("deslocamentoMinimo").asText()), readWindows(node.path("candidato")),
          readWindows(node.path("concomitante"))).avaliar(inicio, fim);
    } catch (java.time.DateTimeException | IllegalArgumentException ex) {
      return RuleDecision.INCONCLUSIVE;
    }
  }

  private RuleDecision avaliar(LocalDate inicio, LocalDate fim) {
    if (de.isAfter(ate) || deslocamentoMinimo.isNegative()
        || candidato.isEmpty() || concomitante.isEmpty()) {
      return RuleDecision.INCONCLUSIVE;
    }
    var inicioCobertura = de.atStartOfDay(fuso).toInstant();
    var fimCobertura = ate.plusDays(1).atStartOfDay(fuso).toInstant();
    if (!inside(candidato, inicioCobertura, fimCobertura) || !inside(concomitante, inicioCobertura, fimCobertura)) {
      return RuleDecision.INCONCLUSIVE;
    }
    var inicioConcomitancia = inicio.atStartOfDay(fuso).toInstant();
    var fimConcomitancia = fim.plusDays(1).atStartOfDay(fuso).toInstant();
    for (var a : candidato) {
      if (!a.inicio().isBefore(fimConcomitancia) || !inicioConcomitancia.isBefore(a.fim())) continue;
      for (var b : concomitante) {
        if (!b.inicio().isBefore(fimConcomitancia) || !inicioConcomitancia.isBefore(b.fim())) continue;
        if (a.inicio().isBefore(b.fim()) && b.inicio().isBefore(a.fim())) return RuleDecision.DENY;
        var intervalo = !a.fim().isAfter(b.inicio()) ? Duration.between(a.fim(), b.inicio())
            : Duration.between(b.fim(), a.inicio());
        if (intervalo.compareTo(deslocamentoMinimo) < 0) return RuleDecision.DENY;
      }
    }
    return !de.isAfter(inicio) && !ate.isBefore(fim) && fim.getYear() < 9999
        ? RuleDecision.ALLOW : RuleDecision.INCONCLUSIVE;
  }

  private static boolean inside(List<Expediente> expedientes, Instant inicio, Instant fim) {
    return expedientes.stream().allMatch(e -> !e.inicio().isBefore(inicio) && !e.fim().isAfter(fim));
  }
}
