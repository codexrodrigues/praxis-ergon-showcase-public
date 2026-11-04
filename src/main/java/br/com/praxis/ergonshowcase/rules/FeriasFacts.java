package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.domain.Afastamento;
import br.com.praxis.ergonshowcase.domain.FeriasGozo;
import br.com.praxis.ergonshowcase.domain.Frequencia;
import br.com.praxis.ergonshowcase.domain.ParametroContagemFerias;
import br.com.praxis.ergonshowcase.domain.PeriodoAquisitivoFerias;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.rules.EffectiveVacationParameterResolver.EffectiveParametroFeriasFacts;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.FeriasGozoRuleContext;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.ParametroContagemFeriasRuleContext;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.PeriodoAquisitivoFeriasRuleContext;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.praxisplatform.rules.facts.PraxisFactTreeBuilder;

/**
 * Grafo ontológico de fatos de férias e períodos aquisitivos ({@code PARAMETROS_CONTA}, {@code
 * PERAQFER} e {@code FERIAS}).
 */
public final class FeriasFacts {
  private static final LocalDate OPEN_ENDED_DATE = LocalDate.of(9999, 12, 31);

  private FeriasFacts() {}

  public static ObjectNode ofParametroContagem(ParametroContagemFeriasRuleContext ctx) {
    ParametroContagemFerias d = ctx.draft();
    Integer empCodigo = d != null && d.getEmpCodigo() != null ? d.getEmpCodigo() : 1;

    ObjectNode root = PraxisFactTreeBuilder.create()
        .putDraft(node -> populateParametroDraft(node, d))
        .putConcept("derived", node -> {})
        .putConcept("tenant", node -> node.put("empCodigo", empCodigo))
        .putContext(node -> node.put("operation", ParametroContagemFeriasRuleSet.OPERATION))
        .build();

    ArrayNode existingNode = root.putArray("existingRules");
    int overlappingActiveCount = 0;
    LocalDate draftStart = d != null ? d.getDtini() : null;
    LocalDate draftEnd = d != null && d.getDtfim() != null ? d.getDtfim() : OPEN_ENDED_DATE;
    boolean draftActive = d == null || d.getActive() == null || d.getActive();

    if (ctx.existingRules() != null) {
      for (ParametroContagemFerias cand : ctx.existingRules()) {
        if (cand == null) {
          continue;
        }
        existingNode.add(
            PraxisFactTreeBuilder.create()
                .put("id", cand.getId())
                .put("empCodigo", cand.getEmpCodigo())
                .put("regimeJuridico", cand.getRegimeJuridico())
                .put("tipoVinculo", cand.getTipoVinculo())
                .put("categoria", cand.getCategoria())
                .put("subcategoria", cand.getSubcategoria())
                .put("dtini", cand.getDtini())
                .put("dtfim", cand.getDtfim())
                .put("active", cand.getActive() != null ? cand.getActive() : Boolean.TRUE)
                .build());

        if (d != null
            && draftActive
            && draftStart != null
            && (cand.getActive() == null || cand.getActive())
            && (d.getId() == null || !Objects.equals(d.getId(), cand.getId()))
            && sameParametroContagemScope(d, cand)
            && cand.getDtini() != null) {
          LocalDate candEnd = cand.getDtfim() != null ? cand.getDtfim() : OPEN_ENDED_DATE;
          if (!draftStart.isAfter(candEnd) && !cand.getDtini().isAfter(draftEnd)) {
            overlappingActiveCount++;
          }
        }
      }
    }

    int finalOverlappingActiveCount = overlappingActiveCount;
    root.set(
        "scope",
        PraxisFactTreeBuilder.create()
            .put("overlappingActiveCount", finalOverlappingActiveCount)
            .build());

    return root;
  }

  public static ObjectNode ofPeriodoAquisitivo(
      EffectiveVacationParameterResolver vacationParameterResolver,
      PeriodoAquisitivoFeriasRuleContext ctx) {
    PeriodoAquisitivoFerias d = ctx.draft();
    Vinculo v = ctx.vinculo();
    Integer empCodigo = d != null && d.getEmpCodigo() != null ? d.getEmpCodigo() : 1;

    EffectiveParametroFeriasFacts effective =
        vacationParameterResolver.resolveEffectiveFacts(
            ctx.parametroCandidates(),
            ctx.resolvedParametroBase(),
            d,
            v,
            d != null ? d.getDtini() : null);

    int faltasFromFrequencias = countUnjustifiedAbsencesInPa(d, ctx.activeFrequencias());
    int faltasApuradas =
        d != null && d.getFaltasApuradas() != null
            ? Math.max(d.getFaltasApuradas(), faltasFromFrequencias)
            : faltasFromFrequencias;

    int diasAfastFromEntity =
        d != null && d.getDiasAfastamentoSuspensivo() != null
            ? d.getDiasAfastamentoSuspensivo()
            : 0;
    int diasAfastFromList = countSuspensiveLeaveDaysInPa(d, ctx.activeAfastamentos());
    int diasAfastEfetivos = Math.max(diasAfastFromEntity, diasAfastFromList);

    int maxDiasDireitoPelaEscalaFaltas =
        EffectiveVacationParameterResolver.calcularDiasDireitoPorFaltas(
            effective.totDiasFer() != null ? effective.totDiasFer() : 30, faltasApuradas);
    if (diasAfastEfetivos > 180) {
      maxDiasDireitoPelaEscalaFaltas = 0;
    }

    int gozados = d != null && d.getDiasGozados() != null ? d.getDiasGozados() : 0;
    int vendidos = d != null && d.getDiasVendidosAbono() != null ? d.getDiasVendidosAbono() : 0;
    int dobro = d != null && d.getDiasConvertidosDobro() != null ? d.getDiasConvertidosDobro() : 0;
    int totalDiasConsumidos = gozados + vendidos + dobro;

    ObjectNode root = PraxisFactTreeBuilder.create()
        .putDraft(node -> populatePeriodoAquisitivoDraft(node, d, ctx, faltasApuradas, diasAfastEfetivos))
        .putConcept("vinculo", node -> populatePeriodoVinculo(node, v))
        .putConcept(
            "effectiveParametroConta", node -> populateEffectiveParametroNode(node, effective))
        .putConcept("tenant", node -> node.put("empCodigo", empCodigo))
        .putContext(node -> node.put("operation", PeriodoAquisitivoFeriasRuleSet.OPERATION))
        .build();

    ArrayNode existingNode = root.putArray("existingPeriodos");
    int overlappingPeriodsCount = 0;
    int otherOpenPeriodsCount = 0;
    LocalDate draftStart = d != null ? d.getDtini() : null;
    LocalDate draftEnd = d != null ? d.getDtfim() : null;

    if (ctx.existingPeriodos() != null) {
      for (PeriodoAquisitivoFerias cand : ctx.existingPeriodos()) {
        if (cand == null) {
          continue;
        }
        existingNode.add(
            PraxisFactTreeBuilder.create()
                .put("id", cand.getId())
                .put("chave", cand.getChave())
                .put("dtini", cand.getDtini())
                .put("dtfim", cand.getDtfim())
                .put("situacao", cand.getSituacao())
                .put("saldoDiasDisponivel", cand.getSaldoDiasDisponivel())
                .build());

        boolean sameRecord =
            (d != null && d.getId() != null && Objects.equals(d.getId(), cand.getId()))
                || (d != null
                    && d.getId() == null
                    && d.getChave() != null
                    && Objects.equals(d.getChave(), cand.getChave())
                    && Objects.equals(d.getDtini(), cand.getDtini())
                    && Objects.equals(d.getDtfim(), cand.getDtfim()));
        if (sameRecord) {
          continue;
        }
        if ("CANCELADO".equalsIgnoreCase(cand.getSituacao())) {
          continue;
        }
        if (d != null
            && d.getChave() != null
            && Objects.equals(d.getChave(), cand.getChave())
            && (d.getId() == null || !Objects.equals(d.getId(), cand.getId()))) {
          overlappingPeriodsCount++;
        } else if (draftStart != null
            && draftEnd != null
            && cand.getDtini() != null
            && cand.getDtfim() != null
            && !draftStart.isAfter(cand.getDtfim())
            && !cand.getDtini().isAfter(draftEnd)) {
          overlappingPeriodsCount++;
        }

        if (isOpenPeriodoSituacao(cand.getSituacao())
            && (cand.getSaldoDiasDisponivel() == null || cand.getSaldoDiasDisponivel() > 0)) {
          otherOpenPeriodsCount++;
        }
      }
    }

    boolean currentOpen =
        d != null
            && isOpenPeriodoSituacao(d.getSituacao())
            && (d.getTotDiasDireito() == null || d.getTotDiasDireito() - totalDiasConsumidos > 0);
    int openAccumulatedPeriodsCount = otherOpenPeriodsCount + (currentOpen ? 1 : 0);

    int parcelasViolandoPrescricaoCount = 0;
    if (d != null && d.getDtPrescricao() != null && ctx.parcelasDoPeriodo() != null) {
      for (FeriasGozo fg : ctx.parcelasDoPeriodo()) {
        if (fg == null || "CANCELADA".equalsIgnoreCase(fg.getSituacao())) {
          continue;
        }
        LocalDate endRef = resolveFeriasEffectiveEndDate(fg);
        boolean violates =
            "I".equalsIgnoreCase(effective.criterioPrescricao())
                ? (fg.getDtini() != null && fg.getDtini().isAfter(d.getDtPrescricao()))
                : (endRef != null && endRef.isAfter(d.getDtPrescricao()));
        if (violates) {
          parcelasViolandoPrescricaoCount++;
        }
      }
    }

    int finalMaxDiasDireito = maxDiasDireitoPelaEscalaFaltas;
    int finalOverlappingPeriodsCount = overlappingPeriodsCount;
    int finalParcelasViolandoPrescricaoCount = parcelasViolandoPrescricaoCount;
    root.set(
        "derived",
        PraxisFactTreeBuilder.create()
            .put("faltasNoPeriodoCount", faltasApuradas)
            .put("diasAfastamentoNoPeriodoCount", diasAfastEfetivos)
            .put("maxDiasDireitoPelaEscalaFaltas", finalMaxDiasDireito)
            .put("totalDiasConsumidos", totalDiasConsumidos)
            .put("openAccumulatedPeriodsCount", openAccumulatedPeriodsCount)
            .put("overlappingPeriodsCount", finalOverlappingPeriodsCount)
            .put("parcelasViolandoPrescricaoCount", finalParcelasViolandoPrescricaoCount)
            .build());

    return root;
  }

  public static ObjectNode ofFeriasGozo(
      EffectiveVacationParameterResolver vacationParameterResolver, FeriasGozoRuleContext ctx) {
    FeriasGozo d = ctx.draft();
    PeriodoAquisitivoFerias pa = ctx.periodoAquisitivo();
    Vinculo v = ctx.vinculo();
    Integer empCodigo = d != null && d.getEmpCodigo() != null ? d.getEmpCodigo() : 1;

    EffectiveParametroFeriasFacts effective =
        vacationParameterResolver.resolveEffectiveFacts(
            ctx.parametroCandidates(),
            ctx.resolvedParametroBase(),
            pa,
            v,
            d != null ? d.getDtini() : null);

    LocalDate dtfimEfetivo = resolveFeriasEffectiveEndDate(d);
    int ordemNoVinculo = resolveOrdemPaNoVinculo(pa, ctx);

    ObjectNode root = PraxisFactTreeBuilder.create()
        .putDraft(node -> populateFeriasGozoDraft(node, d, pa, dtfimEfetivo))
        .putConcept("vinculo", node -> populateGozoVinculo(node, v))
        .putConcept("substituto", node -> populateGozoSubstituto(node, d, ctx, empCodigo))
        .putConcept("pa", node -> populateGozoPa(node, pa, d, ordemNoVinculo))
        .putConcept(
            "effectiveParametroConta", node -> populateEffectiveParametroNode(node, effective))
        .putConcept("tenant", node -> node.put("empCodigo", empCodigo))
        .putContext(node -> node.put("operation", FeriasGozoRuleSet.OPERATION))
        .build();

    int overlappingNormalFrequenciasCount =
        countOverlappingNormalFrequencias(d, dtfimEfetivo, ctx.activeFrequencias());
    int overlappingAfastamentosCount =
        countOverlappingAfastamentos(d, dtfimEfetivo, ctx.activeAfastamentos());

    ArrayNode existingNode = root.putArray("existingParcelas");
    int overlappingFeriasGozoCount = 0;
    int duplicateOrdemParcelaCount = 0;
    int otherParcelasWithAbonoConstCount = 0;
    int otherParcelasWithAdiant13InYearCount = 0;
    int otherParcelasWithAbonoPecuniarioInPaCount = 0;
    int otherParcelasNoPaCount = 0;
    int otherDiasGozadosPa = 0;
    int otherDiasAbonoPa = 0;
    int maxOtherParcelaDiasNoPa = 0;

    if (ctx.existingParcelasDoVinculo() != null) {
      for (FeriasGozo cand : ctx.existingParcelasDoVinculo()) {
        if (cand == null) {
          continue;
        }
        existingNode.add(
            PraxisFactTreeBuilder.create()
                .put("id", cand.getId())
                .put("periodoAquisitivoId", cand.getPeriodoAquisitivoId())
                .put("chavePeraqfer", cand.getChavePeraqfer())
                .put("ordemParcela", cand.getOrdemParcela())
                .put("dtini", cand.getDtini())
                .put("dtprevfim", cand.getDtprevfim())
                .put("dtfim", cand.getDtfim())
                .put("diasGozo", cand.getDiasGozo())
                .put("abonoPecuniario", cand.getAbonoPecuniario())
                .put("diasAbonoVendidos", cand.getDiasAbonoVendidos())
                .put("situacao", cand.getSituacao())
                .build());

        if (d != null && d.getId() != null && Objects.equals(d.getId(), cand.getId())) {
          continue;
        }
        if ("CANCELADA".equalsIgnoreCase(cand.getSituacao())) {
          continue;
        }
        LocalDate candEnd = resolveFeriasEffectiveEndDate(cand);
        if (d != null
            && d.getDtini() != null
            && dtfimEfetivo != null
            && cand.getDtini() != null
            && candEnd != null
            && !d.getDtini().isAfter(candEnd)
            && !cand.getDtini().isAfter(dtfimEfetivo)) {
          overlappingFeriasGozoCount++;
        }

        if (d != null
            && d.getDtini() != null
            && cand.getDtini() != null
            && d.getDtini().getYear() == cand.getDtini().getYear()
            && Boolean.TRUE.equals(cand.getAdiantamento13())) {
          otherParcelasWithAdiant13InYearCount++;
        }

        boolean samePa =
            (d != null
                    && d.getPeriodoAquisitivoId() != null
                    && Objects.equals(d.getPeriodoAquisitivoId(), cand.getPeriodoAquisitivoId()))
                || (d != null
                    && d.getChavePeraqfer() != null
                    && Objects.equals(d.getChavePeraqfer(), cand.getChavePeraqfer()));
        if (samePa) {
          otherParcelasNoPaCount++;
          int candGozo = cand.getDiasGozo() != null ? cand.getDiasGozo() : 0;
          int candAbono =
              Boolean.TRUE.equals(cand.getAbonoPecuniario()) && cand.getDiasAbonoVendidos() != null
                  ? cand.getDiasAbonoVendidos()
                  : 0;
          otherDiasGozadosPa += candGozo;
          otherDiasAbonoPa += candAbono;
          if (candGozo > maxOtherParcelaDiasNoPa) {
            maxOtherParcelaDiasNoPa = candGozo;
          }
          if (d != null
              && d.getOrdemParcela() != null
              && Objects.equals(d.getOrdemParcela(), cand.getOrdemParcela())) {
            duplicateOrdemParcelaCount++;
          }
          if (Boolean.TRUE.equals(cand.getAbonoConstitucional())) {
            otherParcelasWithAbonoConstCount++;
          }
          if (Boolean.TRUE.equals(cand.getAbonoPecuniario()) || candAbono > 0) {
            otherParcelasWithAbonoPecuniarioInPaCount++;
          }
        }
      }
    }

    boolean currentActive = d != null && !"CANCELADA".equalsIgnoreCase(d.getSituacao());
    int currentGozo = currentActive && d.getDiasGozo() != null ? d.getDiasGozo() : 0;
    int currentAbono =
        currentActive
                && Boolean.TRUE.equals(d.getAbonoPecuniario())
                && d.getDiasAbonoVendidos() != null
            ? d.getDiasAbonoVendidos()
            : 0;
    int totalParcelasNoPa = otherParcelasNoPaCount + (currentActive ? 1 : 0);
    int totalDiasGozadosPa = otherDiasGozadosPa + currentGozo;
    int totalDiasAbonoPa = otherDiasAbonoPa + currentAbono;
    int diasDobroPa =
        pa != null && pa.getDiasConvertidosDobro() != null ? pa.getDiasConvertidosDobro() : 0;
    int totalDiasConsumidosPa = totalDiasGozadosPa + totalDiasAbonoPa + diasDobroPa;
    int totDiasDireitoPa =
        pa != null && pa.getTotDiasDireito() != null
            ? pa.getTotDiasDireito()
            : (d != null && d.getTotDiasDireitoPa() != null ? d.getTotDiasDireitoPa() : 30);
    int saldoRemanescentePa = totDiasDireitoPa - totalDiasConsumidosPa;
    int maxParcelaDiasNoPa = Math.max(maxOtherParcelaDiasNoPa, currentGozo);

    boolean violatesDtPrescricao = false;
    if (pa != null && pa.getDtPrescricao() != null && d != null && d.getDtini() != null) {
      violatesDtPrescricao =
          "I".equalsIgnoreCase(effective.criterioPrescricao())
              ? d.getDtini().isAfter(pa.getDtPrescricao())
              : (dtfimEfetivo != null && dtfimEfetivo.isAfter(pa.getDtPrescricao()));
    }

    Long antecedenciaDias = null;
    if (d != null && d.getDtVendaAbono() != null && d.getDtini() != null) {
      antecedenciaDias = ChronoUnit.DAYS.between(d.getDtVendaAbono(), d.getDtini());
    }

    Integer maxDiasAbonoPelaFracao = null;
    if (effective.numDiasAbonoFer() != null
        && effective.denDiasAbonoFer() != null
        && effective.denDiasAbonoFer() > 0) {
      int baseCalculo =
          "G".equalsIgnoreCase(effective.abonoDirGozo())
              ? (d != null && d.getDiasGozo() != null ? d.getDiasGozo() : 0)
              : totDiasDireitoPa;
      maxDiasAbonoPelaFracao =
          (baseCalculo * effective.numDiasAbonoFer()) / effective.denDiasAbonoFer();
    }

    root.set(
        "derived",
        PraxisFactTreeBuilder.create()
            .put("antecedenciaDias", antecedenciaDias)
            .put("maxDiasAbonoPelaFracao", maxDiasAbonoPelaFracao)
            .put("overlappingNormalFrequenciasCount", overlappingNormalFrequenciasCount)
            .put("overlappingAfastamentosCount", overlappingAfastamentosCount)
            .put("overlappingFeriasGozoCount", overlappingFeriasGozoCount)
            .put("duplicateOrdemParcelaCount", duplicateOrdemParcelaCount)
            .put("otherParcelasWithAbonoConstCount", otherParcelasWithAbonoConstCount)
            .put("otherParcelasWithAdiant13InYearCount", otherParcelasWithAdiant13InYearCount)
            .put(
                "otherParcelasWithAbonoPecuniarioInPaCount",
                otherParcelasWithAbonoPecuniarioInPaCount)
            .put("violatesDtPrescricao", violatesDtPrescricao)
            .put("totalParcelasNoPa", totalParcelasNoPa)
            .put("totalDiasGozadosPa", totalDiasGozadosPa)
            .put("totalDiasAbonoPa", totalDiasAbonoPa)
            .put("totalDiasConsumidosPa", totalDiasConsumidosPa)
            .put("saldoRemanescentePa", saldoRemanescentePa)
            .put("maxParcelaDiasNoPa", maxParcelaDiasNoPa)
            .put("enforceStrictResidualBalance", ctx.enforceStrictResidualBalance())
            .build());

    return root;
  }

  private static void populateParametroDraft(
      PraxisFactTreeBuilder draftNode, ParametroContagemFerias d) {
    if (d == null) {
      return;
    }
    draftNode
        .put("id", d.getId())
        .put("empCodigo", d.getEmpCodigo())
        .put("regimeJuridico", d.getRegimeJuridico())
        .put("tipoVinculo", d.getTipoVinculo())
        .put("categoria", d.getCategoria())
        .put("subcategoria", d.getSubcategoria())
        .put("dtini", d.getDtini())
        .put("dtfim", d.getDtfim())
        .put("temFerias", d.getTemFerias())
        .put("totDiasFer", d.getTotDiasFer())
        .put("minPerFer", d.getMinPerFer())
        .put("maxPerFer", d.getMaxPerFer())
        .put("minDiasPerFer", d.getMinDiasPerFer())
        .put("maxDiasPerFer", d.getMaxDiasPerFer())
        .put("minDiasPerPrincFer", d.getMinDiasPerPrincFer())
        .put("abonoDirGozo", d.getAbonoDirGozo())
        .put("numDiasAbonoFer", d.getNumDiasAbonoFer())
        .put("denDiasAbonoFer", d.getDenDiasAbonoFer())
        .put("minDiasPvendFer", d.getMinDiasPvendFer())
        .put("maxDiasVendFer", d.getMaxDiasVendFer())
        .put("abonoConstFer", d.getAbonoConstFer())
        .put("adiant13Fer", d.getAdiant13Fer())
        .put("adiantSalFer", d.getAdiantSalFer())
        .put("emprestFer", d.getEmprestFer())
        .put("permiteGozoAntesPaFer", d.getPermiteGozoAntesPaFer())
        .put("permiteFerPrimPer", d.getPermiteFerPrimPer())
        .put("permiteFerSegunPer", d.getPermiteFerSegunPer())
        .put("permiteFerTercPer", d.getPermiteFerTercPer())
        .put("permiteFerPerInc", d.getPermiteFerPerInc())
        .put("criterioPrescricao", d.getCriterioPrescricao())
        .put("permiteGozoAposPrescricao", d.getPermiteGozoAposPrescricao())
        .put("permiteLancConcPa", d.getPermiteLancConcPa())
        .put("exigeExercicio", d.getExigeExercicio())
        .put("semPgtoMulta", d.getSemPgtoMulta())
        .put("contDobroFerias", d.getContDobroFerias())
        .put("minDiasPeraqfer", d.getMinDiasPeraqfer())
        .put("maxDiasPeraqfer", d.getMaxDiasPeraqfer())
        .put("temLicEsp", d.getTemLicEsp())
        .put("anosLicEsp", d.getAnosLicEsp())
        .put("mesesLicEspPer", d.getMesesLicEspPer())
        .put("vendLicEsp", d.getVendLicEsp())
        .put("abonoDirGozoLicEsp", d.getAbonoDirGozoLicEsp())
        .put("minDiasPvendLicEsp", d.getMinDiasPvendLicEsp())
        .put("maxDiasVendLicEsp", d.getMaxDiasVendLicEsp())
        .put("numDiasAbonoLicEsp", d.getNumDiasAbonoLicEsp())
        .put("denDiasAbonoLicEsp", d.getDenDiasAbonoLicEsp())
        .put("contDobroLicEsp", d.getContDobroLicEsp())
        .put("minDiasPcontDobLicEsp", d.getMinDiasPcontDobLicEsp())
        .put("maxDiasContDobLicEsp", d.getMaxDiasContDobLicEsp())
        .put("temProgressao", d.getTemProgressao())
        .put("anosProgressao", d.getAnosProgressao())
        .put("temTempoServ", d.getTemTempoServ())
        .put("anos1TempoServ", d.getAnos1TempoServ())
        .put("temSextaParte", d.getTemSextaParte())
        .put("anosSextaParte", d.getAnosSextaParte())
        .put("documentoLegalFeriasId", d.getDocumentoLegalFeriasId())
        .put("documentoLegalProgressaoId", d.getDocumentoLegalProgressaoId())
        .put("observacoes", d.getObservacoes())
        .put("active", d.getActive() != null ? d.getActive() : Boolean.TRUE);
  }

  private static void populatePeriodoAquisitivoDraft(
      PraxisFactTreeBuilder draftNode,
      PeriodoAquisitivoFerias d,
      PeriodoAquisitivoFeriasRuleContext ctx,
      int faltasApuradas,
      int diasAfastEfetivos) {
    boolean lockedAverbacaoMutation = ctx.lockedAverbacaoMutation();
    if (d != null) {
      if (ctx.previousSnapshot() != null
          && Boolean.TRUE.equals(ctx.previousSnapshot().getGeradoPorAverbacao())) {
        PeriodoAquisitivoFerias prev = ctx.previousSnapshot();
        if (!Objects.equals(prev.getDtini(), d.getDtini())
            || !Objects.equals(prev.getDtfim(), d.getDtfim())
            || !Objects.equals(prev.getChave(), d.getChave())) {
          lockedAverbacaoMutation = true;
        }
      }
      draftNode
          .put("id", d.getId())
          .put("empCodigo", d.getEmpCodigo())
          .put("numfunc", d.getNumfunc())
          .put("numvinc", d.getNumvinc())
          .put("chave", d.getChave())
          .put("exercicio", d.getExercicio())
          .put("dtini", d.getDtini())
          .put("dtfim", d.getDtfim())
          .put("totDiasDireito", d.getTotDiasDireito() != null ? d.getTotDiasDireito() : 0)
          .put("faltasApuradas", faltasApuradas)
          .put("diasAfastamentoSuspensivo", diasAfastEfetivos)
          .put("diasGozados", d.getDiasGozados() != null ? d.getDiasGozados() : 0)
          .put("diasVendidosAbono", d.getDiasVendidosAbono() != null ? d.getDiasVendidosAbono() : 0)
          .put(
              "diasConvertidosDobro",
              d.getDiasConvertidosDobro() != null ? d.getDiasConvertidosDobro() : 0)
          .put(
              "saldoDiasDisponivel",
              d.getSaldoDiasDisponivel() != null ? d.getSaldoDiasDisponivel() : 0)
          .put("situacao", d.getSituacao())
          .put("dtPrescricao", d.getDtPrescricao())
          .put("criterioPrescricao", d.getCriterioPrescricao())
          .put("permiteGozoPosPrescricao", d.getPermiteGozoPosPrescricao())
          .put("dtPgtoComMulta", d.getDtPgtoComMulta())
          .put("semPgtoMulta", d.getSemPgtoMulta())
          .put("maxPerFer", d.getMaxPerFer())
          .put("minPerFer", d.getMinPerFer())
          .put("maxDiasPerFer", d.getMaxDiasPerFer())
          .put("minDiasPerFer", d.getMinDiasPerFer())
          .put("minDiasPerPrincFer", d.getMinDiasPerPrincFer())
          .put("abonoDirGozo", d.getAbonoDirGozo())
          .put("abonoConstFer", d.getAbonoConstFer())
          .put("adiant13Fer", d.getAdiant13Fer())
          .put("adiantSalFer", d.getAdiantSalFer())
          .put("emprestFer", d.getEmprestFer())
          .put("permiteFerEstePer", d.getPermiteFerEstePer())
          .put("maxDiasVendFer", d.getMaxDiasVendFer())
          .put("minDiasPvendFer", d.getMinDiasPvendFer())
          .put("numDiasAbonoFer", d.getNumDiasAbonoFer())
          .put("denDiasAbonoFer", d.getDenDiasAbonoFer())
          .put("contDobroFerias", d.getContDobroFerias())
          .put("minDiasPeraqfer", d.getMinDiasPeraqfer())
          .put("maxDiasPeraqfer", d.getMaxDiasPeraqfer())
          .put(
              "geradoPorAverbacao",
              d.getGeradoPorAverbacao() != null ? d.getGeradoPorAverbacao() : Boolean.FALSE)
          .put("documentoLegalId", d.getDocumentoLegalId())
          .put("atoPublicacaoId", d.getAtoPublicacaoId())
          .put("observacoes", d.getObservacoes())
          .put("lockedAverbacaoMutation", lockedAverbacaoMutation);
    } else {
      draftNode
          .put("faltasApuradas", faltasApuradas)
          .put("diasAfastamentoSuspensivo", diasAfastEfetivos);
    }
  }

  private static void populatePeriodoVinculo(PraxisFactTreeBuilder vinculoNode, Vinculo v) {
    if (v != null) {
      vinculoNode
          .put("dtexerc", v.getDtexerc())
          .put("dtvac", v.getDtvac())
          .put("dtaposent", v.getDtaposent())
          .put("situacao", v.getSituacao())
          .put("regimeJuridico", v.getRegimejur())
          .put("tipoVinculo", v.getTipovinc())
          .put("categoria", v.getCategoria())
          .put("subcategoria", v.getSubcategoria());
    } else {
      vinculoNode
          .put("dtexerc", null)
          .put("dtvac", null)
          .put("dtaposent", null)
          .put("situacao", null);
    }
  }

  private static void populateFeriasGozoDraft(
      PraxisFactTreeBuilder draftNode,
      FeriasGozo d,
      PeriodoAquisitivoFerias pa,
      LocalDate dtfimEfetivo) {
    if (d == null) {
      return;
    }
    draftNode
        .put("id", d.getId())
        .put("empCodigo", d.getEmpCodigo())
        .put("numfunc", d.getNumfunc())
        .put("numvinc", d.getNumvinc())
        .put("periodoAquisitivoId", d.getPeriodoAquisitivoId())
        .put("chavePeraqfer", d.getChavePeraqfer())
        .put("ordemParcela", d.getOrdemParcela() != null ? d.getOrdemParcela() : 1)
        .put("dtini", d.getDtini())
        .put("dtprevfim", d.getDtprevfim())
        .put("dtfim", d.getDtfim())
        .put("dtfimEfetivo", dtfimEfetivo)
        .put("diasGozo", d.getDiasGozo())
        .put(
            "totDiasDireitoPa",
            pa != null && pa.getTotDiasDireito() != null
                ? pa.getTotDiasDireito()
                : d.getTotDiasDireitoPa())
        .put("abonoPecuniario", d.getAbonoPecuniario())
        .put("iniFimAbono", d.getIniFimAbono())
        .put("diasAbonoVendidos", d.getDiasAbonoVendidos() != null ? d.getDiasAbonoVendidos() : 0)
        .put("dtVendaAbono", d.getDtVendaAbono())
        .put("dtPagtoAbono", d.getDtPagtoAbono())
        .put("abonoConstitucional", d.getAbonoConstitucional())
        .put("adiantamento13", d.getAdiantamento13())
        .put("adiantamentoSalario", d.getAdiantamentoSalario())
        .put("emprestimoFerias", d.getEmprestimoFerias())
        .put("mesAnoFolha", d.getMesAnoFolha())
        .put("numFolha", d.getNumFolha())
        .put("situacao", d.getSituacao())
        .put("dtInterrupcao", d.getDtInterrupcao())
        .put("motivoInterrupcao", d.getMotivoInterrupcao())
        .put("numfuncSubstituto", d.getNumfuncSubstituto())
        .put("numvincSubstituto", d.getNumvincSubstituto())
        .put("documentoLegalId", d.getDocumentoLegalId())
        .put("atoPublicacaoId", d.getAtoPublicacaoId())
        .put("observacoes", d.getObservacoes());
  }

  private static void populateGozoVinculo(PraxisFactTreeBuilder vinculoNode, Vinculo v) {
    if (v != null) {
      vinculoNode
          .put("dtexerc", v.getDtexerc())
          .put("dtvac", v.getDtvac())
          .put("dtaposent", v.getDtaposent())
          .put("situacao", v.getSituacao());
    } else {
      vinculoNode
          .put("dtexerc", null)
          .put("dtvac", null)
          .put("dtaposent", null)
          .put("situacao", null);
    }
  }

  private static void populateGozoSubstituto(
      PraxisFactTreeBuilder substNode,
      FeriasGozo d,
      FeriasGozoRuleContext ctx,
      Integer empCodigo) {
    Long subFunc = d != null ? d.getNumfuncSubstituto() : null;
    Integer subVinc = d != null ? d.getNumvincSubstituto() : null;
    boolean pairComplete =
        (subFunc == null && subVinc == null)
            || (subFunc != null && subVinc != null && subFunc > 0 && subVinc > 0);
    boolean selfSubstitution =
        subFunc != null
            && subVinc != null
            && d != null
            && Objects.equals(subFunc, d.getNumfunc())
            && Objects.equals(subVinc, d.getNumvinc());
    boolean vinculoValidoNoOrgao;
    if (subFunc == null && subVinc == null) {
      vinculoValidoNoOrgao = true;
    } else if (ctx.substitutoVinculoValidoOverride() != null) {
      vinculoValidoNoOrgao = ctx.substitutoVinculoValidoOverride();
    } else if (ctx.substitutoVinculo() != null) {
      vinculoValidoNoOrgao =
          Objects.equals(ctx.substitutoVinculo().getEmpCodigo(), empCodigo)
              && !"VACANTE".equalsIgnoreCase(ctx.substitutoVinculo().getSituacao());
    } else {
      vinculoValidoNoOrgao = true;
    }
    substNode
        .put("pairComplete", pairComplete)
        .put("selfSubstitution", selfSubstitution)
        .put("vinculoValidoNoOrgao", vinculoValidoNoOrgao)
        .put("activeSubstitutionConflict", ctx.activeSubstitutionConflict());
  }

  private static void populateGozoPa(
      PraxisFactTreeBuilder paNode,
      PeriodoAquisitivoFerias pa,
      FeriasGozo d,
      int ordemNoVinculo) {
    if (pa != null) {
      boolean apto =
          pa.getTotDiasDireito() != null
              && pa.getTotDiasDireito() > 0
              && (pa.getSituacao() == null
                  || !Set.of("PRESCRITO", "CANCELADO", "CONVERTIDO_DOBRO", "SUSPENSO")
                      .contains(pa.getSituacao().toUpperCase()));
      paNode
          .put("id", pa.getId())
          .put("chave", pa.getChave())
          .put("ordemNoVinculo", ordemNoVinculo)
          .put("dtini", pa.getDtini())
          .put("dtfim", pa.getDtfim())
          .put("dtPrescricao", pa.getDtPrescricao())
          .put("totDiasDireito", pa.getTotDiasDireito() != null ? pa.getTotDiasDireito() : 30)
          .put(
              "diasConvertidosDobro",
              pa.getDiasConvertidosDobro() != null ? pa.getDiasConvertidosDobro() : 0)
          .put("situacao", pa.getSituacao())
          .put("aptoConcessao", apto);
    } else {
      paNode
          .put("id", null)
          .put("chave", null)
          .put("ordemNoVinculo", ordemNoVinculo)
          .put("dtini", null)
          .put("dtfim", null)
          .put("dtPrescricao", null)
          .put(
              "totDiasDireito",
              d != null && d.getTotDiasDireitoPa() != null ? d.getTotDiasDireitoPa() : 30)
          .put("diasConvertidosDobro", 0)
          .put("situacao", null)
          .put("aptoConcessao", false);
    }
  }

  private static void populateEffectiveParametroNode(
      PraxisFactTreeBuilder effNode, EffectiveParametroFeriasFacts eff) {
    effNode
        .put("parametroEncontrado", eff.parametroEncontrado())
        .put("parametroId", eff.parametroContagemId())
        .put("escopoGlobal", eff.empCodigo() == null)
        .put("temFerias", eff.temFerias())
        .put("totDiasFer", eff.totDiasFer())
        .put("maxPerFer", eff.maxPerFer())
        .put("minPerFer", eff.minPerFer())
        .put("maxDiasPerFer", eff.maxDiasPerFer())
        .put("minDiasPerFer", eff.minDiasPerFer())
        .put("minDiasPerPrincFer", eff.minDiasPerPrincFer())
        .put("abonoDirGozo", eff.abonoDirGozo())
        .put("permiteAbonoPecuniario", eff.permiteAbonoPecuniario())
        .put("numDiasAbonoFer", eff.numDiasAbonoFer())
        .put("denDiasAbonoFer", eff.denDiasAbonoFer())
        .put("maxDiasVendFer", eff.maxDiasVendFer())
        .put("minDiasPvendFer", eff.minDiasPvendFer())
        .put("abonoConstFer", eff.abonoConstFer())
        .put("adiant13Fer", eff.adiant13Fer())
        .put("adiantSalFer", eff.adiantSalFer())
        .put("emprestFer", eff.emprestFer())
        .put("contDobroFerias", eff.contDobroFerias())
        .put("minDiasPeraqfer", eff.minDiasPeraqfer())
        .put("maxDiasPeraqfer", eff.maxDiasPeraqfer())
        .put("criterioPrescricao", eff.criterioPrescricao())
        .put("permiteGozoAposPrescricao", eff.permiteGozoAposPrescricao())
        .put("semPgtoMulta", eff.semPgtoMulta())
        .put("permiteGozoAntesPaFer", eff.permiteGozoAntesPaFer())
        .put("permiteFerPrimPer", eff.permiteFerPrimPer())
        .put("permiteFerSegunPer", eff.permiteFerSegunPer())
        .put("permiteFerTercPer", eff.permiteFerTercPer())
        .put("permiteFerPerInc", eff.permiteFerPerInc())
        .put("permiteLancConcPa", eff.permiteLancConcPa());
  }

  private static boolean sameParametroContagemScope(
      ParametroContagemFerias a, ParametroContagemFerias b) {
    return Objects.equals(a.getEmpCodigo(), b.getEmpCodigo())
        && Objects.equals(a.getRegimeJuridico(), b.getRegimeJuridico())
        && Objects.equals(a.getTipoVinculo(), b.getTipoVinculo())
        && Objects.equals(a.getCategoria(), b.getCategoria())
        && Objects.equals(a.getSubcategoria(), b.getSubcategoria());
  }

  private static boolean isOpenPeriodoSituacao(String situacao) {
    if (situacao == null) {
      return true;
    }
    return Set.of("EM_AQUISICAO", "DISPONIVEL", "ADQUIRIDO", "PARCIALMENTE_GOZADO")
        .contains(situacao.toUpperCase());
  }

  private static int countUnjustifiedAbsencesInPa(
      PeriodoAquisitivoFerias pa, List<Frequencia> frequencias) {
    if (pa == null
        || pa.getDtini() == null
        || pa.getDtfim() == null
        || frequencias == null
        || frequencias.isEmpty()) {
      return 0;
    }
    int total = 0;
    for (Frequencia f : frequencias) {
      if (f == null
          || "CANCELADO".equalsIgnoreCase(f.getSituacao())
          || "ESTORNADO".equalsIgnoreCase(f.getSituacao())) {
        continue;
      }
      boolean isFalta =
          "FALTA".equalsIgnoreCase(f.getTipofreq())
              || (f.getCodigoFrequenciaEntidade() != null
                  && f.getCodigoFrequenciaEntidade().getQtdFaltas() != null
                  && f.getCodigoFrequenciaEntidade().getQtdFaltas().signum() > 0);
      if (!isFalta || f.getDtini() == null) {
        continue;
      }
      LocalDate fEnd = f.getDtfim() != null ? f.getDtfim() : f.getDtini();
      LocalDate start = f.getDtini().isBefore(pa.getDtini()) ? pa.getDtini() : f.getDtini();
      LocalDate end = fEnd.isAfter(pa.getDtfim()) ? pa.getDtfim() : fEnd;
      if (!start.isAfter(end)) {
        int qtd =
            f.getQuantidade() != null && f.getQuantidade().intValue() > 0
                ? f.getQuantidade().intValue()
                : (int) (ChronoUnit.DAYS.between(start, end) + 1);
        total += qtd;
      }
    }
    return total;
  }

  private static int countSuspensiveLeaveDaysInPa(
      PeriodoAquisitivoFerias pa, List<Afastamento> afastamentos) {
    if (pa == null
        || pa.getDtini() == null
        || pa.getDtfim() == null
        || afastamentos == null
        || afastamentos.isEmpty()) {
      return 0;
    }
    int total = 0;
    for (Afastamento a : afastamentos) {
      if (a == null
          || "CANCELADO".equalsIgnoreCase(a.getSituacao())
          || "INTERROMPIDO".equalsIgnoreCase(a.getSituacao())
          || a.getDtini() == null) {
        continue;
      }
      if (a.getCodigoFrequenciaEntidade() != null
          && Boolean.TRUE.equals(a.getCodigoFrequenciaEntidade().getOnus())) {
        continue;
      }
      LocalDate aEnd =
          a.getDtfim() != null
              ? a.getDtfim()
              : (a.getDtprevfim() != null ? a.getDtprevfim() : pa.getDtfim());
      LocalDate start = a.getDtini().isBefore(pa.getDtini()) ? pa.getDtini() : a.getDtini();
      LocalDate end = aEnd.isAfter(pa.getDtfim()) ? pa.getDtfim() : aEnd;
      if (!start.isAfter(end)) {
        total += (int) (ChronoUnit.DAYS.between(start, end) + 1);
      }
    }
    return total;
  }

  private static LocalDate resolveFeriasEffectiveEndDate(FeriasGozo fg) {
    if (fg == null) {
      return null;
    }
    if ("INTERROMPIDA".equalsIgnoreCase(fg.getSituacao()) && fg.getDtInterrupcao() != null) {
      return fg.getDtInterrupcao();
    }
    return fg.getDtfim() != null ? fg.getDtfim() : fg.getDtprevfim();
  }

  private static int resolveOrdemPaNoVinculo(
      PeriodoAquisitivoFerias pa, FeriasGozoRuleContext ctx) {
    if (ctx.ordemPaNoVinculoOverride() != null && ctx.ordemPaNoVinculoOverride() > 0) {
      return ctx.ordemPaNoVinculoOverride();
    }
    if (pa == null) {
      return 1;
    }
    if (ctx.existingPeriodosDoVinculo() != null && !ctx.existingPeriodosDoVinculo().isEmpty()) {
      int rank = 1;
      for (PeriodoAquisitivoFerias other : ctx.existingPeriodosDoVinculo()) {
        if (other == null || "CANCELADO".equalsIgnoreCase(other.getSituacao())) {
          continue;
        }
        if (pa.getId() != null && Objects.equals(pa.getId(), other.getId())) {
          continue;
        }
        if (other.getDtini() != null
            && pa.getDtini() != null
            && other.getDtini().isBefore(pa.getDtini())) {
          rank++;
        } else if (other.getDtini() != null
            && pa.getDtini() != null
            && other.getDtini().isEqual(pa.getDtini())
            && other.getChave() != null
            && pa.getChave() != null
            && other.getChave() < pa.getChave()) {
          rank++;
        }
      }
      return Math.max(rank, pa.getChave() != null && pa.getChave() > 0 ? pa.getChave() : 1);
    }
    return pa.getChave() != null && pa.getChave() > 0 ? pa.getChave() : 1;
  }

  private static int countOverlappingNormalFrequencias(
      FeriasGozo d, LocalDate dtfimEfetivo, List<Frequencia> frequencias) {
    if (d == null
        || d.getDtini() == null
        || dtfimEfetivo == null
        || frequencias == null
        || frequencias.isEmpty()) {
      return 0;
    }
    int count = 0;
    for (Frequencia f : frequencias) {
      if (f == null
          || "CANCELADO".equalsIgnoreCase(f.getSituacao())
          || "ESTORNADO".equalsIgnoreCase(f.getSituacao())
          || f.getDtini() == null) {
        continue;
      }
      if (!"NORMAL".equalsIgnoreCase(f.getTipofreq())) {
        continue;
      }
      LocalDate fEnd = f.getDtfim() != null ? f.getDtfim() : f.getDtini();
      if (!d.getDtini().isAfter(fEnd) && !f.getDtini().isAfter(dtfimEfetivo)) {
        count++;
      }
    }
    return count;
  }

  private static int countOverlappingAfastamentos(
      FeriasGozo d, LocalDate dtfimEfetivo, List<Afastamento> afastamentos) {
    if (d == null
        || d.getDtini() == null
        || dtfimEfetivo == null
        || afastamentos == null
        || afastamentos.isEmpty()) {
      return 0;
    }
    int count = 0;
    for (Afastamento a : afastamentos) {
      if (a == null
          || "CANCELADO".equalsIgnoreCase(a.getSituacao())
          || a.getDtini() == null) {
        continue;
      }
      LocalDate aEnd =
          a.getDtfim() != null
              ? a.getDtfim()
              : (a.getDtprevfim() != null ? a.getDtprevfim() : OPEN_ENDED_DATE);
      if (!d.getDtini().isAfter(aEnd) && !a.getDtini().isAfter(dtfimEfetivo)) {
        count++;
      }
    }
    return count;
  }
}
