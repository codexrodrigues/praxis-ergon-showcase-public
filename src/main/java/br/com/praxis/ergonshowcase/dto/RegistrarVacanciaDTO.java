package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Encerramento funcional documentado, conforme VINCULOS.TAB e comentários do banco. */
public record RegistrarVacanciaDTO(
    @NotNull
        @PastOrPresent
        @Schema(
            description =
                "Data em que cessa o exercício deste vínculo; não pode anteceder seu início nem estar no futuro.")
        @UISchema(
            type = org.praxisplatform.uischema.FieldDataType.DATE,
            controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
            icon = "calendar_today",
            label = "Data de vacância",
            order = 1)
        LocalDate data,
    @NotBlank
        @Size(max = 20)
        @Schema(
            description =
                "Forma jurídica do desligamento registrada no assentamento, como EXONERACAO ou DEMISSAO; não determina automaticamente efeitos de folha.")
        @UISchema(label = "Forma da vacância", order = 2)
        String forma,
    @NotBlank
        @Size(max = 2000)
        @Schema(
            description =
                "Fundamentação administrativa do encerramento, com referência ao ato que sustenta a vacância.")
        @UISchema(label = "Fundamentação", order = 3)
        String motivo,
    @Schema(
            description =
                "Data limite de pagamento após a vacância; quando preenchida, não pode anteceder o desligamento.")
        @UISchema(
            type = org.praxisplatform.uischema.FieldDataType.DATE,
            controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
            icon = "calendar_today",
            label = "Pagamento até",
            group = "Aposentadoria e vacância",
            order = 4)
        LocalDate dtPgtoAte) {}
