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
public class CargoService
    extends ErgonResourceService<
        Cargo, CargoResponseDTO, Integer, CargoFilterDTO, CreateCargoDTO, UpdateCargoDTO>
    implements VersionedCreateUpdateResourceService<
        CargoResponseDTO, Integer, CargoFilterDTO, CreateCargoDTO, UpdateCargoDTO> {
  @org.springframework.beans.factory.annotation.Autowired private ErgonInvariants invariants;
  @PersistenceContext protected EntityManager entityManager;
  protected final CargoMapper mapper;
  private final TenantContext tenant;

  public CargoService(CargoRepository repository, CargoMapper mapper, TenantContext tenant) {
    super(repository, Cargo.class, "cargo");
    this.mapper = mapper;
    this.tenant = tenant;
  }

  @Override
  protected List<org.praxisplatform.uischema.exporting.CollectionExportField> exportFields() {
    return List.of(
        field("id", "Código", "integer"),
        field("nome", "Cargo", "string"),
        field("empCodigo", "Órgão", "integer"),
        field("categoria", "Categoria", "string"),
        field("vagasAutorizadas", "Vagas autorizadas", "integer"));
  }

  @Override
  protected CargoMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected CargoFilterDTO emptyFilter() {
    return new CargoFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(Cargo e) {
    return OptionalLong.of(e.getVersion());
  }

  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .categoricalGroupByBucket("categoria", "categoria")
        .numericHistogramMeasureField("vagasAutorizadas", "vagasAutorizadas")
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
  public ResourceRepresentationResult<CargoResponseDTO> update(
      Integer id, UpdateCargoDTO dto, ResourceVersionUpdatePrecondition<Integer> precondition) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    var readOnlyKeys = validateCustomFields(dto, "request");
    var entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    beforeUpdate(id, entity, dto);
    var old = CustomFieldSupport.snapshotReadOnlyValues(entity, readOnlyKeys);
    mapper.applyUpdate(entity, dto);
    invariants.cargo(entity);
    CustomFieldSupport.restoreReadOnlyValues(entity, readOnlyKeys, old);
    var saved = getRepository().save(entity);
    afterUpdate(id, saved, dto);
    entityManager.flush();
    entityManager.refresh(saved);
    return enriched(toResourceRepresentation(saved));
  }

  @Override
  protected void beforeCreate(CreateCargoDTO dto, Cargo e) {
    invariants.cargo(e);
    checkOrg(e.getEmpCodigo());
    if (getRepository().existsById(e.getCargo()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cargo já cadastrado.");
  }

  @Override
  protected void beforeUpdate(Integer id, Cargo e, UpdateCargoDTO dto) {
    checkOrg(e.getEmpCodigo());
  }

  private void checkOrg(Integer orgao) {
    if (!java.util.Objects.equals(orgao, tenant.orgao()))
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Órgão do cargo diverge de X-Tenant.");
  }
}
