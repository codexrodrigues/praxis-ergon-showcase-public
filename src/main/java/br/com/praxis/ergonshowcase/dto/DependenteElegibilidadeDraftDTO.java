package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
    description =
        "Rascunho do formulário de dependente para determinação reativa de idade e elegibilidade tributária a IRRF.")
public record DependenteElegibilidadeDraftDTO(
    @Schema(description = "Classificação legal do tipo de dependência informada no rascunho.")
        @UISchema(
            label = "Tipo de dependência",
            group = "Enquadramento",
            order = 1,
            type = FieldDataType.TEXT,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "category",
            endpoint = ApiPaths.TIPO_DEPENDENTE_OPTIONS,
            valueField = "id",
            displayField = "label")
        String tipodepen,
    @Schema(description = "Data de nascimento do dependente informada no rascunho.")
        @UISchema(
            label = "Data de nascimento",
            group = "Enquadramento",
            order = 2,
            type = FieldDataType.DATE,
            controlType = FieldControlType.DATE_PICKER,
            icon = "cake")
        LocalDate dtnasc,
    @Schema(description = "Indica se o dependente cursa ensino superior ou técnico no rascunho.")
        @UISchema(
            label = "Universitário",
            group = "Enquadramento",
            order = 3,
            type = FieldDataType.BOOLEAN,
            controlType = FieldControlType.CHECKBOX,
            icon = "school")
        Boolean universitario,
    @Schema(description = "Indica se o dependente possui invalidez permanente atestada no rascunho.")
        @UISchema(
            label = "Invalidez atestada",
            group = "Enquadramento",
            order = 4,
            type = FieldDataType.BOOLEAN,
            controlType = FieldControlType.CHECKBOX,
            icon = "medical_information")
        Boolean invalido) {}
