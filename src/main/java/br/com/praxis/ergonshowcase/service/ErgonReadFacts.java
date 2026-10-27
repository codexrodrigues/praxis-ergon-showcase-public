package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.*;
import jakarta.persistence.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Projeta fatos consultados em lote, sem consultas por linha nos mappers. */
@Component
public class ErgonReadFacts {
  @PersistenceContext private EntityManager em;

  public void enrich(List<?> responses) {
    var cargos =
        responses.stream()
            .filter(CargoResponseDTO.class::isInstance)
            .map(CargoResponseDTO.class::cast)
            .toList();
    if (!cargos.isEmpty()) {
      var ids = cargos.stream().map(CargoResponseDTO::getId).toList();
      // A mesma pessoa/vínculo ocupa uma única vaga quando titular e comissão coincidem.
      var rows =
          em.createQuery(
                  "select c.cargo, count(v) from Cargo c left join Vinculo v on (v.cargo=c.cargo or v.cargoComissao=c.cargo) and v.situacao in ('ATIVO','AFASTADO','CEDIDO') where c.cargo in :ids group by c.cargo",
                  Object[].class)
              .setParameter("ids", ids)
              .getResultList();
      Map<Integer, Long> ocupadas = new HashMap<>();
      rows.forEach(row -> ocupadas.put((Integer) row[0], (Long) row[1]));
      cargos.forEach(
          c ->
              c.setSaldoVagasDisponiveis(
                  c.getVagasAutorizadas() == null
                      ? null
                      : c.getVagasAutorizadas() - ocupadas.getOrDefault(c.getId(), 0L)));
    }
    var vinculos =
        responses.stream()
            .filter(VinculoResponseDTO.class::isInstance)
            .map(VinculoResponseDTO.class::cast)
            .toList();
    if (!vinculos.isEmpty()) {
      var ids = vinculos.stream().map(VinculoResponseDTO::getId).toList();
      var rows =
          em.createQuery(
                  "select v.id, min(a.id), count(a) from Vinculo v left join Afastamento a on a.numfunc=v.id.numfunc and a.numvinc=v.id.numero and a.situacao='HOMOLOGADO' and a.dtfim is null where v.id in :ids group by v.id",
                  Object[].class)
              .setParameter("ids", ids)
              .getResultList();
      Map<VinculoId, Long> abertos = new HashMap<>();
      rows.forEach(
          row -> {
            if (((Long) row[2]) == 1L) abertos.put((VinculoId) row[0], (Long) row[1]);
          });
      vinculos.forEach(v -> v.setAfastamentoAbertoId(abertos.get(v.getId())));
    }
  }

  public <T> T enrichOne(T response) {
    enrich(List.of(response));
    return response;
  }
}
