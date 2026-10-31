package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Rascunho para determinação reativa de duração e reflexos do afastamento sem persistência. */
public record AfastamentoPreviaDraftDTO(
    @Schema(description = "Tipo macro de frequência informado no formulário.")
        @UISchema(
            label = "Tipo de frequência",
            group = "Modalidade",
            order = 10,
            type = FieldDataType.TEXT,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "category",
            endpoint = ApiPaths.TIPO_FREQUENCIA_OPTIONS,
            valueField = "id",
            displayField = "label")
        String tipofreq,
    @Schema(description = "Código numérico da licença informado no formulário.")
        @UISchema(
            label = "Código da licença",
            group = "Modalidade",
            order = 20,
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.INPUT,
            icon = "tag")
        Integer codfreq,
    @Schema(description = "Data de início do afastamento informada no formulário.")
        @UISchema(
            label = "Início do afastamento",
            group = "Período",
            order = 30,
            type = FieldDataType.DATE,
            controlType = FieldControlType.DATE_PICKER,
            icon = "event_available")
        LocalDate dtini,
    @Schema(description = "Data de término previsto informada no formulário.")
        @UISchema(
            label = "Término previsto",
            group = "Período",
            order = 40,
            type = FieldDataType.DATE,
            controlType = FieldControlType.DATE_PICKER,
            icon = "event")
        LocalDate dtprevfim) {}
