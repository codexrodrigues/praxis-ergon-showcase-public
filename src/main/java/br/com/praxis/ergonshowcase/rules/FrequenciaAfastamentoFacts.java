package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.domain.Afastamento;
import br.com.praxis.ergonshowcase.domain.CodigoFrequencia;
import br.com.praxis.ergonshowcase.domain.Frequencia;
import br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria;
import br.com.praxis.ergonshowcase.domain.ServidorPessoa;
import br.com.praxis.ergonshowcase.domain.TipoFrequencia;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.AfastamentoRuleContext;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.FrequenciaRuleContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.praxisplatform.rules.facts.PraxisFactTreeBuilder;

/**
 * Grafo ontológico de fatos de lançamento de frequência ({@code FREQUENCIAS}) e concessão de
 * afastamento ({@code AFASTAMENTOS}), incluindo a projeção estatutária derivada de {@code
 * REGRAS_FREQ}.
 */
public final class FrequenciaAfastamentoFacts {
  private static final LocalDate OPEN_ENDED_DATE = LocalDate.of(9999, 12, 31);
  private static final Set<String> INACTIVE_SITUACOES = Set.of("CANCELADO", "REJEITADO");

  private record OccurrenceRange(Long id, LocalDate start, LocalDate end, int days) {}

  private FrequenciaAfastamentoFacts() {}

  public static ObjectNode ofFrequencia(ObjectMapper mapper, FrequenciaRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Frequencia draft = ctx.draft();
    Integer empCodigo = draft != null && draft.getEmpCodigo() != null ? draft.getEmpCodigo() : 1;
    int durationDays = computeFrequenciaDurationDays(draft);
    BigDecimal effectiveQuantity =
        draft != null && draft.getQuantidade() != null
            ? draft.getQuantidade().stripTrailingZeros()
            : BigDecimal.valueOf(durationDays);
    LocalDate eventStart = draft != null ? draft.getDtini() : null;
    LocalDate eventEnd =
        draft != null ? (draft.getDtfim() != null ? draft.getDtfim() : draft.getDtini()) : null;

    ObjectNode root = PraxisFactTreeBuilder.create()
        .putDraft(node -> populateFrequenciaDraft(node, draft, ctx, durationDays, effectiveQuantity))
        .putConcept("vinculo", node -> populateVinculo(node, ctx.vinculo(), empCodigo))
        .putConcept("pessoa", node -> populatePessoa(node, ctx.pessoa()))
        .putConcept(
            "tipoFrequencia",
            node -> populateTipoFrequencia(
                node,
                draft != null ? draft.getTipoFrequenciaEntidade() : null,
                ctx.codigoFrequencia()))
        .putConcept(
            "codigoFrequencia", node -> populateCodigoFrequencia(node, ctx.codigoFrequencia()))
        .putConcept(
            "documentoLegal",
            node -> DependenteElegibilidadeFacts.populateDocumentoLegal(
                node,
                draft != null ? draft.getDocumentoLegal() : null,
                draft != null ? draft.getDocumentoLegalId() : null,
                empCodigo))
        .putConcept(
            "effectiveStatutoryRule",
            node -> populateFrequenciaStatutoryRuleNode(
                node,
                ctx.effectiveStatutoryRule(),
                draft,
                durationDays,
                eventStart,
                eventEnd,
                ctx.existingFrequencias(),
                ctx.activeAfastamentos()))
        .putConcept("tenant", node -> node.put("empCodigo", empCodigo))
        .putContext(node -> node
            .put("resourceKey", FrequenciaLancamentoRuleSet.RESOURCE_KEY)
            .put("ruleSetKey", FrequenciaLancamentoRuleSet.RULESET_KEY))
        .build();

    ArrayNode existingArray = mapper.createArrayNode();
    if (ctx.existingFrequencias() != null) {
      for (Frequencia existing : ctx.existingFrequencias()) {
        if (existing != null) {
          existingArray.add(toFrequenciaSummaryNode(existing));
        }
      }
    }
    root.set("existingFrequencias", existingArray);

    ArrayNode activeAfastArray = mapper.createArrayNode();
    if (ctx.activeAfastamentos() != null) {
      for (Afastamento afast : ctx.activeAfastamentos()) {
        if (afast != null) {
          activeAfastArray.add(toAfastamentoSummaryNode(afast));
        }
      }
    }
    root.set("activeAfastamentos", activeAfastArray);

    return root;
  }

  public static ObjectNode ofAfastamento(ObjectMapper mapper, AfastamentoRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Afastamento draft = ctx.draft();
    Integer empCodigo = draft != null && draft.getEmpCodigo() != null ? draft.getEmpCodigo() : 1;
    Integer durationDays = computeAfastamentoDurationDays(draft);
    LocalDate eventStart = draft != null ? draft.getDtini() : null;
    LocalDate eventEnd = resolveAfastamentoEndDate(draft);

    ObjectNode root = PraxisFactTreeBuilder.create()
        .putDraft(node -> populateAfastamentoDraft(node, draft, ctx, durationDays))
        .putConcept("vinculo", node -> populateVinculo(node, ctx.vinculo(), empCodigo))
        .putConcept("pessoa", node -> populatePessoa(node, ctx.pessoa()))
        .putConcept(
            "tipoFrequencia",
            node -> populateTipoFrequencia(
                node,
                draft != null ? draft.getTipoFrequenciaEntidade() : null,
                ctx.codigoFrequencia()))
        .putConcept(
            "codigoFrequencia", node -> populateCodigoFrequencia(node, ctx.codigoFrequencia()))
        .putConcept(
            "documentoLegal",
            node -> DependenteElegibilidadeFacts.populateDocumentoLegal(
                node,
                draft != null ? draft.getDocumentoLegal() : null,
                draft != null ? draft.getDocumentoLegalId() : null,
                empCodigo))
        .putConcept(
            "afastamentoOrigem",
            node -> populateAfastamentoOrigem(node, draft, ctx.afastamentoOrigem()))
        .putConcept(
            "substitutoVinculo",
            node -> populateSubstitutoVinculo(node, ctx.substitutoVinculo(), empCodigo))
        .putConcept(
            "effectiveStatutoryRule",
            node -> populateAfastamentoStatutoryRuleNode(
                node,
                ctx.effectiveStatutoryRule(),
                draft,
                durationDays != null ? durationDays : 1,
                eventStart,
                eventEnd,
                ctx.existingAfastamentos(),
                ctx.activeFrequencias(),
                ctx.afastamentoOrigem()))
        .putConcept("tenant", node -> node.put("empCodigo", empCodigo))
        .putContext(node -> node
            .put("resourceKey", AfastamentoConcessaoRuleSet.RESOURCE_KEY)
            .put("ruleSetKey", AfastamentoConcessaoRuleSet.RULESET_KEY))
        .build();

    ArrayNode existingArray = mapper.createArrayNode();
    if (ctx.existingAfastamentos() != null) {
      for (Afastamento existing : ctx.existingAfastamentos()) {
        if (existing != null) {
          existingArray.add(toAfastamentoSummaryNode(existing));
        }
      }
    }
    root.set("existingAfastamentos", existingArray);

    ArrayNode activeFreqArray = mapper.createArrayNode();
    if (ctx.activeFrequencias() != null) {
      for (Frequencia freq : ctx.activeFrequencias()) {
        if (freq != null) {
          activeFreqArray.add(toFrequenciaSummaryNode(freq));
        }
      }
    }
    root.set("activeFrequencias", activeFreqArray);

    return root;
  }

  private static void populateFrequenciaDraft(
      PraxisFactTreeBuilder draftNode,
      Frequencia draft,
      FrequenciaRuleContext ctx,
      int durationDays,
      BigDecimal effectiveQuantity) {
    if (draft == null) {
      return;
    }
    draftNode
        .put("id", draft.getId())
        .put("empCodigo", draft.getEmpCodigo())
        .put("numfunc", draft.getNumfunc())
        .put("numvinc", draft.getNumvinc())
        .put("tipofreq", draft.getTipofreq())
        .put("codfreq", draft.getCodfreq());
    if (draft.getDtini() != null) {
      draftNode.put("dtini", draft.getDtini());
    }
    Integer horaEntradaMinutes =
        draft.getHoraEntrada() != null
            ? draft.getHoraEntrada().getHour() * 60 + draft.getHoraEntrada().getMinute()
            : null;
    Integer horaSaidaMinutes =
        draft.getHoraSaida() != null
            ? draft.getHoraSaida().getHour() * 60 + draft.getHoraSaida().getMinute()
            : null;
    draftNode
        .put("dtfim", draft.getDtfim())
        .put("horaEntradaMinutes", horaEntradaMinutes)
        .put("horaSaidaMinutes", horaSaidaMinutes)
        .put("quantidade", draft.getQuantidade())
        .put("effectiveQuantity", effectiveQuantity)
        .put("effectiveDurationDays", durationDays)
        .put("situacao", draft.getSituacao())
        .put("documentoLegalId", draft.getDocumentoLegalId())
        .put("retroactiveDays", resolveRetroactiveDays(draft, ctx.referenceDate()));
  }

  private static void populateAfastamentoDraft(
      PraxisFactTreeBuilder draftNode,
      Afastamento draft,
      AfastamentoRuleContext ctx,
      Integer durationDays) {
    if (draft == null) {
      return;
    }
    draftNode
        .put("id", draft.getId())
        .put("empCodigo", draft.getEmpCodigo())
        .put("numfunc", draft.getNumfunc())
        .put("numvinc", draft.getNumvinc())
        .put("tipofreq", draft.getTipofreq())
        .put("codfreq", draft.getCodfreq());
    if (draft.getDtini() != null) {
      draftNode.put("dtini", draft.getDtini());
    }
    draftNode
        .put("dtprevfim", draft.getDtprevfim())
        .put("dtfim", draft.getDtfim())
        .put("quantidadeDias", draft.getQuantidadeDias())
        .put("effectiveDurationDays", durationDays != null ? durationDays : 1)
        .put("prorrogacao", draft.getProrrogacao())
        .put("inPlaceProrrogacao", ctx.inPlaceProrrogacao())
        .put("previsaoAnterior", epochDay(ctx.previsaoAnterior()))
        .put("previsaoAtual", epochDay(draft.getDtprevfim()))
        .put("afastamentoOrigemId", draft.getAfastamentoOrigemId())
        .put("numfuncSubstituto", draft.getNumfuncSubstituto())
        .put("numvincSubstituto", draft.getNumvincSubstituto())
        .put("documentoLegalId", draft.getDocumentoLegalId())
        .put("atoPublicacaoId", draft.getAtoPublicacaoId())
        .put("situacao", draft.getSituacao());
  }

  private static void populateFrequenciaStatutoryRuleNode(
      PraxisFactTreeBuilder node,
      RegraFrequenciaEstatutaria rule,
      Frequencia draft,
      int draftDurationDays,
      LocalDate draftStart,
      LocalDate draftEnd,
      List<Frequencia> existingFrequencias,
      List<Afastamento> activeAfastamentos) {
    populateBaseStatutoryRuleNode(node, rule);

    int prorrogWindowDays =
        rule != null && rule.getIntProrrogDias() != null ? rule.getIntProrrogDias() : 0;
    List<OccurrenceRange> peers = new ArrayList<>();
    if (draft != null && draftStart != null && existingFrequencias != null) {
      for (Frequencia f : existingFrequencias) {
        if (f == null || isInactive(f.getSituacao())) {
          continue;
        }
        if (draft.getId() != null && Objects.equals(draft.getId(), f.getId())) {
          continue;
        }
        if (!Objects.equals(draft.getTipofreq(), f.getTipofreq())
            || !Objects.equals(draft.getCodfreq(), f.getCodfreq())) {
          continue;
        }
        LocalDate fStart = f.getDtini();
        if (fStart == null) {
          continue;
        }
        LocalDate fEnd = f.getDtfim() != null ? f.getDtfim() : fStart;
        peers.add(new OccurrenceRange(f.getId(), fStart, fEnd, computeFrequenciaDurationDays(f)));
      }
    }

    LocalDate boundedDraftEnd =
        draftStart != null
            ? (draftEnd != null && !draftEnd.isBefore(draftStart)
                ? draftEnd
                : draftStart.plusDays(Math.max(1, draftDurationDays) - 1L))
            : null;
    populateStatutoryAccumulationMetrics(
        node,
        draft != null ? draft.getId() : null,
        draftStart,
        boundedDraftEnd,
        draftDurationDays,
        prorrogWindowDays,
        peers,
        null);

    int overlappingAfastamentosCount = 0;
    if (draftStart != null && activeAfastamentos != null) {
      LocalDate effectiveDraftEnd = draftEnd != null ? draftEnd : draftStart;
      for (Afastamento a : activeAfastamentos) {
        if (a == null || isInactive(a.getSituacao()) || a.getDtini() == null) {
          continue;
        }
        LocalDate aEnd = resolveAfastamentoEndDate(a);
        if (!draftStart.isAfter(aEnd) && !a.getDtini().isAfter(effectiveDraftEnd)) {
          overlappingAfastamentosCount++;
        }
      }
    }
    node.put("overlappingAfastamentosCount", overlappingAfastamentosCount);
  }

  private static void populateAfastamentoStatutoryRuleNode(
      PraxisFactTreeBuilder node,
      RegraFrequenciaEstatutaria rule,
      Afastamento draft,
      int draftDurationDays,
      LocalDate draftStart,
      LocalDate draftEnd,
      List<Afastamento> existingAfastamentos,
      List<Frequencia> activeFrequencias,
      Afastamento afastamentoOrigem) {
    populateBaseStatutoryRuleNode(node, rule);

    int prorrogWindowDays =
        rule != null && rule.getIntProrrogDias() != null ? rule.getIntProrrogDias() : 0;
    List<OccurrenceRange> peers = new ArrayList<>();
    if (draft != null && draftStart != null && existingAfastamentos != null) {
      for (Afastamento a : existingAfastamentos) {
        if (a == null || isInactive(a.getSituacao())) {
          continue;
        }
        if (draft.getId() != null && Objects.equals(draft.getId(), a.getId())) {
          continue;
        }
        if (!Objects.equals(draft.getTipofreq(), a.getTipofreq())
            || !Objects.equals(draft.getCodfreq(), a.getCodfreq())) {
          continue;
        }
        LocalDate aStart = a.getDtini();
        if (aStart == null) {
          continue;
        }
        Integer aDaysObj = computeAfastamentoDurationDays(a);
        int aDays = aDaysObj != null ? aDaysObj : 1;
        LocalDate aEnd =
            a.getDtfim() != null
                ? a.getDtfim()
                : (a.getDtprevfim() != null
                    ? a.getDtprevfim()
                    : aStart.plusDays(Math.max(1, aDays) - 1L));
        peers.add(new OccurrenceRange(a.getId(), aStart, aEnd, aDays));
      }
    }

    OccurrenceRange explicitOriginRange = null;
    if (afastamentoOrigem != null
        && !isInactive(afastamentoOrigem.getSituacao())
        && afastamentoOrigem.getDtini() != null
        && (draft == null || !Objects.equals(draft.getId(), afastamentoOrigem.getId()))) {
      Integer origDaysObj = computeAfastamentoDurationDays(afastamentoOrigem);
      int origDays = origDaysObj != null ? origDaysObj : 1;
      LocalDate origEnd =
          afastamentoOrigem.getDtfim() != null
              ? afastamentoOrigem.getDtfim()
              : (afastamentoOrigem.getDtprevfim() != null
                  ? afastamentoOrigem.getDtprevfim()
                  : afastamentoOrigem.getDtini().plusDays(Math.max(1, origDays) - 1L));
      explicitOriginRange =
          new OccurrenceRange(
              afastamentoOrigem.getId(), afastamentoOrigem.getDtini(), origEnd, origDays);
    }

    LocalDate boundedDraftEnd = null;
    if (draftStart != null) {
      if (draftEnd != null
          && !draftEnd.equals(OPEN_ENDED_DATE)
          && !draftEnd.isBefore(draftStart)) {
        boundedDraftEnd = draftEnd;
      } else {
        boundedDraftEnd = draftStart.plusDays(Math.max(1, draftDurationDays) - 1L);
      }
    }

    populateStatutoryAccumulationMetrics(
        node,
        draft != null ? draft.getId() : null,
        draftStart,
        boundedDraftEnd,
        draftDurationDays,
        prorrogWindowDays,
        peers,
        explicitOriginRange);

    int overlappingFrequenciasCount = 0;
    if (draftStart != null && activeFrequencias != null) {
      LocalDate effectiveDraftEnd = draftEnd != null ? draftEnd : OPEN_ENDED_DATE;
      for (Frequencia f : activeFrequencias) {
        if (f == null || isInactive(f.getSituacao()) || f.getDtini() == null) {
          continue;
        }
        LocalDate fEnd = f.getDtfim() != null ? f.getDtfim() : f.getDtini();
        if (!draftStart.isAfter(fEnd) && !f.getDtini().isAfter(effectiveDraftEnd)) {
          overlappingFrequenciasCount++;
        }
      }
    }
    node.put("overlappingFrequenciasCount", overlappingFrequenciasCount);
  }

  private static void populateStatutoryAccumulationMetrics(
      PraxisFactTreeBuilder node,
      Long draftId,
      LocalDate draftStart,
      LocalDate boundedDraftEnd,
      int draftDurationDays,
      int prorrogWindowDays,
      List<OccurrenceRange> peers,
      OccurrenceRange explicitOriginRange) {
    if (draftStart == null || boundedDraftEnd == null) {
      node.put("daysSincePreviousOccurrence", null)
          .put("accumulatedProrrogDays", draftDurationDays)
          .put("projectedConsecMesDays", draftDurationDays)
          .put("projectedInterpMesDays", draftDurationDays)
          .put("projectedConsecAnoDays", draftDurationDays)
          .put("projectedInterpAnoDays", draftDurationDays);
      return;
    }

    OccurrenceRange draftRange =
        new OccurrenceRange(draftId, draftStart, boundedDraftEnd, draftDurationDays);

    Integer daysSincePrevious = null;
    for (OccurrenceRange p : peers) {
      if (p.end().isBefore(draftStart)) {
        int gap = Math.max(0, Math.toIntExact(ChronoUnit.DAYS.between(p.end(), draftStart) - 1));
        if (daysSincePrevious == null || gap < daysSincePrevious) {
          daysSincePrevious = gap;
        }
      }
    }

    // 1. Cadeia multi-hop de prorrogação (accumulatedProrrogDays)
    List<OccurrenceRange> prorrogChain = new ArrayList<>();
    Set<OccurrenceRange> prorrogVisited = new HashSet<>();
    prorrogChain.add(draftRange);
    prorrogVisited.add(draftRange);
    if (explicitOriginRange != null) {
      OccurrenceRange matchedPeer =
          peers.stream()
              .filter(
                  p ->
                      (explicitOriginRange.id() != null
                              && Objects.equals(explicitOriginRange.id(), p.id()))
                          || (Objects.equals(explicitOriginRange.start(), p.start())
                              && Objects.equals(explicitOriginRange.end(), p.end())))
              .findFirst()
              .orElse(explicitOriginRange);
      if (prorrogVisited.add(matchedPeer)) {
        prorrogChain.add(matchedPeer);
      }
    }
    if (prorrogWindowDays > 0) {
      boolean expanded = true;
      while (expanded) {
        expanded = false;
        for (OccurrenceRange p : peers) {
          if (prorrogVisited.contains(p)) {
            continue;
          }
          for (OccurrenceRange member : List.copyOf(prorrogChain)) {
            if (p.end().isBefore(member.start())) {
              int gap =
                  Math.max(
                      0, Math.toIntExact(ChronoUnit.DAYS.between(p.end(), member.start()) - 1));
              if (gap <= prorrogWindowDays) {
                prorrogVisited.add(p);
                prorrogChain.add(p);
                expanded = true;
                break;
              }
            }
          }
        }
      }
    }
    int accumulatedProrrogDays =
        prorrogChain.stream().mapToInt(OccurrenceRange::days).sum();

    // 2. Cadeia multi-hop de ocorrências consecutivas (gap == 0 dias entre fim e início)
    List<OccurrenceRange> consecCluster = new ArrayList<>();
    Set<OccurrenceRange> consecVisited = new HashSet<>();
    consecCluster.add(draftRange);
    consecVisited.add(draftRange);
    boolean clusterExpanded = true;
    while (clusterExpanded) {
      clusterExpanded = false;
      for (OccurrenceRange p : peers) {
        if (consecVisited.contains(p)) {
          continue;
        }
        for (OccurrenceRange member : List.copyOf(consecCluster)) {
          if (areContiguousOrOverlapping(p, member)) {
            consecVisited.add(p);
            consecCluster.add(p);
            clusterExpanded = true;
            break;
          }
        }
      }
    }

    List<OccurrenceRange> allRanges = new ArrayList<>(peers.size() + 1);
    allRanges.add(draftRange);
    allRanges.addAll(peers);

    // 3. Limites mensais por mês-calendário tocado pelo rascunho
    YearMonth startMonth = YearMonth.from(draftStart);
    YearMonth endMonth = YearMonth.from(boundedDraftEnd);
    int maxConsecMes = 0;
    int maxInterpMes = 0;
    for (YearMonth ym = startMonth; !ym.isAfter(endMonth); ym = ym.plusMonths(1)) {
      int monthConsec = 0;
      for (OccurrenceRange r : consecCluster) {
        monthConsec += daysInYearMonth(r, ym);
      }
      int monthInterp = 0;
      for (OccurrenceRange r : allRanges) {
        monthInterp += daysInYearMonth(r, ym);
      }
      if (monthConsec > maxConsecMes) {
        maxConsecMes = monthConsec;
      }
      if (monthInterp > maxInterpMes) {
        maxInterpMes = monthInterp;
      }
    }

    // 4. Limites anuais por ano-calendário tocado pelo rascunho
    int startYear = draftStart.getYear();
    int endYear = boundedDraftEnd.getYear();
    int maxConsecAno = 0;
    int maxInterpAno = 0;
    for (int year = startYear; year <= endYear; year++) {
      int yearConsec = 0;
      for (OccurrenceRange r : consecCluster) {
        yearConsec += daysInYear(r, year);
      }
      int yearInterp = 0;
      for (OccurrenceRange r : allRanges) {
        yearInterp += daysInYear(r, year);
      }
      if (yearConsec > maxConsecAno) {
        maxConsecAno = yearConsec;
      }
      if (yearInterp > maxInterpAno) {
        maxInterpAno = yearInterp;
      }
    }

    node.put("daysSincePreviousOccurrence", daysSincePrevious)
        .put("accumulatedProrrogDays", accumulatedProrrogDays)
        .put("projectedConsecMesDays", maxConsecMes)
        .put("projectedInterpMesDays", maxInterpMes)
        .put("projectedConsecAnoDays", maxConsecAno)
        .put("projectedInterpAnoDays", maxInterpAno);
  }

  private static boolean areContiguousOrOverlapping(OccurrenceRange a, OccurrenceRange b) {
    if (a.end().isBefore(b.start())) {
      return ChronoUnit.DAYS.between(a.end(), b.start()) <= 1;
    }
    if (b.end().isBefore(a.start())) {
      return ChronoUnit.DAYS.between(b.end(), a.start()) <= 1;
    }
    return true;
  }

  private static int daysInYearMonth(OccurrenceRange r, YearMonth ym) {
    LocalDate mStart = ym.atDay(1);
    LocalDate mEnd = ym.atEndOfMonth();
    if (r.end().isBefore(mStart) || r.start().isAfter(mEnd)) {
      return 0;
    }
    if (!r.start().isBefore(mStart) && !r.end().isAfter(mEnd)) {
      return r.days();
    }
    LocalDate oStart = r.start().isAfter(mStart) ? r.start() : mStart;
    LocalDate oEnd = r.end().isBefore(mEnd) ? r.end() : mEnd;
    return Math.toIntExact(ChronoUnit.DAYS.between(oStart, oEnd) + 1);
  }

  private static int daysInYear(OccurrenceRange r, int year) {
    LocalDate yStart = LocalDate.of(year, 1, 1);
    LocalDate yEnd = LocalDate.of(year, 12, 31);
    if (r.end().isBefore(yStart) || r.start().isAfter(yEnd)) {
      return 0;
    }
    if (!r.start().isBefore(yStart) && !r.end().isAfter(yEnd)) {
      return r.days();
    }
    LocalDate oStart = r.start().isAfter(yStart) ? r.start() : yStart;
    LocalDate oEnd = r.end().isBefore(yEnd) ? r.end() : yEnd;
    return Math.toIntExact(ChronoUnit.DAYS.between(oStart, oEnd) + 1);
  }

  private static void populateBaseStatutoryRuleNode(
      PraxisFactTreeBuilder node, RegraFrequenciaEstatutaria rule) {
    if (rule == null) {
      node.put("present", false)
          .put("id", null)
          .put("ruleKey", null)
          .put("naoSeAplica", false)
          .put("qtdMinDias", null)
          .put("qtdMaxDias", null)
          .put("intervMinDias", null)
          .put("intProrrogDias", null)
          .put("qtdMaxConsecMes", null)
          .put("qtdMaxInterpMes", null)
          .put("qtdMaxConsecAno", null)
          .put("qtdMaxInterpAno", null)
          .put("exigeHomologacao", false)
          .put("permiteConcomitancia", false)
          .put("exigeDocumentoLegal", false)
          .put("prorrogacaoPermitida", true)
          .put("documentoLegalId", null);
      return;
    }

    boolean exigeDoc =
        rule.getDocumentoLegalId() != null
            || (rule.getRuleParams() != null
                && ("S".equalsIgnoreCase(String.valueOf(rule.getRuleParams().get("pontlei")))
                    || Boolean.TRUE.equals(rule.getRuleParams().get("exigeDocumentoLegal"))));
    boolean prorrogPermitida =
        rule.getRuleParams() == null
            || !"N".equalsIgnoreCase(String.valueOf(rule.getRuleParams().get("prorrogacao")));

    node.put("present", true)
        .put("id", rule.getId())
        .put("ruleKey", rule.getRuleKey())
        .put("naoSeAplica", Boolean.TRUE.equals(rule.getNaoSeAplica()))
        .put("qtdMinDias", rule.getQtdMinDias())
        .put("qtdMaxDias", rule.getQtdMaxDias())
        .put("intervMinDias", rule.getIntervMinDias())
        .put("intProrrogDias", rule.getIntProrrogDias())
        .put("qtdMaxConsecMes", rule.getQtdMaxConsecMes())
        .put("qtdMaxInterpMes", rule.getQtdMaxInterpMes())
        .put("qtdMaxConsecAno", rule.getQtdMaxConsecAno())
        .put("qtdMaxInterpAno", rule.getQtdMaxInterpAno())
        .put("exigeHomologacao", Boolean.TRUE.equals(rule.getExigeHomologacao()))
        .put("permiteConcomitancia", Boolean.TRUE.equals(rule.getPermiteConcomitancia()))
        .put("exigeDocumentoLegal", exigeDoc)
        .put("prorrogacaoPermitida", prorrogPermitida)
        .put("documentoLegalId", rule.getDocumentoLegalId());
  }

  private static void populateVinculo(
      PraxisFactTreeBuilder node, Vinculo vinculo, Integer expectedEmpCodigo) {
    if (vinculo == null) {
      node.put("present", false)
          .put("sameOrgao", false)
          .put("dtexerc", null)
          .put("dtvac", null)
          .put("dtaposent", null)
          .put("situacao", null);
      return;
    }
    node.put("present", true)
        .put(
            "sameOrgao",
            expectedEmpCodigo == null || Objects.equals(vinculo.getEmpCodigo(), expectedEmpCodigo))
        .put("dtexerc", vinculo.getDtexerc())
        .put("dtvac", vinculo.getDtvac())
        .put("dtaposent", vinculo.getDtaposent())
        .put("situacao", vinculo.getSituacao())
        .put("regimejur", vinculo.getRegimejur())
        .put("tipovinc", vinculo.getTipovinc())
        .put("categoria", vinculo.getCategoria())
        .put("subcategoria", vinculo.getSubcategoria());
  }

  private static void populatePessoa(PraxisFactTreeBuilder node, ServidorPessoa pessoa) {
    if (pessoa == null) {
      node.put("present", false).put("sexo", null);
      return;
    }
    node.put("present", true).put("sexo", pessoa.getSexo());
  }

  private static void populateTipoFrequencia(
      PraxisFactTreeBuilder node, TipoFrequencia tipoFreq, CodigoFrequencia codigo) {
    TipoFrequencia resolved =
        tipoFreq != null
            ? tipoFreq
            : (codigo != null ? codigo.getTipoFrequenciaEntidade() : null);
    if (resolved == null) {
      node.put("present", false)
          .put("tipo", null)
          .put("nome", null)
          .put("influiConta", false)
          .put("horaExtra", false);
      return;
    }
    node.put("present", true)
        .put("tipo", resolved.getTipo())
        .put("nome", resolved.getNome())
        .put("influiConta", Boolean.TRUE.equals(resolved.getInfluiConta()))
        .put("horaExtra", Boolean.TRUE.equals(resolved.getHoraExtra()));
  }

  private static void populateCodigoFrequencia(
      PraxisFactTreeBuilder node, CodigoFrequencia codigo) {
    if (codigo == null) {
      node.put("present", false)
          .put("tipofreq", null)
          .put("codigo", null)
          .put("viaFrequencia", false)
          .put("afastamento", false)
          .put("horaEntradaSaida", false)
          .put("regrasFreq", false)
          .put("sexo", null)
          .put("qtdmin", null)
          .put("qtdmax", null);
      return;
    }
    node.put("present", true)
        .put("tipofreq", codigo.getId() != null ? codigo.getId().tipofreq() : null)
        .put("codigo", codigo.getId() != null ? codigo.getId().codigo() : null)
        .put("viaFrequencia", Boolean.TRUE.equals(codigo.getViaFrequencia()))
        .put("afastamento", Boolean.TRUE.equals(codigo.getAfastamento()))
        .put("horaEntradaSaida", Boolean.TRUE.equals(codigo.getHoraEntradaSaida()))
        .put("regrasFreq", Boolean.TRUE.equals(codigo.getRegrasFreq()))
        .put("sexo", codigo.getSexo())
        .put("qtdmin", codigo.getQtdmin())
        .put("qtdmax", codigo.getQtdmax());
  }

  private static void populateAfastamentoOrigem(
      PraxisFactTreeBuilder node, Afastamento draft, Afastamento origem) {
    if (origem == null) {
      node.put("present", false)
          .put("sameVinculo", false)
          .put("selfReference", false)
          .put("activeOrigin", false)
          .put("chronologicalAfterOrigin", false)
          .put("gapDays", null);
      return;
    }
    boolean sameVinculo =
        draft != null
            && Objects.equals(origem.getNumfunc(), draft.getNumfunc())
            && Objects.equals(origem.getNumvinc(), draft.getNumvinc());
    boolean selfRef =
        draft != null
            && draft.getId() != null
            && Objects.equals(draft.getId(), origem.getId());
    boolean activeOrigin = !isInactive(origem.getSituacao());
    LocalDate origEnd =
        origem.getDtfim() != null
            ? origem.getDtfim()
            : (origem.getDtprevfim() != null ? origem.getDtprevfim() : origem.getDtini());
    boolean chronologicalAfterOrigin =
        draft != null
            && draft.getDtini() != null
            && origEnd != null
            && draft.getDtini().isAfter(origEnd);
    Integer gapDays =
        chronologicalAfterOrigin
            ? Math.max(0, Math.toIntExact(ChronoUnit.DAYS.between(origEnd, draft.getDtini()) - 1))
            : null;

    node.put("present", true)
        .put("sameVinculo", sameVinculo)
        .put("selfReference", selfRef)
        .put("activeOrigin", activeOrigin)
        .put("chronologicalAfterOrigin", chronologicalAfterOrigin)
        .put("gapDays", gapDays);
  }

  private static void populateSubstitutoVinculo(
      PraxisFactTreeBuilder node, Vinculo substituto, Integer expectedEmpCodigo) {
    if (substituto == null) {
      node.put("present", false).put("sameOrgao", false).put("ativo", false);
      return;
    }
    node.put("present", true)
        .put(
            "sameOrgao",
            expectedEmpCodigo == null
                || Objects.equals(substituto.getEmpCodigo(), expectedEmpCodigo))
        .put("ativo", "ATIVO".equals(substituto.getSituacao()));
  }

  private static ObjectNode toFrequenciaSummaryNode(Frequencia f) {
    return PraxisFactTreeBuilder.create()
        .put("id", f.getId())
        .put("numfunc", f.getNumfunc())
        .put("numvinc", f.getNumvinc())
        .put("tipofreq", f.getTipofreq())
        .put("codfreq", f.getCodfreq())
        .put("dtini", f.getDtini())
        .put("dtfim", f.getDtfim())
        .put("situacao", f.getSituacao())
        .build();
  }

  private static ObjectNode toAfastamentoSummaryNode(Afastamento a) {
    return PraxisFactTreeBuilder.create()
        .put("id", a.getId())
        .put("numfunc", a.getNumfunc())
        .put("numvinc", a.getNumvinc())
        .put("tipofreq", a.getTipofreq())
        .put("codfreq", a.getCodfreq())
        .put("dtini", a.getDtini())
        .put("dtprevfim", a.getDtprevfim())
        .put("dtfim", a.getDtfim())
        .put("situacao", a.getSituacao())
        .build();
  }

  private static int computeFrequenciaDurationDays(Frequencia f) {
    if (f == null || f.getDtini() == null) {
      return 1;
    }
    if (f.getDtfim() != null && !f.getDtfim().isBefore(f.getDtini())) {
      return Math.toIntExact(ChronoUnit.DAYS.between(f.getDtini(), f.getDtfim()) + 1);
    }
    return 1;
  }

  private static Integer computeAfastamentoDurationDays(Afastamento a) {
    if (a == null) {
      return null;
    }
    if (a.getQuantidadeDias() != null && a.getQuantidadeDias() > 0) {
      return a.getQuantidadeDias();
    }
    if (a.getDtini() == null) {
      return null;
    }
    LocalDate end = a.getDtfim() != null ? a.getDtfim() : a.getDtprevfim();
    if (end != null && !end.isBefore(a.getDtini())) {
      return Math.toIntExact(ChronoUnit.DAYS.between(a.getDtini(), end) + 1);
    }
    return null;
  }

  private static LocalDate resolveAfastamentoEndDate(Afastamento a) {
    if (a == null) {
      return OPEN_ENDED_DATE;
    }
    if (a.getDtfim() != null) {
      return a.getDtfim();
    }
    if (a.getDtprevfim() != null) {
      return a.getDtprevfim();
    }
    return OPEN_ENDED_DATE;
  }

  private static Integer resolveRetroactiveDays(Frequencia draft, LocalDate referenceDate) {
    if (draft == null) {
      return null;
    }
    if (draft.getCustomFields() != null && draft.getCustomFields().get("retroactiveDays") != null) {
      Object raw = draft.getCustomFields().get("retroactiveDays");
      if (raw instanceof Number n) {
        return n.intValue();
      }
      try {
        return Integer.parseInt(raw.toString());
      } catch (NumberFormatException ignored) {
        // fallback to referenceDate calculation
      }
    }
    if (referenceDate != null && draft.getDtini() != null) {
      return Math.max(0, Math.toIntExact(ChronoUnit.DAYS.between(draft.getDtini(), referenceDate)));
    }
    return null;
  }

  private static Long epochDay(LocalDate date) {
    return date == null ? null : date.toEpochDay();
  }

  private static boolean isInactive(String situacao) {
    return situacao != null && INACTIVE_SITUACOES.contains(situacao);
  }
}
