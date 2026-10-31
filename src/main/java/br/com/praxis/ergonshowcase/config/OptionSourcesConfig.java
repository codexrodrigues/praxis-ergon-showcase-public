package br.com.praxis.ergonshowcase.config;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import java.util.*;
import org.praxisplatform.uischema.options.*;
import org.springframework.context.annotation.*;

/** Diretórios de identidades: a seleção não substitui as invariantes transacionais do serviço. */
@Configuration
public class OptionSourcesConfig {
  @Bean
  public OptionSourceRegistry options() {
    return OptionSourceRegistry.builder()
        .add(Orgao.class, source(ApiPaths.ORGAO_SOURCE, ApiPaths.ORGAOS, "empresa", "nome",
            "administracao-pessoal.empresas", "ficha-institucional", "ativa",
            List.of("true"), List.of("false"), List.of("fantasia", "municipio"), false,
            "Órgão inativo; mantenha apenas referências históricas já existentes.",
            Map.of("true", "Ativo", "false", "Inativo")))
        .add(Cargo.class, source(ApiPaths.CARGO_SOURCE, ApiPaths.CARGOS, "cargo", "nome",
            "administracao-pessoal.cargos", "ficha-cargo", "situacaoLookup",
            List.of("VIGENTE"), List.of("EXTINTO"), List.of("categoria", "tipoCargo"), true,
            "Cargo extinto; novos provimentos não são permitidos.",
            Map.of("VIGENTE", "Vigente", "EXTINTO", "Extinto")))
        // Pessoas e atos legais não têm estado ativo/inativo no schema. Não inventar elegibilidade.
        .add(ServidorPessoa.class, source(ApiPaths.PESSOA_SOURCE, ApiPaths.PESSOAS, "numero", "nome",
            "administracao-pessoal.funcionarios", "ficha-cadastral", null,
            List.of(), List.of(), List.of("nomeSocial"), false,
            "Cadastro civil indisponível no escopo autorizado.", Map.of()))
        .add(Vinculo.class, source(ApiPaths.VINCULO_SOURCE, ApiPaths.VINCULOS, "id", "matric",
            "administracao-pessoal.vinculos", "resumo-funcional", "situacao",
            List.of("ATIVO", "AFASTADO", "CEDIDO"), List.of("VACANTE", "APOSENTADO"),
            List.of("regimejur", "tipovinc"), true,
            "Vínculo encerrado; mantenha apenas referências históricas já existentes.",
            Map.of("ATIVO", "Ativo", "AFASTADO", "Afastado", "CEDIDO", "Cedido",
                "VACANTE", "Vacante", "APOSENTADO", "Aposentado")))
        .add(Setor.class, source(ApiPaths.SETOR_SOURCE, ApiPaths.SETORES, "setor", "nome",
            "administracao-pessoal.setores", "ficha-unidade", "situacaoLookup",
            List.of("VIGENTE"), List.of("ENCERRADO", "FUTURO"), List.of("sigla", "tipoSetor"), true,
            "Unidade fora da vigência; mantenha apenas referências históricas já existentes.",
            Map.of("VIGENTE", "Vigente", "ENCERRADO", "Encerrado", "FUTURO", "Vigência futura")))
        .add(DocumentoLegal.class, source(ApiPaths.DOCUMENTO_SOURCE, ApiPaths.DOCUMENTOS_LEGAIS, "id",
            "numeroDocumento", "administracao-pessoal.documentos-legais", "ficha-documento", null,
            List.of(), List.of(), List.of("tipoDocumento", "anoDocumento"), true,
            "Ato legal indisponível no escopo autorizado.", Map.of()))
        .build();
  }

  private OptionSourceDescriptor source(String key, String path, String id, String label,
      String entityKey, String surface, String status, List<String> allowed, List<String> blocked,
      List<String> descriptions, boolean scoped, String disabledReason, Map<String, String> statusLabels) {
    var dependencies = scoped ? Map.of("empCodigo", "empCodigo") : Map.<String, String>of();
    var filtering = new LookupFilteringDescriptor(List.of(), Map.of(),
        List.of(new LookupSortOption("descricao", label, "asc", "Descrição crescente"),
            new LookupSortOption("descricao-desc", label, "desc", "Descrição decrescente")),
        "descricao", List.of(), "Pesquisar pela descrição", List.of());
    // O provedor JPA oferece busca textual; estratégias especializadas exigiriam outro provedor.
    var displayFields = descriptions.stream().map(property -> switch (property) {
      case "fantasia" -> display(property, "Sigla", "apartment");
      case "municipio" -> display(property, "Município", "location_city");
      case "categoria" -> display(property, "Categoria", "category");
      case "tipoCargo" -> display(property, "Tipo de cargo", "work");
      case "nomeSocial" -> display(property, "Nome social", "person");
      case "regimejur" -> display(property, "Regime jurídico", "gavel");
      case "tipovinc" -> display(property, "Tipo de vínculo", "badge");
      case "sigla" -> display(property, "Sigla", "account_tree");
      case "tipoSetor" -> display(property, "Tipo de unidade", "account_tree");
      case "tipoDocumento" -> display(property, "Espécie do ato", "description");
      case "anoDocumento" -> display(property, "Ano do ato", "event");
      default -> throw new IllegalArgumentException("Campo de diretório sem curadoria: " + property);
    }).toList();
    var lookup = new EntityLookupDescriptor(entityKey, key.equals(ApiPaths.VINCULO_SOURCE) ? "matric" : id, descriptions, status, null, null,
        List.of(label), dependencies,
        new LookupSelectionPolicy(null, status, allowed, blocked, true, disabledReason, disabledReason),
        new LookupCapabilities(true, true, true, false, false, true, true, false, false, false),
        new LookupDetailDescriptor(null, null, "drawer", "surface", surface, "drawer", null,
            "view", entityKey, surface),
        new LookupDisplayDescriptor("directory", "form", "comfortable", "inline", "list", label,
            displayFields, descriptions, List.of(), null, false, true, true, status != null,
            false, true, statusLabels, 0), filtering);
    return new OptionSourceDescriptor(key, OptionSourceType.RESOURCE_ENTITY, path, null, null,
        label, id, scoped ? List.of("empCodigo") : List.of(), dependencies,
        OptionSourcePolicy.defaults(), lookup);
  }

  private LookupDisplayFieldDescriptor display(String property, String label, String icon) {
    return new LookupDisplayFieldDescriptor(property, property, label, icon, "text", "neutral", null);
  }
}
