package br.com.praxis.ergonshowcase.dto;
import org.praxisplatform.uischema.annotation.Threshold;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.annotation.ResourceIdentity;
import org.praxisplatform.uischema.extension.annotation.*;
@org.praxisplatform.uischema.fact.ResourceFact(key = "diasFinalidade", resourceKey = "administracao-pessoal.averbacoes-tempo-servico", label = "Dias por finalidade", description = "Dias certificados destinados à finalidade funcional declarada.", dataType = "integer", icon = "analytics", tone = "info", sourceKind = "property")
@org.praxisplatform.uischema.fact.ResourceFact(key = "diasDesaverbados", resourceKey = "administracao-pessoal.averbacoes-tempo-servico", label = "Dias desaverbados", description = "Dias retirados do aproveitamento do tempo certificado.", dataType = "integer", icon = "analytics", tone = "info", sourceKind = "property")
@org.praxisplatform.uischema.fact.ResourceFact(key = "totalDias", resourceKey = "administracao-pessoal.averbacoes-tempo-servico", label = "Dias reconhecidos", description = "Dias certificados antes do desconto parcial por desaverbação.", dataType = "integer", unit = "days", icon = "timelapse", tone = "info", sourceKind = "property", thresholds = {@Threshold(max = 365, tone = "info", label = "Até um ano"), @Threshold(min = 366, tone = "success", label = "Tempo extenso")})
@Getter @Setter
@ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "pessoaNome", metadataFields = {"instituicao", "finalidade"})
@Schema(description = "Averbação completa de serviço anterior, finalidade, desaverbação parcial e lastro documental.")
public class AverbacaoTempoServicoResponseDTO extends AverbacaoTempoServicoDados {
  @Schema(description = "Identidade imutável da averbação para navegação e referência por períodos aquisitivos.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Identidade da averbação", helpText = "Identificador estável do assentamento.", readOnly = true, tableHidden = true, formHidden = true, icon = "tag", preset = UISchemaPreset.ENTERPRISE_ID)
  private Long id;
  @Override
  @Schema(description = "Síntese da certidão e do tempo reconhecido para identificar a averbação no assentamento funcional.", accessMode = Schema.AccessMode.READ_ONLY)
  @org.praxisplatform.uischema.annotation.ResourceReference(resourceKey = "administracao-pessoal.averbacoes-tempo-servico", valueField = "id", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  public String getDescricao() { return super.getDescricao(); }

}
