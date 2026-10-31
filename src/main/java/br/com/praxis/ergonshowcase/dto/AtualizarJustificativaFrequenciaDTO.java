package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.dto.PartialUpdateInput;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Schema(
    description =
        "Manutenção parcial especializada para atualização de justificativa, documento comprobatório e data final da ocorrência.")
public class AtualizarJustificativaFrequenciaDTO extends PartialUpdateInput {

  @Size(max = 2000)
  @Schema(description = "Justificativa operacional ou fundamentação de abono da ocorrência.")
  @UISchema(
      label = "Justificativa e observações",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes",
      group = "Justificativa e Amparo Legal",
      order = 10,
      helpText = "Informe a justificativa ou parecer de revisão da ocorrência.")
  private String obs;

  @Schema(description = "Identificador do documento comprobatório ou processo anexo.")
  @UISchema(
      label = "Documento comprobatório",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "menu_book",
      group = "Justificativa e Amparo Legal",
      order = 20,
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o documento legal ou atestado comprobatório.")
  private Long documentoLegalId;

  @Schema(description = "Data final atualizada da ocorrência de frequência.")
  @UISchema(
      label = "Data final",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available",
      group = "Justificativa e Amparo Legal",
      order = 30,
      helpText = "Data de término do período da ocorrência.")
  private LocalDate dtfim;

  @JsonSetter("obs")
  public void setObs(String obs) {
    this.obs = obs;
    supplied("obs");
  }

  @JsonSetter("documentoLegalId")
  public void setDocumentoLegalId(Long documentoLegalId) {
    this.documentoLegalId = documentoLegalId;
    supplied("documentoLegalId");
  }

  @JsonSetter("dtfim")
  public void setDtfim(LocalDate dtfim) {
    this.dtfim = dtfim;
    supplied("dtfim");
  }
}
