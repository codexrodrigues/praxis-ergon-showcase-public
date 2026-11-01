package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.DocumentoLegal;
import br.com.praxis.ergonshowcase.dto.DocumentoLegalFilterDTO;
import br.com.praxis.ergonshowcase.dto.DocumentoLegalResponseDTO;
import br.com.praxis.ergonshowcase.mapper.DocumentoLegalMapper;
import br.com.praxis.ergonshowcase.repository.DocumentoLegalRepository;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.service.base.ResourceFilterAccessScope;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Fronteira estritamente de leitura para referências e inspeção contextual de documentos legais. */
@Service
public class DocumentoLegalService
    extends ErgonReadOnlyResourceService<
        DocumentoLegal, DocumentoLegalResponseDTO, Long, DocumentoLegalFilterDTO> {
  private final DocumentoLegalMapper mapper;
  private final TenantContext tenant;

  public DocumentoLegalService(DocumentoLegalRepository repository, DocumentoLegalMapper mapper) {
    this(repository, mapper, new TenantContext());
  }

  @Autowired
  public DocumentoLegalService(
      DocumentoLegalRepository repository, DocumentoLegalMapper mapper, TenantContext tenant) {
    super(repository, DocumentoLegal.class, "id");
    this.mapper = mapper;
    this.tenant = tenant != null ? tenant : new TenantContext();
  }

  @Override
  protected DocumentoLegalMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected DocumentoLegalFilterDTO emptyFilter() {
    return new DocumentoLegalFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(DocumentoLegal entity) {
    return OptionalLong.of(entity.getVersion());
  }

  @Override
  protected ResourceFilterAccessScope<DocumentoLegal> resolveResourceFilterAccessScope() {
    var explicit = tenant.explicitOrgao();
    if (explicit.isEmpty()) {
      return ResourceFilterAccessScope.unrestricted();
    }
    int orgao = explicit.getAsInt();
    return ResourceFilterAccessScope.restricted(
        (root, query, cb) -> cb.equal(root.get("empCodigo"), orgao));
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var datePresentation =
        new CollectionExportFieldPresentation(
            "date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    return List.of(
        field("id", "ID", "integer"),
        field("empCodigo", "Órgão", "integer"),
        field("tipoDocumento", "Espécie", "string"),
        field("numeroDocumento", "Número", "string"),
        field("anoDocumento", "Ano", "integer"),
        presentedField("dataPublicacao", "Publicação", "date", datePresentation),
        field("veiculoPublicacao", "Veículo", "string"),
        field("ementa", "Ementa", "string"));
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "numeroDocumento", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("tipoDocumento", "tipoDocumento")
        .categoricalGroupByBucket("anoDocumento", "anoDocumento")
        .temporalTimeSeriesField("dataPublicacao", "dataPublicacao")
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
}
