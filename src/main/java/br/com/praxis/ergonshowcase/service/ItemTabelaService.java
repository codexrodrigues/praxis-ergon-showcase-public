package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.ItemTabela;
import br.com.praxis.ergonshowcase.domain.ItemTabelaId;
import br.com.praxis.ergonshowcase.dto.ItemTabelaFilterDTO;
import br.com.praxis.ergonshowcase.dto.ItemTabelaResponseDTO;
import br.com.praxis.ergonshowcase.mapper.ItemTabelaMapper;
import br.com.praxis.ergonshowcase.repository.ItemTabelaRepository;
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

/** Serviço estritamente de leitura para os itens de tabelas gerais de domínio (HADES.ITEMTABELA_). */
@Service
public class ItemTabelaService
    extends ErgonReadOnlyResourceService<
        ItemTabela, ItemTabelaResponseDTO, ItemTabelaId, ItemTabelaFilterDTO> {
  private final ItemTabelaMapper mapper;
  private final TenantContext tenant;

  public ItemTabelaService(ItemTabelaRepository repository, ItemTabelaMapper mapper) {
    this(repository, mapper, new TenantContext());
  }

  @Autowired
  public ItemTabelaService(
      ItemTabelaRepository repository, ItemTabelaMapper mapper, TenantContext tenant) {
    super(repository, ItemTabela.class, "id.tab", "id.item");
    this.mapper = mapper;
    this.tenant = tenant != null ? tenant : new TenantContext();
  }

  @Override
  protected ItemTabelaMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected ItemTabelaFilterDTO emptyFilter() {
    return new ItemTabelaFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(ItemTabela entity) {
    return OptionalLong.of(entity.getVersion());
  }

  @Override
  protected ResourceFilterAccessScope<ItemTabela> resolveResourceFilterAccessScope() {
    var explicit = tenant.explicitOrgao();
    if (explicit.isEmpty()) {
      return ResourceFilterAccessScope.unrestricted();
    }
    int orgao = explicit.getAsInt();
    return ResourceFilterAccessScope.restricted(
        (root, query, cb) ->
            cb.or(cb.isNull(root.get("empCodigo")), cb.equal(root.get("empCodigo"), orgao)));
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var boolPresentation =
        new CollectionExportFieldPresentation(
            "boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    return List.of(
        field("id", "Identificador", "string"),
        field("tab", "Tabela", "string"),
        field("item", "Código do item", "string"),
        field("descr", "Descrição", "string"),
        field("empCodigo", "Órgão", "integer"),
        field("ordem", "Ordem", "integer"),
        presentedField("ativo", "Ativo", "boolean", boolPresentation));
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "descr", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("tab", "id.tab")
        .categoricalGroupByBucket("ativo", "ativo")
        .numericHistogramMeasureField("ordem", "ordem")
        .build();
  }

  @Override
  public StatsSupportMode getGroupByStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }

  @Override
  public StatsSupportMode getDistributionStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }
}
