package br.com.praxis.ergonshowcase.config;

import br.com.praxis.ergonshowcase.domain.DependenteId;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.repository.AfastamentoRepository;
import br.com.praxis.ergonshowcase.repository.CargoRepository;
import br.com.praxis.ergonshowcase.repository.DefinicaoCampoCustomizadoRepository;
import br.com.praxis.ergonshowcase.repository.DependenteRepository;
import br.com.praxis.ergonshowcase.repository.FeriasGozoRepository;
import br.com.praxis.ergonshowcase.repository.FrequenciaRepository;
import br.com.praxis.ergonshowcase.repository.OrgaoRepository;
import br.com.praxis.ergonshowcase.repository.ParametroContagemFeriasRepository;
import br.com.praxis.ergonshowcase.repository.PeriodoAquisitivoFeriasRepository;
import br.com.praxis.ergonshowcase.repository.RadarFeriasPrescricaoRepository;
import br.com.praxis.ergonshowcase.repository.VinculoRepository;
import java.util.Map;
import java.util.Optional;
import org.praxisplatform.uischema.capability.ResourceStateSnapshot;
import org.praxisplatform.uischema.capability.ResourceStateSnapshotProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Disponibilidade de ações derivada do mesmo estado persistido que governa a execução. */
@Component
public class VinculoStateProvider implements ResourceStateSnapshotProvider {
  @Autowired private OrgaoRepository orgaos;

  @Autowired private CargoRepository cargos;

  @Autowired private DependenteRepository dependentes;

  @Autowired private AfastamentoRepository afastamentos;

  @Autowired private FrequenciaRepository frequencias;

  @Autowired private DefinicaoCampoCustomizadoRepository camposCustomizados;

  @Autowired private ParametroContagemFeriasRepository parametrosContagemFerias;

  @Autowired private PeriodoAquisitivoFeriasRepository periodosAquisitivosFerias;

  @Autowired private FeriasGozoRepository feriasGozo;

  @Autowired(required = false)
  private RadarFeriasPrescricaoRepository radarFeriasPrescricao;

  @Autowired private TenantContext tenant;

  private final VinculoRepository repository;

  public VinculoStateProvider(VinculoRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<ResourceStateSnapshot> resolve(String key, Object id) {
    if ("administracao-pessoal.parametros-contagem-ferias".equals(key) && id != null) {
      Long paramId = id instanceof Long typed ? typed : Long.valueOf(id.toString());
      return parametrosContagemFerias
          .findById(paramId)
          .filter(
              p ->
                  p.getEmpCodigo() == null
                      || java.util.Objects.equals(p.getEmpCodigo(), tenant.orgao()))
          .map(
              p ->
                  new ResourceStateSnapshot(
                      Boolean.TRUE.equals(p.getActive()) ? "ATIVO" : "INATIVO",
                      Map.of(
                          "temFerias", Boolean.TRUE.equals(p.getTemFerias()),
                          "vigenciaAberta", p.getDtfim() == null,
                          "escopoGlobal", p.getEmpCodigo() == null,
                          "permiteParcelamento", p.getMaxPerFer() != null && p.getMaxPerFer() > 1,
                          "permiteAbonoPecuniario",
                              Boolean.TRUE.equals(p.getTemFerias())
                                  && p.getAbonoDirGozo() != null
                                  && !"N".equals(p.getAbonoDirGozo()))));
    }
    if ("administracao-pessoal.periodos-aquisitivos-ferias".equals(key) && id != null) {
      Long paId = id instanceof Long typed ? typed : Long.valueOf(id.toString());
      return periodosAquisitivosFerias
          .findById(paId)
          .filter(pa -> java.util.Objects.equals(pa.getEmpCodigo(), tenant.orgao()))
          .map(
              pa -> {
                boolean saldoPositivo =
                    pa.getSaldoDiasDisponivel() != null && pa.getSaldoDiasDisponivel() > 0;
                return new ResourceStateSnapshot(
                    pa.getSituacao(),
                    Map.of(
                        "saldoPositivo",
                        saldoPositivo,
                        "possuiSaldoDisponivel",
                        saldoPositivo,
                        "possuiOverridesAtivos",
                        br.com.praxis.ergonshowcase.mapper.PeriodoAquisitivoFeriasMapper
                            .hasActiveOverrides(pa),
                        "emRiscoPrescricao",
                        "PRESCRITO".equals(pa.getSituacao())
                            || (pa.getDtPrescricao() != null && saldoPositivo),
                        "geradoPorAverbacao",
                        Boolean.TRUE.equals(pa.getGeradoPorAverbacao())));
              });
    }
    if ("administracao-pessoal.ferias-gozo".equals(key) && id != null) {
      Long fgId = id instanceof Long typed ? typed : Long.valueOf(id.toString());
      return feriasGozo
          .findById(fgId)
          .filter(fg -> java.util.Objects.equals(fg.getEmpCodigo(), tenant.orgao()))
          .map(
              fg -> {
                boolean comAbono =
                    Boolean.TRUE.equals(fg.getAbonoPecuniario())
                        || (fg.getDiasAbonoVendidos() != null && fg.getDiasAbonoVendidos() > 0);
                boolean interrompida =
                    "INTERROMPIDA".equals(fg.getSituacao()) || fg.getDtInterrupcao() != null;
                return new ResourceStateSnapshot(
                    fg.getSituacao(),
                    Map.of(
                        "abonoPecuniario", comAbono,
                        "comAbonoVendido", comAbono,
                        "comSubstitutoDesignado",
                            fg.getNumfuncSubstituto() != null && fg.getNumvincSubstituto() != null,
                        "interrompida", interrompida,
                        "parcelaInterrompida", interrompida));
              });
    }
    if ("administracao-pessoal.radar-ferias-prescricao".equals(key) && id != null) {
      Long radarId = id instanceof Long typed ? typed : Long.valueOf(id.toString());
      if (radarFeriasPrescricao != null) {
        return radarFeriasPrescricao
            .findById(radarId)
            .filter(r -> java.util.Objects.equals(r.getEmpCodigo(), tenant.orgao()))
            .map(
                r -> {
                  String faixa =
                      r.getFaixaUrgenciaPrescricao() != null
                          ? r.getFaixaUrgenciaPrescricao()
                          : "REGULAR";
                  boolean critica =
                      "VENCIDO_PRESCRITO".equals(faixa) || "CRITICO_30D".equals(faixa);
                  boolean descoberto =
                      r.getSaldoSemProgramacao() != null && r.getSaldoSemProgramacao() > 0;
                  boolean acumulo = Boolean.TRUE.equals(r.getEmRiscoAcumuloPeriodos());
                  return new ResourceStateSnapshot(
                      faixa,
                      Map.of(
                          "urgenciaCriticaOuVencida", critica,
                          "possuiSaldoDescoberto", descoberto,
                          "emRiscoImediatoGovernanca", critica || acumulo));
                });
      }
      if (periodosAquisitivosFerias != null) {
        return periodosAquisitivosFerias
            .findById(radarId)
            .filter(pa -> java.util.Objects.equals(pa.getEmpCodigo(), tenant.orgao()))
            .map(
                pa -> {
                  String faixa =
                      br.com.praxis.ergonshowcase.mapper.RadarFeriasPrescricaoMapper
                          .calcularFaixaUrgencia(
                              pa.getSituacao(),
                              pa.getSaldoDiasDisponivel(),
                              pa.getDtPrescricao(),
                              java.time.LocalDate.now());
                  boolean critica =
                      "VENCIDO_PRESCRITO".equals(faixa) || "CRITICO_30D".equals(faixa);
                  boolean descoberto =
                      pa.getSaldoDiasDisponivel() != null && pa.getSaldoDiasDisponivel() > 0;
                  return new ResourceStateSnapshot(
                      faixa,
                      Map.of(
                          "urgenciaCriticaOuVencida", critica,
                          "possuiSaldoDescoberto", descoberto,
                          "emRiscoImediatoGovernanca", critica));
                });
      }
    }
    if ("administracao-pessoal.campos-customizados".equals(key) && id != null) {
      Long campoId = id instanceof Long typed ? typed : Long.valueOf(id.toString());
      return camposCustomizados
          .findById(campoId)
          .filter(
              c ->
                  c.getEmpCodigo() == null
                      || java.util.Objects.equals(c.getEmpCodigo(), tenant.orgao()))
          .map(
              c ->
                  new ResourceStateSnapshot(
                      Boolean.TRUE.equals(c.getActive()) ? "ATIVO" : "INATIVO",
                      Map.of("required", Boolean.TRUE.equals(c.getRequired()))));
    }
    if ("administracao-pessoal.frequencias".equals(key) && id != null) {
      Long freqId = id instanceof Long typed ? typed : Long.valueOf(id.toString());
      return frequencias
          .findById(freqId)
          .filter(f -> java.util.Objects.equals(f.getEmpCodigo(), tenant.orgao()))
          .map(
              f ->
                  new ResourceStateSnapshot(
                      f.getSituacao(),
                      Map.of("possuiDocumento", f.getDocumentoLegalId() != null)));
    }
    if ("administracao-pessoal.afastamentos".equals(key) && id != null) {
      Long afastamentoId = id instanceof Long typed ? typed : Long.valueOf(id.toString());
      return afastamentos
          .findById(afastamentoId)
          .filter(a -> java.util.Objects.equals(a.getEmpCodigo(), tenant.orgao()))
          .map(
              a -> {
                String state =
                    "HOMOLOGADO".equals(a.getSituacao()) && a.getDtfim() != null
                        ? "ENCERRADO"
                        : a.getSituacao();
                return new ResourceStateSnapshot(state, Map.of("emAberto", a.getDtfim() == null));
              });
    }
    if ("administracao-pessoal.dependentes".equals(key) && id != null) {
      String[] parts = id.toString().split(":");
      DependenteId depId =
          id instanceof DependenteId typed
              ? typed
              : new DependenteId(Long.valueOf(parts[0]), Integer.valueOf(parts[1]));
      return dependentes
          .findById(depId)
          .map(
              d -> {
                String state = d.getDtfim() == null ? "VIGENTE" : "ENCERRADO";
                return new ResourceStateSnapshot(
                    state, Map.of("invalido", Boolean.TRUE.equals(d.getInvalido())));
              });
    }
    if ("administracao-pessoal.empresas".equals(key) && id != null)
      return orgaos
          .findById(Integer.valueOf(id.toString()))
          .map(
              o ->
                  ResourceStateSnapshot.of(
                      Boolean.TRUE.equals(o.getAtiva()) ? "ATIVO" : "INATIVO"));
    if ("administracao-pessoal.cargos".equals(key) && id != null)
      return cargos
          .findByCargoAndEmpCodigo(Integer.valueOf(id.toString()), tenant.orgao())
          .map(c -> ResourceStateSnapshot.of(c.getDtExtincao() == null ? "VIGENTE" : "EXTINTO"));
    if (!"administracao-pessoal.vinculos".equals(key) || id == null) return Optional.empty();
    String[] parts = id.toString().split(":");
    VinculoId keyId =
      id instanceof VinculoId typed
          ? typed
          : new VinculoId(Long.valueOf(parts[0]), Integer.valueOf(parts[1]));
    return repository.findById(keyId).map(e -> ResourceStateSnapshot.of(e.getSituacao()));
  }
}
