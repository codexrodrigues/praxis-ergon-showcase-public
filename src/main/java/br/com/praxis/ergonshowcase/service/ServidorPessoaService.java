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
  @PersistenceContext protected EntityManager entityManager;

  @Override
  public org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder<
          ServidorPessoa>
      getSpecificationsBuilder() {
    return new br.com.praxis.ergonshowcase.config.PessoaSpecificationsBuilder();
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
  protected OptionalLong getEntityResourceVersion(ServidorPessoa e) {
    return OptionalLong.of(e.getVersion());
  }

  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder().temporalTimeSeriesField("dtnasc", "dtnasc").build();
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
  public ResourceRepresentationResult<ServidorPessoaResponseDTO> update(
      Long id, UpdateServidorPessoaDTO dto, ResourceVersionUpdatePrecondition<Long> precondition) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    var readOnlyKeys = validateCustomFields(dto, "request");
    var entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    beforeUpdate(id, entity, dto);
    var old = CustomFieldSupport.snapshotReadOnlyValues(entity, readOnlyKeys);
    mapper.applyUpdate(entity, dto);
    invariants.pessoa(entity);
    CustomFieldSupport.restoreReadOnlyValues(entity, readOnlyKeys, old);
    var saved = getRepository().save(entity);
    afterUpdate(id, saved, dto);
    entityManager.flush();
    entityManager.refresh(saved);
    return toResourceRepresentation(saved);
  }

  @Override
  protected void beforeCreate(CreateServidorPessoaDTO dto, ServidorPessoa e) {
    invariants.pessoa(e);
    if (getRepository().existsById(e.getNumero()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Identidade já cadastrada.");
  }
}
