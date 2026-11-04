package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.domain.ParametroContagemFerias;
import br.com.praxis.ergonshowcase.domain.PeriodoAquisitivoFerias;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolvedor determinístico canônico do Merge com Precedência de Férias
 * ({@code PARAMETROS_CONTA (+) PERAQFER -> FERIAS}, Tarefa 24610 de {@code PCK_FERIAS.PKB} e
 * {@code PCK_PERAQFER.PKB}).
 *
 * <p>Executa em duas etapas determinísticas:
 * <ol>
 *   <li><b>Resolução por especificidade de {@link ParametroContagemFerias}:</b> localiza o
 *       parâmetro ativo vigente na data de referência ({@code pa.dtfim} ou {@code ferias.dtini})
 *       para a chave {@code (empCodigo, regimeJuridico, tipoVinculo, categoria, subcategoria)},
 *       com fallback para regra global ({@code empCodigo == null});
 *   <li><b>Merge de overrides individuais de {@link PeriodoAquisitivoFerias}:</b> onde {@code 'I'}
 *       (indiferente/herda) ou {@code null} preserva o atributo herdado de
 *       {@link ParametroContagemFerias}, enquanto {@code 'S'}, {@code 'N'}, {@code 'D'},
 *       {@code 'G'} ou valor numérico explícito no período aquisitivo sobrescreve a regra geral
 *       para aquele ciclo específico.
 * </ol>
 */
public final class EffectiveVacationParameterResolver {

  public record EffectiveParametroFeriasFacts(
      Long parametroContagemId,
      Integer empCodigo,
      boolean parametroEncontrado,
      boolean temFerias,
      Integer totDiasFer,
      Integer maxPerFer,
      Integer minPerFer,
      Integer maxDiasPerFer,
      Integer minDiasPerFer,
      Integer minDiasPerPrincFer,
      String abonoDirGozo,
      Integer numDiasAbonoFer,
      Integer denDiasAbonoFer,
      Integer maxDiasVendFer,
      Integer minDiasPvendFer,
      boolean abonoConstFer,
      boolean adiant13Fer,
      boolean adiantSalFer,
      boolean emprestFer,
      boolean contDobroFerias,
      Integer minDiasPeraqfer,
      Integer maxDiasPeraqfer,
      String criterioPrescricao,
      boolean permiteGozoAposPrescricao,
      boolean permiteGozoAntesPaFer,
      boolean permiteFerPrimPer,
      boolean permiteFerSegunPer,
      boolean permiteFerTercPer,
      boolean permiteFerPerInc,
      boolean permiteLancConcPa,
      boolean semPgtoMulta,
      boolean possuiOverridesIndividuais) {

    public boolean permiteGozoAntesTerminoPaParaOrdem(int ordemPaNoVinculo) {
      if (permiteGozoAntesPaFer) {
        return true;
      }
      return switch (ordemPaNoVinculo) {
        case 1 -> permiteFerPrimPer;
        case 2 -> permiteFerSegunPer;
        case 3 -> permiteFerTercPer;
        default -> permiteFerPerInc;
      };
    }

    public boolean permiteAbonoPecuniario() {
      return abonoDirGozo != null && !"N".equalsIgnoreCase(abonoDirGozo);
    }

    public ObjectNode toJsonNode(ObjectMapper mapper) {
      ObjectNode node = mapper.createObjectNode();
      putLong(node, "parametroContagemId", parametroContagemId);
      putInt(node, "empCodigo", empCodigo);
      node.put("parametroEncontrado", parametroEncontrado);
      node.put("temFerias", temFerias);
      putInt(node, "totDiasFer", totDiasFer);
      putInt(node, "maxPerFer", maxPerFer);
      putInt(node, "minPerFer", minPerFer);
      putInt(node, "maxDiasPerFer", maxDiasPerFer);
      putInt(node, "minDiasPerFer", minDiasPerFer);
      putInt(node, "minDiasPerPrincFer", minDiasPerPrincFer);
      putText(node, "abonoDirGozo", abonoDirGozo);
      putInt(node, "numDiasAbonoFer", numDiasAbonoFer);
      putInt(node, "denDiasAbonoFer", denDiasAbonoFer);
      putInt(node, "maxDiasVendFer", maxDiasVendFer);
      putInt(node, "minDiasPvendFer", minDiasPvendFer);
      node.put("abonoConstFer", abonoConstFer);
      node.put("adiant13Fer", adiant13Fer);
      node.put("adiantSalFer", adiantSalFer);
      node.put("emprestFer", emprestFer);
      node.put("contDobroFerias", contDobroFerias);
      putInt(node, "minDiasPeraqfer", minDiasPeraqfer);
      putInt(node, "maxDiasPeraqfer", maxDiasPeraqfer);
      putText(node, "criterioPrescricao", criterioPrescricao);
      node.put("permiteGozoAposPrescricao", permiteGozoAposPrescricao);
      node.put("permiteGozoAntesPaFer", permiteGozoAntesPaFer);
      node.put("permiteFerPrimPer", permiteFerPrimPer);
      node.put("permiteFerSegunPer", permiteFerSegunPer);
      node.put("permiteFerTercPer", permiteFerTercPer);
      node.put("permiteFerPerInc", permiteFerPerInc);
      node.put("permiteLancConcPa", permiteLancConcPa);
      node.put("semPgtoMulta", semPgtoMulta);
      node.put("possuiOverridesIndividuais", possuiOverridesIndividuais);
      node.put("permiteAbonoPecuniario", permiteAbonoPecuniario());
      return node;
    }
  }

  public Optional<ParametroContagemFerias> resolveActiveParameter(
      List<ParametroContagemFerias> candidates,
      Integer empCodigo,
      Vinculo vinculo,
      LocalDate referenceDate) {
    if (candidates == null || candidates.isEmpty()) return Optional.empty();
    Integer targetEmp = empCodigo != null ? empCodigo : vinculo == null ? null : vinculo.getEmpCodigo();
    var criteria = new java.util.ArrayList<org.praxisplatform.rules.parametric.EffectiveDatedMatrixMatcher.Criterion<ParametroContagemFerias>>();
    // Preserve the domain's ordered specificity and its treatment of absent vínculo dimensions.
    for (int bit : new int[] {16, 8, 4, 2, 1})
      criteria.add(new org.praxisplatform.rules.parametric.EffectiveDatedMatrixMatcher.Criterion<>(
          row -> (specificityScore(row, targetEmp, vinculo) & bit) == 0 ? null : Boolean.TRUE, Boolean.TRUE));
    var matcher = new org.praxisplatform.rules.parametric.EffectiveDatedMatrixMatcher<ParametroContagemFerias>(
        row -> referenceDate == null ? null : row.getDtini(),
        row -> referenceDate == null ? null : row.getDtfim(), criteria,
        Comparator.comparing(ParametroContagemFerias::getDtini, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(ParametroContagemFerias::getId, Comparator.nullsLast(Comparator.naturalOrder())));
    return matcher.match(candidates.stream().filter(Objects::nonNull)
        .filter(row -> Boolean.TRUE.equals(row.getActive())).filter(row -> matchesScope(row, targetEmp, vinculo)).toList(),
        referenceDate == null ? LocalDate.MIN : referenceDate);
  }

  public EffectiveParametroFeriasFacts resolveEffectiveFacts(
      List<ParametroContagemFerias> candidates,
      ParametroContagemFerias explicitBase,
      PeriodoAquisitivoFerias pa,
      Vinculo vinculo,
      LocalDate referenceDate) {
    LocalDate effectiveRefDate =
        referenceDate != null
            ? referenceDate
            : (pa != null && pa.getDtfim() != null
                ? pa.getDtfim()
                : (pa != null ? pa.getDtini() : null));
    Integer targetEmp =
        pa != null && pa.getEmpCodigo() != null
            ? pa.getEmpCodigo()
            : (vinculo != null
                ? vinculo.getEmpCodigo()
                : (explicitBase != null ? explicitBase.getEmpCodigo() : null));

    ParametroContagemFerias base =
        explicitBase != null
            ? explicitBase
            : resolveActiveParameter(candidates, targetEmp, vinculo, effectiveRefDate).orElse(null);

    boolean candidatesProvided =
        explicitBase != null || (candidates != null && !candidates.isEmpty());
    boolean parametroEncontrado = base != null || !candidatesProvided;
    boolean temFerias = base != null ? Boolean.TRUE.equals(base.getTemFerias()) : !candidatesProvided;

    Integer totDiasFer =
        base != null && base.getTotDiasFer() != null
            ? base.getTotDiasFer()
            : (pa != null && pa.getTotDiasDireito() != null && base == null
                ? Math.max(30, pa.getTotDiasDireito())
                : 30);

    Integer maxPerFer =
        firstNonNull(
            pa != null ? pa.getMaxPerFer() : null,
            base != null ? base.getMaxPerFer() : null,
            3);
    Integer minPerFer =
        firstNonNull(
            pa != null ? pa.getMinPerFer() : null,
            base != null ? base.getMinPerFer() : null,
            1);
    Integer maxDiasPerFer =
        firstNonNull(
            pa != null ? pa.getMaxDiasPerFer() : null,
            base != null ? base.getMaxDiasPerFer() : null,
            totDiasFer);
    Integer minDiasPerFer =
        firstNonNull(
            pa != null ? pa.getMinDiasPerFer() : null,
            base != null ? base.getMinDiasPerFer() : null);
    Integer minDiasPerPrincFer =
        firstNonNull(
            pa != null ? pa.getMinDiasPerPrincFer() : null,
            base != null ? base.getMinDiasPerPrincFer() : null);

    String abonoDirGozo = resolveAbonoDirGozo(pa, base);
    Integer numDiasAbonoFer =
        firstNonNull(
            pa != null ? pa.getNumDiasAbonoFer() : null,
            base != null ? base.getNumDiasAbonoFer() : null,
            "N".equalsIgnoreCase(abonoDirGozo) ? null : 1);
    Integer denDiasAbonoFer =
        firstNonNull(
            pa != null ? pa.getDenDiasAbonoFer() : null,
            base != null ? base.getDenDiasAbonoFer() : null,
            "N".equalsIgnoreCase(abonoDirGozo) ? null : 3);
    Integer maxDiasVendFer =
        firstNonNull(
            pa != null ? pa.getMaxDiasVendFer() : null,
            base != null ? base.getMaxDiasVendFer() : null);
    Integer minDiasPvendFer =
        firstNonNull(
            pa != null ? pa.getMinDiasPvendFer() : null,
            base != null ? base.getMinDiasPvendFer() : null);

    boolean abonoConstFer =
        mergeTriStateBoolean(
            pa != null ? pa.getAbonoConstFer() : null,
            base != null ? base.getAbonoConstFer() : null,
            true);
    boolean adiant13Fer =
        mergeTriStateBoolean(
            pa != null ? pa.getAdiant13Fer() : null,
            base != null ? base.getAdiant13Fer() : null,
            true);
    boolean adiantSalFer =
        mergeTriStateBoolean(
            pa != null ? pa.getAdiantSalFer() : null,
            base != null ? base.getAdiantSalFer() : null,
            true);
    boolean emprestFer =
        mergeTriStateBoolean(
            pa != null ? pa.getEmprestFer() : null,
            base != null ? base.getEmprestFer() : null,
            true);
    boolean contDobroFerias =
        mergeTriStateBoolean(
            pa != null ? pa.getContDobroFerias() : null,
            base != null ? base.getContDobroFerias() : null,
            base == null);

    Integer minDiasPeraqfer =
        firstNonNull(
            pa != null ? pa.getMinDiasPeraqfer() : null,
            base != null ? base.getMinDiasPeraqfer() : null);
    Integer maxDiasPeraqfer =
        firstNonNull(
            pa != null ? pa.getMaxDiasPeraqfer() : null,
            base != null ? base.getMaxDiasPeraqfer() : null);

    String criterioPrescricao = resolveCriterioPrescricao(pa, base);
    boolean permiteGozoAposPrescricao =
        mergeTriStateBoolean(
            pa != null ? pa.getPermiteGozoPosPrescricao() : null,
            base != null ? base.getPermiteGozoAposPrescricao() : null,
            false);

    String paPermiteEstePer =
        pa != null && pa.getPermiteFerEstePer() != null
            ? pa.getPermiteFerEstePer().trim().toUpperCase()
            : "I";
    boolean overridePermiteEstePer =
        "S".equals(paPermiteEstePer) || "N".equals(paPermiteEstePer);
    boolean basePermiteAntesPa =
        base != null ? Boolean.TRUE.equals(base.getPermiteGozoAntesPaFer()) : true;
    boolean permiteGozoAntesPaFer =
        overridePermiteEstePer ? "S".equals(paPermiteEstePer) : basePermiteAntesPa;

    boolean permiteFerPrimPer =
        overridePermiteEstePer
            ? "S".equals(paPermiteEstePer)
            : (base != null && base.getPermiteFerPrimPer() != null
                ? base.getPermiteFerPrimPer()
                : basePermiteAntesPa);
    boolean permiteFerSegunPer =
        overridePermiteEstePer
            ? "S".equals(paPermiteEstePer)
            : (base != null && base.getPermiteFerSegunPer() != null
                ? base.getPermiteFerSegunPer()
                : basePermiteAntesPa);
    boolean permiteFerTercPer =
        overridePermiteEstePer
            ? "S".equals(paPermiteEstePer)
            : (base != null && base.getPermiteFerTercPer() != null
                ? base.getPermiteFerTercPer()
                : basePermiteAntesPa);
    boolean permiteFerPerInc =
        overridePermiteEstePer
            ? "S".equals(paPermiteEstePer)
            : (base != null && base.getPermiteFerPerInc() != null
                ? base.getPermiteFerPerInc()
                : basePermiteAntesPa);

    boolean permiteLancConcPa =
        base != null ? Boolean.TRUE.equals(base.getPermiteLancConcPa()) : true;
    boolean semPgtoMulta =
        mergeTriStateBoolean(
            pa != null ? pa.getSemPgtoMulta() : null,
            base != null ? base.getSemPgtoMulta() : null,
            false);

    return new EffectiveParametroFeriasFacts(
        base != null ? base.getId() : null,
        targetEmp,
        parametroEncontrado,
        temFerias,
        totDiasFer,
        maxPerFer,
        minPerFer,
        maxDiasPerFer,
        minDiasPerFer,
        minDiasPerPrincFer,
        abonoDirGozo,
        numDiasAbonoFer,
        denDiasAbonoFer,
        maxDiasVendFer,
        minDiasPvendFer,
        abonoConstFer,
        adiant13Fer,
        adiantSalFer,
        emprestFer,
        contDobroFerias,
        minDiasPeraqfer,
        maxDiasPeraqfer,
        criterioPrescricao,
        permiteGozoAposPrescricao,
        permiteGozoAntesPaFer,
        permiteFerPrimPer,
        permiteFerSegunPer,
        permiteFerTercPer,
        permiteFerPerInc,
        permiteLancConcPa,
        semPgtoMulta,
        hasIndividualOverrides(pa));
  }

  public List<ParametroContagemFerias> loadCandidates(EntityManager em, Vinculo vinculo) {
    if (em == null || vinculo == null || !notBlank(vinculo.getRegimejur()) || !notBlank(vinculo.getTipovinc())) {
      return List.of();
    }
    var q =
        em.createQuery(
            "select x from ParametroContagemFerias x where x.active=true and (x.empCodigo=:org or x.empCodigo is null) and x.regimeJuridico=:rj and x.tipoVinculo=:tv",
            ParametroContagemFerias.class);
    if (q == null) {
      return List.of();
    }
    var r1 = q.setParameter("org", vinculo.getEmpCodigo()).setParameter("rj", vinculo.getRegimejur());
    var r2 = r1 != null ? r1.setParameter("tv", vinculo.getTipovinc()) : null;
    var flushed = r2 != null ? r2.setFlushMode(FlushModeType.COMMIT) : null;
    var list =
        (flushed != null ? flushed : r2) != null
            ? (flushed != null ? flushed : r2).getResultList()
            : List.<ParametroContagemFerias>of();
    return list != null ? list : List.of();
  }

  public ParametroContagemFerias resolveActiveParameter(
      EntityManager em, Vinculo vinculo, LocalDate referenceDate) {
    List<ParametroContagemFerias> candidates = loadCandidates(em, vinculo);
    return resolveActiveParameter(
            candidates,
            vinculo != null ? vinculo.getEmpCodigo() : null,
            vinculo,
            referenceDate)
        .orElse(null);
  }

  public EffectiveParametroFeriasFacts resolveAndMerge(
      EntityManager em, PeriodoAquisitivoFerias pa, LocalDate referenceDate) {
    Vinculo vinculo = null;
    if (em != null && pa != null && pa.getNumfunc() != null && pa.getNumvinc() != null) {
      vinculo = em.find(Vinculo.class, new VinculoId(pa.getNumfunc(), pa.getNumvinc()));
    }
    List<ParametroContagemFerias> candidates = loadCandidates(em, vinculo);
    return resolveEffectiveFacts(candidates, null, pa, vinculo, referenceDate);
  }

  public static int computeRightDaysAfterAbsences(int totDiasFerBase, int faltasInjustificadas) {
    return calcularDiasDireitoPorFaltas(totDiasFerBase, faltasInjustificadas);
  }

  public static int computeSuggestedAbonoDays(
      EffectiveParametroFeriasFacts effective, Integer totDiasDireitoPa, Integer baseDiasDisponiveis) {
    return calcularDiasAbonoPecuniarioSugerido(
        effective,
        totDiasDireitoPa != null ? totDiasDireitoPa : 30,
        baseDiasDisponiveis != null ? baseDiasDisponiveis : 0);
  }

  /**
   * Calcula a redução progressiva de dias de direito a férias em função das faltas injustificadas
   * apuradas no ciclo aquisitivo (Art. 130 CLT / Estatuto Padrão Ergon):
   * <ul>
   *   <li>Até 5 faltas: 30 dias (100% de {@code totDiasFerBase})
   *   <li>6 a 14 faltas: 24 dias (80% de {@code totDiasFerBase})
   *   <li>15 a 23 faltas: 18 dias (60% de {@code totDiasFerBase})
   *   <li>24 a 32 faltas: 12 dias (40% de {@code totDiasFerBase})
   *   <li>Acima de 32 faltas: 0 dias (perda do direito no ciclo)
   * </ul>
   */
  public static int calcularDiasDireitoPorFaltas(int totDiasFerBase, int faltasInjustificadas) {
    int base = totDiasFerBase > 0 ? totDiasFerBase : 30;
    int faltas = Math.max(0, faltasInjustificadas);
    if (faltas <= 5) {
      return base;
    }
    if (faltas <= 14) {
      return base == 30 ? 24 : (int) Math.round(base * 0.80);
    }
    if (faltas <= 23) {
      return base == 30 ? 18 : (int) Math.round(base * 0.60);
    }
    if (faltas <= 32) {
      return base == 30 ? 12 : (int) Math.round(base * 0.40);
    }
    return 0;
  }

  /**
   * Calcula o teto de dias de abono pecuniário a partir do merge efetivo
   * ({@code numDiasAbonoFer / denDiasAbonoFer} sobre {@code totDiasDireitoPa} quando
   * {@code abonoDirGozo == 'D'} ou sobre o saldo/base de gozo quando {@code 'G'}), respeitando
   * {@code maxDiasVendFer} quando parametrizado.
   */
  public static int calcularDiasAbonoPecuniarioSugerido(
      EffectiveParametroFeriasFacts effective, int totDiasDireitoPa, int baseDiasDisponiveis) {
    if (effective == null || !effective.permiteAbonoPecuniario()) {
      return 0;
    }
    int num = effective.numDiasAbonoFer() != null ? effective.numDiasAbonoFer() : 1;
    int den =
        effective.denDiasAbonoFer() != null && effective.denDiasAbonoFer() > 0
            ? effective.denDiasAbonoFer()
            : 3;
    int baseCalculo =
        "D".equalsIgnoreCase(effective.abonoDirGozo())
            ? Math.max(0, totDiasDireitoPa)
            : Math.max(0, baseDiasDisponiveis > 0 ? baseDiasDisponiveis : totDiasDireitoPa);
    int calculado = (baseCalculo * num) / den;
    if (effective.maxDiasVendFer() != null && effective.maxDiasVendFer() >= 0) {
      calculado = Math.min(calculado, effective.maxDiasVendFer());
    }
    return Math.max(0, calculado);
  }

  private static String resolveAbonoDirGozo(
      PeriodoAquisitivoFerias pa, ParametroContagemFerias base) {
    if (pa != null && pa.getAbonoDirGozo() != null && !pa.getAbonoDirGozo().isBlank()) {
      String ov = pa.getAbonoDirGozo().trim().toUpperCase();
      if (!"I".equals(ov)) {
        return ov;
      }
    }
    if (base != null && base.getAbonoDirGozo() != null && !base.getAbonoDirGozo().isBlank()) {
      String bv = base.getAbonoDirGozo().trim().toUpperCase();
      if (!"I".equals(bv)) {
        return bv;
      }
    }
    return "D";
  }

  private static String resolveCriterioPrescricao(
      PeriodoAquisitivoFerias pa, ParametroContagemFerias base) {
    if (pa != null && pa.getCriterioPrescricao() != null && !pa.getCriterioPrescricao().isBlank()) {
      return pa.getCriterioPrescricao().trim().toUpperCase();
    }
    if (base != null
        && base.getCriterioPrescricao() != null
        && !base.getCriterioPrescricao().isBlank()) {
      return base.getCriterioPrescricao().trim().toUpperCase();
    }
    return "F";
  }

  public static boolean mergeTriStateBoolean(
      String paOverride, Boolean baseValue, boolean fallbackDefault) {
    return org.praxisplatform.rules.parametric.TriStateOverrideResolver.resolve(
        paOverride != null && java.util.Set.of("S", "N", "I").contains(paOverride.trim().toUpperCase(java.util.Locale.ROOT)) ? paOverride : null,
        baseValue == null ? null : baseValue ? "S" : "N").orElse(fallbackDefault);
  }

  public static boolean hasIndividualOverrides(PeriodoAquisitivoFerias pa) {
    if (pa == null) {
      return false;
    }
    return pa.getMaxPerFer() != null
        || pa.getMinPerFer() != null
        || pa.getMaxDiasPerFer() != null
        || pa.getMinDiasPerFer() != null
        || pa.getMinDiasPerPrincFer() != null
        || pa.getMaxDiasVendFer() != null
        || pa.getMinDiasPvendFer() != null
        || pa.getNumDiasAbonoFer() != null
        || pa.getDenDiasAbonoFer() != null
        || pa.getMinDiasPeraqfer() != null
        || pa.getMaxDiasPeraqfer() != null
        || isExplicitTriState(pa.getAbonoDirGozo())
        || isExplicitTriState(pa.getAbonoConstFer())
        || isExplicitTriState(pa.getAdiant13Fer())
        || isExplicitTriState(pa.getAdiantSalFer())
        || isExplicitTriState(pa.getEmprestFer())
        || isExplicitTriState(pa.getPermiteFerEstePer())
        || isExplicitTriState(pa.getContDobroFerias())
        || isExplicitTriState(pa.getPermiteGozoPosPrescricao());
  }

  private static boolean isExplicitTriState(String val) {
    return val != null && !val.isBlank() && !"I".equalsIgnoreCase(val.trim());
  }

  private static boolean matchesScope(
      ParametroContagemFerias p, Integer targetEmp, Vinculo vinculo) {
    if (p.getEmpCodigo() != null
        && targetEmp != null
        && !Objects.equals(p.getEmpCodigo(), targetEmp)) {
      return false;
    }
    if (vinculo == null) {
      return true;
    }
    if (notBlank(p.getRegimeJuridico())
        && notBlank(vinculo.getRegimejur())
        && !p.getRegimeJuridico().trim().equalsIgnoreCase(vinculo.getRegimejur().trim())) {
      return false;
    }
    if (notBlank(p.getTipoVinculo())
        && notBlank(vinculo.getTipovinc())
        && !p.getTipoVinculo().trim().equalsIgnoreCase(vinculo.getTipovinc().trim())) {
      return false;
    }
    if (notBlank(p.getCategoria())
        && notBlank(vinculo.getCategoria())
        && !p.getCategoria().trim().equalsIgnoreCase(vinculo.getCategoria().trim())) {
      return false;
    }
    if (notBlank(p.getSubcategoria())
        && notBlank(vinculo.getSubcategoria())
        && !p.getSubcategoria().trim().equalsIgnoreCase(vinculo.getSubcategoria().trim())) {
      return false;
    }
    return true;
  }

  private static int specificityScore(
      ParametroContagemFerias p, Integer targetEmp, Vinculo vinculo) {
    int score = 0;
    if (p.getEmpCodigo() != null && Objects.equals(p.getEmpCodigo(), targetEmp)) {
      score += 16;
    }
    if (vinculo != null) {
      if (notBlank(p.getRegimeJuridico())
          && p.getRegimeJuridico().trim().equalsIgnoreCase(safeTrim(vinculo.getRegimejur()))) {
        score += 8;
      }
      if (notBlank(p.getTipoVinculo())
          && p.getTipoVinculo().trim().equalsIgnoreCase(safeTrim(vinculo.getTipovinc()))) {
        score += 4;
      }
      if (notBlank(p.getCategoria())
          && p.getCategoria().trim().equalsIgnoreCase(safeTrim(vinculo.getCategoria()))) {
        score += 2;
      }
      if (notBlank(p.getSubcategoria())
          && p.getSubcategoria().trim().equalsIgnoreCase(safeTrim(vinculo.getSubcategoria()))) {
        score += 1;
      }
    } else {
      if (notBlank(p.getRegimeJuridico())) score += 8;
      if (notBlank(p.getTipoVinculo())) score += 4;
      if (notBlank(p.getCategoria())) score += 2;
      if (notBlank(p.getSubcategoria())) score += 1;
    }
    return score;
  }

  @SafeVarargs
  private static <T> T firstNonNull(T... values) {
    if (values == null) {
      return null;
    }
    for (T value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private static boolean notBlank(String s) {
    return s != null && !s.isBlank();
  }

  private static String safeTrim(String s) {
    return s == null ? "" : s.trim();
  }

  private static void putText(ObjectNode node, String field, String value) {
    if (value == null) {
      node.putNull(field);
    } else {
      node.put(field, value);
    }
  }

  private static void putInt(ObjectNode node, String field, Integer value) {
    if (value == null) {
      node.putNull(field);
    } else {
      node.put(field, value);
    }
  }

  private static void putLong(ObjectNode node, String field, Long value) {
    if (value == null) {
      node.putNull(field);
    } else {
      node.put(field, value);
    }
  }
}
