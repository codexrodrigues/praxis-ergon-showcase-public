package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.annotation.*;

/** Ato coletivo de homologação, sem inferir aprovação jurídica a partir do decurso do tempo. */
@Schema(description="Homologação administrativa coletiva de estágios de vínculos ativos ainda não homologados.")
public record HomologarEstagioLoteDTO(
    @NotNull
    @Schema(description="Data de homologação registrada pelo ato coletivo, igual ou posterior ao exercício de cada vínculo.")
    @UISchema(label="Data da homologação",group="Homologação",type=FieldDataType.DATE,controlType=FieldControlType.DATE_PICKER,icon="event")
    LocalDate dtHomologEstagio,
    @NotNull @Positive
    @Schema(description="Documento do órgão responsável que formaliza a homologação coletiva dos estágios selecionados.")
    @UISchema(label="Ato de homologação",group="Fundamentação",type=FieldDataType.TEXT,controlType=FieldControlType.ENTITY_LOOKUP,icon="gavel",endpoint=ApiPaths.DOCUMENTO_OPTIONS,valueField="id",displayField="label")
    Long documentoLegalId,
    @Size(max=2000)
    @DomainGovernance(kind=DomainGovernanceKind.PRIVACY, classification=DomainClassification.CONFIDENTIAL,
        dataCategory=DomainDataCategory.PERSONAL,
        reason="Justificativa do assentamento de estágio pode conter informação pessoal dos servidores abrangidos pelo ato.",
        aiUsage=@AiUsagePolicy(visibility=AiVisibilityMode.MASK,trainingUse=AiTrainingUseMode.DENY))
    @Schema(description="Justificativa complementar retida na intenção protegida da operação; não altera o motivo cadastral do vínculo.")
    @UISchema(label="Justificativa",group="Fundamentação",controlType=FieldControlType.TEXTAREA,tableHidden=true)
    String observacao) {}
