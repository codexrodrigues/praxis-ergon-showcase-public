package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.rules.EffectiveVacationParameterResolver;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;

/** Precedência de PERAQLICESP sobre PARAMETROS_CONTA, sem executar packages/folha. */
record EffectiveSpecialLeavePolicy(
    boolean permiteVenda, String baseAbono, Integer numeradorAbono, Integer denominadorAbono,
    Integer minimoVenda, Integer maximoVenda, boolean permiteAntecipacao, boolean permiteDobro, Integer minimoDobro, Integer maximoDobro) {

  static EffectiveSpecialLeavePolicy resolve(
      PeriodoAquisitivoLicencaEspecial pa, ParcelaLicencaEspecial parcela, EntityManager em) {
    var vinculo = em.find(Vinculo.class, new VinculoId(pa.getNumfunc(), pa.getNumvinc()));
    var resolver = new EffectiveVacationParameterResolver();
    // Venda: GREATEST(fim da aquisição, exercício), PCK_ERG_VENDA_LICESP.
    // Gozo: fim da aquisição, ou fim do gozo se anterior ao exercício; fallback no início.
    LocalDate referencia = pa.getDtfim();
    if (parcela instanceof VendaLicencaEspecial) {
      if (vinculo != null && vinculo.getDtexerc() != null
          && (referencia == null || referencia.isBefore(vinculo.getDtexerc())))
        referencia = vinculo.getDtexerc();
    } else if (parcela instanceof LicencaEspecialGozo g && referencia != null
        && vinculo != null && vinculo.getDtexerc() != null && referencia.isBefore(vinculo.getDtexerc())) {
      referencia = g.getDtfim() != null ? g.getDtfim() : g.getDtprevfim();
    }
    if (referencia == null) referencia = parcela == null ? pa.getDtini() : parcela.getDtini();
    var matriz = resolver.resolveActiveParameter(em, vinculo, referencia);
    if (matriz == null && parcela instanceof LicencaEspecialGozo
        && !java.util.Objects.equals(referencia, parcela.getDtini()))
      matriz = resolver.resolveActiveParameter(em, vinculo, parcela.getDtini());

    boolean venda = org.praxisplatform.rules.parametric.TriStateOverrideResolver.resolve(pa.getVendLicEsp(),
        matriz == null || matriz.getVendLicEsp() == null ? null : matriz.getVendLicEsp() ? "S" : "N").orElse(false);
    boolean antecipacao = org.praxisplatform.rules.parametric.TriStateOverrideResolver.resolve(
        pa.getPermGozoLicEspAntFimPe() == null ? null : pa.getPermGozoLicEspAntFimPe().name(),
        matriz == null || matriz.getPermGozoLicEspAntFimPe() == null ? null : matriz.getPermGozoLicEspAntFimPe() ? "S" : "N").orElse(false);
    return new EffectiveSpecialLeavePolicy(venda,
        prefer(pa.getAbonoDirGozoLicEsp(), matriz == null ? null : matriz.getAbonoDirGozoLicEsp()),
        prefer(pa.getNumDiasAbonoLicEsp(), matriz == null ? null : matriz.getNumDiasAbonoLicEsp()),
        prefer(pa.getDenDiasAbonoLicEsp(), matriz == null ? null : matriz.getDenDiasAbonoLicEsp()),
        prefer(pa.getMinDiasPvendLicEsp(), matriz == null ? null : matriz.getMinDiasPvendLicEsp()),
        prefer(pa.getMaxDiasVendLicEsp(), matriz == null ? null : matriz.getMaxDiasVendLicEsp()), antecipacao,
        org.praxisplatform.rules.parametric.TriStateOverrideResolver.resolve(pa.getContDobroLicEsp(),
            matriz == null || matriz.getContDobroLicEsp() == null ? null : matriz.getContDobroLicEsp() ? "S" : "N").orElse(false),
        prefer(pa.getMinDiasPcontDobLicEsp(),matriz==null?null:matriz.getMinDiasPcontDobLicEsp()),
        prefer(pa.getMaxDiasContDobLicEsp(),matriz==null?null:matriz.getMaxDiasContDobLicEsp()));
  }

  private static <N extends Number> N prefer(N individual, N geral) {
    return org.praxisplatform.rules.parametric.TriStateOverrideResolver.resolveNumber(individual, geral).orElse(null);
  }
  private static String prefer(String individual, String geral) { return individual != null ? individual : geral; }
}
