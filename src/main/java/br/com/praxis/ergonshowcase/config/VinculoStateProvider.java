package br.com.praxis.ergonshowcase.config;

import br.com.praxis.ergonshowcase.domain.DependenteId;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.repository.AfastamentoRepository;
import br.com.praxis.ergonshowcase.repository.CargoRepository;
import br.com.praxis.ergonshowcase.repository.DefinicaoCampoCustomizadoRepository;
import br.com.praxis.ergonshowcase.repository.DependenteRepository;
import br.com.praxis.ergonshowcase.repository.FrequenciaRepository;
import br.com.praxis.ergonshowcase.repository.OrgaoRepository;
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

  @Autowired private TenantContext tenant;

  private final VinculoRepository repository;

  public VinculoStateProvider(VinculoRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<ResourceStateSnapshot> resolve(String key, Object id) {
    if ("administracao-pessoal.campos-customizados".equals(key) && id != null) {
      Long campoId = id instanceof Long typed ? typed : Long.valueOf(id.toString());
      return camposCustomizados
          .findById(campoId)
          .filter(c -> java.util.Objects.equals(c.getEmpCodigo(), tenant.orgao()))
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
