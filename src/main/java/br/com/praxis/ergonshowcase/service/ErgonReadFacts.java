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
                  "select c.cargo, count(v) from Cargo c left join Vinculo v on v.empCodigo=c.empCodigo and (v.cargo=c.cargo or v.cargoComissao=c.cargo) and v.situacao in ('ATIVO','AFASTADO','CEDIDO') where c.cargo in :ids group by c.cargo",
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
    var linkedCargoIds = new HashSet<Integer>();
    cargos.forEach(
        c -> {
          if (c.getCargoAglutinador() != null) linkedCargoIds.add(c.getCargoAglutinador());
        });
    responses.stream()
        .filter(VinculoResponseDTO.class::isInstance)
        .map(VinculoResponseDTO.class::cast)
        .forEach(
            v -> {
              if (v.getCargoComissao() != null) linkedCargoIds.add(v.getCargoComissao());
            });
    Map<Integer, String> cargoNames = new HashMap<>();
    if (!linkedCargoIds.isEmpty())
      em.createQuery("select c.cargo,c.nome from Cargo c where c.cargo in :ids", Object[].class)
          .setParameter("ids", linkedCargoIds)
          .getResultList()
          .forEach(row -> cargoNames.put((Integer) row[0], (String) row[1]));
    cargos.forEach(c -> c.setCargoAglutinadorNome(cargoNames.get(c.getCargoAglutinador())));
    responses.stream()
        .filter(VinculoResponseDTO.class::isInstance)
        .map(VinculoResponseDTO.class::cast)
        .forEach(v -> v.setCargoComissaoNome(cargoNames.get(v.getCargoComissao())));
    var vinculos =
        responses.stream()
            .filter(VinculoResponseDTO.class::isInstance)
            .map(VinculoResponseDTO.class::cast)
            .toList();
    if (!vinculos.isEmpty()) {
      var ids = vinculos.stream().map(VinculoResponseDTO::getId).toList();
      var rows =
          em.createQuery(
                  "select v.id, min(a.id), count(a) from Vinculo v left join Afastamento a on a.empCodigo=v.empCodigo and a.numfunc=v.id.numfunc and a.numvinc=v.id.numero and a.situacao='HOMOLOGADO' and a.dtfim is null where v.id in :ids group by v.id",
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
    if (em != null) {
      enrichAssentamentosTempoLicenca(responses);
    }
  }

  private void enrichAssentamentosTempoLicenca(List<?> responses) {
    var assentamentos =
        responses.stream()
            .filter(AssentamentoTempoLicencaDados.class::isInstance)
            .map(AssentamentoTempoLicencaDados.class::cast)
            .toList();
    if (assentamentos.isEmpty()) return;

    var missingOrgaos =
        assentamentos.stream()
            .filter(a -> a.getEmpCodigo() != null && a.getOrgaoNome() == null)
            .map(AssentamentoTempoLicencaDados::getEmpCodigo)
            .distinct()
            .toList();
    if (!missingOrgaos.isEmpty()) {
      Map<Integer, String> nomes = new HashMap<>();
      em.createQuery("select o.empresa, o.nome from Orgao o where o.empresa in :ids", Object[].class)
          .setParameter("ids", missingOrgaos)
          .getResultList()
          .forEach(row -> nomes.put((Integer) row[0], (String) row[1]));
      assentamentos.forEach(
          a -> {
            if (a.getOrgaoNome() == null && a.getEmpCodigo() != null)
              a.setOrgaoNome(nomes.get(a.getEmpCodigo()));
          });
    }

    var missingPessoas =
        assentamentos.stream()
            .filter(a -> a.getNumfunc() != null && a.getPessoaNome() == null)
            .map(AssentamentoTempoLicencaDados::getNumfunc)
            .distinct()
            .toList();
    if (!missingPessoas.isEmpty()) {
      Map<Long, String> nomes = new HashMap<>();
      em.createQuery(
              "select p.numero, p.nome from ServidorPessoa p where p.numero in :ids",
              Object[].class)
          .setParameter("ids", missingPessoas)
          .getResultList()
          .forEach(row -> nomes.put((Long) row[0], (String) row[1]));
      assentamentos.forEach(
          a -> {
            if (a.getPessoaNome() == null && a.getNumfunc() != null)
              a.setPessoaNome(nomes.get(a.getNumfunc()));
          });
    }

    var missingVinculos =
        assentamentos.stream()
            .filter(
                a ->
                    a.getNumfunc() != null
                        && a.getNumvinc() != null
                        && a.getVinculoMatricula() == null)
            .map(a -> new VinculoId(a.getNumfunc(), a.getNumvinc()))
            .distinct()
            .toList();
    if (!missingVinculos.isEmpty()) {
      Map<VinculoId, String> matriculas = new HashMap<>();
      em.createQuery(
              "select v.id, v.matric from Vinculo v where v.id in :ids", Object[].class)
          .setParameter("ids", missingVinculos)
          .getResultList()
          .forEach(row -> matriculas.put((VinculoId) row[0], (String) row[1]));
      assentamentos.forEach(
          a -> {
            if (a.getVinculoMatricula() == null
                && a.getNumfunc() != null
                && a.getNumvinc() != null)
              a.setVinculoMatricula(
                  matriculas.get(new VinculoId(a.getNumfunc(), a.getNumvinc())));
          });
    }

    var missingDocs = new HashSet<Long>();
    assentamentos.forEach(
        a -> {
          if (a.getDocumentoLegalId() != null && a.getDocumentoLegalNumero() == null)
            missingDocs.add(a.getDocumentoLegalId());
          if (a.getAtoPublicacaoId() != null && a.getAtoPublicacaoNumero() == null)
            missingDocs.add(a.getAtoPublicacaoId());
        });
    if (!missingDocs.isEmpty()) {
      Map<Long, String> numeros = new HashMap<>();
      em.createQuery(
              "select d.id, d.numeroDocumento from DocumentoLegal d where d.id in :ids",
              Object[].class)
          .setParameter("ids", missingDocs)
          .getResultList()
          .forEach(row -> numeros.put((Long) row[0], (String) row[1]));
      assentamentos.forEach(
          a -> {
            if (a.getDocumentoLegalNumero() == null && a.getDocumentoLegalId() != null)
              a.setDocumentoLegalNumero(numeros.get(a.getDocumentoLegalId()));
            if (a.getAtoPublicacaoNumero() == null && a.getAtoPublicacaoId() != null)
              a.setAtoPublicacaoNumero(numeros.get(a.getAtoPublicacaoId()));
          });
    }
  }

  public <T> T enrichOne(T response) {
    enrich(List.of(response));
    return response;
  }
}
