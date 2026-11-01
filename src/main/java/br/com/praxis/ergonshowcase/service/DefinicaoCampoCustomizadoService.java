package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.DefinicaoCampoCustomizado;
import br.com.praxis.ergonshowcase.dto.AlterarSituacaoCampoCustomizadoDTO;
import br.com.praxis.ergonshowcase.dto.AtualizarApresentacaoCampoCustomizadoDTO;
import br.com.praxis.ergonshowcase.dto.AtualizarGovernancaCampoCustomizadoDTO;
import br.com.praxis.ergonshowcase.dto.CreateDefinicaoCampoCustomizadoDTO;
import br.com.praxis.ergonshowcase.dto.DefinicaoCampoCustomizadoFilterDTO;
import br.com.praxis.ergonshowcase.dto.DefinicaoCampoCustomizadoResponseDTO;
import br.com.praxis.ergonshowcase.dto.UpdateDefinicaoCampoCustomizadoDTO;
import br.com.praxis.ergonshowcase.mapper.DefinicaoCampoCustomizadoMapper;
import br.com.praxis.ergonshowcase.repository.DefinicaoCampoCustomizadoRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Consumer;
import org.praxisplatform.uischema.concurrency.ResourceRepresentationResult;
import org.praxisplatform.uischema.concurrency.ResourceVersionPreconditionException;
import org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.service.base.VersionedCreateUpdateResourceService;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Operações transacionais, invariantes e governança do catálogo de campos customizados por órgão. */
@Service
public class DefinicaoCampoCustomizadoService
    extends ErgonResourceService<
        DefinicaoCampoCustomizado,
        DefinicaoCampoCustomizadoResponseDTO,
        Long,
        DefinicaoCampoCustomizadoFilterDTO,
        CreateDefinicaoCampoCustomizadoDTO,
        UpdateDefinicaoCampoCustomizadoDTO>
    implements VersionedCreateUpdateResourceService<
        DefinicaoCampoCustomizadoResponseDTO,
        Long,
        DefinicaoCampoCustomizadoFilterDTO,
        CreateDefinicaoCampoCustomizadoDTO,
        UpdateDefinicaoCampoCustomizadoDTO> {

  @Autowired(required = false)
  private ErgonInvariants invariants;

  @PersistenceContext protected EntityManager entityManager;
  protected final DefinicaoCampoCustomizadoMapper mapper;
  private final TenantContext tenant;
  private final ErgonRuleEngineFacade ruleEngineFacade;

  public DefinicaoCampoCustomizadoService(
      DefinicaoCampoCustomizadoRepository repository,
      DefinicaoCampoCustomizadoMapper mapper,
      TenantContext tenant) {
    this(repository, mapper, tenant, new ErgonRuleEngineFacade());
  }

  @Autowired
  public DefinicaoCampoCustomizadoService(
      DefinicaoCampoCustomizadoRepository repository,
      DefinicaoCampoCustomizadoMapper mapper,
      TenantContext tenant,
      ErgonRuleEngineFacade ruleEngineFacade) {
    super(repository, DefinicaoCampoCustomizado.class, "id");
    this.mapper = mapper;
    this.tenant = tenant != null ? tenant : new TenantContext();
    this.ruleEngineFacade =
        ruleEngineFacade != null ? ruleEngineFacade : new ErgonRuleEngineFacade();
  }

  public ErgonRuleEngineFacade getRuleEngineFacade() {
    return ruleEngineFacade;
  }

  /**
   * Valida sintática e estruturalmente {@code DefinicaoCampoCustomizado.validationRules} usando
   * {@link org.praxisplatform.rules.jsonlogic.PraxisJsonLogicEngine#validate}.
   */
  public void validateCampoCustomizadoValidationRules(DefinicaoCampoCustomizado entity) {
    if (entity != null) {
      ruleEngineFacade.validateCustomFieldValidationRules(entity.getValidationRules());
    }
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var boolPresentation =
        new CollectionExportFieldPresentation(
            "boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    return List.of(
        field("id", "ID", "integer"),
        field("empCodigo", "Órgão", "integer"),
        field("orgaoNome", "Nome do órgão", "string"),
        field("resourceKey", "Recurso alvo", "string"),
        field("fieldKey", "Chave técnica", "string"),
        field("label", "Rótulo de exibição", "string"),
        field("groupName", "Grupo do formulário", "string"),
        field("fieldOrder", "Ordem no grupo", "integer"),
        field("fieldType", "Tipo de dado", "string"),
        field("controlType", "Controle visual", "string"),
        presentedField("required", "Obrigatório", "boolean", boolPresentation),
        field("optionTabela", "Tabela de opções", "string"),
        presentedField("active", "Ativo", "boolean", boolPresentation));
  }

  @Override
  protected DefinicaoCampoCustomizadoMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected DefinicaoCampoCustomizadoFilterDTO emptyFilter() {
    return new DefinicaoCampoCustomizadoFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(DefinicaoCampoCustomizado entity) {
    return OptionalLong.of(entity.getVersion());
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
            "id",
            "label",
            Set.of(
                StatsMetric.COUNT,
                StatsMetric.SUM,
                StatsMetric.AVG,
                StatsMetric.MIN,
                StatsMetric.MAX,
                StatsMetric.MEDIAN,
                StatsMetric.PERCENTILE_90,
                StatsMetric.PERCENTILE_95))
        .groupByBucket(
            "resourceKey",
            "resourceKey",
            Set.of(
                StatsMetric.COUNT,
                StatsMetric.SUM,
                StatsMetric.AVG,
                StatsMetric.MEDIAN,
                StatsMetric.PERCENTILE_90))
        .categoricalGroupByBucket("fieldType", "fieldType")
        .categoricalGroupByBucket("controlType", "controlType")
        .categoricalGroupByBucket("active", "active")
        .temporalTimeSeriesField("updatedAt", "updatedAt")
        .numericHistogramMeasureField("fieldOrder", "fieldOrder")
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
  protected void beforeCreate(
      CreateDefinicaoCampoCustomizadoDTO dto, DefinicaoCampoCustomizado entity) {
    checkOrg(entity.getEmpCodigo());
    validateCampoCustomizadoValidationRules(entity);
    if (invariants != null) {
      invariants.campoCustomizado(entity, true);
    }
  }

  @Override
  protected void beforeUpdate(
      Long id, DefinicaoCampoCustomizado entity, UpdateDefinicaoCampoCustomizadoDTO dto) {
    checkOrg(entity.getEmpCodigo());
  }

  @Override
  @Transactional
  public ResourceRepresentationResult<DefinicaoCampoCustomizadoResponseDTO> update(
      Long id,
      UpdateDefinicaoCampoCustomizadoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    DefinicaoCampoCustomizado entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    beforeUpdate(id, entity, dto);
    mapper.applyUpdate(entity, dto);
    validateCampoCustomizadoValidationRules(entity);
    if (invariants != null) {
      invariants.campoCustomizado(entity, false);
    }
    var saved = getRepository().save(entity);
    afterUpdate(id, saved, dto);
    entityManager.flush();
    entityManager.refresh(saved);
    return toResourceRepresentation(saved);
  }

  private ResourceRepresentationResult<DefinicaoCampoCustomizadoResponseDTO> command(
      Long id,
      ResourceVersionUpdatePrecondition<Long> precondition,
      Consumer<DefinicaoCampoCustomizado> action) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    DefinicaoCampoCustomizado entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    checkOrg(entity.getEmpCodigo());
    action.accept(entity);
    validateCampoCustomizadoValidationRules(entity);
    if (invariants != null) {
      invariants.campoCustomizado(entity, false);
    }
    var saved = getRepository().save(entity);
    entityManager.flush();
    entityManager.refresh(saved);
    return toResourceRepresentation(saved);
  }

  @Transactional
  public ResourceRepresentationResult<DefinicaoCampoCustomizadoResponseDTO> atualizarApresentacao(
      Long id,
      AtualizarApresentacaoCampoCustomizadoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (DefinicaoCampoCustomizado d) -> {
          if (dto.wasSupplied("label")) d.setLabel(dto.getLabel());
          if (dto.wasSupplied("helpText")) d.setHelpText(dto.getHelpText());
          if (dto.wasSupplied("groupName")) d.setGroupName(dto.getGroupName());
          if (dto.wasSupplied("fieldOrder")) d.setFieldOrder(dto.getFieldOrder());
          if (dto.wasSupplied("controlType")) d.setControlType(dto.getControlType());
        });
  }

  @Transactional
  public ResourceRepresentationResult<DefinicaoCampoCustomizadoResponseDTO> atualizarGovernanca(
      Long id,
      AtualizarGovernancaCampoCustomizadoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (DefinicaoCampoCustomizado d) -> {
          if (dto.wasSupplied("required")) d.setRequired(dto.getRequired());
          if (dto.wasSupplied("optionTabela")) d.setOptionTabela(dto.getOptionTabela());
          if (dto.wasSupplied("validationRules")) d.setValidationRules(dto.getValidationRules());
          if (dto.wasSupplied("governancePolicy")) d.setGovernancePolicy(dto.getGovernancePolicy());
        });
  }

  @Transactional
  public ResourceRepresentationResult<DefinicaoCampoCustomizadoResponseDTO> ativarCampoCustomizado(
      Long id,
      AlterarSituacaoCampoCustomizadoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (DefinicaoCampoCustomizado d) -> {
          if (Boolean.TRUE.equals(d.getActive())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "O campo customizado já se encontra ativo no órgão.");
          }
          d.setActive(true);
        });
  }

  @Transactional
  public ResourceRepresentationResult<DefinicaoCampoCustomizadoResponseDTO>
      desativarCampoCustomizado(
          Long id,
          AlterarSituacaoCampoCustomizadoDTO dto,
          ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (DefinicaoCampoCustomizado d) -> {
          if (!Boolean.TRUE.equals(d.getActive())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "O campo customizado já se encontra inativo no órgão.");
          }
          d.setActive(false);
        });
  }

  private void checkOrg(Integer orgao) {
    if (orgao != null && !Objects.equals(orgao, tenant.orgao())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Órgão do campo customizado diverge de X-Tenant.");
    }
  }
}
