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
        CargoResponseDTO, Integer, CargoFilterDTO, CreateCargoDTO, UpdateCargoDTO>,
        DuplicateDraftResourceService<CargoResponseDTO, Integer, CargoFilterDTO,
            CreateCargoDTO, UpdateCargoDTO, CargoDuplicateDraftDTO> {
  @org.springframework.beans.factory.annotation.Autowired private ErgonInvariants invariants;
  protected final CargoMapper mapper;
  private final TenantContext tenant;
  private final CargoRepository cargos;

  public CargoService(CargoRepository repository, CargoMapper mapper, TenantContext tenant) {
    super(repository, Cargo.class, "cargo");
    this.mapper = mapper;
    this.tenant = tenant;
    this.cargos = repository;
  }

  /**
   * Preenchimento inicial de classificação, sem reproduzir autorização legal de vagas.
   * Identidade, órgão, ato, datas, agregação e atributos livres não são copiados.
   * A transação própria de leitura não descarrega alterações pendentes do chamador.
   */
  @Override
  @org.springframework.security.access.prepost.PreAuthorize(
      "hasAuthority('ERGON_READ') and hasAuthority('ERGON_WRITE')")
  @Transactional(readOnly = true, propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public CargoDuplicateDraftDTO duplicateDraft(Integer sourceId) {
    var source = cargos.findByCargoAndEmpCodigo(sourceId, tenant.orgao())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
            "duplicate-draft-unavailable"));
    if (source.getDtExtincao() != null)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "duplicate-draft-unavailable");
    var draft = new CargoDuplicateDraftDTO();
    draft.setNome(source.getNome());
    draft.setCategoria(source.getCategoria());
    draft.setSubcategoria(source.getSubcategoria());
    draft.setTipoCargo(source.getTipoCargo());
    draft.setCargoFuncao(source.getCargoFuncao());
    draft.setEscolaridade(source.getEscolaridade());
    draft.setCbo(source.getCbo());
    return draft;
  }

  @Override
  protected List<org.praxisplatform.uischema.exporting.CollectionExportField> exportFields() {
    return List.of(
        field("id", "Código", "integer"),
        field("nome", "Cargo", "string"),
        field("empCodigo", "Órgão", "integer"),
        field("categoria", "Categoria", "string"),
        presentedField("vagasAutorizadas", "Vagas autorizadas", "integer",
            new org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation(
                "number", "0", null, "pt-BR", null, null, null, "Não informado")),
        presentedField("controleVaga", "Controle de vagas", "boolean",
            new org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation(
                "boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado")));
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
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket(
            "id",
            "cargo",
            "nome",
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
            "categoria",
            "categoria",
            Set.of(
                StatsMetric.COUNT,
                StatsMetric.SUM,
                StatsMetric.AVG,
                StatsMetric.MEDIAN,
                StatsMetric.PERCENTILE_90))
        .temporalTimeSeriesField("dtInicioContrVaga", "dtInicioContrVaga")
        .numericHistogramMeasureField("vagasAutorizadas", "vagasAutorizadas")
        .build();
  }



  @Override
  protected void applyResourceUpdate(Integer id, Cargo entity, UpdateCargoDTO dto) {
    mapper.applyUpdate(entity, dto);
    checkOrg(entity.getEmpCodigo());
    invariants.cargo(entity);
  }

  @Override
  protected void beforeCreate(CreateCargoDTO dto, Cargo e) {
    checkOrg(e.getEmpCodigo());
    invariants.cargo(e);
    if (getRepository().existsById(e.getCargo()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cargo já cadastrado.");
  }

  @Override
  protected void beforeUpdate(Integer id, Cargo e, UpdateCargoDTO dto) {
    checkOrg(e.getEmpCodigo());
  }

  @Override
  protected void beforeDelete(Integer id, Cargo entity) {
    if (entity != null) {
      checkOrg(entity.getEmpCodigo());
    }
    if (getEntityManager() != null && id != null && entity != null) {
      long ocupacoes =
          getEntityManager()
              .createQuery(
                  "select count(v) from Vinculo v where v.empCodigo=:org and (v.cargo=:c or v.cargoComissao=:c)",
                  Long.class)
              .setParameter("org", entity.getEmpCodigo())
              .setParameter("c", id)
              .getSingleResult();
      if (ocupacoes > 0) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Não é permitido excluir cargo que possui vínculos funcionais associados.");
      }
    }
  }

  private void checkOrg(Integer orgao) {
    if (!java.util.Objects.equals(orgao, tenant.orgao()))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Órgão do cargo diverge de X-Tenant.");
  }
  @Override protected java.util.Optional<org.praxisplatform.uischema.capability.ResourceStateSnapshot> toResourceStateSnapshot(Cargo e) {
    return java.util.Optional.of(snapshotState(e));
  }
  private org.praxisplatform.uischema.capability.ResourceStateSnapshot snapshotState(Cargo e) {
    return org.praxisplatform.uischema.capability.ResourceStateSnapshot.of(e.getDtExtincao() == null ? "VIGENTE" : "EXTINTO");
  }

  @Override protected String resolveResourceKey() { return "administracao-pessoal.cargos"; }

}
