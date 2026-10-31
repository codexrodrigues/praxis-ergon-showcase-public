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
 * Catálogo mestre de tabelas gerais de domínio do Ergon/Hades (equivalente a HADES.TABELA). Agrupa
 * códigos parametrizáveis como estado civil, escolaridade, regimes jurídicos, categorias, formas de
 * vacância e listas de opções de campos customizados.
 */
@Getter
@Setter
@Entity
@Table(name = "tabelas")
@Comment(
    "Catálogo mestre de tabelas gerais de domínio do Ergon/Hades (equivalente a HADES.TABELA). Agrupa códigos parametrizáveis como estado civil, escolaridade, regimes jurídicos, categorias, formas de vacância e listas de opções de campos customizados.")
public class Tabela {
  @Id
  /**
   * Identificador textual único da tabela geral de domínio (ex.: ERG_ESTCIVIL, ERG_ESCOLA,
   * ERG_REGIME_JUR, Parentesco).
   */
  @Comment(
      "Identificador textual único da tabela geral de domínio (ex.: ERG_ESTCIVIL, ERG_ESCOLA, ERG_REGIME_JUR, Parentesco).")
  @DefaultSortColumn
  @Column(name = "tab", nullable = false, length = 40)
  private String tab;

  /** Sistema proprietário da tabela geral no ecossistema (ex.: Ergon, Hades). */
  @Comment("Sistema proprietário da tabela geral no ecossistema (ex.: Ergon, Hades).")
  @Column(name = "sis", nullable = false, length = 20)
  private String sis;

  /** Descrição funcional da finalidade da tabela geral de domínio. */
  @Comment("Descrição funcional da finalidade da tabela geral de domínio.")
  @OptionLabel
  @Column(name = "descr", nullable = false, length = 180)
  private String descr;

  /**
   * Indica se os itens da tabela geral variam por órgão/empresa (equivalente a MULTI_EMP S/N no
   * legado).
   */
  @Comment(
      "Indica se os itens da tabela geral variam por órgão/empresa (equivalente a MULTI_EMP S/N no legado).")
  @Column(name = "multi_emp", nullable = false)
  private Boolean multiEmp = false;

  /** Tipo de dado predominante do código do item ([A]lfanumérico ou [N]umérico). */
  @Comment("Tipo de dado predominante do código do item ([A]lfanumérico ou [N]umérico).")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "tipo", nullable = true, length = 1, columnDefinition = "character(1)")
  private String tipo;

  /** Tamanho máximo recomendado para o código do item nesta tabela geral. */
  @Comment("Tamanho máximo recomendado para o código do item nesta tabela geral.")
  @Column(name = "taminfo", nullable = true)
  private Integer taminfo;

  /** Rótulo semântico da coluna auxiliar item1 para esta tabela geral, quando utilizada. */
  @Comment("Rótulo semântico da coluna auxiliar item1 para esta tabela geral, quando utilizada.")
  @Column(name = "label_item1", nullable = true, length = 60)
  private String labelItem1;

  /** Rótulo semântico da coluna auxiliar item2 para esta tabela geral, quando utilizada. */
  @Comment("Rótulo semântico da coluna auxiliar item2 para esta tabela geral, quando utilizada.")
  @Column(name = "label_item2", nullable = true, length = 60)
  private String labelItem2;

  /** Rótulo semântico da coluna auxiliar item3 para esta tabela geral, quando utilizada. */
  @Comment("Rótulo semântico da coluna auxiliar item3 para esta tabela geral, quando utilizada.")
  @Column(name = "label_item3", nullable = true, length = 60)
  private String labelItem3;

  /** Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match). */
  @Comment(
      "Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match).")
  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  /** Proveniência temporal do assentamento funcional ou cadastral. */
  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();
}
