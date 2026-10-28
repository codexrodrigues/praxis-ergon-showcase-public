package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.extension.annotation.UISchema;

public record EncerrarCessaoDTO(
    @NotNull
        @PastOrPresent
        @Schema(description = "Data efetiva do encerramento da cessão.")
        @UISchema(label = "Encerramento", group = "Fundamentação do ato", type = org.praxisplatform.uischema.FieldDataType.DATE,
            controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER, icon = "event")
        LocalDate dtfimCessao) {}
