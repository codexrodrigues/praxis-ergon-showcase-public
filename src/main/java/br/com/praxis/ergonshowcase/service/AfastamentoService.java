package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.AfastamentoMapper;
import br.com.praxis.ergonshowcase.repository.AfastamentoRepository;
import jakarta.persistence.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import org.praxisplatform.uischema.concurrency.*;
import org.praxisplatform.uischema.customfields.CustomFieldSupport;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.service.base.*;
import org.praxisplatform.uischema.stats.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Operações transacionais, invariantes e workflow de homologação de licenças e afastamentos. */
@Service
public class AfastamentoService
    extends ErgonResourceService<
        Afastamento,
        AfastamentoResponseDTO,
        Long,
        AfastamentoFilterDTO,
        CreateAfastamentoDTO,
        UpdateAfastamentoDTO>
    implements VersionedCreateUpdateResourceService<
        AfastamentoResponseDTO,
        Long,
        AfastamentoFilterDTO,
        CreateAfastamentoDTO,
        UpdateAfastamentoDTO> {

  @Autowired private ErgonInvariants invariants;
  @PersistenceContext protected EntityManager entityManager;
  protected final AfastamentoMapper mapper;
  private final TenantContext tenant;

  public AfastamentoService(
      AfastamentoRepository repository, AfastamentoMapper mapper, TenantContext tenant) {
    super(repository, Afastamento.class, "id");
    this.mapper = mapper;
    this.tenant = tenant;
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var datePresentation =
        new CollectionExportFieldPresentation(
            "date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    var boolPresentation =
        new CollectionExportFieldPresentation(
            "boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    var intPresentation =
        new CollectionExportFieldPresentation(
            "number", "0", null, "pt-BR", null, null, null, "Não informado");
    return List.of(
        field("id", "Código do afastamento", "integer"),
        field("empCodigo", "Órgão", "integer"),
        field("numfunc", "Servidor afastado", "integer"),
        field("numvinc", "Sequencial do vínculo", "integer"),
        field("vinculoMatricula", "Matrícula do vínculo", "string"),
        field("tipofreq", "Tipo de frequência", "string"),
        field("codfreq", "Código da licença", "integer"),
        field("codigoFrequenciaNome", "Modalidade de licença", "string"),
        presentedField("dtini", "Início do afastamento", "date", datePresentation),
        presentedField("dtprevfim", "Término previsto", "date", datePresentation),
        presentedField("dtfim", "Término efetivo", "date", datePresentation),
        presentedField("quantidadeDias", "Duração (dias)", "integer", intPresentation),
        field("situacao", "Situação", "string"),
        presentedField("prorrogacao", "É prorrogação", "boolean", boolPresentation));
  }

  @Override
  protected AfastamentoMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected AfastamentoFilterDTO emptyFilter() {
    return new AfastamentoFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(Afastamento entity) {
    return OptionalLong.of(entity.getVersion());
  }

  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "motivo", Set.of(StatsMetric.COUNT, StatsMetric.SUM))
        .categoricalGroupByBucket("situacao", "situacao")
        .categoricalGroupByBucket("tipofreq", "tipofreq")
        .temporalTimeSeriesField("dtini", "dtini")
        .numericHistogramMeasureField("quantidadeDias", "quantidadeDias")
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
  protected void beforeCreate(CreateAfastamentoDTO dto, Afastamento entity) {
    checkOrg(entity.getEmpCodigo());
    invariants.afastamento(entity, true);
  }

  @Override
  protected void beforeUpdate(Long id, Afastamento entity, UpdateAfastamentoDTO dto) {
    checkOrg(entity.getEmpCodigo());
  }

  @Override
  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> update(
      Long id, UpdateAfastamentoDTO dto, ResourceVersionUpdatePrecondition<Long> precondition) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    var readOnlyKeys = validateCustomFields(dto, "request");
    Afastamento entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    beforeUpdate(id, entity, dto);
    var old = CustomFieldSupport.snapshotReadOnlyValues(entity, readOnlyKeys);
    mapper.applyUpdate(entity, dto);
    invariants.afastamento(entity, false);
    CustomFieldSupport.restoreReadOnlyValues(entity, readOnlyKeys, old);
    var saved = getRepository().save(entity);
    afterUpdate(id, saved, dto);
    entityManager.flush();
    entityManager.refresh(saved);
    return toResourceRepresentation(saved);
  }

  private ResourceRepresentationResult<AfastamentoResponseDTO> command(
      Long id,
      ResourceVersionUpdatePrecondition<Long> precondition,
      Consumer<Afastamento> action) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    Afastamento entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    checkOrg(entity.getEmpCodigo());
    action.accept(entity);
    invariants.afastamento(entity, false);
    var saved = getRepository().save(entity);
    entityManager.flush();
    entityManager.refresh(saved);
    return toResourceRepresentation(saved);
  }

  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> atualizarSubstituto(
      Long id,
      AtualizarSubstitutoAfastamentoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Afastamento a) -> {
          if (dto.wasSupplied("numfuncSubstituto"))
            a.setNumfuncSubstituto(dto.getNumfuncSubstituto());
          if (dto.wasSupplied("numvincSubstituto"))
            a.setNumvincSubstituto(dto.getNumvincSubstituto());
          if (dto.wasSupplied("documentoLegalId"))
            a.setDocumentoLegalId(dto.getDocumentoLegalId());
        });
  }

  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> atualizarAtosPublicacao(
      Long id,
      AtualizarAtosPublicacaoAfastamentoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Afastamento a) -> {
          if (dto.wasSupplied("documentoLegalId"))
            a.setDocumentoLegalId(dto.getDocumentoLegalId());
          if (dto.wasSupplied("atoPublicacaoId"))
            a.setAtoPublicacaoId(dto.getAtoPublicacaoId());
          if (dto.wasSupplied("prorrogacao") && dto.getProrrogacao() != null)
            a.setProrrogacao(dto.getProrrogacao());
          if (dto.wasSupplied("afastamentoOrigemId"))
            a.setAfastamentoOrigemId(dto.getAfastamentoOrigemId());
        });
  }

  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> homologarAfastamento(
      Long id,
      HomologarAfastamentoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Afastamento a) -> {
          if (!"PENDENTE_HOMOLOGACAO".equals(a.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente afastamentos pendentes de homologação podem ser homologados.");
          }
          a.setDocumentoLegalId(dto.documentoLegalId());
          if (dto.atoPublicacaoId() != null) {
            a.setAtoPublicacaoId(dto.atoPublicacaoId());
          }
          if (dto.dtprevfim() != null) {
            a.setDtprevfim(dto.dtprevfim());
            if (!dto.dtprevfim().isBefore(a.getDtini())) {
              a.setQuantidadeDias(
                  Math.toIntExact(ChronoUnit.DAYS.between(a.getDtini(), dto.dtprevfim()) + 1));
            }
          }
          a.setSituacao("HOMOLOGADO");
          Vinculo v =
              entityManager.find(
                  Vinculo.class,
                  new VinculoId(a.getNumfunc(), a.getNumvinc()),
                  LockModeType.PESSIMISTIC_WRITE);
          if (v != null && a.getDtfim() == null && "ATIVO".equals(v.getSituacao())) {
            v.setSituacao("AFASTADO");
            v.setDocumentoLegalId(dto.documentoLegalId());
          }
        });
  }

  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> rejeitarAfastamento(
      Long id,
      RejeitarAfastamentoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Afastamento a) -> {
          if (!"PENDENTE_HOMOLOGACAO".equals(a.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente afastamentos pendentes de homologação podem ser rejeitados.");
          }
          a.setSituacao("REJEITADO");
          a.setMotivo(
              (a.getMotivo() == null ? "" : a.getMotivo() + " | ")
                  + "Indeferimento: "
                  + dto.motivoRejeicao());
          if (dto.documentoLegalId() != null) {
            a.setDocumentoLegalId(dto.documentoLegalId());
          }
        });
  }

  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> prorrogarAfastamento(
      Long id,
      ProrrogarAfastamentoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Afastamento a) -> {
          if (!"HOMOLOGADO".equals(a.getSituacao()) || a.getDtfim() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A prorrogação exige um afastamento homologado e ainda em aberto.");
          }
          if (a.getDtprevfim() != null && !dto.novaDataPrevisaoFim().isAfter(a.getDtprevfim())) {
            ErgonInvariants.fail(
                "A nova data de término previsto deve ser posterior à previsão atual.");
          }
          a.setDtprevfim(dto.novaDataPrevisaoFim());
          a.setQuantidadeDias(
              Math.toIntExact(
                  ChronoUnit.DAYS.between(a.getDtini(), dto.novaDataPrevisaoFim()) + 1));
          a.setProrrogacao(true);
          a.setDocumentoLegalId(dto.documentoLegalId());
          if (dto.motivoComplementar() != null && !dto.motivoComplementar().isBlank()) {
            a.setMotivo(
                (a.getMotivo() == null ? "" : a.getMotivo() + " | ")
                    + "Prorrogação: "
                    + dto.motivoComplementar());
          }
        });
  }

  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> encerrarAfastamento(
      Long id,
      EncerrarAfastamentoItemDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Afastamento a) -> {
          if (!"HOMOLOGADO".equals(a.getSituacao()) || a.getDtfim() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "O encerramento exige um afastamento homologado e ainda sem término efetivo.");
          }
          ErgonInvariants.chronology(
              a.getDtini(),
              dto.dtfim(),
              "A data de término efetivo não pode anteceder o início do afastamento.");
          a.setDtfim(dto.dtfim());
          a.setQuantidadeDias(
              Math.toIntExact(ChronoUnit.DAYS.between(a.getDtini(), dto.dtfim()) + 1));
          a.setDocumentoLegalId(dto.documentoLegalId());
          if (dto.atoPublicacaoId() != null) {
            a.setAtoPublicacaoId(dto.atoPublicacaoId());
          }
          reconciliarSituacaoVinculoAoEncerrar(a, dto.documentoLegalId());
        });
  }

  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> cancelarAfastamento(
      Long id,
      CancelarAfastamentoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Afastamento a) -> {
          if (!Set.of("PENDENTE_HOMOLOGACAO", "HOMOLOGADO").contains(a.getSituacao())
              || a.getDtfim() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente afastamentos pendentes ou homologados em aberto podem ser cancelados.");
          }
          a.setSituacao("CANCELADO");
          a.setDocumentoLegalId(dto.documentoLegalId());
          a.setMotivo(
              (a.getMotivo() == null ? "" : a.getMotivo() + " | ")
                  + "Cancelado: "
                  + dto.motivoCancelamento());
          reconciliarSituacaoVinculoAoEncerrar(a, dto.documentoLegalId());
        });
  }

  private void reconciliarSituacaoVinculoAoEncerrar(Afastamento a, Long documentoLegalId) {
    long outrosAbertos =
        entityManager
            .createQuery(
                "select count(x) from Afastamento x where x.numfunc=:p and x.numvinc=:v and x.id<>:id and x.situacao='HOMOLOGADO' and x.dtfim is null",
                Long.class)
            .setParameter("p", a.getNumfunc())
            .setParameter("v", a.getNumvinc())
            .setParameter("id", a.getId())
            .setFlushMode(FlushModeType.COMMIT)
            .getSingleResult();
    if (outrosAbertos == 0) {
      Vinculo v =
          entityManager.find(
              Vinculo.class,
              new VinculoId(a.getNumfunc(), a.getNumvinc()),
              LockModeType.PESSIMISTIC_WRITE);
      if (v != null && "AFASTADO".equals(v.getSituacao())) {
        v.setSituacao("ATIVO");
        if (documentoLegalId != null) {
          v.setDocumentoLegalId(documentoLegalId);
        }
      }
    }
  }

  private void checkOrg(Integer orgao) {
    if (!Objects.equals(orgao, tenant.orgao())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Órgão do afastamento diverge de X-Tenant.");
    }
  }
}
