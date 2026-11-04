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
@Schema(description = "Documentação da parcela: propriedades ausentes preservam o valor persistido; nulo explícito só é aceito quando o domínio permitir.")
public class LicencaEspecialGozoSubstitutoPatchDTO extends org.praxisplatform.uischema.dto.PartialUpdateInput {
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
  @Schema(description = "Servidor designado para substituir o titular durante a parcela de licença. A existência e a situação funcional são verificadas no banco; não é uma declaração do cliente de que o substituto está ativo.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.funcionarios", valueField = "numfuncSubstituto", surfaceId = "ficha-cadastral", previewSurfaceId = "ficha-cadastral", detailPresentation = "drawer")
  @UISchema(label = "Servidor substituto", helpText = "Selecione o servidor designado para a substituição.", group = "Substituição", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.PESSOA_OPTIONS, valueField = "id", displayField = "label", icon = "supervisor_account", tableHidden = true, preset = UISchemaPreset.ENTERPRISE_ID)
  private Long numfuncSubstituto;
  @com.fasterxml.jackson.annotation.JsonSetter("numfuncSubstituto")
  public void setNumfuncSubstituto(Long value) { supplied("numfuncSubstituto");this.numfuncSubstituto=value; }
}
