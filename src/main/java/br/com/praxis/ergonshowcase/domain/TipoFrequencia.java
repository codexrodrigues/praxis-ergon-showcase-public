package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * Catálogo de naturezas/tipos macro de ocorrências de frequência e afastamento (equivalente a
 * TIPO_FREQ_ na tela ERGadm00033 do legado Ergon). Agrupa os códigos de frequência e define
 * reflexos em contagem de tempo e sobrejornada.
 */
@Getter
@Setter
@Entity
@Table(name = "tipos_frequencia")
@Comment(
    "Catálogo de naturezas/tipos macro de ocorrências de frequência e afastamento (equivalente a TIPO_FREQ_ na tela ERGadm00033 do legado Ergon). Agrupa os códigos de frequência e define reflexos em contagem de tempo e sobrejornada.")
public class TipoFrequencia {
  /** Referência relacional para leitura do dispositivo legal no catálogo de tipos de frequência. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "documento_legal_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private DocumentoLegal documentoLegal;

  @Id
  /**
   * Código identificador único do tipo de frequência (ex.: NORMAL, FALTA, LICENCA, AFASTAMENTO,
   * HORA_EXTRA).
   */
  @Comment(
      "Código identificador único do tipo de frequência (ex.: NORMAL, FALTA, LICENCA, AFASTAMENTO, HORA_EXTRA).")
  @Column(name = "tipo", nullable = false, length = 20)
  private String tipo;

  @org.praxisplatform.uischema.annotation.OptionLabel
  @org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn
  /** Nome descritivo do tipo de frequência apresentado em consultas e relatórios. */
  @Comment("Nome descritivo do tipo de frequência apresentado em consultas e relatórios.")
  @Column(name = "nome", nullable = false, length = 100)
  private String nome;

  /**
   * Indica se ocorrências deste tipo impactam a contagem de tempo de serviço, aposentadoria ou
   * adicionais temporais (INFLUI_CONTA).
   */
  @Comment(
      "Indica se ocorrências deste tipo impactam a contagem de tempo de serviço, aposentadoria ou adicionais temporais (INFLUI_CONTA).")
  @Column(name = "influi_conta", nullable = false)
  private Boolean influiConta = false;

  /**
   * Indica se ocorrências deste tipo caracterizam prestação de serviço extraordinário (HORA_EXTRA).
   */
  @Comment(
      "Indica se ocorrências deste tipo caracterizam prestação de serviço extraordinário (HORA_EXTRA).")
  @Column(name = "hora_extra", nullable = false)
  private Boolean horaExtra = false;

  /**
   * Percentual padrão de acréscimo remuneratório quando o tipo de frequência representa hora
   * extraordinária.
   */
  @Comment(
      "Percentual padrão de acréscimo remuneratório quando o tipo de frequência representa hora extraordinária.")
  @Column(name = "percentual_extra", nullable = true, precision = 5, scale = 2)
  private BigDecimal percentualExtra;

  /**
   * Referência relacional explícita ao dispositivo legal que rege o tipo de frequência (substitui
   * PONTLEI).
   */
  @Comment(
      "Referência relacional explícita ao dispositivo legal que rege o tipo de frequência (substitui PONTLEI).")
  @Column(name = "documento_legal_id", nullable = true)
  private Long documentoLegalId;

  /** Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match). */
  @Comment(
      "Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match).")
  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  /** Proveniência temporal do assentamento funcional ou cadastral. */
  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();
}
