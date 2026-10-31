package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.Frequencia;
import br.com.praxis.ergonshowcase.dto.AbonarFaltaFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.AtualizarHorarioFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.AtualizarJustificativaFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.CancelarFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.CreateFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.FrequenciaFilterDTO;
import br.com.praxis.ergonshowcase.dto.FrequenciaResponseDTO;
import br.com.praxis.ergonshowcase.dto.HomologarFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.RejeitarFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.UpdateFrequenciaDTO;
import br.com.praxis.ergonshowcase.mapper.FrequenciaMapper;
import br.com.praxis.ergonshowcase.repository.FrequenciaRepository;
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
import org.praxisplatform.uischema.customfields.CustomFieldSupport;
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

/** Operações transacionais, invariantes e workflow de homologação de ocorrências de frequência. */
@Service
public class FrequenciaService
    extends ErgonResourceService<
        Frequencia,
        FrequenciaResponseDTO,
        Long,
        FrequenciaFilterDTO,
        CreateFrequenciaDTO,
        UpdateFrequenciaDTO>
    implements VersionedCreateUpdateResourceService<
        FrequenciaResponseDTO,
        Long,
        FrequenciaFilterDTO,
        CreateFrequenciaDTO,
        UpdateFrequenciaDTO> {

  @Autowired private ErgonInvariants invariants;
  @PersistenceContext protected EntityManager entityManager;
  protected final FrequenciaMapper mapper;
  private final TenantContext tenant;

  public FrequenciaService(
      FrequenciaRepository repository, FrequenciaMapper mapper, TenantContext tenant) {
    super(repository, Frequencia.class, "id");
    this.mapper = mapper;
    this.tenant = tenant;
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var datePresentation =
        new CollectionExportFieldPresentation(
            "date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    var decimalPresentation =
        new CollectionExportFieldPresentation(
            "number", "0.00", null, "pt-BR", null, null, null, "Não informado");
    return List.of(
        field("id", "ID da frequência", "integer"),
        field("empCodigo", "Órgão", "integer"),
        field("numfunc", "Servidor", "integer"),
        field("numvinc", "Vínculo funcional", "integer"),
        field("vinculoMatricula", "Matrícula do vínculo", "string"),
        field("tipofreq", "Tipo de frequência", "string"),
        field("codfreq", "Código de frequência", "integer"),
        field("codigoFrequenciaNome", "Modalidade de frequência", "string"),
        presentedField("dtini", "Data de início", "date", datePresentation),
        presentedField("dtfim", "Data final", "date", datePresentation),
        presentedField("quantidade", "Quantidade apurada", "number", decimalPresentation),
        field("situacao", "Situação", "string"),
        field("obs", "Justificativa", "string"));
  }

  @Override
  protected FrequenciaMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected FrequenciaFilterDTO emptyFilter() {
    return new FrequenciaFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(Frequencia entity) {
    return OptionalLong.of(entity.getVersion());
  }

  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "obs", Set.of(StatsMetric.COUNT, StatsMetric.SUM))
        .categoricalGroupByBucket("situacao", "situacao")
        .categoricalGroupByBucket("tipofreq", "tipofreq")
        .temporalTimeSeriesField("dtini", "dtini")
        .numericHistogramMeasureField("quantidade", "quantidade")
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
  protected void beforeCreate(CreateFrequenciaDTO dto, Frequencia entity) {
    checkOrg(entity.getEmpCodigo());
    invariants.frequencia(entity, true);
  }

  @Override
  protected void beforeUpdate(Long id, Frequencia entity, UpdateFrequenciaDTO dto) {
    checkOrg(entity.getEmpCodigo());
  }

  @Override
  @Transactional
  public ResourceRepresentationResult<FrequenciaResponseDTO> update(
      Long id, UpdateFrequenciaDTO dto, ResourceVersionUpdatePrecondition<Long> precondition) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    var readOnlyKeys = validateCustomFields(dto, "request");
    Frequencia entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    beforeUpdate(id, entity, dto);
    var old = CustomFieldSupport.snapshotReadOnlyValues(entity, readOnlyKeys);
    mapper.applyUpdate(entity, dto);
    invariants.frequencia(entity, false);
    CustomFieldSupport.restoreReadOnlyValues(entity, readOnlyKeys, old);
    var saved = getRepository().save(entity);
    afterUpdate(id, saved, dto);
    entityManager.flush();
    entityManager.refresh(saved);
    return toResourceRepresentation(saved);
  }

  private ResourceRepresentationResult<FrequenciaResponseDTO> command(
      Long id,
      ResourceVersionUpdatePrecondition<Long> precondition,
      Consumer<Frequencia> action) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    Frequencia entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    checkOrg(entity.getEmpCodigo());
    action.accept(entity);
    invariants.frequencia(entity, false);
    var saved = getRepository().save(entity);
    entityManager.flush();
    entityManager.refresh(saved);
    return toResourceRepresentation(saved);
  }

  @Transactional
  public ResourceRepresentationResult<FrequenciaResponseDTO> atualizarHorario(
      Long id,
      AtualizarHorarioFrequenciaDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Frequencia f) -> {
          if (dto.wasSupplied("horaEntrada")) f.setHoraEntrada(dto.getHoraEntrada());
          if (dto.wasSupplied("horaSaida")) f.setHoraSaida(dto.getHoraSaida());
          if (dto.wasSupplied("quantidade")) f.setQuantidade(dto.getQuantidade());
        });
  }

  @Transactional
  public ResourceRepresentationResult<FrequenciaResponseDTO> atualizarJustificativa(
      Long id,
      AtualizarJustificativaFrequenciaDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Frequencia f) -> {
          if (dto.wasSupplied("obs")) f.setObs(dto.getObs());
          if (dto.wasSupplied("documentoLegalId")) f.setDocumentoLegalId(dto.getDocumentoLegalId());
          if (dto.wasSupplied("dtfim")) f.setDtfim(dto.getDtfim());
        });
  }

  @Transactional
  public ResourceRepresentationResult<FrequenciaResponseDTO> homologarFrequencia(
      Long id,
      HomologarFrequenciaDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Frequencia f) -> {
          if (!"PENDENTE_HOMOLOGACAO".equals(f.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente ocorrências de frequência pendentes de homologação podem ser homologadas.");
          }
          if (dto.getDocumentoLegalId() != null) {
            f.setDocumentoLegalId(dto.getDocumentoLegalId());
          }
          if (dto.getObservacoes() != null && !dto.getObservacoes().isBlank()) {
            f.setObs(
                (f.getObs() == null ? "" : f.getObs() + " | ")
                    + "Homologação: "
                    + dto.getObservacoes());
          }
          f.setSituacao("HOMOLOGADO");
        });
  }

  @Transactional
  public ResourceRepresentationResult<FrequenciaResponseDTO> abonarFaltaFrequencia(
      Long id,
      AbonarFaltaFrequenciaDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Frequencia f) -> {
          if (!"PENDENTE_HOMOLOGACAO".equals(f.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente ocorrências pendentes de homologação podem receber abono formal.");
          }
          f.setDocumentoLegalId(dto.getDocumentoLegalId());
          f.setObs(
              (f.getObs() == null ? "" : f.getObs() + " | ")
                  + "Abono: "
                  + dto.getJustificativa());
          f.setSituacao("HOMOLOGADO");
        });
  }

  @Transactional
  public ResourceRepresentationResult<FrequenciaResponseDTO> rejeitarFrequencia(
      Long id,
      RejeitarFrequenciaDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Frequencia f) -> {
          if (!"PENDENTE_HOMOLOGACAO".equals(f.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente ocorrências pendentes de homologação podem ser rejeitadas.");
          }
          f.setSituacao("REJEITADO");
          f.setObs(
              (f.getObs() == null ? "" : f.getObs() + " | ")
                  + "Rejeição: "
                  + dto.getMotivoRejeicao());
        });
  }

  @Transactional
  public ResourceRepresentationResult<FrequenciaResponseDTO> cancelarFrequencia(
      Long id,
      CancelarFrequenciaDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Frequencia f) -> {
          if (!Set.of("PENDENTE_HOMOLOGACAO", "HOMOLOGADO").contains(f.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente ocorrências pendentes ou homologadas podem ser canceladas.");
          }
          f.setSituacao("CANCELADO");
          f.setObs(
              (f.getObs() == null ? "" : f.getObs() + " | ")
                  + "Cancelamento: "
                  + dto.getMotivoCancelamento());
        });
  }

  private void checkOrg(Integer orgao) {
    if (!Objects.equals(orgao, tenant.orgao())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Órgão da frequência diverge de X-Tenant.");
    }
  }
}
