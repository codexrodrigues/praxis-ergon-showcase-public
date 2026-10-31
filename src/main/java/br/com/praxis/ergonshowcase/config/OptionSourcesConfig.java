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
        .add(TipoDependente.class, source(ApiPaths.TIPO_DEPENDENTE_SOURCE, ApiPaths.TIPOS_DEPENDENTE,
            "tipodepen", "descricao", "administracao-pessoal.tipos-dependente", "ficha-tipo-dependente",
            null, List.of(), List.of(), List.of("geraIr", "idadeLimite"), false,
            "Classificação de dependência indisponível no escopo autorizado.", Map.of()))
        .add(Dependente.class, source(ApiPaths.DEPENDENTE_SOURCE, ApiPaths.DEPENDENTES, "id", "nome",
            "administracao-pessoal.dependentes", "ficha-dependente", "situacaoLookup",
            List.of("VIGENTE"), List.of("ENCERRADO", "FUTURO"), List.of("parentesco", "tipodepen"),
            false, "Dependência encerrada ou futura; mantenha apenas referências históricas já existentes.",
            Map.of("VIGENTE", "Vigente", "ENCERRADO", "Encerrado", "FUTURO", "Vigência futura")))
        .add(TipoFrequencia.class, source(ApiPaths.TIPO_FREQUENCIA_SOURCE, ApiPaths.TIPOS_FREQUENCIA,
            "tipo", "nome", "administracao-pessoal.tipos-frequencia", "ficha-tipo-frequencia",
            null, List.of(), List.of(), List.of("influiConta", "horaExtra"), false,
            "Natureza de frequência indisponível no escopo autorizado.", Map.of()))
        .add(CodigoFrequencia.class, source(ApiPaths.CODIGO_FREQUENCIA_SOURCE, ApiPaths.CODIGOS_FREQUENCIA,
            "id", "nome", "administracao-pessoal.codigos-frequencia", "ficha-codigo-frequencia",
            null, List.of(), List.of(), List.of("nomeAbrev", "formatoQtd"), false,
            "Código de frequência indisponível no escopo autorizado.", Map.of()))
        .add(Afastamento.class, source(ApiPaths.AFASTAMENTO_SOURCE, ApiPaths.AFASTAMENTOS,
            "id", "motivo", "administracao-pessoal.afastamentos", "ficha-afastamento",
            "situacao", List.of("HOMOLOGADO", "PENDENTE_HOMOLOGACAO"),
            List.of("REJEITADO", "CANCELADO"), List.of("tipofreq", "dtini"), true,
            "Licença rejeitada ou cancelada; mantenha apenas referências históricas já existentes.",
            Map.of("HOMOLOGADO", "Homologado", "PENDENTE_HOMOLOGACAO", "Pendente",
                "REJEITADO", "Rejeitado", "CANCELADO", "Cancelado")))
        .add(RegraFrequenciaEstatutaria.class, source(ApiPaths.REGRA_FREQUENCIA_ESTATUTARIA_SOURCE,
            ApiPaths.REGRAS_FREQUENCIA_ESTATUTARIA, "id", "ruleKey",
            "administracao-pessoal.regras-frequencia-estatutaria", "ficha-regra-frequencia-estatutaria",
            "ativa", List.of("true"), List.of("false"), List.of("regimeJuridico", "tipoVinculo"), false,
            "Regra estatutária inativa; mantenha apenas referências históricas já existentes.",
            Map.of("true", "Ativa", "false", "Inativa")))
        .add(Frequencia.class, source(ApiPaths.FREQUENCIA_SOURCE, ApiPaths.FREQUENCIAS,
            "id", "obs", "administracao-pessoal.frequencias", "ficha-frequencia",
            "situacao", List.of("HOMOLOGADO", "PENDENTE_HOMOLOGACAO"),
            List.of("REJEITADO", "CANCELADO"), List.of("tipofreq", "dtini"), true,
            "Ocorrência de frequência rejeitada ou cancelada; mantenha apenas referências históricas já existentes.",
            Map.of("HOMOLOGADO", "Homologado", "PENDENTE_HOMOLOGACAO", "Pendente",
                "REJEITADO", "Rejeitado", "CANCELADO", "Cancelado")))
        .add(Tabela.class, source(ApiPaths.TABELA_SOURCE, ApiPaths.TABELAS,
            "tab", "descr", "administracao-pessoal.tabelas", "ficha-tabela",
            null, List.of(), List.of(), List.of("sis", "tipo"), false,
            "Tabela geral indisponível no escopo autorizado.", Map.of()))
        .add(ItemTabela.class, source(ApiPaths.ITEM_TABELA_SOURCE, ApiPaths.ITENS_TABELA,
            "id", "descr", "administracao-pessoal.itens-tabela", "ficha-item-tabela",
            null, List.of(), List.of(), List.of("tab", "item"), false,
            "Item de tabela indisponível no escopo autorizado.", Map.of()))
        .add(DefinicaoCampoCustomizado.class, source(ApiPaths.CAMPO_CUSTOMIZADO_SOURCE, ApiPaths.CAMPOS_CUSTOMIZADOS,
            "id", "label", "administracao-pessoal.campos-customizados", "ficha-campo-customizado",
            "active", List.of("true"), List.of("false"), List.of("resourceKey", "fieldKey"), true,
            "Definição de campo customizado inativa; mantenha apenas referências históricas já existentes.",
            Map.of("true", "Ativo", "false", "Inativo")))
        .add(ParametroContagemFerias.class, source(ApiPaths.PARAMETRO_CONTAGEM_FERIAS_SOURCE,
            ApiPaths.PARAMETROS_CONTAGEM_FERIAS, "id", "regimeJuridico",
            "administracao-pessoal.parametros-contagem-ferias", "ficha-parametro-contagem-ferias",
            "active", List.of("true"), List.of("false"), List.of("tipoVinculo", "categoria"), true,
            "Parâmetro estatutário de férias inativo; mantenha apenas referências históricas já existentes.",
            Map.of("true", "Ativo", "false", "Inativo")))
        .add(PeriodoAquisitivoFerias.class, source(ApiPaths.PERIODO_AQUISITIVO_FERIAS_SOURCE,
            ApiPaths.PERIODOS_AQUISITIVOS_FERIAS, "id", "chave",
            "administracao-pessoal.periodos-aquisitivos-ferias", "ficha-periodo-aquisitivo",
            "situacao", List.of("DISPONIVEL", "PARCIALMENTE_GOZADO", "EM_AQUISICAO"),
            List.of("QUITADO", "PRESCRITO", "CANCELADO", "SUSPENSO"), List.of("exercicio", "dtini"), true,
            "Período aquisitivo sem saldo disponível ou encerrado; mantenha apenas referências históricas.",
            Map.of("EM_AQUISICAO", "Em aquisição", "DISPONIVEL", "Disponível",
                "PARCIALMENTE_GOZADO", "Parcialmente gozado", "QUITADO", "Quitado",
                "PRESCRITO", "Prescrito", "SUSPENSO", "Suspenso", "CANCELADO", "Cancelado")))
        .add(FeriasGozo.class, source(ApiPaths.FERIAS_GOZO_SOURCE, ApiPaths.FERIAS_GOZO,
            "id", "chavePeraqfer", "administracao-pessoal.ferias-gozo", "ficha-ferias-gozo",
            "situacao", List.of("PROGRAMADA", "HOMOLOGADA", "EM_GOZO", "GOZADA"),
            List.of("CANCELADA", "INTERROMPIDA"), List.of("ordemParcela", "dtini"), true,
            "Parcela de gozo de férias cancelada ou interrompida; mantenha apenas referências históricas.",
            Map.of("PROGRAMADA", "Programada", "HOMOLOGADA", "Homologada", "EM_GOZO", "Em gozo",
                "GOZADA", "Gozada", "INTERROMPIDA", "Interrompida", "CANCELADA", "Cancelada")))
        .build();
  }

  private OptionSourceDescriptor source(String key, String path, String id, String label,
      String entityKey, String surface, String status, List<String> allowed, List<String> blocked,
      List<String> descriptions, boolean scoped, String disabledReason, Map<String, String> statusLabels) {
    var dependencies = key.equals(ApiPaths.PERIODO_AQUISITIVO_FERIAS_SOURCE)
        ? Map.of("empCodigo", "empCodigo", "numfunc", "numfunc", "numvinc", "numvinc")
        : key.equals(ApiPaths.FERIAS_GOZO_SOURCE)
        ? Map.of("empCodigo", "empCodigo", "periodoAquisitivoId", "periodoAquisitivoId")
        : scoped ? Map.of("empCodigo", "empCodigo")
        : key.equals(ApiPaths.DEPENDENTE_SOURCE) ? Map.of("numfunc", "numfunc")
        : key.equals(ApiPaths.CODIGO_FREQUENCIA_SOURCE) ? Map.of("tipofreq", "tipofreq")
        : key.equals(ApiPaths.ITEM_TABELA_SOURCE) ? Map.of("tab", "tab")
        : Map.<String, String>of();
    var dependsOn = key.equals(ApiPaths.PERIODO_AQUISITIVO_FERIAS_SOURCE)
        ? List.of("empCodigo", "numfunc", "numvinc")
        : key.equals(ApiPaths.FERIAS_GOZO_SOURCE)
        ? List.of("empCodigo", "periodoAquisitivoId")
        : scoped ? List.of("empCodigo")
        : key.equals(ApiPaths.DEPENDENTE_SOURCE) ? List.of("numfunc")
        : key.equals(ApiPaths.CODIGO_FREQUENCIA_SOURCE) ? List.of("tipofreq")
        : key.equals(ApiPaths.ITEM_TABELA_SOURCE) ? List.of("tab")
        : List.<String>of();
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
      case "regimeJuridico" -> display(property, "Regime jurídico", "gavel");
      case "tipovinc" -> display(property, "Tipo de vínculo", "badge");
      case "tipoVinculo" -> display(property, "Tipo de vínculo", "badge");
      case "sigla" -> display(property, "Sigla", "account_tree");
      case "tipoSetor" -> display(property, "Tipo de unidade", "account_tree");
      case "tipoDocumento" -> display(property, "Espécie do ato", "description");
      case "anoDocumento" -> display(property, "Ano do ato", "event");
      case "geraIr" -> display(property, "Deduz IRRF", "receipt_long");
      case "idadeLimite" -> display(property, "Idade limite", "cake");
      case "parentesco" -> display(property, "Parentesco", "family_restroom");
      case "tipodepen" -> display(property, "Modalidade", "category");
      case "influiConta" -> display(property, "Influi contagem", "schedule");
      case "horaExtra" -> display(property, "Hora extra", "more_time");
      case "nomeAbrev" -> display(property, "Abreviação", "short_text");
      case "formatoQtd" -> display(property, "Unidade", "straighten");
      case "tipofreq" -> display(property, "Tipo de frequência", "category");
      case "dtini" -> display(property, "Início", "event_available");
      case "sis" -> display(property, "Subsistema", "apps");
      case "tipo" -> display(property, "Tipo de dado", "data_object");
      case "tab" -> display(property, "Tabela", "table_view");
      case "item" -> display(property, "Item", "vpn_key");
      case "resourceKey" -> display(property, "Recurso alvo", "extension");
      case "fieldKey" -> display(property, "Chave técnica", "vpn_key");
      case "exercicio" -> display(property, "Exercício", "calendar_month");
      case "ordemParcela" -> display(property, "Parcela", "format_list_numbered");
      default -> throw new IllegalArgumentException("Campo de diretório sem curadoria: " + property);
    }).toList();
    String codeProperty = key.equals(ApiPaths.VINCULO_SOURCE)
        ? "matric"
        : key.equals(ApiPaths.DEPENDENTE_SOURCE)
            ? "cpf"
            : key.equals(ApiPaths.CODIGO_FREQUENCIA_SOURCE)
                ? "mnemonico"
                : key.equals(ApiPaths.ITEM_TABELA_SOURCE)
                    ? "item"
                    : key.equals(ApiPaths.CAMPO_CUSTOMIZADO_SOURCE)
                        ? "fieldKey"
                        : key.equals(ApiPaths.PERIODO_AQUISITIVO_FERIAS_SOURCE)
                            ? "chave"
                            : key.equals(ApiPaths.FERIAS_GOZO_SOURCE) ? "ordemParcela" : id;
    var lookup = new EntityLookupDescriptor(entityKey, codeProperty, descriptions, status, null, null,
        List.of(label), dependencies,
        new LookupSelectionPolicy(null, status, allowed, blocked, true, disabledReason, disabledReason),
        new LookupCapabilities(true, true, true, false, false, true, true, false, false, false),
        new LookupDetailDescriptor(null, null, "drawer", "surface", surface, "drawer", null,
            "view", entityKey, surface),
        new LookupDisplayDescriptor("directory", "form", "comfortable", "inline", "list", label,
            displayFields, descriptions, List.of(), null, false, true, true, status != null,
            false, true, statusLabels, 0), filtering);
    return new OptionSourceDescriptor(key, OptionSourceType.RESOURCE_ENTITY, path, null, null,
        label, id, dependsOn, dependencies,
        OptionSourcePolicy.defaults(), lookup);
  }

  private LookupDisplayFieldDescriptor display(String property, String label, String icon) {
    return new LookupDisplayFieldDescriptor(property, property, label, icon, "text", "neutral", null);
  }
}
