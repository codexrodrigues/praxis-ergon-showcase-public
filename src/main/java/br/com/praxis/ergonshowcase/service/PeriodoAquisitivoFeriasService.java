package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.Afastamento;
import br.com.praxis.ergonshowcase.domain.Frequencia;
import br.com.praxis.ergonshowcase.domain.PeriodoAquisitivoFerias;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.PeriodoAquisitivoFeriasMapper;
import br.com.praxis.ergonshowcase.repository.PeriodoAquisitivoFeriasRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import org.praxisplatform.uischema.concurrency.ResourceRepresentationResult;
import org.praxisplatform.uischema.concurrency.ResourceVersionPreconditionException;
import org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition;
import org.praxisplatform.uischema.customfields.CustomFieldSupport;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.service.base.ResourceFilterAccessScope;
import org.praxisplatform.uischema.service.base.VersionedCreateUpdateResourceService;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Operações transacionais, invariantes e ciclo de vida dos períodos aquisitivos de férias ({@code
 * PERAQFER} / {@code PCK_PERAQFER}).
 */
@Service
public class PeriodoAquisitivoFeriasService
    extends ErgonResourceService<
        PeriodoAquisitivoFerias,
        PeriodoAquisitivoFeriasResponseDTO,
        Long,
        PeriodoAquisitivoFeriasFilterDTO,
        CreatePeriodoAquisitivoFeriasDTO,
        UpdatePeriodoAquisitivoFeriasDTO>
    implements VersionedCreateUpdateResourceService<
        PeriodoAquisitivoFeriasResponseDTO,
        Long,
        PeriodoAquisitivoFeriasFilterDTO,
        CreatePeriodoAquisitivoFeriasDTO,
        UpdatePeriodoAquisitivoFeriasDTO> {

  @Autowired(required = false)
  private ErgonInvariants invariants;

  @PersistenceContext protected EntityManager entityManager;
  protected final PeriodoAquisitivoFeriasMapper mapper;
  private final TenantContext tenant;

  public PeriodoAquisitivoFeriasService(
      PeriodoAquisitivoFeriasRepository repository,
      PeriodoAquisitivoFeriasMapper mapper,
      TenantContext tenant) {
    super(repository, PeriodoAquisitivoFerias.class, "id");
    this.mapper = mapper;
    this.tenant = tenant != null ? tenant : new TenantContext();
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
        field("id", "ID do PA", "integer"),
        field("empCodigo", "Órgão", "integer"),
        field("numfunc", "Servidor", "integer"),
        field("numvinc", "Vínculo", "integer"),
        field("vinculoMatricula", "Matrícula", "string"),
        presentedField("chave", "Sequencial do PA", "integer", intPresentation),
        presentedField("exercicio", "Ano de exercício", "integer", intPresentation),
        presentedField("dtini", "Início do PA", "date", datePresentation),
        presentedField("dtfim", "Término do PA", "date", datePresentation),
        presentedField("totDiasDireito", "Dias de direito", "integer", intPresentation),
        presentedField("faltasApuradas", "Faltas apuradas", "integer", intPresentation),
        presentedField("diasGozados", "Dias gozados", "integer", intPresentation),
        presentedField("diasVendidosAbono", "Dias em abono", "integer", intPresentation),
        presentedField("diasConvertidosDobro", "Dias em dobro", "integer", intPresentation),
        presentedField("saldoDiasDisponivel", "Saldo disponível", "integer", intPresentation),
        field("situacao", "Situação", "string"),
        presentedField("dtPrescricao", "Data de prescrição", "date", datePresentation),
        presentedField("geradoPorAverbacao", "Gerado por averbação", "boolean", boolPresentation));
  }

  @Override
  protected PeriodoAquisitivoFeriasMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected PeriodoAquisitivoFeriasFilterDTO emptyFilter() {
    return new PeriodoAquisitivoFeriasFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(PeriodoAquisitivoFerias entity) {
    return OptionalLong.of(entity.getVersion());
  }

  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  protected ResourceFilterAccessScope<PeriodoAquisitivoFerias> resolveResourceFilterAccessScope() {
    int orgao = tenant.orgao();
    return ResourceFilterAccessScope.restricted(
        (root, query, cb) -> cb.equal(root.get("empCodigo"), orgao));
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket(
            "id", "id", "situacao", Set.of(StatsMetric.COUNT, StatsMetric.SUM))
        .categoricalGroupByBucket("situacao", "situacao")
        .categoricalGroupByBucket("exercicio", "exercicio")
        .categoricalGroupByBucket("geradoPorAverbacao", "geradoPorAverbacao")
        .temporalTimeSeriesField("dtini", "dtini")
        .temporalTimeSeriesField("dtfim", "dtfim")
        .numericHistogramMeasureField("saldoDiasDisponivel", "saldoDiasDisponivel")
        .numericHistogramMeasureField("totDiasDireito", "totDiasDireito")
        .numericHistogramMeasureField("faltasApuradas", "faltasApuradas")
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
      CreatePeriodoAquisitivoFeriasDTO dto, PeriodoAquisitivoFerias entity) {
    checkOrg(entity.getEmpCodigo());
    if (invariants != null) {
      invariants.periodoAquisitivoFerias(entity, true);
    }
  }

  @Override
  protected void beforeUpdate(
      Long id, PeriodoAquisitivoFerias entity, UpdatePeriodoAquisitivoFeriasDTO dto) {
    checkOrg(entity.getEmpCodigo());
  }

  @Override
  @Transactional
  public ResourceRepresentationResult<PeriodoAquisitivoFeriasResponseDTO> update(
      Long id,
      UpdatePeriodoAquisitivoFeriasDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    var readOnlyKeys = validateCustomFields(dto, "request");
    PeriodoAquisitivoFerias entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    beforeUpdate(id, entity, dto);
    var old = CustomFieldSupport.snapshotReadOnlyValues(entity, readOnlyKeys);
    mapper.applyUpdate(entity, dto);
    if (invariants != null) {
      invariants.periodoAquisitivoFerias(entity, false);
    }
    CustomFieldSupport.restoreReadOnlyValues(entity, readOnlyKeys, old);
    var saved = getRepository().save(entity);
    afterUpdate(id, saved, dto);
    entityManager.flush();
    entityManager.refresh(saved);
    return readFacts != null
        ? enriched(toResourceRepresentation(saved))
        : toResourceRepresentation(saved);
  }

  private ResourceRepresentationResult<PeriodoAquisitivoFeriasResponseDTO> command(
      Long id,
      ResourceVersionUpdatePrecondition<Long> precondition,
      Consumer<PeriodoAquisitivoFerias> action) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    PeriodoAquisitivoFerias entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    checkOrg(entity.getEmpCodigo());
    action.accept(entity);
    if (invariants != null && !"CANCELADO".equals(entity.getSituacao())) {
      invariants.periodoAquisitivoFerias(entity, false);
    }
    var saved = getRepository().save(entity);
    entityManager.flush();
    entityManager.refresh(saved);
    return readFacts != null
        ? enriched(toResourceRepresentation(saved))
        : toResourceRepresentation(saved);
  }

  @Transactional
  public ResourceRepresentationResult<PeriodoAquisitivoFeriasResponseDTO> atualizarOverrides(
      Long id,
      AtualizarOverridesPeriodoAquisitivoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (PeriodoAquisitivoFerias pa) -> {
          pa.setMaxPerFer(dto.getMaxPerFer());
          pa.setMinPerFer(dto.getMinPerFer());
          pa.setMaxDiasPerFer(dto.getMaxDiasPerFer());
          pa.setMinDiasPerFer(dto.getMinDiasPerFer());
          pa.setMinDiasPerPrincFer(dto.getMinDiasPerPrincFer());
          if (dto.getAbonoDirGozo() != null) pa.setAbonoDirGozo(dto.getAbonoDirGozo());
          if (dto.getAbonoConstFer() != null) pa.setAbonoConstFer(dto.getAbonoConstFer());
          if (dto.getAdiant13Fer() != null) pa.setAdiant13Fer(dto.getAdiant13Fer());
          if (dto.getAdiantSalFer() != null) pa.setAdiantSalFer(dto.getAdiantSalFer());
          if (dto.getEmprestFer() != null) pa.setEmprestFer(dto.getEmprestFer());
          if (dto.getPermiteFerEstePer() != null)
            pa.setPermiteFerEstePer(dto.getPermiteFerEstePer());
          pa.setMaxDiasVendFer(dto.getMaxDiasVendFer());
          pa.setMinDiasPvendFer(dto.getMinDiasPvendFer());
          pa.setNumDiasAbonoFer(dto.getNumDiasAbonoFer());
          pa.setDenDiasAbonoFer(dto.getDenDiasAbonoFer());
          if (dto.getContDobroFerias() != null) pa.setContDobroFerias(dto.getContDobroFerias());
          pa.setMinDiasPeraqfer(dto.getMinDiasPeraqfer());
          pa.setMaxDiasPeraqfer(dto.getMaxDiasPeraqfer());
        });
  }

  @Transactional
  public ResourceRepresentationResult<PeriodoAquisitivoFeriasResponseDTO> atualizarPrescricaoMulta(
      Long id,
      AtualizarPrescricaoMultaPeriodoAquisitivoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (PeriodoAquisitivoFerias pa) -> {
          pa.setTotDiasDireito(dto.getTotDiasDireito());
          pa.setFaltasApuradas(dto.getFaltasApuradas());
          pa.setDiasAfastamentoSuspensivo(dto.getDiasAfastamentoSuspensivo());
          pa.setDtPrescricao(dto.getDtPrescricao());
          if (dto.getCriterioPrescricao() != null)
            pa.setCriterioPrescricao(dto.getCriterioPrescricao());
          if (dto.getPermiteGozoPosPrescricao() != null)
            pa.setPermiteGozoPosPrescricao(dto.getPermiteGozoPosPrescricao());
          pa.setDtPgtoComMulta(dto.getDtPgtoComMulta());
          if (dto.getSemPgtoMulta() != null) pa.setSemPgtoMulta(dto.getSemPgtoMulta());
          if (dto.getDocumentoLegalId() != null) pa.setDocumentoLegalId(dto.getDocumentoLegalId());
          if (dto.getObservacoes() != null) pa.setObservacoes(dto.getObservacoes());
          PeriodoAquisitivoFeriasMapper.recomputeSaldoAndSituacao(pa);
        });
  }

  @Transactional
  public ResourceRepresentationResult<PeriodoAquisitivoFeriasResponseDTO>
      suspenderPeriodoAquisitivo(
          Long id,
          TransicaoPeriodoAquisitivoFeriasDTO dto,
          ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (PeriodoAquisitivoFerias pa) -> {
          if (!Set.of("EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO")
              .contains(pa.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente períodos em aquisição, disponíveis ou parcialmente gozados podem ser suspensos.");
          }
          pa.setSituacao("SUSPENSO");
          if (dto.getDocumentoLegalId() != null) pa.setDocumentoLegalId(dto.getDocumentoLegalId());
          pa.setObservacoes(dto.getObservacoes());
        });
  }

  @Transactional
  public ResourceRepresentationResult<PeriodoAquisitivoFeriasResponseDTO>
      homologarPeriodoAquisitivo(
          Long id,
          TransicaoPeriodoAquisitivoFeriasDTO dto,
          ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (PeriodoAquisitivoFerias pa) -> {
          if (!Set.of("EM_AQUISICAO", "SUSPENSO").contains(pa.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente períodos aquisitivos em aquisição ou suspensos podem ser homologados.");
          }
          pa.setSituacao("DISPONIVEL");
          PeriodoAquisitivoFeriasMapper.recomputeSaldoAndSituacao(pa);
          if (dto.getDocumentoLegalId() != null) pa.setDocumentoLegalId(dto.getDocumentoLegalId());
          pa.setObservacoes(dto.getObservacoes());
        });
  }

  @Transactional
  public ResourceRepresentationResult<PeriodoAquisitivoFeriasResponseDTO>
      reativarPeriodoAquisitivo(
          Long id,
          TransicaoPeriodoAquisitivoFeriasDTO dto,
          ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (PeriodoAquisitivoFerias pa) -> {
          if (!"SUSPENSO".equals(pa.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente períodos aquisitivos suspensos podem ser reativados.");
          }
          if (dto.getDtPrescricao() != null) pa.setDtPrescricao(dto.getDtPrescricao());
          if (dto.getDocumentoLegalId() != null) pa.setDocumentoLegalId(dto.getDocumentoLegalId());
          pa.setObservacoes(dto.getObservacoes());
          pa.setSituacao("DISPONIVEL");
          PeriodoAquisitivoFeriasMapper.recomputeSaldoAndSituacao(pa);
        });
  }

  @Transactional
  public ResourceRepresentationResult<PeriodoAquisitivoFeriasResponseDTO>
      prescreverPeriodoAquisitivo(
          Long id,
          TransicaoPeriodoAquisitivoFeriasDTO dto,
          ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (PeriodoAquisitivoFerias pa) -> {
          if (!Set.of("DISPONIVEL", "PARCIALMENTE_GOZADO").contains(pa.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente períodos aquisitivos disponíveis ou parcialmente gozados podem ser declarados prescritos.");
          }
          if (dto.getDtPrescricao() != null) pa.setDtPrescricao(dto.getDtPrescricao());
          pa.setSituacao("PRESCRITO");
          pa.setSaldoDiasDisponivel(0);
          if (dto.getDocumentoLegalId() != null) pa.setDocumentoLegalId(dto.getDocumentoLegalId());
          pa.setObservacoes(dto.getObservacoes());
        });
  }

  @Transactional
  public ResourceRepresentationResult<PeriodoAquisitivoFeriasResponseDTO>
      converterDobroPeriodoAquisitivo(
          Long id,
          ConverterDobroPeriodoAquisitivoDTO dto,
          ResourceVersionUpdatePrecondition<Long> precondition) {
    return converterSaldoDobro(id, dto, precondition);
  }

  @Transactional
  public ResourceRepresentationResult<PeriodoAquisitivoFeriasResponseDTO> converterSaldoDobro(
      Long id,
      ConverterDobroPeriodoAquisitivoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (PeriodoAquisitivoFerias pa) -> {
          if (!Set.of("DISPONIVEL", "PARCIALMENTE_GOZADO").contains(pa.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente períodos disponíveis ou parcialmente gozados admitem conversão em dobro.");
          }
          if ("N".equals(pa.getContDobroFerias())) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "O período aquisitivo possui override que veda conversão em contagem em dobro.");
          }
          int saldoAtual = pa.getSaldoDiasDisponivel() != null ? pa.getSaldoDiasDisponivel() : 0;
          if (dto.getDiasConvertidosDobro() > saldoAtual) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "A quantidade de dias a converter em dobro excede o saldo disponível do período aquisitivo.");
          }
          int acumulado =
              (pa.getDiasConvertidosDobro() != null ? pa.getDiasConvertidosDobro() : 0)
                  + dto.getDiasConvertidosDobro();
          pa.setDiasConvertidosDobro(acumulado);
          if (dto.getDocumentoLegalId() != null) pa.setDocumentoLegalId(dto.getDocumentoLegalId());
          pa.setObservacoes(dto.getObservacoes());
          PeriodoAquisitivoFeriasMapper.recomputeSaldoAndSituacao(pa);
        });
  }

  @Transactional
  public ResourceRepresentationResult<PeriodoAquisitivoFeriasResponseDTO>
      cancelarPeriodoAquisitivo(
          Long id,
          TransicaoPeriodoAquisitivoFeriasDTO dto,
          ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (PeriodoAquisitivoFerias pa) -> {
          if ("CANCELADO".equals(pa.getSituacao()) || "QUITADO".equals(pa.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Períodos aquisitivos quitados ou já cancelados não podem ser cancelados.");
          }
          pa.setSituacao("CANCELADO");
          pa.setSaldoDiasDisponivel(0);
          if (dto.getDocumentoLegalId() != null) pa.setDocumentoLegalId(dto.getDocumentoLegalId());
          pa.setObservacoes(dto.getObservacoes());
        });
  }

  @Transactional
  public ResourceRepresentationResult<PeriodoAquisitivoFeriasResponseDTO>
      recalcularFaltasDireitoPeriodo(
          Long id,
          TransicaoPeriodoAquisitivoFeriasDTO dto,
          ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (PeriodoAquisitivoFerias pa) -> {
          if (!Set.of("EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO", "SUSPENSO")
              .contains(pa.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente períodos em aquisição, disponíveis, parcialmente gozados ou suspensos admitem recálculo de faltas e direito.");
          }
          int faltas = 0;
          int diasSuspensivos = 0;
          LocalDate iniPa = pa.getDtini();
          LocalDate fimPa = pa.getDtfim();
          if (entityManager != null && iniPa != null && fimPa != null) {
            var qFreq =
                entityManager.createQuery(
                    "select f from Frequencia f where f.numfunc=:f and f.numvinc=:v and f.situacao='HOMOLOGADO'",
                    Frequencia.class);
            if (qFreq != null) {
              var p1 = qFreq.setParameter("f", pa.getNumfunc());
              var p2 = p1 != null ? p1.setParameter("v", pa.getNumvinc()) : null;
              var freqs = p2 != null ? p2.getResultList() : List.<Frequencia>of();
              if (freqs != null) {
                for (Frequencia f : freqs) {
                  if (f.getDtini() == null) continue;
                  LocalDate fEnd = f.getDtfim() != null ? f.getDtfim() : f.getDtini();
                  if (f.getDtini().isAfter(fimPa) || fEnd.isBefore(iniPa)) continue;
                  boolean desconta =
                      "FALTA".equalsIgnoreCase(f.getTipofreq())
                          || (f.getCodigoFrequenciaEntidade() != null
                              && f.getCodigoFrequenciaEntidade().getQtdFaltas() != null
                              && f.getCodigoFrequenciaEntidade()
                                      .getQtdFaltas()
                                      .compareTo(BigDecimal.ZERO)
                                  > 0);
                  if (desconta) {
                    int qtd =
                        f.getQuantidade() != null && f.getQuantidade().intValue() > 0
                            ? f.getQuantidade().intValue()
                            : Math.max(
                                1,
                                Math.toIntExact(
                                    ChronoUnit.DAYS.between(
                                            f.getDtini().isBefore(iniPa) ? iniPa : f.getDtini(),
                                            fEnd.isAfter(fimPa) ? fimPa : fEnd)
                                        + 1));
                    faltas += qtd;
                  }
                }
              }
            }
            var qAfast =
                entityManager.createQuery(
                    "select a from Afastamento a where a.numfunc=:f and a.numvinc=:v and a.situacao='HOMOLOGADO'",
                    Afastamento.class);
            if (qAfast != null) {
              var p1 = qAfast.setParameter("f", pa.getNumfunc());
              var p2 = p1 != null ? p1.setParameter("v", pa.getNumvinc()) : null;
              var afasts = p2 != null ? p2.getResultList() : List.<Afastamento>of();
              if (afasts != null) {
                for (Afastamento a : afasts) {
                  if (a.getDtini() == null) continue;
                  LocalDate aEnd =
                      a.getDtfim() != null
                          ? a.getDtfim()
                          : (a.getDtprevfim() != null ? a.getDtprevfim() : fimPa);
                  if (a.getDtini().isAfter(fimPa) || aEnd.isBefore(iniPa)) continue;
                  LocalDate overlapIni = a.getDtini().isBefore(iniPa) ? iniPa : a.getDtini();
                  LocalDate overlapFim = aEnd.isAfter(fimPa) ? fimPa : aEnd;
                  int dias =
                      Math.max(1, Math.toIntExact(ChronoUnit.DAYS.between(overlapIni, overlapFim) + 1));
                  diasSuspensivos += dias;
                }
              }
            }
          }
          pa.setFaltasApuradas(faltas);
          pa.setDiasAfastamentoSuspensivo(diasSuspensivos);
          int diasDireito = calcularDiasDireitoPorFaltas(faltas);
          pa.setTotDiasDireito(diasDireito);
          if (dto != null) {
            if (dto.getDocumentoLegalId() != null) pa.setDocumentoLegalId(dto.getDocumentoLegalId());
            if (dto.getObservacoes() != null) pa.setObservacoes(dto.getObservacoes());
          }
          PeriodoAquisitivoFeriasMapper.recomputeSaldoAndSituacao(pa);
        });
  }

  public static int calcularDiasDireitoPorFaltas(int faltasInjustificadas) {
    if (faltasInjustificadas <= 5) return 30;
    if (faltasInjustificadas <= 14) return 24;
    if (faltasInjustificadas <= 23) return 18;
    if (faltasInjustificadas <= 32) return 12;
    return 0;
  }

  private void checkOrg(Integer orgao) {
    if (!Objects.equals(orgao, tenant.orgao())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Órgão do período aquisitivo de férias diverge de X-Tenant.");
    }
  }
}
