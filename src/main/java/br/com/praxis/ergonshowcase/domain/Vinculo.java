package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Vínculos funcionais do servidor público com a administração (equivalente a VINCULOS na tela
 * ERGadm00229 do legado Ergon). Preserva a chave composta natural (numfunc, numero) sem a coluna
 * paralela ID_REG de contorno de ROWID e sem a tabela sombra VINCULOS_PND.
 */
@Getter
@Setter
@Entity
@Table(name = "vinculos")
@Comment(
    "Vínculos funcionais do servidor público com a administração (equivalente a VINCULOS na tela ERGadm00229 do legado Ergon). Preserva a chave composta natural (numfunc, numero) sem a coluna paralela ID_REG de contorno de ROWID e sem a tabela sombra VINCULOS_PND.")
public class Vinculo {
  /** Referência relacional para leitura de orgao no assentamento. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "emp_codigo",
      referencedColumnName = "empresa",
      insertable = false,
      updatable = false)
  private Orgao orgao;

  /** Referência relacional para leitura de pessoa no assentamento. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "numfunc",
      referencedColumnName = "numero",
      insertable = false,
      updatable = false)
  private ServidorPessoa pessoa;

  /** Referência relacional para leitura de cargoEntidade no assentamento. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cargo", referencedColumnName = "cargo", insertable = false, updatable = false)
  private Cargo cargoEntidade;

  /** Referência relacional para leitura de lotacao no assentamento. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "setor", referencedColumnName = "setor", insertable = false, updatable = false)
  private Setor lotacao;

  /** Referência relacional para leitura de exercicio no assentamento. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "setor_exercicio",
      referencedColumnName = "setor",
      insertable = false,
      updatable = false)
  private Setor exercicio;

  /** Referência relacional para leitura de documentoLegal no assentamento. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "documento_legal_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private DocumentoLegal documentoLegal;

  /** Identidade natural composta, imutável após inclusão. */
  @EmbeddedId private VinculoId id;

  /** Código do órgão/entidade pública ao qual o vínculo funcional está vinculado. */
  @Comment("Código do órgão/entidade pública ao qual o vínculo funcional está vinculado.")
  @Column(name = "emp_codigo", nullable = false)
  private Integer empCodigo;

  @org.praxisplatform.uischema.annotation.OptionLabel
  @org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn
  /** Matrícula funcional institucional atribuída ao vínculo no órgão. */
  @Comment("Matrícula funcional institucional atribuída ao vínculo no órgão.")
  @Column(name = "matric", nullable = true, length = 20)
  private String matric;

  /** Matrícula de referência externa ou do eSocial associada ao vínculo. */
  @Comment("Matrícula de referência externa ou do eSocial associada ao vínculo.")
  @Column(name = "matricula_externa", nullable = true, length = 20)
  private String matriculaExterna;

  /**
   * Estado explícito do ciclo de vida do vínculo (ATIVO, AFASTADO, CEDIDO, APOSENTADO, VACANTE),
   * substituindo inferências implícitas e tabelas _PND.
   */
  @Comment(
      "Estado explícito do ciclo de vida do vínculo (ATIVO, AFASTADO, CEDIDO, APOSENTADO, VACANTE), substituindo inferências implícitas e tabelas _PND.")
  @Column(name = "situacao", nullable = false, length = 30)
  private String situacao;

  /** Regime jurídico do vínculo funcional (ex.: ESTATUTARIO, CLT, ESPECIAL, COMISSIONADO). */
  @Comment("Regime jurídico do vínculo funcional (ex.: ESTATUTARIO, CLT, ESPECIAL, COMISSIONADO).")
  @Column(name = "regimejur", nullable = false, length = 20)
  private String regimejur;

  /** Tipo de vínculo funcional (ex.: EFETIVO, COMISSAO, TEMPORARIO, CEDIDO, REQUISITADO). */
  @Comment("Tipo de vínculo funcional (ex.: EFETIVO, COMISSAO, TEMPORARIO, CEDIDO, REQUISITADO).")
  @Column(name = "tipovinc", nullable = false, length = 20)
  private String tipovinc;

  /** Categoria funcional do vínculo (ex.: GERAL, MAGISTERIO, SAUDE, GUARDA_MUNICIPAL). */
  @Comment("Categoria funcional do vínculo (ex.: GERAL, MAGISTERIO, SAUDE, GUARDA_MUNICIPAL).")
  @Column(name = "categoria", nullable = false, length = 20)
  private String categoria;

  /** Subcategoria ou classe funcional dentro da categoria do vínculo. */
  @Comment("Subcategoria ou classe funcional dentro da categoria do vínculo.")
  @Column(name = "subcategoria", nullable = true, length = 40)
  private String subcategoria;

  /** Cargo público de provimento titular ou em comissão ocupado pelo vínculo. */
  @Comment("Cargo público de provimento titular ou em comissão ocupado pelo vínculo.")
  @Column(name = "cargo", nullable = true)
  private Integer cargo;

  /** Referência, padrão ou nível salarial atual do servidor na tabela de vencimentos. */
  @Comment("Referência, padrão ou nível salarial atual do servidor na tabela de vencimentos.")
  @Column(name = "referencia", nullable = true, length = 20)
  private String referencia;

  /** Unidade organizacional (setor) de lotação e exercício atual do vínculo. */
  @Comment("Unidade organizacional (setor) de lotação e exercício atual do vínculo.")
  @Column(name = "setor", nullable = true, length = 30)
  private String setor;

  /** Carga horária semanal contratual ou estatutária do vínculo em horas (ex.: 20, 30, 40). */
  @Comment("Carga horária semanal contratual ou estatutária do vínculo em horas (ex.: 20, 30, 40).")
  @Column(name = "jornada_semanal", nullable = true)
  private Integer jornadaSemanal;

  /** Data do ato de nomeação ou designação do servidor. */
  @Comment("Data do ato de nomeação ou designação do servidor.")
  @Column(name = "dtnom", nullable = true)
  private LocalDate dtnom;

  /** Data da assinatura do termo de posse no cargo público. */
  @Comment("Data da assinatura do termo de posse no cargo público.")
  @Column(name = "dtposse", nullable = true)
  private LocalDate dtposse;

  /** Data de início do efetivo exercício funcional no vínculo. */
  @Comment("Data de início do efetivo exercício funcional no vínculo.")
  @Column(name = "dtexerc", nullable = false)
  private LocalDate dtexerc;

  /** Data de admissão considerada para declaração anual RAIS/eSocial. */
  @Comment("Data de admissão considerada para declaração anual RAIS/eSocial.")
  @Column(name = "dtadm_rais", nullable = true)
  private LocalDate dtadmRais;

  /** Data de homologação da aprovação no estágio probatório. */
  @Comment("Data de homologação da aprovação no estágio probatório.")
  @Column(name = "dt_homolog_estagio", nullable = true)
  private LocalDate dtHomologEstagio;

  /** Classificação obtida pelo servidor no concurso público de ingresso. */
  @Comment("Classificação obtida pelo servidor no concurso público de ingresso.")
  @Column(name = "classifconc", nullable = true)
  private Integer classifconc;

  /** Data de homologação do concurso público de ingresso. */
  @Comment("Data de homologação do concurso público de ingresso.")
  @Column(name = "dtconc", nullable = true)
  private LocalDate dtconc;

  /** Data de início de vigência para contratos por prazo determinado/temporários. */
  @Comment("Data de início de vigência para contratos por prazo determinado/temporários.")
  @Column(name = "dtinicontr", nullable = true)
  private LocalDate dtinicontr;

  /** Data de término originalmente prevista para contrato temporário. */
  @Comment("Data de término originalmente prevista para contrato temporário.")
  @Column(name = "dtfimcontr", nullable = true)
  private LocalDate dtfimcontr;

  /** Data limite após prorrogação formal de contrato temporário. */
  @Comment("Data limite após prorrogação formal de contrato temporário.")
  @Column(name = "dtprorrogcontr", nullable = true)
  private LocalDate dtprorrogcontr;

  /** Modalidade de requisição ou cessão funcional (domínio ERG_TIPO_REQUIS em itemtabelas). */
  @Comment("Modalidade de requisição ou cessão funcional (domínio ERG_TIPO_REQUIS em itemtabelas).")
  @Column(name = "tipo_req", nullable = true, length = 20)
  private String tipoReq;

  /** Nome do órgão cessionário ou cedente envolvido na requisição/cessão. */
  @Comment("Nome do órgão cessionário ou cedente envolvido na requisição/cessão.")
  @Column(name = "orgao_req", nullable = true, length = 100)
  private String orgaoReq;

  /** Cargo em comissão ou função exercida no órgão de destino da cessão. */
  @Comment("Cargo em comissão ou função exercida no órgão de destino da cessão.")
  @Column(name = "funcao_req", nullable = true, length = 60)
  private String funcaoReq;

  /** Responsabilidade pelo ônus remuneratório da cessão (domínio ERG_ONUS em itemtabelas). */
  @Comment("Responsabilidade pelo ônus remuneratório da cessão (domínio ERG_ONUS em itemtabelas).")
  @Column(name = "tipo_onus_req", nullable = true, length = 20)
  private String tipoOnusReq;

  /**
   * Regra de ressarcimento financeiro entre órgãos na cessão (domínio ERG_RESSARCIMENTO em
   * itemtabelas).
   */
  @Comment(
      "Regra de ressarcimento financeiro entre órgãos na cessão (domínio ERG_RESSARCIMENTO em itemtabelas).")
  @Column(name = "tipo_ressarc_req", nullable = true, length = 20)
  private String tipoRessarcReq;

  /** Data de início do período de cessão ou requisição para outro órgão. */
  @Comment("Data de início do período de cessão ou requisição para outro órgão.")
  @Column(name = "dtini_cessao", nullable = true)
  private LocalDate dtiniCessao;

  /** Data final do período de cessão ou requisição. */
  @Comment("Data final do período de cessão ou requisição.")
  @Column(name = "dtfim_cessao", nullable = true)
  private LocalDate dtfimCessao;

  /** Data de concessão da aposentadoria do servidor neste vínculo. */
  @Comment("Data de concessão da aposentadoria do servidor neste vínculo.")
  @Column(name = "dtaposent", nullable = true)
  private LocalDate dtaposent;

  /**
   * Modalidade legal de aposentadoria concedida (ex.: VOLUNTARIA INTEGRAL, COMPULSORIA, INVALIDEZ).
   */
  @Comment(
      "Modalidade legal de aposentadoria concedida (ex.: VOLUNTARIA INTEGRAL, COMPULSORIA, INVALIDEZ).")
  @Column(name = "tipoapos", nullable = true, length = 30)
  private String tipoapos;

  /** Percentual ou fator de proporcionalidade dos proventos de aposentadoria. */
  @Comment("Percentual ou fator de proporcionalidade dos proventos de aposentadoria.")
  @Column(name = "fator_proporc_aposent", nullable = true, precision = 5, scale = 2)
  private BigDecimal fatorProporcAposent;

  /** Data de vacância, exoneração, demissão ou desligamento do vínculo. */
  @Comment("Data de vacância, exoneração, demissão ou desligamento do vínculo.")
  @Column(name = "dtvac", nullable = true)
  private LocalDate dtvac;

  /**
   * Forma jurídica da vacância (ex.: EXONERACAO A PEDIDO, DEMISSAO, POSSE OUTRO CARGO INACUMULAVEL,
   * FALECIMENTO).
   */
  @Comment(
      "Forma jurídica da vacância (ex.: EXONERACAO A PEDIDO, DEMISSAO, POSSE OUTRO CARGO INACUMULAVEL, FALECIMENTO).")
  @Column(name = "formavac", nullable = true, length = 20)
  private String formavac;

  /** Fundamentação ou observação detalhada sobre o encerramento/vacância do vínculo. */
  @Comment("Fundamentação ou observação detalhada sobre o encerramento/vacância do vínculo.")
  @Column(name = "motivovac", nullable = true, length = 2000)
  private String motivovac;

  /** Data limite até a qual o pagamento de folha permanece habilitado após vacância. */
  @Comment("Data limite até a qual o pagamento de folha permanece habilitado após vacância.")
  @Column(name = "dt_pgto_ate", nullable = true)
  private LocalDate dtPgtoAte;

  /** Indica se o vínculo sofre retenção de Imposto de Renda na Fonte. */
  @Comment("Indica se o vínculo sofre retenção de Imposto de Renda na Fonte.")
  @Column(name = "desconta_ir", nullable = false)
  private Boolean descontaIr = true;

  /** Indica se o servidor recebe correspondências e contracheques impressos via correio. */
  @Comment("Indica se o servidor recebe correspondências e contracheques impressos via correio.")
  @Column(name = "correio", nullable = false)
  private Boolean correio = false;

  /** Indica se o vínculo preenche cota legal de pessoa com deficiência (PcD). */
  @Comment("Indica se o vínculo preenche cota legal de pessoa com deficiência (PcD).")
  @Column(name = "info_cota", nullable = false)
  private Boolean infoCota = false;

  /** Banco específico para pagamento deste vínculo, quando distinto do padrão do funcionário. */
  @Comment(
      "Banco específico para pagamento deste vínculo, quando distinto do padrão do funcionário.")
  @Column(name = "banco", nullable = true)
  private Integer banco;

  /** Agência bancária específica para pagamento deste vínculo. */
  @Comment("Agência bancária específica para pagamento deste vínculo.")
  @Column(name = "agencia", nullable = true, length = 10)
  private String agencia;

  /** Conta corrente específica para pagamento deste vínculo. */
  @Comment("Conta corrente específica para pagamento deste vínculo.")
  @Column(name = "conta", nullable = true, length = 25)
  private String conta;

  /** Forma de pagamento específica deste vínculo (domínio ERG_TIPO_PAG). */
  @Comment("Forma de pagamento específica deste vínculo (domínio ERG_TIPO_PAG).")
  @Column(name = "tipopag", nullable = true, length = 10)
  private String tipopag;

  /** Data de opção pelo regime do FGTS (para vínculos celetistas/temporários). */
  @Comment("Data de opção pelo regime do FGTS (para vínculos celetistas/temporários).")
  @Column(name = "dtopfgts", nullable = true)
  private LocalDate dtopfgts;

  /** Data de retroação da opção pelo FGTS. */
  @Comment("Data de retroação da opção pelo FGTS.")
  @Column(name = "dtretrfgts", nullable = true)
  private LocalDate dtretrfgts;

  /** Código do banco depositário da conta vinculada de FGTS. */
  @Comment("Código do banco depositário da conta vinculada de FGTS.")
  @Column(name = "bancofgts", nullable = true)
  private Integer bancofgts;

  /** Agência depositária da conta vinculada de FGTS. */
  @Comment("Agência depositária da conta vinculada de FGTS.")
  @Column(name = "agenciafgts", nullable = true, length = 10)
  private String agenciafgts;

  /** Número da conta vinculada de FGTS. */
  @Comment("Número da conta vinculada de FGTS.")
  @Column(name = "contafgts", nullable = true, length = 25)
  private String contafgts;

  /** Saldo ou valor base de referência para fins rescisórios de FGTS. */
  @Comment("Saldo ou valor base de referência para fins rescisórios de FGTS.")
  @Column(name = "valor_dep_fgts", nullable = true, precision = 11, scale = 2)
  private BigDecimal valorDepFgts;

  /** Regime previdenciário ao qual o vínculo é filiado (ex.: RPPS, RGPS). */
  @Comment("Regime previdenciário ao qual o vínculo é filiado (ex.: RPPS, RGPS).")
  @Column(name = "regimeprev", nullable = true, length = 30)
  private String regimeprev;

  /**
   * Plano previdenciário ou fundo específico dentro do regime (ex.: FUNDO FINANCEIRO, FUNDO
   * CAPITALIZADO, PREVCOM).
   */
  @Comment(
      "Plano previdenciário ou fundo específico dentro do regime (ex.: FUNDO FINANCEIRO, FUNDO CAPITALIZADO, PREVCOM).")
  @Column(name = "planoprev", nullable = true, length = 40)
  private String planoprev;

  /** Número do vínculo anterior do mesmo servidor para averbação/continuidade de tempo. */
  @Comment("Número do vínculo anterior do mesmo servidor para averbação/continuidade de tempo.")
  @Column(name = "numvincant", nullable = true)
  private Integer numvincant;

  /**
   * Número do vínculo posterior do mesmo servidor em caso de reenquadramento ou nova investidura.
   */
  @Comment(
      "Número do vínculo posterior do mesmo servidor em caso de reenquadramento ou nova investidura.")
  @Column(name = "numvincpos", nullable = true)
  private Integer numvincpos;

  /**
   * Observações gerais, histórico de ingresso ou anotações de assentamento funcional do vínculo.
   */
  @Comment(
      "Observações gerais, histórico de ingresso ou anotações de assentamento funcional do vínculo.")
  @Column(name = "motivo", nullable = true, length = 2000)
  private String motivo;

  /**
   * Referência relacional explícita ao diploma legal ou portaria de provimento do vínculo
   * (substitui PONTLEI).
   */
  @Comment(
      "Referência relacional explícita ao diploma legal ou portaria de provimento do vínculo (substitui PONTLEI).")
  @Column(name = "documento_legal_id", nullable = true)
  private Long documentoLegalId;

  /**
   * Referência relacional explícita à publicação oficial do ato de investidura/vacância (substitui
   * PONTPUBL/PONTPROC/PONTARQS).
   */
  @Comment(
      "Referência relacional explícita à publicação oficial do ato de investidura/vacância (substitui PONTPUBL/PONTPROC/PONTARQS).")
  @Column(name = "ato_publicacao_id", nullable = true)
  private Long atoPublicacaoId;

  /**
   * Atributos de negócio customizados por órgão em formato JSONB governado por
   * custom_field_definitions (substitui FLEX_CAMPO_01..40).
   */
  @Comment(
      "Atributos de negócio customizados por órgão em formato JSONB governado por custom_field_definitions (substitui FLEX_CAMPO_01..40).")
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_fields", nullable = false)
  @org.praxisplatform.uischema.customfields.CustomFields(
      resourceKey = "administracao-pessoal.vinculos")
  private Map<String, Object> customFields = new LinkedHashMap<>();

  /** Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match). */
  @Comment(
      "Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match).")
  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  /**
   * Código da unidade organizacional de exercício efetivo do servidor (HSETOR_ no Ergon), podendo
   * diferir do setor de lotação estrutural em designações ou cessões.
   */
  @Comment(
      "Código da unidade organizacional de exercício efetivo do servidor (HSETOR_ no Ergon), podendo diferir do setor de lotação estrutural em designações ou cessões.")
  @Column(name = "setor_exercicio", nullable = true, length = 30)
  private String setorExercicio;

  /**
   * Código do cargo em comissão ou função de confiança exercido cumulativamente pelo servidor
   * ocupante de cargo efetivo.
   */
  @Comment(
      "Código do cargo em comissão ou função de confiança exercido cumulativamente pelo servidor ocupante de cargo efetivo.")
  @Column(name = "cargo_comissao", nullable = true)
  private Integer cargoComissao;

  /** Proveniência temporal do assentamento funcional ou cadastral. */
  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();
}
