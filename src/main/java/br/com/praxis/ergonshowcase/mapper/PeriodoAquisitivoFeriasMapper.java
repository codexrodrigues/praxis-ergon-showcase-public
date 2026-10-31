package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.PeriodoAquisitivoFerias;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.*;
import java.util.LinkedHashMap;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PeriodoAquisitivoFeriasMapper
    implements ResourceMapper<
        PeriodoAquisitivoFerias,
        PeriodoAquisitivoFeriasResponseDTO,
        CreatePeriodoAquisitivoFeriasDTO,
        UpdatePeriodoAquisitivoFeriasDTO,
        Long> {

  private final TenantContext tenant;

  public PeriodoAquisitivoFeriasMapper() {
    this(new TenantContext());
  }

  @Autowired
  public PeriodoAquisitivoFeriasMapper(TenantContext tenant) {
    this.tenant = tenant != null ? tenant : new TenantContext();
  }

  @Override
  public PeriodoAquisitivoFeriasResponseDTO toResponse(PeriodoAquisitivoFerias entity) {
    var dto = new PeriodoAquisitivoFeriasResponseDTO();
    dto.setId(entity.getId());
    dto.setPeriodoResumo(resumo(entity));
    dto.setEmpCodigo(entity.getEmpCodigo());
    dto.setOrgaoNome(entity.getOrgao() != null ? entity.getOrgao().getNome() : null);
    dto.setNumfunc(entity.getNumfunc());
    dto.setPessoaNome(entity.getPessoa() != null ? entity.getPessoa().getNome() : null);
    dto.setNumvinc(entity.getNumvinc());
    if (entity.getNumfunc() != null && entity.getNumvinc() != null) {
      dto.setVinculoId(new VinculoId(entity.getNumfunc(), entity.getNumvinc()));
    }
    dto.setVinculoMatricula(entity.getVinculo() != null ? entity.getVinculo().getMatric() : null);
    dto.setChave(entity.getChave());
    dto.setExercicio(entity.getExercicio());
    dto.setDtini(entity.getDtini());
    dto.setDtfim(entity.getDtfim());
    dto.setTotDiasDireito(entity.getTotDiasDireito());
    dto.setFaltasApuradas(entity.getFaltasApuradas());
    dto.setDiasAfastamentoSuspensivo(entity.getDiasAfastamentoSuspensivo());
    dto.setDiasGozados(entity.getDiasGozados());
    dto.setDiasVendidosAbono(entity.getDiasVendidosAbono());
    dto.setDiasConvertidosDobro(entity.getDiasConvertidosDobro());
    dto.setSaldoDiasDisponivel(entity.getSaldoDiasDisponivel());
    dto.setSituacao(entity.getSituacao());
    dto.setDtPrescricao(entity.getDtPrescricao());
    dto.setCriterioPrescricao(entity.getCriterioPrescricao());
    dto.setPermiteGozoPosPrescricao(entity.getPermiteGozoPosPrescricao());
    dto.setDtPgtoComMulta(entity.getDtPgtoComMulta());
    dto.setSemPgtoMulta(entity.getSemPgtoMulta());
    dto.setMaxPerFer(entity.getMaxPerFer());
    dto.setMinPerFer(entity.getMinPerFer());
    dto.setMaxDiasPerFer(entity.getMaxDiasPerFer());
    dto.setMinDiasPerFer(entity.getMinDiasPerFer());
    dto.setMinDiasPerPrincFer(entity.getMinDiasPerPrincFer());
    dto.setAbonoDirGozo(entity.getAbonoDirGozo());
    dto.setAbonoConstFer(entity.getAbonoConstFer());
    dto.setAdiant13Fer(entity.getAdiant13Fer());
    dto.setAdiantSalFer(entity.getAdiantSalFer());
    dto.setEmprestFer(entity.getEmprestFer());
    dto.setPermiteFerEstePer(entity.getPermiteFerEstePer());
    dto.setMaxDiasVendFer(entity.getMaxDiasVendFer());
    dto.setMinDiasPvendFer(entity.getMinDiasPvendFer());
    dto.setNumDiasAbonoFer(entity.getNumDiasAbonoFer());
    dto.setDenDiasAbonoFer(entity.getDenDiasAbonoFer());
    dto.setContDobroFerias(entity.getContDobroFerias());
    dto.setMinDiasPeraqfer(entity.getMinDiasPeraqfer());
    dto.setMaxDiasPeraqfer(entity.getMaxDiasPeraqfer());
    dto.setGeradoPorAverbacao(entity.getGeradoPorAverbacao());
    dto.setDocumentoLegalId(entity.getDocumentoLegalId());
    dto.setDocumentoLegalNumero(
        entity.getDocumentoLegal() != null ? entity.getDocumentoLegal().getNumeroDocumento() : null);
    dto.setAtoPublicacaoId(entity.getAtoPublicacaoId());
    dto.setAtoPublicacaoNumero(
        entity.getAtoPublicacao() != null ? entity.getAtoPublicacao().getNumeroDocumento() : null);
    dto.setObservacoes(entity.getObservacoes());
    dto.setPossuiSaldoDisponivel(
        entity.getSaldoDiasDisponivel() != null
            && entity.getSaldoDiasDisponivel() > 0
            && !"PRESCRITO".equals(entity.getSituacao())
            && !"CANCELADO".equals(entity.getSituacao()));
    dto.setPossuiOverridesAtivos(hasActiveOverrides(entity));
    dto.setEmRiscoPrescricao(
        "PRESCRITO".equals(entity.getSituacao())
            || (entity.getDtPrescricao() != null
                && entity.getSaldoDiasDisponivel() != null
                && entity.getSaldoDiasDisponivel() > 0));
    dto.setCustomFields(
        entity.getCustomFields() != null
            ? new LinkedHashMap<>(entity.getCustomFields())
            : new LinkedHashMap<>());
    return dto;
  }

  @Override
  public PeriodoAquisitivoFerias newEntity(CreatePeriodoAquisitivoFeriasDTO dto) {
    var entity = new PeriodoAquisitivoFerias();
    apply(dto, entity);
    recomputeSaldoAndSituacao(entity);
    return entity;
  }

  @Override
  public void applyUpdate(
      PeriodoAquisitivoFerias entity, UpdatePeriodoAquisitivoFeriasDTO dto) {
    apply(dto, entity);
    recomputeSaldoAndSituacao(entity);
  }

  @Override
  public Long extractId(PeriodoAquisitivoFerias entity) {
    return entity.getId();
  }

  public static void recomputeSaldoAndSituacao(PeriodoAquisitivoFerias entity) {
    int direito = entity.getTotDiasDireito() != null ? entity.getTotDiasDireito() : 30;
    int gozados = entity.getDiasGozados() != null ? entity.getDiasGozados() : 0;
    int vendidos = entity.getDiasVendidosAbono() != null ? entity.getDiasVendidosAbono() : 0;
    int dobro = entity.getDiasConvertidosDobro() != null ? entity.getDiasConvertidosDobro() : 0;
    int saldo = Math.max(0, direito - gozados - vendidos - dobro);
    entity.setSaldoDiasDisponivel(saldo);
    String sit = entity.getSituacao();
    if (!"SUSPENSO".equals(sit) && !"PRESCRITO".equals(sit) && !"CANCELADO".equals(sit)) {
      if (saldo == 0 && direito > 0) {
        entity.setSituacao("QUITADO");
      } else if (gozados + vendidos + dobro > 0) {
        entity.setSituacao("PARCIALMENTE_GOZADO");
      } else {
        entity.setSituacao("DISPONIVEL");
      }
    }
  }

  private void apply(PeriodoAquisitivoFeriasDados dto, PeriodoAquisitivoFerias entity) {
    entity.setEmpCodigo(dto.getEmpCodigo());
    if (entity.getEmpCodigo() == null) {
      entity.setEmpCodigo(tenant.orgao());
    }
    entity.setNumfunc(dto.getNumfunc());
    entity.setNumvinc(dto.getNumvinc());
    entity.setChave(dto.getChave());
    entity.setExercicio(dto.getExercicio());
    entity.setDtini(dto.getDtini());
    entity.setDtfim(dto.getDtfim());
    entity.setTotDiasDireito(dto.getTotDiasDireito());
    if (entity.getTotDiasDireito() == null) {
      entity.setTotDiasDireito(30);
    }
    entity.setFaltasApuradas(dto.getFaltasApuradas());
    if (entity.getFaltasApuradas() == null) {
      entity.setFaltasApuradas(0);
    }
    entity.setDiasAfastamentoSuspensivo(dto.getDiasAfastamentoSuspensivo());
    if (entity.getDiasAfastamentoSuspensivo() == null) {
      entity.setDiasAfastamentoSuspensivo(0);
    }
    entity.setDiasGozados(dto.getDiasGozados());
    if (entity.getDiasGozados() == null) {
      entity.setDiasGozados(0);
    }
    entity.setDiasVendidosAbono(dto.getDiasVendidosAbono());
    if (entity.getDiasVendidosAbono() == null) {
      entity.setDiasVendidosAbono(0);
    }
    entity.setDiasConvertidosDobro(dto.getDiasConvertidosDobro());
    if (entity.getDiasConvertidosDobro() == null) {
      entity.setDiasConvertidosDobro(0);
    }
    entity.setDtPrescricao(dto.getDtPrescricao());
    entity.setCriterioPrescricao(dto.getCriterioPrescricao());
    entity.setCriterioPrescricao(defaultIndiferente(entity.getCriterioPrescricao(), "I"));
    entity.setPermiteGozoPosPrescricao(dto.getPermiteGozoPosPrescricao());
    entity.setPermiteGozoPosPrescricao(defaultIndiferente(entity.getPermiteGozoPosPrescricao(), "I"));
    entity.setDtPgtoComMulta(dto.getDtPgtoComMulta());
    entity.setSemPgtoMulta(dto.getSemPgtoMulta());
    entity.setSemPgtoMulta(defaultIndiferente(entity.getSemPgtoMulta(), "N"));
    entity.setMaxPerFer(dto.getMaxPerFer());
    entity.setMinPerFer(dto.getMinPerFer());
    entity.setMaxDiasPerFer(dto.getMaxDiasPerFer());
    entity.setMinDiasPerFer(dto.getMinDiasPerFer());
    entity.setMinDiasPerPrincFer(dto.getMinDiasPerPrincFer());
    entity.setAbonoDirGozo(dto.getAbonoDirGozo());
    entity.setAbonoDirGozo(defaultIndiferente(entity.getAbonoDirGozo(), "I"));
    entity.setAbonoConstFer(dto.getAbonoConstFer());
    entity.setAbonoConstFer(defaultIndiferente(entity.getAbonoConstFer(), "I"));
    entity.setAdiant13Fer(dto.getAdiant13Fer());
    entity.setAdiant13Fer(defaultIndiferente(entity.getAdiant13Fer(), "I"));
    entity.setAdiantSalFer(dto.getAdiantSalFer());
    entity.setAdiantSalFer(defaultIndiferente(entity.getAdiantSalFer(), "I"));
    entity.setEmprestFer(dto.getEmprestFer());
    entity.setEmprestFer(defaultIndiferente(entity.getEmprestFer(), "I"));
    entity.setPermiteFerEstePer(dto.getPermiteFerEstePer());
    entity.setPermiteFerEstePer(defaultIndiferente(entity.getPermiteFerEstePer(), "I"));
    entity.setMaxDiasVendFer(dto.getMaxDiasVendFer());
    entity.setMinDiasPvendFer(dto.getMinDiasPvendFer());
    entity.setNumDiasAbonoFer(dto.getNumDiasAbonoFer());
    entity.setDenDiasAbonoFer(dto.getDenDiasAbonoFer());
    entity.setContDobroFerias(dto.getContDobroFerias());
    entity.setContDobroFerias(defaultIndiferente(entity.getContDobroFerias(), "I"));
    entity.setMinDiasPeraqfer(dto.getMinDiasPeraqfer());
    entity.setMaxDiasPeraqfer(dto.getMaxDiasPeraqfer());
    entity.setGeradoPorAverbacao(dto.getGeradoPorAverbacao());
    entity.setGeradoPorAverbacao(Boolean.TRUE.equals(entity.getGeradoPorAverbacao()));
    entity.setDocumentoLegalId(dto.getDocumentoLegalId());
    entity.setAtoPublicacaoId(dto.getAtoPublicacaoId());
    entity.setObservacoes(dto.getObservacoes());
    entity.setObservacoes(norm(entity.getObservacoes()));
    entity.setCustomFields(dto.getCustomFields());
    entity.setCustomFields(
        entity.getCustomFields() != null
            ? new LinkedHashMap<>(entity.getCustomFields())
            : new LinkedHashMap<>());
  }

  public static boolean hasActiveOverrides(PeriodoAquisitivoFerias e) {
    return e.getMaxPerFer() != null
        || e.getMinPerFer() != null
        || e.getMaxDiasPerFer() != null
        || e.getMinDiasPerFer() != null
        || e.getMinDiasPerPrincFer() != null
        || e.getMaxDiasVendFer() != null
        || e.getMinDiasPvendFer() != null
        || e.getNumDiasAbonoFer() != null
        || e.getDenDiasAbonoFer() != null
        || e.getMinDiasPeraqfer() != null
        || e.getMaxDiasPeraqfer() != null
        || isOverriddenFlag(e.getAbonoDirGozo())
        || isOverriddenFlag(e.getAbonoConstFer())
        || isOverriddenFlag(e.getAdiant13Fer())
        || isOverriddenFlag(e.getAdiantSalFer())
        || isOverriddenFlag(e.getEmprestFer())
        || isOverriddenFlag(e.getPermiteFerEstePer())
        || isOverriddenFlag(e.getContDobroFerias())
        || isOverriddenFlag(e.getCriterioPrescricao())
        || isOverriddenFlag(e.getPermiteGozoPosPrescricao());
  }

  private static boolean isOverriddenFlag(String flag) {
    return flag != null && !flag.isBlank() && !"I".equalsIgnoreCase(flag.trim());
  }

  public static String resumo(PeriodoAquisitivoFerias e) {
    String ch = e.getChave() != null ? "#" + e.getChave() : "#?";
    String ex = e.getExercicio() != null ? " (" + e.getExercicio() + ")" : "";
    String ini = e.getDtini() != null ? e.getDtini().toString() : "?";
    String fim = e.getDtfim() != null ? e.getDtfim().toString() : "?";
    return "PA " + ch + ex + ": " + ini + " a " + fim;
  }

  private static String norm(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  private static String defaultIndiferente(String s, String fallback) {
    return s == null || s.isBlank() ? fallback : s.trim().toUpperCase();
  }
}
