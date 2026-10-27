package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * Catálogo relacional unificado de atos administrativos, diplomas legais, publicações em Diário
 * Oficial e metadados de anexos. Substitui de forma relacional e auditável os ponteiros numéricos
 * mágicos sem FK do legado (PONTLEI, PONTPUBL, PONTPROC e PONTARQS).
 */
@Getter
@Setter
@Entity
@Table(name = "documentos_legais")
@Comment(
    "Catálogo relacional unificado de atos administrativos, diplomas legais, publicações em Diário Oficial e metadados de anexos. Substitui de forma relacional e auditável os ponteiros numéricos mágicos sem FK do legado (PONTLEI, PONTPUBL, PONTPROC e PONTARQS).")
public class DocumentoLegal {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  /** Identificador único surrogate do documento legal ou ato de publicação. */
  @Comment("Identificador único surrogate do documento legal ou ato de publicação.")
  @Column(name = "id", nullable = false)
  private Long id;

  /** Órgão público emissor ou proprietário do ato administrativo/documento legal. */
  @Comment("Órgão público emissor ou proprietário do ato administrativo/documento legal.")
  @Column(name = "emp_codigo", nullable = false)
  private Integer empCodigo;

  /**
   * Espécie do ato ou documento legal (ex.: LEI, DECRETO, PORTARIA, RESOLUCAO, EDITAL, DESPACHO).
   */
  @Comment(
      "Espécie do ato ou documento legal (ex.: LEI, DECRETO, PORTARIA, RESOLUCAO, EDITAL, DESPACHO).")
  @Column(name = "tipo_documento", nullable = false, length = 40)
  private String tipoDocumento;

  /**
   * Número identificador oficial do ato ou documento (tratado como texto para suportar sufixos como
   * 1.240-A).
   */
  @Comment(
      "Número identificador oficial do ato ou documento (tratado como texto para suportar sufixos como 1.240-A).")
  @Column(name = "numero_documento", nullable = false, length = 40)
  private String numeroDocumento;

  /** Ano de emissão ou promulgação do documento legal. */
  @Comment("Ano de emissão ou promulgação do documento legal.")
  @Column(name = "ano_documento", nullable = false)
  private Integer anoDocumento;

  /**
   * Data de publicação oficial do ato no Diário Oficial ou boletim interno (substitui a data de
   * HAD_PUBLICACOES/PONTPUBL).
   */
  @Comment(
      "Data de publicação oficial do ato no Diário Oficial ou boletim interno (substitui a data de HAD_PUBLICACOES/PONTPUBL).")
  @Column(name = "data_publicacao", nullable = true)
  private LocalDate dataPublicacao;

  /** Número da edição do Diário Oficial ou publicação em que o ato foi veiculado. */
  @Comment("Número da edição do Diário Oficial ou publicação em que o ato foi veiculado.")
  @Column(name = "numero_publicacao", nullable = true, length = 40)
  private String numeroPublicacao;

  /** Meio oficial de publicação (ex.: Diário Oficial do Município, DOE, DOU, Mural Oficial). */
  @Comment(
      "Meio oficial de publicação (ex.: Diário Oficial do Município, DOE, DOU, Mural Oficial).")
  @Column(name = "veiculo_publicacao", nullable = true, length = 80)
  private String veiculoPublicacao;

  /** Ementa, assunto ou motivo jurídico do ato administrativo ou diploma legal. */
  @Comment("Ementa, assunto ou motivo jurídico do ato administrativo ou diploma legal.")
  @Column(name = "ementa", nullable = false, length = 1000)
  private String ementa;

  /**
   * Número do processo administrativo ou protocolo eletrônico (ex.: Processo SEI / eSocial,
   * substituindo PONTPROC).
   */
  @Comment(
      "Número do processo administrativo ou protocolo eletrônico (ex.: Processo SEI / eSocial, substituindo PONTPROC).")
  @Column(name = "processo_administrativo", nullable = true, length = 60)
  private String processoAdministrativo;

  /**
   * URI ou chave de armazenamento em nuvem do documento digitalizado anexo (substituindo
   * PONTARQS/HAD_ARQSX).
   */
  @Comment(
      "URI ou chave de armazenamento em nuvem do documento digitalizado anexo (substituindo PONTARQS/HAD_ARQSX).")
  @Column(name = "arquivo_url", nullable = true, length = 500)
  private String arquivoUrl;

  /** Nome original do arquivo digitalizado associado ao ato. */
  @Comment("Nome original do arquivo digitalizado associado ao ato.")
  @Column(name = "arquivo_nome", nullable = true, length = 200)
  private String arquivoNome;

  /** Tipo MIME do arquivo anexo (ex.: application/pdf). */
  @Comment("Tipo MIME do arquivo anexo (ex.: application/pdf).")
  @Column(name = "arquivo_mime_type", nullable = true, length = 100)
  private String arquivoMimeType;

  /** Observações complementares sobre vigência, efeitos financeiros ou retificações do ato. */
  @Comment("Observações complementares sobre vigência, efeitos financeiros ou retificações do ato.")
  @Column(name = "observacao", nullable = true, length = 2000)
  private String observacao;

  /** Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match). */
  @Comment(
      "Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match).")
  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  /** Proveniência temporal do assentamento funcional ou cadastral. */
  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();
}
