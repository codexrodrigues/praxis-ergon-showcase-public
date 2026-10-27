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
  @PersistenceContext protected EntityManager entityManager;
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
  protected OptionalLong getEntityResourceVersion(Orgao e) {
    return OptionalLong.of(e.getVersion());
  }

  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .categoricalGroupByBucket("ativa", "ativa")
        .categoricalGroupByBucket("ufSigla", "ufSigla")
        .build();
  }

  @Override
  public StatsSupportMode getGroupByStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }

  @Override
  public StatsSupportMode getTimeSeriesStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }

  @Override
  public StatsSupportMode getDistributionStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }

  @Override
  public StatsSupportMode getComparisonStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }

  @Override
  @Transactional
  public ResourceRepresentationResult<OrgaoResponseDTO> update(
      Integer id, UpdateOrgaoDTO dto, ResourceVersionUpdatePrecondition<Integer> precondition) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    var readOnlyKeys = validateCustomFields(dto, "request");
    var entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    beforeUpdate(id, entity, dto);
    var old = CustomFieldSupport.snapshotReadOnlyValues(entity, readOnlyKeys);
    mapper.applyUpdate(entity, dto);
    invariants.orgao(entity);
    CustomFieldSupport.restoreReadOnlyValues(entity, readOnlyKeys, old);
    var saved = getRepository().save(entity);
    afterUpdate(id, saved, dto);
    entityManager.flush();
    entityManager.refresh(saved);
    return toResourceRepresentation(saved);
  }

  @Override
  protected void beforeCreate(CreateOrgaoDTO dto, Orgao e) {
    invariants.orgao(e);
    if (getRepository().existsById(e.getEmpresa()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Identidade já cadastrada.");
  }
}
