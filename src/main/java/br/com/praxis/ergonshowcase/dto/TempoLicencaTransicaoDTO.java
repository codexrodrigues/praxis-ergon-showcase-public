package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.extension.annotation.*;

@Getter @Setter
@Schema(description = "Lastro de uma transição de tempo/licença: data de efeito, justificativa e, para desaverbação ou conversão, quantidade de dias originais. Não altera identidade nem aceita estado de destino do cliente.")
public class TempoLicencaTransicaoDTO {
  @NotNull
  @Schema(description = "Data de efeito do ato. No encerramento/interrupção é o último dia efetivamente usufruído; no pagamento é a liquidação; no estorno é a compensação, que não pode anteceder o pagamento original.")
  @UISchema(label = "Data de efeito", helpText = "Data em que o ato produz efeito funcional.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dataEfeito;
  @NotBlank @Size(max = 2000)
  @Schema(description = "Justificativa administrativa do ato, necessária inclusive no estorno e na reconciliação de saldos.")
  @UISchema(label = "Justificativa", helpText = "Registre o fundamento da decisão administrativa.", controlType = FieldControlType.TEXTAREA, icon = "notes")
  private String justificativa;
  @Min(1) @Max(999)
  @Schema(description = "Dias originais retirados ou restituídos pela desaverbação/conversão. Não informa saldo, dias gozados nem total vendido, que são apurados pelo servidor.", nullable = true)
  @UISchema(label = "Dias do ato", helpText = "Quantidade original alcançada pelo ato, quando exigida.", type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "timelapse")
  private Integer dias;
}
