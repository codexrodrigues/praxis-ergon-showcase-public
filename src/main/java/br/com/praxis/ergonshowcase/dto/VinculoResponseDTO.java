package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.domain.VinculoId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@Schema(
    description =
        "Assentamentos funcionais por pessoa e número sequencial, com enquadramento e exercício.")
@ResourceIdentity(
    keyField = "id",
    titleField = "matric",
    displayLabelField = "matric",
    subtitleField = "pessoaNome",
    metadataFields = {"orgaoNome", "cargoNome", "situacao"})
public class VinculoResponseDTO extends VinculoDados {
  @Schema(
      description =
          "Identidade do único afastamento homologado aberto deste vínculo; nulo quando não há exatamente um.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Afastamento aberto",
      group = "Afastamentos",
      readOnly = true,
      tableHidden = true)
  private Long afastamentoAbertoId;

  @Schema(
      description =
          "Identidade imutável formada pelo número funcional e pelo número do vínculo, como 1001:1.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Identificação",
      preset = UISchemaPreset.ENTERPRISE_ID,
      readOnly = true,
      formHidden = true,
      group = "Identificação",
      order = 0)
  private VinculoId id;

  @Schema(
      description =
          "Órgão proprietário do cargo ou vínculo, preservado durante a manutenção cadastral.")
  @UISchema(
      label = "Código do órgão",
      readOnly = true,
      tableHidden = true,
      group = "Lotação",
      order = 101)
  private Integer empCodigo;

  @Schema(description = "Nome oficial do órgão de origem para leitura do assentamento.")
  @UISchema(
      label = "Órgão",
      readOnly = true,
      formHidden = true,
      group = "Lotação",
      order = 102,
      icon = "apartment",
      quickViewVisible = true,
      quickViewOrder = 5)
  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  private String orgaoNome;

  @Schema(
      description =
          "Número funcional da pessoa titular do vínculo, primeiro componente da identidade.")
  @UISchema(
      label = "Número funcional",
      readOnly = true,
      group = "Identificação funcional",
      order = 1)
  private Long numfunc;

  @Schema(description = "Número sequencial do vínculo da pessoa, segundo componente da identidade.")
  @UISchema(label = "Vínculo", readOnly = true, group = "Identificação funcional", order = 2)
  private Integer numero;

  @Schema(description = "Nome civil da pessoa titular para identificação humana do assentamento.")
  @UISchema(
      label = "Pessoa servidora",
      readOnly = true,
      formHidden = true,
      group = "Identificação funcional",
      order = 3,
      quickViewVisible = true,
      quickViewOrder = 2)
  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfunc",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identifica a pessoa titular do vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String pessoaNome;

  @Schema(description = "Nomenclatura oficial do cargo de provimento associado ao vínculo.")
  @UISchema(
      label = "Cargo",
      readOnly = true,
      formHidden = true,
      group = "Lotação",
      order = 51,
      quickViewVisible = true,
      quickViewOrder = 3)
  @ResourceReference(
      resourceKey = "administracao-pessoal.cargos",
      valueField = "cargo",
      surfaceId = "ficha-cargo",
      previewSurfaceId = "ficha-cargo",
      detailPresentation = "drawer")
  private String cargoNome;

  @Schema(
      description =
          "Estado funcional atual: ativo, afastado, cedido, aposentado ou vacante; alterado apenas pela operação de negócio pertinente.")
  @UISchema(
      label = "Situação",
      preset = UISchemaPreset.ENTERPRISE_STATUS,
      readOnly = true,
      group = "Exercício",
      order = 110,
      quickViewVisible = true,
      quickViewOrder = 4)
  private String situacao;

  @Schema(description = "Data de início do efetivo exercício neste vínculo.")
  @UISchema(
      label = "Início de exercício",
      readOnly = true,
      controlType = FieldControlType.DATE_PICKER,
      group = "Exercício",
      order = 120,
      quickViewVisible = true,
      quickViewOrder = 6,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "calendar_today")
  private LocalDate dtexerc;

  @Schema(description = "Data do encerramento funcional por vacância, quando registrado.")
  @UISchema(
      label = "Vacância",
      readOnly = true,
      controlType = FieldControlType.DATE_PICKER,
      group = "Exercício",
      order = 130,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "calendar_today")
  private LocalDate dtvac;

  @Schema(
      description =
          "Quantidade de dias corridos desde o exercício até a vacância ou a data atual; não desconta afastamentos nem certifica tempo previdenciário.")
  @UISchema(label = "Dias desde o exercício", readOnly = true, group = "Exercício", order = 140)
  private Long diasDesdeExercicio;

  @Schema(
      description = "Data da homologação do estágio probatório registrada por comando.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Homologação do estágio",
      group = "Ingresso, concurso e estágio probatório",
      tableHidden = true,
      readOnly = true,
      formHidden = true,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "calendar_today")
  private LocalDate dtHomologEstagio;

  @Schema(
      description = "Modalidade administrativa de requisição ou cessão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Modalidade da cessão",
      group = "Cessão e requisição",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private String tipoReq;

  @Schema(
      description = "Órgão destinatário ou de origem da cessão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Órgão da cessão",
      group = "Cessão e requisição",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private String orgaoReq;

  @Schema(
      description = "Função desempenhada no órgão da cessão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Função na cessão",
      group = "Cessão e requisição",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private String funcaoReq;

  @Schema(
      description = "Responsabilidade pelo ônus financeiro da cessão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Ônus da cessão",
      group = "Cessão e requisição",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private String tipoOnusReq;

  @Schema(
      description = "Forma de ressarcimento entre os órgãos da cessão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Ressarcimento da cessão",
      group = "Cessão e requisição",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private String tipoRessarcReq;

  @Schema(
      description = "Data inicial da cessão registrada por comando.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Início da cessão",
      group = "Cessão e requisição",
      tableHidden = true,
      readOnly = true,
      formHidden = true,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "calendar_today")
  private LocalDate dtiniCessao;

  @Schema(
      description = "Data final da cessão registrada por comando.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Fim da cessão",
      group = "Cessão e requisição",
      tableHidden = true,
      readOnly = true,
      formHidden = true,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "calendar_today")
  private LocalDate dtfimCessao;

  @Schema(
      description = "Data de concessão da aposentadoria do vínculo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Aposentadoria",
      group = "Aposentadoria e vacância",
      tableHidden = true,
      readOnly = true,
      formHidden = true,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "calendar_today")
  private LocalDate dtaposent;

  @Schema(
      description = "Modalidade jurídica de aposentadoria concedida.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Modalidade de aposentadoria",
      group = "Aposentadoria e vacância",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private String tipoapos;

  @Schema(
      description = "Fator de proporcionalidade da aposentadoria concedida.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Proporção da aposentadoria",
      group = "Aposentadoria e vacância",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private java.math.BigDecimal fatorProporcAposent;

  @Schema(
      description = "Forma jurídica da vacância registrada.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Forma da vacância",
      group = "Aposentadoria e vacância",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private String formavac;

  @Schema(
      description = "Fundamentação administrativa da vacância registrada.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Fundamentação da vacância",
      group = "Aposentadoria e vacância",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private String motivovac;

  @Schema(
      description = "Data limite de pagamento após a vacância.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Pagamento até",
      group = "Aposentadoria e vacância",
      tableHidden = true,
      readOnly = true,
      formHidden = true,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "calendar_today")
  private LocalDate dtPgtoAte;

  @Schema(
      description = "Denominação da unidade de lotação do vínculo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Lotação",
      group = "Enquadramento e lotação",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private String setorNome;

  @Schema(
      description = "Denominação da unidade de exercício do vínculo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Exercício",
      group = "Enquadramento e lotação",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private String setorExercicioNome;

  @Schema(
      description =
          "Indica vínculo ativo em exercício cujo estágio ainda não foi homologado; não certifica elegibilidade jurídica.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Em estágio probatório",
      group = "Ingresso, concurso e estágio probatório",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private Boolean emEstagioProbatorio;

  @Schema(
      description = "Indica cargo em comissão associado ao vínculo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Com cargo em comissão",
      group = "Enquadramento e lotação",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private Boolean possuiCargoComissao;

  @Schema(
      description = "Indica estado funcional CEDIDO persistido.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Em cessão",
      group = "Cessão e requisição",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private Boolean emCessaoAtiva;
}
