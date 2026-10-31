package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

/**
 * Itens das tabelas gerais de domínio do Ergon/Hades (equivalente a HADES.ITEMTABELA_). Preserva a
 * chave composta natural imutável (tab, item) para consumo via @EmbeddedId / CompositeResourceId no
 * praxis-metadata-starter.
 */
@Getter
@Setter
@Entity
@Table(name = "itemtabelas")
@Comment(
    "Itens das tabelas gerais de domínio do Ergon/Hades (equivalente a HADES.ITEMTABELA_). Preserva a chave composta natural imutável (tab, item) para consumo via @EmbeddedId / CompositeResourceId no praxis-metadata-starter.")
public class ItemTabela {
  /** Identidade natural composta, imutável após inclusão. */
  @EmbeddedId private ItemTabelaId id;

  /** Descrição legível do item apresentada em selects, tabelas e relatórios. */
  @Comment("Descrição legível do item apresentada em selects, tabelas e relatórios.")
  @OptionLabel
  @Column(name = "descr", nullable = false, length = 200)
  private String descr;

  /**
   * Órgão específico ao qual o item se restringe quando a tabela geral é multi-empresa (NULL para
   * itens globais).
   */
  @Comment(
      "Órgão específico ao qual o item se restringe quando a tabela geral é multi-empresa (NULL para itens globais).")
  @Column(name = "emp_codigo", nullable = true)
  private Integer empCodigo;

  /** Atributo complementar 1 do item (ex.: ordem de escolaridade, código eSocial ou agrupador). */
  @Comment(
      "Atributo complementar 1 do item (ex.: ordem de escolaridade, código eSocial ou agrupador).")
  @Column(name = "item1", nullable = true, length = 120)
  private String item1;

  /** Atributo complementar 2 do item (ex.: sigla auxiliar ou equivalência legal). */
  @Comment("Atributo complementar 2 do item (ex.: sigla auxiliar ou equivalência legal).")
  @Column(name = "item2", nullable = true, length = 120)
  private String item2;

  /** Atributo complementar 3 em texto livre ou expressão de parametrização associada ao item. */
  @Comment(
      "Atributo complementar 3 em texto livre ou expressão de parametrização associada ao item.")
  @Column(name = "item3", nullable = true, columnDefinition = "text")
  private String item3;

  /** Atributo complementar 4 para metadados adicionais de integração. */
  @Comment("Atributo complementar 4 para metadados adicionais de integração.")
  @Column(name = "item4", nullable = true, length = 255)
  private String item4;

  /** Ordem numérica de exibição do item em listas de seleção. */
  @Comment("Ordem numérica de exibição do item em listas de seleção.")
  @DefaultSortColumn
  @Column(name = "ordem", nullable = true)
  private Integer ordem;

  /** Indica se o item de domínio está ativo para seleção em novos cadastros. */
  @Comment("Indica se o item de domínio está ativo para seleção em novos cadastros.")
  @Column(name = "ativo", nullable = false)
  private Boolean ativo = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "tab",
      referencedColumnName = "tab",
      insertable = false,
      updatable = false)
  private Tabela tabelaEntidade;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "emp_codigo",
      referencedColumnName = "empresa",
      insertable = false,
      updatable = false)
  private Orgao orgao;

  /** Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match). */
  @Comment(
      "Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match).")
  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  /** Proveniência temporal do assentamento funcional ou cadastral. */
  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();
}
