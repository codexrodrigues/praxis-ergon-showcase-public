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
public class OrgaoService
    extends ErgonResourceService<
        Orgao, OrgaoResponseDTO, Integer, OrgaoFilterDTO, CreateOrgaoDTO, UpdateOrgaoDTO>
    implements VersionedCreateUpdateResourceService<
        OrgaoResponseDTO, Integer, OrgaoFilterDTO, CreateOrgaoDTO, UpdateOrgaoDTO> {
  @org.springframework.beans.factory.annotation.Autowired private ErgonInvariants invariants;
  protected final OrgaoMapper mapper;
  private final TenantContext tenant;

  public OrgaoService(OrgaoRepository repository, OrgaoMapper mapper, TenantContext tenant) {
    super(repository, Orgao.class, "empresa");
    this.mapper = mapper;
    this.tenant = tenant;
  }

  @Override
  protected List<org.praxisplatform.uischema.exporting.CollectionExportField> exportFields() {
    return List.of(
        field("id", "Código", "integer"),
        field("nome", "Órgão", "string"),
        field("ufSigla", "UF", "string"),
        field("ativa", "Ativo", "boolean"));
  }

  @Override
  protected OrgaoMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected OrgaoFilterDTO emptyFilter() {
    return new OrgaoFilterDTO();
  }


  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket(
            "id",
            "empresa",
            "nome",
            Set.of(
                StatsMetric.COUNT,
                StatsMetric.MEDIAN,
                StatsMetric.PERCENTILE_90))
        .groupByBucket(
            "ativa",
            "ativa",
            Set.of(
                StatsMetric.COUNT,
                StatsMetric.MEDIAN,
                StatsMetric.PERCENTILE_90))
        .categoricalGroupByBucket("ufSigla", "ufSigla")
        .build();
  }

  @Override
  protected void applyResourceUpdate(Integer id, Orgao entity, UpdateOrgaoDTO dto) {
    mapper.applyUpdate(entity, dto);
    invariants.orgao(entity);
  }

  @Override
  protected void beforeCreate(CreateOrgaoDTO dto, Orgao e) {
    assertEntityAccessAllowed(e);
    invariants.orgao(e);
    if (getRepository().existsById(e.getEmpresa()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Identidade já cadastrada.");
  }

  @Override
  protected void beforeDelete(Integer id, Orgao entity) {
    if (getEntityManager() != null && id != null) {
      long vinculos =
          getEntityManager()
              .createQuery("select count(v) from Vinculo v where v.empCodigo=:e", Long.class)
              .setParameter("e", id)
              .getSingleResult();
      if (vinculos > 0) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Não é permitido excluir órgão que possui vínculos funcionais cadastrados.");
      }
    }
  }
  @Override protected java.util.Optional<org.praxisplatform.uischema.capability.ResourceStateSnapshot> toResourceStateSnapshot(Orgao e) {
    return java.util.Optional.of(snapshotState(e));
  }
  private org.praxisplatform.uischema.capability.ResourceStateSnapshot snapshotState(Orgao e) {
    return org.praxisplatform.uischema.capability.ResourceStateSnapshot.of(Boolean.TRUE.equals(e.getAtiva()) ? "ATIVO" : "INATIVO");
  }

  @Override protected String resolveResourceKey() { return "administracao-pessoal.empresas"; }

}
