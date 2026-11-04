package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.repository.*;
import jakarta.persistence.*;
import java.util.*;
import org.praxisplatform.uischema.concurrency.*;
import org.praxisplatform.uischema.customfields.CustomFieldSupport;
import org.praxisplatform.uischema.service.base.*;
import org.praxisplatform.uischema.stats.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Operações do cadastro com pré-condição de versão e preservação da identidade carregada. */
@Service
public class ServidorPessoaService
    extends ErgonResourceService<
        ServidorPessoa,
        ServidorPessoaResponseDTO,
        Long,
        ServidorPessoaFilterDTO,
        CreateServidorPessoaDTO,
        UpdateServidorPessoaDTO>
    implements VersionedCreateUpdateResourceService<
        ServidorPessoaResponseDTO,
        Long,
        ServidorPessoaFilterDTO,
        CreateServidorPessoaDTO,
        UpdateServidorPessoaDTO> {
  @org.springframework.beans.factory.annotation.Autowired private ErgonInvariants invariants;

  @Override
  public org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder<
          ServidorPessoa>
      getSpecificationsBuilder() {
    return new org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder<>();
  }

  protected final ServidorPessoaMapper mapper;
  private final TenantContext tenant;

  public ServidorPessoaService(
      ServidorPessoaRepository repository, ServidorPessoaMapper mapper, TenantContext tenant) {
    super(repository, ServidorPessoa.class, "numero");
    this.mapper = mapper;
    this.tenant = tenant;
  }

  @Override
  protected List<org.praxisplatform.uischema.exporting.CollectionExportField> exportFields() {
    return List.of(field("id", "Número funcional", "integer"));
  }

  @Override
  protected ServidorPessoaMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected ServidorPessoaFilterDTO emptyFilter() {
    return new ServidorPessoaFilterDTO();
  }


  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .add(
            new org.praxisplatform.uischema.stats.StatsFieldDescriptor(
                "id",
                "numero",
                "nome",
                Set.of(
                    StatsMetric.COUNT,
                    StatsMetric.DISTINCT_COUNT,
                    StatsMetric.SUM,
                    StatsMetric.AVG,
                    StatsMetric.MIN,
                    StatsMetric.MAX,
                    StatsMetric.MEDIAN,
                    StatsMetric.PERCENTILE_90,
                    StatsMetric.PERCENTILE_95),
                true,
                false,
                true,
                true,
                true))
        .groupByBucket(
            "escolaridade",
            "escolaridade",
            Set.of(
                StatsMetric.COUNT,
                StatsMetric.MEDIAN,
                StatsMetric.PERCENTILE_90))
        .temporalTimeSeriesField("dtnasc", "dtnasc")
        .build();
  }

  @Override
  protected void applyResourceUpdate(Long id, ServidorPessoa entity, UpdateServidorPessoaDTO dto) {
    mapper.applyUpdate(entity, dto);
    invariants.pessoa(entity);
  }

  @Override
  protected void beforeCreate(CreateServidorPessoaDTO dto, ServidorPessoa e) {
    invariants.pessoa(e);
    if (getRepository().existsById(e.getNumero()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Identidade já cadastrada.");
  }

  @Override
  protected void beforeDelete(Long id, ServidorPessoa entity) {
    if (getEntityManager() != null && id != null) {
      long vinculos =
          getEntityManager()
              .createQuery("select count(v) from Vinculo v where v.id.numfunc=:p", Long.class)
              .setParameter("p", id)
              .getSingleResult();
      if (vinculos > 0) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Não é permitido excluir servidor titular que possui vínculos funcionais cadastrados.");
      }
    }
  }
  @Override protected String resolveResourceKey() { return "administracao-pessoal.funcionarios"; }

}
