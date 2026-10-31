package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.DependenteMapper;
import br.com.praxis.ergonshowcase.repository.DependenteRepository;
import jakarta.persistence.*;
import java.util.*;
import java.util.function.Consumer;
import org.praxisplatform.uischema.concurrency.*;
import org.praxisplatform.uischema.customfields.CustomFieldSupport;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.service.base.*;
import org.praxisplatform.uischema.stats.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Operações transacionais e comandos de ciclo de vida do cadastro de dependentes. */
@Service
public class DependenteService
    extends ErgonResourceService<
        Dependente,
        DependenteResponseDTO,
        DependenteId,
        DependenteFilterDTO,
        CreateDependenteDTO,
        UpdateDependenteDTO>
    implements VersionedCreateUpdateResourceService<
        DependenteResponseDTO,
        DependenteId,
        DependenteFilterDTO,
        CreateDependenteDTO,
        UpdateDependenteDTO> {

  @Autowired private ErgonInvariants invariants;
  @PersistenceContext protected EntityManager entityManager;
  protected final DependenteMapper mapper;

  public DependenteService(DependenteRepository repository, DependenteMapper mapper) {
    super(repository, Dependente.class, "id.numfunc", "id.numdep");
    this.mapper = mapper;
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var datePresentation =
        new org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation(
            "date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    var boolPresentation =
        new org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation(
            "boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    return List.of(
        field("id", "Identificador do dependente", "string"),
        field("numfunc", "Servidor titular", "integer"),
        field("numdep", "Sequencial", "integer"),
        field("nome", "Nome do dependente", "string"),
        field("parentesco", "Parentesco", "string"),
        field("tipodepen", "Tipo de dependência", "string"),
        presentedField("dtini", "Início da vigência", "date", datePresentation),
        presentedField("dtfim", "Fim da vigência", "date", datePresentation),
        presentedField("depIr", "Deduz IRRF", "boolean", boolPresentation),
        presentedField("depSalarioFamilia", "Salário-família", "boolean", boolPresentation),
        presentedField("depPlanoSaude", "Plano de saúde", "boolean", boolPresentation),
        presentedField("estudante", "Estudante regular", "boolean", boolPresentation),
        presentedField("universitario", "Universitário", "boolean", boolPresentation));
  }

  @Override
  protected DependenteMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected DependenteFilterDTO emptyFilter() {
    return new DependenteFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(Dependente entity) {
    return OptionalLong.of(entity.getVersion());
  }

  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "nome", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("parentesco", "parentesco")
        .categoricalGroupByBucket("tipodepen", "tipodepen")
        .temporalTimeSeriesField("dtini", "dtini")
        .temporalTimeSeriesField("dtnasc", "dtnasc")
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
  public ResourceRepresentationResult<DependenteResponseDTO> update(
      DependenteId id,
      UpdateDependenteDTO dto,
      ResourceVersionUpdatePrecondition<DependenteId> precondition) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    var readOnlyKeys = validateCustomFields(dto, "request");
    Dependente entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    beforeUpdate(id, entity, dto);
    var old = CustomFieldSupport.snapshotReadOnlyValues(entity, readOnlyKeys);
    mapper.applyUpdate(entity, dto);
    invariants.dependente(entity, false);
    CustomFieldSupport.restoreReadOnlyValues(entity, readOnlyKeys, old);
    var saved = getRepository().save(entity);
    afterUpdate(id, saved, dto);
    entityManager.flush();
    entityManager.refresh(saved);
    return toResourceRepresentation(saved);
  }

  @Override
  protected void beforeCreate(CreateDependenteDTO dto, Dependente entity) {
    invariants.dependente(entity, true);
    if (getRepository().existsById(entity.getId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Dependente já cadastrado para este servidor e sequencial.");
    }
  }

  private ResourceRepresentationResult<DependenteResponseDTO> command(
      DependenteId id,
      ResourceVersionUpdatePrecondition<DependenteId> precondition,
      Consumer<Dependente> action) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    Dependente entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    action.accept(entity);
    invariants.dependente(entity, false);
    var saved = getRepository().save(entity);
    entityManager.flush();
    entityManager.refresh(saved);
    return toResourceRepresentation(saved);
  }

  @Transactional
  public ResourceRepresentationResult<DependenteResponseDTO> atualizarElegibilidade(
      DependenteId id,
      AtualizarElegibilidadeDependenteDTO dto,
      ResourceVersionUpdatePrecondition<DependenteId> precondition) {
    return command(
        id,
        precondition,
        (Dependente d) -> {
          if (dto.wasSupplied("tipodepen")) d.setTipodepen(dto.getTipodepen());
          if (dto.wasSupplied("depIr") && dto.getDepIr() != null) d.setDepIr(dto.getDepIr());
          if (dto.wasSupplied("depSalarioFamilia") && dto.getDepSalarioFamilia() != null)
            d.setDepSalarioFamilia(dto.getDepSalarioFamilia());
          if (dto.wasSupplied("depPlanoSaude") && dto.getDepPlanoSaude() != null)
            d.setDepPlanoSaude(dto.getDepPlanoSaude());
          if (dto.wasSupplied("estudante") && dto.getEstudante() != null)
            d.setEstudante(dto.getEstudante());
          if (dto.wasSupplied("universitario") && dto.getUniversitario() != null)
            d.setUniversitario(dto.getUniversitario());
          if (dto.wasSupplied("excepcional") && dto.getExcepcional() != null)
            d.setExcepcional(dto.getExcepcional());
          if (dto.wasSupplied("representanteLegal") && dto.getRepresentanteLegal() != null)
            d.setRepresentanteLegal(dto.getRepresentanteLegal());
          if (dto.wasSupplied("documentoLegalId")) d.setDocumentoLegalId(dto.getDocumentoLegalId());
        });
  }

  @Transactional
  public ResourceRepresentationResult<DependenteResponseDTO> atualizarCertidao(
      DependenteId id,
      AtualizarCertidaoDependenteDTO dto,
      ResourceVersionUpdatePrecondition<DependenteId> precondition) {
    return command(
        id,
        precondition,
        (Dependente d) -> {
          if (dto.wasSupplied("tipodocCert")) d.setTipodocCert(dto.getTipodocCert());
          if (dto.wasSupplied("numCert")) d.setNumCert(dto.getNumCert());
          if (dto.wasSupplied("livroACert")) d.setLivroACert(dto.getLivroACert());
          if (dto.wasSupplied("folhaCert")) d.setFolhaCert(dto.getFolhaCert());
          if (dto.wasSupplied("cartorioCert")) d.setCartorioCert(dto.getCartorioCert());
          if (dto.wasSupplied("ufCart")) d.setUfCart(dto.getUfCart());
          if (dto.wasSupplied("municipioCart")) d.setMunicipioCart(dto.getMunicipioCart());
          if (dto.wasSupplied("matriculaCert")) d.setMatriculaCert(dto.getMatriculaCert());
          if (dto.wasSupplied("documentoLegalId")) d.setDocumentoLegalId(dto.getDocumentoLegalId());
        });
  }

  @Transactional
  public ResourceRepresentationResult<DependenteResponseDTO> encerrarDependencia(
      DependenteId id,
      EncerrarDependenciaDTO dto,
      ResourceVersionUpdatePrecondition<DependenteId> precondition) {
    return command(
        id,
        precondition,
        (Dependente d) -> {
          if (d.getDtfim() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "A vigência deste dependente já se encontra encerrada.");
          }
          d.setDtfim(dto.dtfim());
          d.setDepIr(false);
          d.setDepSalarioFamilia(false);
          d.setDepPlanoSaude(false);
          if (dto.documentoLegalId() != null) {
            d.setDocumentoLegalId(dto.documentoLegalId());
          }
        });
  }

  @Transactional
  public ResourceRepresentationResult<DependenteResponseDTO> reativarDependencia(
      DependenteId id,
      ReativarDependenciaDTO dto,
      ResourceVersionUpdatePrecondition<DependenteId> precondition) {
    return command(
        id,
        precondition,
        (Dependente d) -> {
          if (d.getDtfim() == null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A reativação exige que a vigência da dependência esteja encerrada.");
          }
          d.setDtfim(null);
          d.setDocumentoLegalId(dto.documentoLegalId());
          d.setUniversitario(dto.universitario());
          d.setDepIr(dto.depIr());
        });
  }

  @Transactional
  public ResourceRepresentationResult<DependenteResponseDTO> homologarInvalidez(
      DependenteId id,
      HomologarInvalidezDependenteDTO dto,
      ResourceVersionUpdatePrecondition<DependenteId> precondition) {
    return command(
        id,
        precondition,
        (Dependente d) -> {
          if (Boolean.TRUE.equals(d.getInvalido())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "A invalidez permanente já foi homologada para este dependente.");
          }
          d.setInvalido(true);
          d.setDtfim(null);
          d.setDocumentoLegalId(dto.documentoLegalId());
          d.setDepIr(dto.depIr());
        });
  }
}
