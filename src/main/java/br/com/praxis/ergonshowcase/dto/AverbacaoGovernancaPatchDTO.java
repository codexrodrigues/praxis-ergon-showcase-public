package br.com.praxis.ergonshowcase.dto;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
import lombok.Getter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.*;
@Getter
@Schema(description = "Documentos e observações: propriedades ausentes preservam o valor persistido; nulo explícito só é aceito quando o domínio permitir.")
public class AverbacaoGovernancaPatchDTO extends org.praxisplatform.uischema.dto.PartialUpdateInput {
  @Schema(description = "Documento que fundamenta juridicamente o reconhecimento do tempo ou a concessão da licença.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.documentos-legais", valueField = "documentoLegalId", surfaceId = "ficha-documento", previewSurfaceId = "ficha-documento", detailPresentation = "drawer")
  @UISchema(label = "Fundamento legal", helpText = "Selecione o documento de embasamento do direito.", group = "Lastro documental", order = 80, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.DOCUMENTO_OPTIONS, valueField = "id", displayField = "label", icon = "gavel", tableHidden = true, preset = UISchemaPreset.LEGAL_DOCUMENT_REFERENCE)
  private Long documentoLegalId;
  @com.fasterxml.jackson.annotation.JsonSetter("documentoLegalId")
  public void setDocumentoLegalId(Long value) { supplied("documentoLegalId"); this.documentoLegalId=value; }
  @Schema(description = "Ato de publicação que dá publicidade ao lançamento, distinto do documento de embasamento legal.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.documentos-legais", valueField = "atoPublicacaoId", surfaceId = "ficha-documento", previewSurfaceId = "ficha-documento", detailPresentation = "drawer")
  @UISchema(label = "Ato de publicação", helpText = "Selecione a publicação correspondente ao assentamento.", group = "Lastro documental", order = 90, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.DOCUMENTO_OPTIONS, valueField = "id", displayField = "label", icon = "description", tableHidden = true, preset = UISchemaPreset.LEGAL_DOCUMENT_REFERENCE)
  private Long atoPublicacaoId;
  @com.fasterxml.jackson.annotation.JsonSetter("atoPublicacaoId")
  public void setAtoPublicacaoId(Long value) { supplied("atoPublicacaoId"); this.atoPublicacaoId=value; }
  @Size(max = 2000)
  @Schema(description = "Justificativa complementar do lançamento para análise funcional; não substitui documento comprobatório nem pode conter credenciais ou dados de saúde desnecessários.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.RESTRICTED, dataCategory = DomainDataCategory.PERSONAL, complianceTags = {"LGPD"}, reason = "Texto livre de assentamento funcional requer revisão antes de divulgação.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.DENY, trainingUse = AiTrainingUseMode.DENY, reasoningUse = AiControlledUseMode.DENY))
  @UISchema(label = "Observações", helpText = "Registre apenas informações necessárias à análise do direito.", group = "Lastro documental", order = 100, type = FieldDataType.TEXT, controlType = FieldControlType.TEXTAREA, icon = "notes", tableHidden = true, visibleForAuthorities = {"ERGON_READ"}, editableForAuthorities = {"ERGON_WRITE"}, fieldAccessReason = "Acesso restrito à manutenção de assentamentos funcionais")
  private String observacoes;
  @com.fasterxml.jackson.annotation.JsonSetter("observacoes")
  public void setObservacoes(String value) { supplied("observacoes"); this.observacoes=value; }
  @Size(max = 2000)
  @Schema(description = "Motivação administrativa do reconhecimento do serviço anterior e referência à análise da certidão.", nullable = true)
  @UISchema(label = "Motivo da averbação", helpText = "Fundamente o reconhecimento sem incluir dados desnecessários.", group = "Lastro documental", order = 200, type = FieldDataType.TEXT, controlType = FieldControlType.TEXTAREA, icon = "notes", tableHidden = true)
  private String motivo;
  @com.fasterxml.jackson.annotation.JsonSetter("motivo")
  public void setMotivo(String value) { supplied("motivo"); this.motivo=value; }
  @Size(max = 2000)
  @Schema(description = "Primeira informação complementar da finalidade de contagem, preservada conforme AVERB_OQUE_CONTA.INFO1.", nullable = true)
  @UISchema(label = "Informação da finalidade", helpText = "Detalhe o enquadramento da finalidade reconhecida.", group = "Efeitos da contagem", order = 260, type = FieldDataType.TEXT, controlType = FieldControlType.TEXTAREA, icon = "info", tableHidden = true)
  private String info1;
  @com.fasterxml.jackson.annotation.JsonSetter("info1")
  public void setInfo1(String value) { supplied("info1"); this.info1=value; }
  @Size(max = 2000)
  @Schema(description = "Segunda informação complementar da finalidade de contagem, preservada conforme AVERB_OQUE_CONTA.INFO2.", nullable = true)
  @UISchema(label = "Complemento da informação", helpText = "Acrescente informações necessárias ao aproveitamento.", group = "Efeitos da contagem", order = 270, type = FieldDataType.TEXT, controlType = FieldControlType.TEXTAREA, icon = "info", tableHidden = true)
  private String info2;
  @com.fasterxml.jackson.annotation.JsonSetter("info2")
  public void setInfo2(String value) { supplied("info2"); this.info2=value; }
  @Size(max = 255)
  @Schema(description = "Referência documental do anexo comprobatório da averbação, sem expor localizadores privados de armazenamento.", nullable = true)
  @UISchema(label = "Referência do anexo", helpText = "Referência de negócio da certidão anexada.", group = "Lastro documental", order = 280, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "attachment", tableHidden = true)
  private String referenciaAnexo;
  @com.fasterxml.jackson.annotation.JsonSetter("referenciaAnexo")
  public void setReferenciaAnexo(String value) { supplied("referenciaAnexo"); this.referenciaAnexo=value; }
}
