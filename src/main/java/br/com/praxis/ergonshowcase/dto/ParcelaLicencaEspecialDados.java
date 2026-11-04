package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.ResourceReference;
import org.praxisplatform.uischema.extension.annotation.*;

@Getter @Setter
public abstract class ParcelaLicencaEspecialDados extends AssentamentoTempoLicencaDados {
  @NotNull
  @Schema(description = "Período aquisitivo de licença especial ao qual a parcela pertence. O serviço resolve órgão, pessoa, vínculo e chave do período a partir desta identidade.")
  @ResourceReference(resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial", valueField = "periodoAquisitivoLicencaId", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @UISchema(label = "Período aquisitivo", helpText = "Selecione o ciclo; suas chaves funcionais serão preenchidas pelo serviço.", group = "Direito de origem", order = 110, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.PERIODO_AQUISITIVO_LICENCA_ESPECIAL_OPTIONS, valueField = "id", displayField = "label", icon = "date_range", tableHidden = true, preset = UISchemaPreset.ENTERPRISE_ID)
  private Long periodoAquisitivoLicencaId;
  @Schema(description = "Espelho da chave de negócio de PERAQLICESP, preenchido pelo período selecionado. Um valor informado divergente é rejeitado na criação.")
  @UISchema(label = "Chave do período", helpText = "Sequencial do ciclo no vínculo; preenchido pelo período.", group = "Direito de origem", order = 120, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", tableHidden = true, preset = UISchemaPreset.ENTERPRISE_CODE)
  private Integer chavePeraqlicesp;
}
