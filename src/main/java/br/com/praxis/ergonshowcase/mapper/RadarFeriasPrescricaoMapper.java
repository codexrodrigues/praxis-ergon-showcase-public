package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.FeriasGozo;
import br.com.praxis.ergonshowcase.domain.PeriodoAquisitivoFerias;
import br.com.praxis.ergonshowcase.domain.RadarFeriasPrescricao;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.RadarFeriasPrescricaoResponseDTO;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

@Component
public class RadarFeriasPrescricaoMapper
    implements ResourceMapper<
        RadarFeriasPrescricao, RadarFeriasPrescricaoResponseDTO, Void, Void, Long> {

  @Override
  public RadarFeriasPrescricaoResponseDTO toResponse(RadarFeriasPrescricao entity) {
    var dto = new RadarFeriasPrescricaoResponseDTO();
    dto.setId(entity.getId());
    dto.setEmpCodigo(entity.getEmpCodigo());
    dto.setOrgaoNome(
        entity.getOrgaoNome() != null
            ? entity.getOrgaoNome()
            : (entity.getOrgao() != null ? entity.getOrgao().getNome() : null));
    dto.setNumfunc(entity.getNumfunc());
    dto.setNumvinc(entity.getNumvinc());
    if (entity.getNumfunc() != null && entity.getNumvinc() != null) {
      dto.setVinculoId(new VinculoId(entity.getNumfunc(), entity.getNumvinc()));
    }
    dto.setVinculoMatricula(
        entity.getVinculoMatricula() != null
            ? entity.getVinculoMatricula()
            : (entity.getVinculo() != null ? entity.getVinculo().getMatric() : null));
    dto.setPessoaNome(
        entity.getPessoaNome() != null
            ? entity.getPessoaNome()
            : (entity.getPessoa() != null ? entity.getPessoa().getNome() : null));
    dto.setSetorCodigo(
        entity.getSetorCodigo() != null
            ? entity.getSetorCodigo()
            : (entity.getVinculo() != null ? entity.getVinculo().getSetor() : null));
    dto.setSetorNome(
        entity.getSetorNome() != null
            ? entity.getSetorNome()
            : (entity.getSetor() != null ? entity.getSetor().getNome() : null));
    dto.setCargoCodigo(
        entity.getCargoCodigo() != null
            ? entity.getCargoCodigo()
            : (entity.getVinculo() != null ? entity.getVinculo().getCargo() : null));
    dto.setCargoNome(
        entity.getCargoNome() != null
            ? entity.getCargoNome()
            : (entity.getCargo() != null ? entity.getCargo().getNome() : null));
    dto.setRegimeJuridico(
        entity.getRegimeJuridico() != null
            ? entity.getRegimeJuridico()
            : (entity.getVinculo() != null ? entity.getVinculo().getRegimejur() : null));
    dto.setTipoVinculo(
        entity.getTipoVinculo() != null
            ? entity.getTipoVinculo()
            : (entity.getVinculo() != null ? entity.getVinculo().getTipovinc() : null));
    dto.setCategoria(
        entity.getCategoria() != null
            ? entity.getCategoria()
            : (entity.getVinculo() != null ? entity.getVinculo().getCategoria() : null));
    dto.setChave(entity.getChave());
    dto.setDtini(entity.getDtini());
    dto.setDtfim(entity.getDtfim());
    dto.setExercicio(entity.getExercicio());
    dto.setSituacao(entity.getSituacao());
    dto.setTotDiasDireito(entity.getTotDiasDireito());
    dto.setFaltasApuradas(entity.getFaltasApuradas());
    dto.setDiasAfastamentoSuspensivo(entity.getDiasAfastamentoSuspensivo());
    dto.setDiasGozadosTotal(entity.getDiasGozadosTotal());
    dto.setDiasAbonoTotal(entity.getDiasAbonoTotal());
    int programados =
        entity.getDiasProgramadosFuturos() != null ? entity.getDiasProgramadosFuturos() : 0;
    dto.setDiasProgramadosFuturos(programados);
    int saldo = entity.getSaldoDiasDisponivel() != null ? entity.getSaldoDiasDisponivel() : 0;
    dto.setSaldoDiasDisponivel(saldo);
    int saldoSemProg =
        entity.getSaldoSemProgramacao() != null
            ? entity.getSaldoSemProgramacao()
            : Math.max(0, saldo - programados);
    dto.setSaldoSemProgramacao(saldoSemProg);
    dto.setDtPrescricao(entity.getDtPrescricao());
    int diasAte =
        entity.getDiasAtePrescricao() != null
            ? entity.getDiasAtePrescricao()
            : calcularDiasAtePrescricao(entity.getDtPrescricao(), LocalDate.now());
    dto.setDiasAtePrescricao(diasAte);
    String faixa =
        entity.getFaixaUrgenciaPrescricao() != null
            ? entity.getFaixaUrgenciaPrescricao()
            : calcularFaixaUrgencia(
                entity.getSituacao(), saldo, entity.getDtPrescricao(), LocalDate.now());
    dto.setFaixaUrgenciaPrescricao(faixa);
    boolean acumulo = Boolean.TRUE.equals(entity.getEmRiscoAcumuloPeriodos());
    dto.setEmRiscoAcumuloPeriodos(acumulo);
    dto.setPossuiParcelaProgramada(Boolean.TRUE.equals(entity.getPossuiParcelaProgramada()));
    dto.setProximaParcelaInicio(entity.getProximaParcelaInicio());
    dto.setProximaParcelaSituacao(entity.getProximaParcelaSituacao());
    dto.setPeriodoResumo(resumo(entity, faixa));
    boolean criticaOuVencida = "VENCIDO_PRESCRITO".equals(faixa) || "CRITICO_30D".equals(faixa);
    dto.setUrgenciaCriticaOuVencida(criticaOuVencida);
    dto.setPossuiSaldoDescoberto(saldoSemProg > 0);
    dto.setEmRiscoImediatoGovernanca(criticaOuVencida || acumulo);
    return dto;
  }

  public static int calcularDiasAtePrescricao(LocalDate dtPrescricao, LocalDate dataReferencia) {
    if (dtPrescricao == null) {
      return 999;
    }
    LocalDate ref = dataReferencia != null ? dataReferencia : LocalDate.now();
    return (int) ChronoUnit.DAYS.between(ref, dtPrescricao);
  }

  public static String calcularFaixaUrgencia(
      String situacao,
      Integer saldoDiasDisponivel,
      LocalDate dtPrescricao,
      LocalDate dataReferencia) {
    int saldo = saldoDiasDisponivel != null ? saldoDiasDisponivel : 0;
    if ("PRESCRITO".equals(situacao) || "CONVERTIDO_DOBRO".equals(situacao)) {
      return "VENCIDO_PRESCRITO";
    }
    if (dtPrescricao == null || saldo <= 0) {
      return "REGULAR";
    }
    int dias = calcularDiasAtePrescricao(dtPrescricao, dataReferencia);
    if (dias < 0) {
      return "VENCIDO_PRESCRITO";
    }
    if (dias <= 30) {
      return "CRITICO_30D";
    }
    if (dias <= 60) {
      return "ALERTA_60D";
    }
    if (dias <= 90) {
      return "ATENCAO_90D";
    }
    return "REGULAR";
  }

  public static RadarFeriasPrescricao projetar(
      PeriodoAquisitivoFerias pa,
      List<FeriasGozo> parcelasDoPa,
      int qtdPeriodosAbertosNoVinculo,
      LocalDate dataReferencia) {
    var r = new RadarFeriasPrescricao();
    r.setId(pa.getId());
    r.setEmpCodigo(pa.getEmpCodigo());
    r.setOrgao(pa.getOrgao());
    r.setOrgaoNome(pa.getOrgao() != null ? pa.getOrgao().getNome() : null);
    r.setNumfunc(pa.getNumfunc());
    r.setPessoa(pa.getPessoa());
    r.setPessoaNome(pa.getPessoa() != null ? pa.getPessoa().getNome() : null);
    r.setNumvinc(pa.getNumvinc());
    r.setVinculo(pa.getVinculo());
    if (pa.getVinculo() != null) {
      r.setVinculoMatricula(pa.getVinculo().getMatric());
      r.setSetorCodigo(pa.getVinculo().getSetor());
      if (pa.getVinculo().getLotacao() != null) {
        r.setSetor(pa.getVinculo().getLotacao());
        r.setSetorNome(pa.getVinculo().getLotacao().getNome());
      }
      r.setCargoCodigo(pa.getVinculo().getCargo());
      if (pa.getVinculo().getCargoEntidade() != null) {
        r.setCargo(pa.getVinculo().getCargoEntidade());
        r.setCargoNome(pa.getVinculo().getCargoEntidade().getNome());
      }
      r.setRegimeJuridico(pa.getVinculo().getRegimejur());
      r.setTipoVinculo(pa.getVinculo().getTipovinc());
      r.setCategoria(pa.getVinculo().getCategoria());
    }
    r.setChave(pa.getChave());
    r.setDtini(pa.getDtini());
    r.setDtfim(pa.getDtfim());
    r.setExercicio(pa.getExercicio());
    r.setSituacao(pa.getSituacao());
    r.setTotDiasDireito(pa.getTotDiasDireito());
    r.setFaltasApuradas(pa.getFaltasApuradas());
    r.setDiasAfastamentoSuspensivo(pa.getDiasAfastamentoSuspensivo());
    r.setDiasGozadosTotal(pa.getDiasGozados());
    r.setDiasAbonoTotal(pa.getDiasVendidosAbono());
    int programados = 0;
    boolean possuiAtiva = false;
    LocalDate proxInicio = null;
    String proxSit = null;
    if (parcelasDoPa != null) {
      for (FeriasGozo fg :
          parcelasDoPa.stream()
              .sorted(
                  Comparator.comparing(
                      FeriasGozo::getDtini, Comparator.nullsLast(Comparator.naturalOrder())))
              .toList()) {
        String sit = fg.getSituacao();
        if ("PROGRAMADA".equals(sit) || "HOMOLOGADA".equals(sit)) {
          int abono =
              Boolean.TRUE.equals(fg.getAbonoPecuniario()) && fg.getDiasAbonoVendidos() != null
                  ? fg.getDiasAbonoVendidos()
                  : 0;
          programados += (fg.getDiasGozo() != null ? fg.getDiasGozo() : 0) + abono;
        }
        if ("PROGRAMADA".equals(sit) || "HOMOLOGADA".equals(sit) || "EM_GOZO".equals(sit)) {
          possuiAtiva = true;
          if (proxInicio == null) {
            proxInicio = fg.getDtini();
            proxSit = sit;
          }
        }
      }
    }
    r.setDiasProgramadosFuturos(programados);
    int saldo = pa.getSaldoDiasDisponivel() != null ? pa.getSaldoDiasDisponivel() : 0;
    r.setSaldoDiasDisponivel(saldo);
    r.setSaldoSemProgramacao(Math.max(0, saldo - programados));
    r.setDtPrescricao(pa.getDtPrescricao());
    r.setDiasAtePrescricao(calcularDiasAtePrescricao(pa.getDtPrescricao(), dataReferencia));
    r.setFaixaUrgenciaPrescricao(
        calcularFaixaUrgencia(pa.getSituacao(), saldo, pa.getDtPrescricao(), dataReferencia));
    r.setEmRiscoAcumuloPeriodos(qtdPeriodosAbertosNoVinculo >= 2);
    r.setPossuiParcelaProgramada(possuiAtiva);
    r.setProximaParcelaInicio(proxInicio);
    r.setProximaParcelaSituacao(proxSit);
    r.setVersion(pa.getVersion() != null ? pa.getVersion() : 0L);
    return r;
  }

  public static String resumo(RadarFeriasPrescricao e, String faixa) {
    String ch = e.getChave() != null ? "#" + e.getChave() : "#?";
    String ex = e.getExercicio() != null ? " (" + e.getExercicio() + ")" : "";
    String ini = e.getDtini() != null ? e.getDtini().toString() : "?";
    String fim = e.getDtfim() != null ? e.getDtfim().toString() : "?";
    String urg = faixa != null ? " [" + faixa + "]" : "";
    return "PA " + ch + ex + ": " + ini + " a " + fim + urg;
  }

  @Override
  public RadarFeriasPrescricao newEntity(Void dto) {
    throw new UnsupportedOperationException("Recurso analítico somente-leitura");
  }

  @Override
  public void applyUpdate(RadarFeriasPrescricao entity, Void dto) {
    throw new UnsupportedOperationException("Recurso analítico somente-leitura");
  }

  @Override
  public Long extractId(RadarFeriasPrescricao entity) {
    return entity.getId();
  }
}
