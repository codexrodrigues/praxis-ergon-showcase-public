package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Resultado determinístico da prévia reativa de licença/afastamento em rascunho. */
public record AfastamentoPreviaPreviewDTO(
    @Schema(description = "Descrição oficial da modalidade de licença localizada no catálogo.")
        @UISchema(
            label = "Modalidade prevista",
            group = "Modalidade",
            order = 10,
            readOnly = true)
        String codigoFrequenciaNomePrevisto,
    @Schema(description = "Quantidade prevista de dias corridos entre início e término previsto.")
        @UISchema(
            label = "Duração prevista (dias)",
            group = "Período",
            order = 20,
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.INPUT,
            icon = "timelapse",
            readOnly = true)
        Integer quantidadeDiasPrevista,
    @Schema(description = "Indica se o código de frequência selecionado implica ônus financeiro.")
        @UISchema(
            label = "Implica ônus (previsto)",
            group = "Modalidade",
            order = 30,
            type = FieldDataType.BOOLEAN,
            controlType = FieldControlType.CHECKBOX,
            icon = "payments",
            readOnly = true)
        Boolean comOnusPrevisto) {}
