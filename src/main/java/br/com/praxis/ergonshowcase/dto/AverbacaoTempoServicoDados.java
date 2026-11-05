package br.com.praxis.ergonshowcase.dto;
import br.com.praxis.ergonshowcase.constants.ApiPaths;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.*;

@Getter @Setter
public abstract class AverbacaoTempoServicoDados extends AssentamentoTempoLicencaDados {
  public AverbacaoTempoServicoDados() { setSituacao("EM_ANALISE"); }
  @Override
  @Schema(description = "Estado do ciclo de vida do assentamento. A criação inicia em EM_ANALISE; alterações de estado exigem a ação publicada, nunca retificação cadastral.", allowableValues = {"EM_ANALISE", "HOMOLOGADA", "PARCIALMENTE_DESAVERBADA", "DESAVERBADA_TOTAL", "CANCELADO"})
  @UISchema(label = "Situação do direito", helpText = "Estado persistido, alterado exclusivamente por ações de negócio.", type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "flag", readOnly = true, options = "[{\"value\":\"EM_ANALISE\",\"label\":\"Em análise\"},{\"value\":\"HOMOLOGADA\",\"label\":\"Homologada\"},{\"value\":\"PARCIALMENTE_DESAVERBADA\",\"label\":\"Parcialmente desaverbada\"},{\"value\":\"DESAVERBADA_TOTAL\",\"label\":\"Desaverbada total\"},{\"value\":\"CANCELADO\",\"label\":\"Cancelado\"}]", tableHidden = false, quickViewVisible = true, quickViewOrder = 3, preset = UISchemaPreset.ENTERPRISE_STATUS)
  public String getSituacao() { return super.getSituacao(); }

  @Override
  @Schema(description = "Primeiro dia do serviço anterior certificado na averbação, distinto da data em que passa a contar para o direito.")
  @UISchema(label = "Início do serviço", helpText = "Primeiro dia do serviço constante da certidão.", group = "Tempo reconhecido", order = 50, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", tableHidden = false, quickViewVisible = true, quickViewOrder = 1, preset = UISchemaPreset.DATE_RANGE)
  public LocalDate getDtini() { return super.getDtini(); }
  @Override
  @Schema(description = "Último dia do serviço anterior certificado na averbação.", nullable = true)
  @UISchema(label = "Término do serviço", helpText = "Último dia do serviço constante da certidão.", group = "Tempo reconhecido", order = 60, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available", tableHidden = false, preset = UISchemaPreset.DATE_RANGE)
  public LocalDate getDtfim() { return super.getDtfim(); }
  @NotBlank @Size(max = 200)
  @Schema(description = "Síntese da certidão e do tempo reconhecido para identificar a averbação no assentamento funcional.")
  
  @UISchema(label = "Averbação", helpText = "Identifique o tempo e sua certidão de origem.", group = "Tempo reconhecido", order = 1, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "history_edu", tableHidden = false, preset = UISchemaPreset.ENTERPRISE_NAME)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private String descricao;
  @Min(1)
  @Schema(description = "Sequencial da averbação dentro do vínculo. Se omitido na inclusão, o servidor reserva a próxima chave.")
  @UISchema(label = "Sequencial da averbação", helpText = "Número único da averbação neste vínculo.", group = "Tempo reconhecido", order = 110, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", tableHidden = true)
  private Integer chave;
  @Size(max = 200)
  @Schema(description = "Instituição onde o tempo foi prestado. O legado tornou seu preenchimento opcional na Tarefa 37486; a ausência não invalida por si a certidão.", nullable = true)
  @UISchema(label = "Instituição de origem", helpText = "Órgão ou empregador que certificou o serviço anterior.", group = "Tempo reconhecido", order = 120, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "account_balance", tableHidden = false)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String instituicao;
  @NotBlank @Pattern(regexp = "PUBLICO|RGPS")
  @Schema(description = "Natureza do tempo admitida pelo showcase: serviço público ou tempo vinculado ao Regime Geral. A finalidade define em que direito será aproveitado.", allowableValues = {"PUBLICO", "RGPS"})
  @UISchema(label = "Natureza do tempo", helpText = "Selecione a origem pública ou RGPS da certidão.", group = "Tempo reconhecido", order = 130, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, options = "[{\"value\":\"PUBLICO\",\"label\":\"Público\"},{\"value\":\"RGPS\",\"label\":\"RGPS\"}]", icon = "category", tableHidden = false, quickViewVisible = true, quickViewOrder = 4)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private String tipoTempo;
  @Size(max = 20)
  @Schema(description = "Sigla do regime previdenciário certificador do tempo. É referência textual do assentamento, sem inferir elegibilidade para aposentadoria.", nullable = true)
  @UISchema(label = "Regime previdenciário", helpText = "Informe o regime constante da certidão.", group = "Tempo reconhecido", order = 140, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "shield", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String regimePrevidenciario;
  @NotBlank @Pattern(regexp = "APOSENTADORIA|LICENCA_ESPECIAL|TEMPO_SERVICO")
  @Schema(description = "Direito para o qual se reconhece o tempo, consolidando AVERB_OQUE_CONTA. Uma certidão não conta automaticamente para todas as finalidades.", allowableValues = {"APOSENTADORIA", "LICENCA_ESPECIAL", "TEMPO_SERVICO"})
  @UISchema(label = "Finalidade da contagem", helpText = "Informe qual direito recebe o tempo reconhecido.", group = "Efeitos da contagem", order = 150, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, options = "[{\"value\":\"APOSENTADORIA\",\"label\":\"Aposentadoria\"},{\"value\":\"LICENCA_ESPECIAL\",\"label\":\"Licença especial\"},{\"value\":\"TEMPO_SERVICO\",\"label\":\"Tempo de serviço\"}]", icon = "rule", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private String finalidade;
  @Size(max = 20)
  @Schema(description = "Complemento da finalidade de contagem. Para dias de licença especial que originam um ciclo, o legado exige o intervalo aquisitivo no formato DDMMYYYY-DDMMYYYY (Tarefa 13239 de PCK_AVERB_OQUE_CONTA).", nullable = true, example = "01012006-31122010")
  @UISchema(label = "Complemento da finalidade", helpText = "Para formar um ciclo de licença, informe DDMMYYYY-DDMMYYYY.", group = "Efeitos da contagem", order = 160, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "post_add", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String complemento;
  @Schema(description = "Data a partir da qual os dias reconhecidos produzem efeito na contagem funcional, distinta do início do serviço prestado.", nullable = true)
  @UISchema(label = "Efeito na contagem", helpText = "Data inicial de aproveitamento do tempo certificado.", group = "Efeitos da contagem", order = 170, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available", tableHidden = true, preset = UISchemaPreset.DATE_RANGE)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private LocalDate dataAContar;
  @Min(0)
  @Schema(description = "Dias reconhecidos na certidão antes de desaverbações. Na omissão, assume os dias corridos inclusivos do período certificado.")
  @MicroVisualization(kind = MicroVisualizationKind.BULLET, valueExpr = "=totalDias", max = 3650, fallbackText = "Dias reconhecidos", ariaLabel = "Dias certificados antes de desaverbações", thresholds = {@Threshold(max = 365, tone = "info", label = "Até um ano"), @Threshold(min = 366, tone = "success", label = "Acima de um ano")})
  @UISchema(label = "Dias reconhecidos", helpText = "Total certificado antes dos descontos por desaverbação.", group = "Efeitos da contagem", order = 180, type = FieldDataType.NUMBER, numericFormat = org.praxisplatform.uischema.NumericFormat.INTEGER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "timelapse", tableHidden = false, quickViewVisible = true, quickViewOrder = 5)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private Integer totalDias;
  @Min(0)
  @Schema(description = "Dias destinados à finalidade escolhida. Se omitidos, correspondem ao total reconhecido. Na licença especial representam os dias de direito do ciclo.")
  @UISchema(label = "Dias para a finalidade", helpText = "Quantidade destinada ao direito; não confunda com duração do serviço.", group = "Efeitos da contagem", order = 185, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "rule", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private Integer diasFinalidade;
  @NotNull
  @Schema(description = "Indica se o assentamento registra sobreposição de períodos; a marcação não concede aproveitamento em duplicidade.")
  @UISchema(label = "Tempo sobreposto", helpText = "Sinalize coincidência temporal identificada na análise.", group = "Efeitos da contagem", order = 190, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "layers", tableHidden = true, preset = UISchemaPreset.BOOLEAN_FLAG)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private Boolean sobrepoe = false;
  @Size(max = 2000)
  @Schema(description = "Motivação administrativa do reconhecimento do serviço anterior e referência à análise da certidão.", nullable = true)
  @UISchema(label = "Motivo da averbação", helpText = "Fundamente o reconhecimento sem incluir dados desnecessários.", group = "Lastro documental", order = 200, type = FieldDataType.TEXT, controlType = FieldControlType.TEXTAREA, icon = "notes", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String motivo;
  @Schema(description = "Data de remoção integral dos efeitos da averbação, quando registrada no histórico.", nullable = true)
  @UISchema(readOnly = true, label = "Data da remoção", helpText = "Preencha quando houver remoção integral do reconhecimento.", group = "Desaverbação", order = 210, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_busy", tableHidden = true, preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dataRemocao;
  @Size(max = 2000)
  @Schema(description = "Justificativa da remoção integral da averbação, preservada separadamente da desaverbação parcial.", nullable = true)
  @UISchema(readOnly = true, label = "Motivo da remoção", helpText = "Fundamente a retirada integral dos efeitos.", group = "Desaverbação", order = 220, type = FieldDataType.TEXT, controlType = FieldControlType.TEXTAREA, icon = "remove_circle", tableHidden = true)
  private String motivoRemocao;
  @NotNull @Min(0)
  @Schema(description = "Dias retirados parcialmente da finalidade de contagem, consolidando o lançamento de DESAVERBACOES_CONTA; não pode exceder o total certificado.")
  @UISchema(readOnly = true, label = "Dias desaverbados", helpText = "Desconto parcial do tempo anteriormente reconhecido.", group = "Desaverbação", order = 230, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "remove", tableHidden = true)
  private Integer diasDesaverbados = 0;
  @Schema(description = "Data de efeito da desaverbação parcial consolidada neste assentamento.", nullable = true)
  @UISchema(readOnly = true, label = "Data da desaverbação", helpText = "Data em que o desconto parcial passa a produzir efeito.", group = "Desaverbação", order = 240, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_busy", tableHidden = true, preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dataDesaverbacao;
  @Size(max = 2000)
  @Schema(description = "Motivação do desconto parcial de dias reconhecidos, conservando o lastro da desaverbação.", nullable = true)
  @UISchema(readOnly = true, label = "Motivo da desaverbação", helpText = "Registre o fundamento da retirada parcial de dias.", group = "Desaverbação", order = 250, type = FieldDataType.TEXT, controlType = FieldControlType.TEXTAREA, icon = "notes", tableHidden = true)
  private String motivoDesaverbacao;
  @Size(max = 2000)
  @Schema(description = "Primeira informação complementar da finalidade de contagem, preservada conforme AVERB_OQUE_CONTA.INFO1.", nullable = true)
  @UISchema(label = "Informação da finalidade", helpText = "Detalhe o enquadramento da finalidade reconhecida.", group = "Efeitos da contagem", order = 260, type = FieldDataType.TEXT, controlType = FieldControlType.TEXTAREA, icon = "info", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String info1;
  @Size(max = 2000)
  @Schema(description = "Segunda informação complementar da finalidade de contagem, preservada conforme AVERB_OQUE_CONTA.INFO2.", nullable = true)
  @UISchema(label = "Complemento da informação", helpText = "Acrescente informações necessárias ao aproveitamento.", group = "Efeitos da contagem", order = 270, type = FieldDataType.TEXT, controlType = FieldControlType.TEXTAREA, icon = "info", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String info2;
  @Size(max = 255)
  @Schema(description = "Referência documental do anexo comprobatório da averbação, sem expor localizadores privados de armazenamento.", nullable = true)
  @UISchema(label = "Referência do anexo", helpText = "Referência de negócio da certidão anexada.", group = "Lastro documental", order = 280, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "attachment", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String referenciaAnexo;
  @CustomFields(resourceKey = "administracao-pessoal.averbacoes-tempo-servico")
  @Schema(description = "Extensões institucionais da averbação governadas pelo catálogo do órgão, substituindo campos flexíveis sem semântica universal.")
  @UISchema(label = "Campos customizados", helpText = "Informações adicionais definidas pelo órgão.", group = "Extensões institucionais", order = 900, tableHidden = true)
  private Map<String, Object> customFields = new LinkedHashMap<>();
  @Schema(description = "Tempo líquido previsto para um novo assentamento, sem reserva ou autorização. A escrita revalida os dados e a política de domínio.",accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Tempo líquido previsto", helpText = "Prévia não persistente; não certifica concessão nem disponibilidade na confirmação.", group = "Prévia de criação", readOnly = true, tableHidden = true, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "calculate")
  private Integer diasPrevistos;
}
