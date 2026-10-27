package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * Cadastro de órgãos, prefeituras, autarquias e fundações públicas do tenant (equivalente à tabela
 * EMPRESAS do legado Ergon/Hades). Define a fronteira organizacional para lotação, vínculos
 * funcionais, regras estatutárias e campos customizados.
 */
@Getter
@Setter
@Entity
@Table(name = "empresas")
@Comment(
    "Cadastro de órgãos, prefeituras, autarquias e fundações públicas do tenant (equivalente à tabela EMPRESAS do legado Ergon/Hades). Define a fronteira organizacional para lotação, vínculos funcionais, regras estatutárias e campos customizados.")
public class Orgao {
  @Id
  /** Código numérico identificador único do órgão ou entidade pública no Ergon (EMP_CODIGO). */
  @Comment(
      "Código numérico identificador único do órgão ou entidade pública no Ergon (EMP_CODIGO).")
  @Column(name = "empresa", nullable = false)
  private Integer empresa;

  @org.praxisplatform.uischema.annotation.OptionLabel
  @org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn
  /**
   * Nome oficial resumido do órgão ou entidade pública utilizado em cabeçalhos, listagens e
   * relatórios oficiais.
   */
  @Comment(
      "Nome oficial resumido do órgão ou entidade pública utilizado em cabeçalhos, listagens e relatórios oficiais.")
  @Column(name = "nome", nullable = false, length = 100)
  private String nome;

  /**
   * Sigla ou nome fantasia do órgão público (ex.: PMN, GMN, IPREV) para identificação rápida na
   * interface.
   */
  @Comment(
      "Sigla ou nome fantasia do órgão público (ex.: PMN, GMN, IPREV) para identificação rápida na interface.")
  @Column(name = "fantasia", nullable = true, length = 60)
  private String fantasia;

  /**
   * Razão social ou denominação jurídica completa do ente público para fins fiscais,
   * previdenciários e eSocial.
   */
  @Comment(
      "Razão social ou denominação jurídica completa do ente público para fins fiscais, previdenciários e eSocial.")
  @Column(name = "razao", nullable = false, length = 200)
  private String razao;

  /**
   * Cadastro Nacional da Pessoa Jurídica (CGC/CNPJ) do órgão público no formato numérico ou
   * formatado.
   */
  @Comment(
      "Cadastro Nacional da Pessoa Jurídica (CGC/CNPJ) do órgão público no formato numérico ou formatado.")
  @Column(name = "cnpj", nullable = false, length = 18)
  private String cnpj;

  /** Sigla da Unidade da Federação (UF) sede do órgão público. */
  @Comment("Sigla da Unidade da Federação (UF) sede do órgão público.")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "uf_sigla", nullable = false, length = 2, columnDefinition = "character(2)")
  private String ufSigla;

  /** Nome do município sede do órgão ou entidade pública. */
  @Comment("Nome do município sede do órgão ou entidade pública.")
  @Column(name = "municipio", nullable = false, length = 60)
  private String municipio;

  /** Código de Endereçamento Postal (CEP) da sede administrativa do órgão. */
  @Comment("Código de Endereçamento Postal (CEP) da sede administrativa do órgão.")
  @Column(name = "cep", nullable = true, length = 10)
  private String cep;

  /** Endereço completo (logradouro, número e complemento) da sede do órgão público. */
  @Comment("Endereço completo (logradouro, número e complemento) da sede do órgão público.")
  @Column(name = "logradouro", nullable = true, length = 150)
  private String logradouro;

  /** Telefone institucional principal de contato do órgão. */
  @Comment("Telefone institucional principal de contato do órgão.")
  @Column(name = "telefone", nullable = true, length = 20)
  private String telefone;

  /** Endereço de correio eletrônico institucional do órgão. */
  @Comment("Endereço de correio eletrônico institucional do órgão.")
  @Column(name = "email", nullable = true, length = 100)
  private String email;

  /** Sítio eletrônico ou portal oficial na internet do órgão público. */
  @Comment("Sítio eletrônico ou portal oficial na internet do órgão público.")
  @Column(name = "web", nullable = true, length = 120)
  private String web;

  /** Indica se o órgão está ativo para novas admissões e movimentações funcionais. */
  @Comment("Indica se o órgão está ativo para novas admissões e movimentações funcionais.")
  @Column(name = "ativa", nullable = false)
  private Boolean ativa = true;

  /** Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match). */
  @Comment(
      "Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match).")
  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  /** Proveniência temporal do assentamento funcional ou cadastral. */
  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();
}
