package br.com.praxis.ergonshowcase.config;

import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.repository.VinculoRepository;
import java.util.Optional;
import org.praxisplatform.uischema.capability.*;
import org.springframework.stereotype.Component;

/** Disponibilidade de ações derivada do mesmo estado persistido que governa a execução. */
@Component
public class VinculoStateProvider implements ResourceStateSnapshotProvider {
  @org.springframework.beans.factory.annotation.Autowired
  private br.com.praxis.ergonshowcase.repository.OrgaoRepository orgaos;

  @org.springframework.beans.factory.annotation.Autowired
  private br.com.praxis.ergonshowcase.repository.CargoRepository cargos;

  private final VinculoRepository repository;

  public VinculoStateProvider(VinculoRepository repository) {
    this.repository = repository;
  }

  public Optional<ResourceStateSnapshot> resolve(String key, Object id) {
    if ("administracao-pessoal.empresas".equals(key) && id != null)
      return orgaos
          .findById(Integer.valueOf(id.toString()))
          .map(
              o ->
                  ResourceStateSnapshot.of(
                      Boolean.TRUE.equals(o.getAtiva()) ? "ATIVO" : "INATIVO"));
    if ("administracao-pessoal.cargos".equals(key) && id != null)
      return cargos
          .findById(Integer.valueOf(id.toString()))
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
