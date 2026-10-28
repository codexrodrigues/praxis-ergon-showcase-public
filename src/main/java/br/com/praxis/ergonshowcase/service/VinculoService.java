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
public class VinculoService
    extends ErgonResourceService<
        Vinculo,
        VinculoResponseDTO,
        VinculoId,
        VinculoFilterDTO,
        CreateVinculoDTO,
        UpdateVinculoDTO>
    implements VersionedCreateUpdateResourceService<
        VinculoResponseDTO, VinculoId, VinculoFilterDTO, CreateVinculoDTO, UpdateVinculoDTO> {
  @org.springframework.beans.factory.annotation.Autowired private ErgonInvariants invariants;
  @PersistenceContext protected EntityManager entityManager;
  protected final VinculoMapper mapper;
  private final TenantContext tenant;

  public VinculoService(VinculoRepository repository, VinculoMapper mapper, TenantContext tenant) {
    super(repository, Vinculo.class, "id.numfunc", "id.numero");
    this.mapper = mapper;
    this.tenant = tenant;
  }

  @Override
  protected List<org.praxisplatform.uischema.exporting.CollectionExportField> exportFields() {
    return List.of(
        field("id", "Vínculo", "string"),
        field("empCodigo", "Órgão", "integer"),
        field("situacao", "Situação", "string"),
        presentedField("dtexerc", "Exercício", "date",
            new org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation(
                "date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado")),
        presentedField("jornadaSemanal", "Jornada semanal", "integer",
            new org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation(
                "number", "0", null, "pt-BR", null, null, null, "Não informado")),
        field("customFields.regime_teletrabalho", "Regime de teletrabalho", "string"));
  }

  @Override
  protected VinculoMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected VinculoFilterDTO emptyFilter() {
    return new VinculoFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(Vinculo e) {
    return OptionalLong.of(e.getVersion());
  }

  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "matric", Set.of(StatsMetric.COUNT, StatsMetric.SUM))
        .categoricalGroupByBucket("situacao", "situacao")
        .temporalTimeSeriesField("dtexerc", "dtexerc")
        .numericHistogramMeasureField("jornadaSemanal", "jornadaSemanal")
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
  public ResourceRepresentationResult<VinculoResponseDTO> update(
      VinculoId id,
      UpdateVinculoDTO dto,
      ResourceVersionUpdatePrecondition<VinculoId> precondition) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    var readOnlyKeys = validateCustomFields(dto, "request");
    var entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    beforeUpdate(id, entity, dto);
    var old = CustomFieldSupport.snapshotReadOnlyValues(entity, readOnlyKeys);
    mapper.applyUpdate(entity, dto);
    invariants.vinculo(entity, false);
    CustomFieldSupport.restoreReadOnlyValues(entity, readOnlyKeys, old);
    var saved = getRepository().save(entity);
    afterUpdate(id, saved, dto);
    entityManager.flush();
    entityManager.refresh(saved);
    return enriched(toResourceRepresentation(saved));
  }

  @Transactional
  public ResourceRepresentationResult<VinculoResponseDTO> registrarVacancia(
      VinculoId id,
      RegistrarVacanciaDTO dto,
      ResourceVersionUpdatePrecondition<VinculoId> precondition) {
    Vinculo e = findEntityById(id);
    entityManager.lock(e, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(e);
    precondition.requireMatch(e.getVersion());
    checkOrg(e.getEmpCodigo());
    if (!"ATIVO".equals(e.getSituacao()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente vínculo ativo pode receber esta ação.");
    if (e.getDtexerc() != null && dto.data().isBefore(e.getDtexerc()))
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Vacância anterior ao exercício.");
    e.setDtvac(dto.data());
    e.setFormavac(dto.forma());
    e.setMotivovac(dto.motivo());
    ErgonInvariants.chronology(dto.data(), dto.dtPgtoAte(), "Pagamento final anterior à vacância.");
    e.setDtPgtoAte(dto.dtPgtoAte());
    e.setSituacao("VACANTE");
    entityManager.flush();
    return toResourceRepresentation(e);
  }

  @Override
  protected void beforeCreate(CreateVinculoDTO dto, Vinculo e) {
    invariants.vinculo(e, true);
    checkOrg(e.getEmpCodigo());
    if (getRepository().existsById(e.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Vínculo já cadastrado.");
    Orgao o = entityManager.find(Orgao.class, e.getEmpCodigo());
    if (o == null || !o.getAtiva())
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Órgão indisponível para admissão.");
    if (entityManager.find(ServidorPessoa.class, e.getId().numfunc()) == null)
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Pessoa inexistente.");
  }

  @Override
  protected void beforeUpdate(VinculoId id, Vinculo e, UpdateVinculoDTO dto) {
    checkOrg(e.getEmpCodigo());
  }

  private void checkOrg(Integer orgao) {
    if (!java.util.Objects.equals(orgao, tenant.orgao()))
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Órgão do vínculo diverge de X-Tenant.");
  }
}
