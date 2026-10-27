package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * Catálogo de classificações legais de dependência funcional e previdenciária (equivalente a
 * TIPO_DEPEN na tela ERGadm00001 do legado Ergon). Define a elegibilidade base a dedução de IRRF e
 * cota de salário-família.
 */
@Getter
@Setter
@Entity
@Table(name = "tipos_dependente")
@Comment(
    "Catálogo de classificações legais de dependência funcional e previdenciária (equivalente a TIPO_DEPEN na tela ERGadm00001 do legado Ergon). Define a elegibilidade base a dedução de IRRF e cota de salário-família.")
public class TipoDependente {
  @Id
  /**
   * Código textual identificador único do tipo de dependência (chave pública estável até 20
   * caracteres).
   */
  @Comment(
      "Código textual identificador único do tipo de dependência (chave pública estável até 20 caracteres).")
  @Column(name = "tipodepen", nullable = false, length = 20)
  private String tipodepen;

  /** Descrição detalhada da modalidade de dependência e sua regra de enquadramento legal. */
  @Comment("Descrição detalhada da modalidade de dependência e sua regra de enquadramento legal.")
  @Column(name = "descricao", nullable = false, length = 200)
  private String descricao;

  /**
   * Indica se esta classificação de dependência habilita dedução na base de cálculo de Imposto de
   * Renda Retido na Fonte.
   */
  @Comment(
      "Indica se esta classificação de dependência habilita dedução na base de cálculo de Imposto de Renda Retido na Fonte.")
  @Column(name = "gera_ir", nullable = false)
  private Boolean geraIr = false;

  /**
   * Indica se esta classificação de dependência habilita percepção de cota de salário-família ou
   * benefício previdenciário.
   */
  @Comment(
      "Indica se esta classificação de dependência habilita percepção de cota de salário-família ou benefício previdenciário.")
  @Column(name = "gera_salario_familia", nullable = false)
  private Boolean geraSalarioFamilia = false;

  /**
   * Idade limite padrão em anos para manutenção automática da dependência sem invalidez (ex.: 21 ou
   * 24 anos).
   */
  @Comment(
      "Idade limite padrão em anos para manutenção automática da dependência sem invalidez (ex.: 21 ou 24 anos).")
  @Column(name = "idade_limite", nullable = true)
  private Integer idadeLimite;

  /**
   * Referência relacional explícita à norma legal que regulamenta o tipo de dependência (substitui
   * PONTLEI).
   */
  @Comment(
      "Referência relacional explícita à norma legal que regulamenta o tipo de dependência (substitui PONTLEI).")
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
