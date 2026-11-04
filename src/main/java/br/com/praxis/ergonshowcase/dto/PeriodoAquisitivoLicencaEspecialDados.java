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
public abstract class PeriodoAquisitivoLicencaEspecialDados extends AssentamentoTempoLicencaDados {
  public PeriodoAquisitivoLicencaEspecialDados() { setSituacao("EM_AQUISICAO"); }
  @Override
  @Schema(description = "Estado do ciclo de vida do assentamento. A criação inicia em EM_AQUISICAO; alterações de estado exigem a ação publicada, nunca retificação cadastral.", allowableValues = {"EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO", "CONVERTIDO_EM_DOBRO", "QUITADO", "SUSPENSO", "PRESCRITO", "CANCELADO"})
  @UISchema(label = "Situação do direito", helpText = "Estado persistido, alterado exclusivamente por ações de negócio.", type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "flag", readOnly = true, options = "[{\"value\":\"EM_AQUISICAO\",\"label\":\"Em aquisição\"},{\"value\":\"DISPONIVEL\",\"label\":\"Disponível\"},{\"value\":\"PARCIALMENTE_GOZADO\",\"label\":\"Parcialmente gozado\"},{\"value\":\"CONVERTIDO_EM_DOBRO\",\"label\":\"Convertido em dobro\"},{\"value\":\"QUITADO\",\"label\":\"Quitado\"},{\"value\":\"SUSPENSO\",\"label\":\"Suspenso\"},{\"value\":\"PRESCRITO\",\"label\":\"Prescrito\"},{\"value\":\"CANCELADO\",\"label\":\"Cancelado\"}]", tableHidden = false, quickViewVisible = true, quickViewOrder = 3, preset = UISchemaPreset.ENTERPRISE_STATUS)
  public String getSituacao() { return super.getSituacao(); }

  @Override
  @Schema(description = "Primeiro dia do ciclo de aquisição de licença-prêmio ou especial no vínculo.")
  @UISchema(label = "Início da aquisição", helpText = "Primeiro dia do ciclo aquisitivo.", group = "Direito adquirido", order = 50, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", tableHidden = false, quickViewVisible = true, quickViewOrder = 1, preset = UISchemaPreset.DATE_RANGE)
  public LocalDate getDtini() { return super.getDtini(); }
  @Override
  @Schema(description = "Data de encerramento da aquisição do direito neste ciclo, considerada na política de antecipação.", nullable = true)
  @UISchema(label = "Fim da aquisição", helpText = "Último dia necessário à aquisição do direito.", group = "Direito adquirido", order = 60, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available", tableHidden = false, preset = UISchemaPreset.DATE_RANGE)
  public LocalDate getDtfim() { return super.getDtfim(); }
  @NotBlank @Size(max = 200)
  @Schema(description = "Identificação do ciclo de aquisição de licença-prêmio ou especial do vínculo, permitindo distinguir quinquênios sucessivos.")
  
  @UISchema(label = "Período de licença", helpText = "Identifique o ciclo aquisitivo no vínculo.", group = "Direito adquirido", order = 1, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "date_range", tableHidden = false, preset = UISchemaPreset.ENTERPRISE_NAME)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private String descricao;
  @Min(1)
  @Schema(description = "Chave do período de licença especial no vínculo, referenciada por parcelas e vendas. Se omitida na inclusão, o servidor reserva a próxima chave.")
  @UISchema(label = "Chave do período", helpText = "Sequencial único do ciclo aquisitivo no vínculo.", group = "Direito adquirido", order = 110, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", tableHidden = true)
  private Integer chave;
  @NotNull @Min(0) @Max(999)
  @Schema(description = "Dias de licença especial atribuídos ao ciclo, conforme PERAQLICESP.DIAS_DIREITO; o direito é independente da duração em anos da aquisição.")
  @UISchema(label = "Dias de direito", helpText = "Dias concedidos pelo regime ao completar a aquisição.", group = "Direito adquirido", order = 120, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "verified", tableHidden = false, quickViewVisible = true, quickViewOrder = 4)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private Integer diasDireito = 90;
  @NotNull @Min(0)
  @Schema(description = "Dias de licença consumidos pela conversão em dobro. O acréscimo de tempo é duas vezes este valor, sem duplicar o débito no saldo de licença.")
  @UISchema(readOnly = true, label = "Dias convertidos em dobro", helpText = "Informe os dias originais de licença convertidos.", group = "Contabilidade do direito", order = 150, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "more_time", tableHidden = true)
  private Integer diasConvertidosDobro = 0;
  @Schema(description = "Data limite prescricional do direito neste ciclo, quando o regime estabelecer prescrição.", nullable = true)
  @UISchema(label = "Prescrição do direito", helpText = "Prazo de utilização previsto para este período.", group = "Prazos do direito", order = 170, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "warning", tableHidden = true, quickViewVisible = true, quickViewOrder = 2, preset = UISchemaPreset.DATE_RANGE)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private LocalDate dtPrescricao;
  @Schema(description = "Data de efeito da conversão de licença não usufruída em contagem de tempo em dobro, conforme DT_CNT_DOBRO.", nullable = true)
  @UISchema(readOnly = true, label = "Efeito da contagem em dobro", helpText = "Data de aproveitamento da conversão em tempo funcional.", group = "Prazos do direito", order = 180, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "more_time", tableHidden = true, preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dtCntDobro;
  @Min(1) @Max(99)
  @Schema(description = "Anos trabalhados para aquisição neste ciclo. Quando ausente, a apuração posterior deverá consultar o parâmetro estatutário do vínculo.", nullable = true)
  @UISchema(label = "Anos para aquisição", helpText = "Override individual do tempo necessário à aquisição.", group = "Parâmetros individuais", order = 190, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tune", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Integer anosLicEsp;
  @Min(1) @Max(99)
  @Schema(description = "Meses de gozo previstos para o direito deste ciclo, priorizando a definição individual de PERAQLICESP sobre a matriz geral.", nullable = true)
  @UISchema(label = "Meses de direito", helpText = "Override individual da extensão da licença concedida.", group = "Parâmetros individuais", order = 200, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tune", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Integer mesesLicEspPer;
  @Pattern(regexp = "D|G|N")
  @Schema(description = "Base de cálculo do abono: D aplica a razão aos dias de direito; G aos dias de gozo; N exclui abono. Ausência preserva a herança do parâmetro geral.", allowableValues = {"D", "G", "N"}, nullable = true)
  @UISchema(label = "Base do abono", helpText = "Direito, gozo ou sem abono; vazio mantém a herança.", group = "Parâmetros individuais", order = 210, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "calculate", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private String abonoDirGozoLicEsp;
  @Min(0)
  @Schema(description = "Numerador da razão dias de abono por dias de licença especial, aplicado à base D ou G selecionada.", nullable = true)
  @UISchema(label = "Numerador do abono", helpText = "Parcela superior da proporção de abono.", group = "Parâmetros individuais", order = 220, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "calculate", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Integer numDiasAbonoLicEsp;
  @Min(1)
  @Schema(description = "Denominador positivo da razão do abono; zero não constitui proporção válida.", nullable = true)
  @UISchema(label = "Denominador do abono", helpText = "Parcela inferior positiva da proporção de abono.", group = "Parâmetros individuais", order = 230, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "calculate", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Integer denDiasAbonoLicEsp;
  @Pattern(regexp = "I|S|N")
  @Schema(description = "Permissão individual de venda: I herda a matriz, S permite e N veda; a precedência por chave do período é documentada na Tarefa 82601.", allowableValues = {"I", "S", "N"})
  @UISchema(label = "Permissão de venda", helpText = "Herdar, permitir ou vedar venda neste ciclo.", group = "Parâmetros individuais", order = 240, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "sell", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private String vendLicEsp = "I";
  @Min(1)
  @Schema(description = "Mínimo de dias que podem ser vendidos por evento neste ciclo, quando houver override individual.", nullable = true)
  @UISchema(label = "Mínimo por venda", helpText = "Piso de dias por evento de venda.", group = "Parâmetros individuais", order = 250, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "sell", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Integer minDiasPvendLicEsp;
  @Min(0)
  @Schema(description = "Teto de dias vendáveis no período aquisitivo inteiro, distinto do mínimo por evento.", nullable = true)
  @UISchema(label = "Teto de dias vendidos", helpText = "Limite acumulado de vendas deste ciclo.", group = "Parâmetros individuais", order = 260, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "sell", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Integer maxDiasVendLicEsp;
  @Pattern(regexp = "I|S|N")
  @Schema(description = "Permissão individual para conversão em dobro: I herda, S permite e N veda, conforme funções da Tarefa 58924 e precedência da Tarefa 82601.", allowableValues = {"I", "S", "N"})
  @UISchema(label = "Permissão de contagem em dobro", helpText = "Herdar, permitir ou vedar conversão neste ciclo.", group = "Parâmetros individuais", order = 270, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "more_time", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private String contDobroLicEsp = "I";
  @Min(1)
  @Schema(description = "Mínimo de dias originais de licença convertíveis em dobro por evento neste ciclo.", nullable = true)
  @UISchema(label = "Mínimo por conversão", helpText = "Piso de dias originais por conversão.", group = "Parâmetros individuais", order = 280, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "more_time", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Integer minDiasPcontDobLicEsp;
  @Min(0)
  @Schema(description = "Máximo acumulado de dias originais convertíveis em dobro por período aquisitivo.", nullable = true)
  @UISchema(label = "Teto de conversão em dobro", helpText = "Teto de dias originais convertidos no ciclo.", group = "Parâmetros individuais", order = 290, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "more_time", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Integer maxDiasContDobLicEsp;
  @NotNull
  @Schema(description = "Política individual de fruição antes do término da aquisição: HERDAR usa a matriz estatutária, PERMITIR autoriza e VEDAR impede antecipação. A escolha não executa a concessão.", type = "string", implementation = String.class, allowableValues = {"HERDAR", "PERMITIR", "VEDAR"}, defaultValue = "HERDAR")
  @UISchema(label = "Gozo antes da aquisição", helpText = "Escolha herdar a regra, permitir ou vedar antecipação.", group = "Parâmetros individuais", order = 300, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, options = "[{\"value\":\"HERDAR\",\"label\":\"Herdar a regra\"},{\"value\":\"PERMITIR\",\"label\":\"Permitir\"},{\"value\":\"VEDAR\",\"label\":\"Vedar\"}]", icon = "event_available", tableHidden = true, preset = UISchemaPreset.ENTERPRISE_STATUS)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private br.com.praxis.ergonshowcase.domain.PoliticaAntecipacaoLicenca permGozoLicEspAntFimPe = br.com.praxis.ergonshowcase.domain.PoliticaAntecipacaoLicenca.HERDAR;
  @Schema(description = "Averbação que originou este período aquisitivo por aproveitamento de tempo e complemento de finalidade.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.averbacoes-tempo-servico", valueField = "averbacaoOrigemId", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @UISchema(label = "Averbação de origem", helpText = "Selecione o tempo reconhecido que formou o ciclo.", group = "Origem do direito", order = 310, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.AVERBACAO_TEMPO_SERVICO_OPTIONS, valueField = "id", displayField = "label", icon = "history_edu", tableHidden = true, preset = UISchemaPreset.ENTERPRISE_ID)
  private Long averbacaoOrigemId;
  @Size(max = 255)
  @Schema(description = "Referência documental do anexo do ciclo aquisitivo, correspondente à finalidade documental de PERAQLICESP.PONTARQS.", nullable = true)
  @UISchema(label = "Referência do anexo", helpText = "Identificação de negócio do documento anexado.", group = "Lastro documental", order = 320, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "attachment", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String referenciaAnexo;
  @CustomFields(resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial")
  @Schema(description = "Extensões institucionais do período aquisitivo, governadas pelo catálogo do órgão em substituição aos flex fields legados.")
  @UISchema(label = "Campos customizados", helpText = "Atributos adicionais definidos pelo órgão para o ciclo.", group = "Extensões institucionais", order = 900, tableHidden = true)
  private Map<String, Object> customFields = new LinkedHashMap<>();
  @Schema(description = "Saldo inicial previsto para um novo assentamento, sem reserva ou autorização. A escrita revalida os dados e a política de domínio.",accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Saldo inicial previsto", helpText = "Prévia não persistente; não certifica concessão nem disponibilidade na confirmação.", group = "Prévia de criação", readOnly = true, tableHidden = true, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "calculate")
  private Integer diasPrevistos;
}
