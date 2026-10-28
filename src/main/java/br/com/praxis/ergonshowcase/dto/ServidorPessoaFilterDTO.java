package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(description = "Critérios de pesquisa de pessoas servidoras na administração de pessoal.")
public class ServidorPessoaFilterDTO implements GenericFilterDTO {
  @Schema(description = "Localiza a pessoa por trecho do nome civil ou social, CPF, filiação materna ou paterna ou endereço eletrônico. Basta um desses atributos corresponder; os demais critérios informados continuam restringindo o resultado.", example = "Maria")
  @UISchema(label = "Busca ampla", group = "Pesquisa", order = 1, icon = "search", type = org.praxisplatform.uischema.FieldDataType.TEXT, controlType = org.praxisplatform.uischema.FieldControlType.INPUT)
  @Filterable(operation = FilterOperation.LIKE, relations = {"nome", "nomeSocial", "cpf", "mae", "pai", "eMail"})
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.PERSONAL, reason = "Busca cadastral pode conter identificação civil, filiação ou contato pessoal.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String buscaAmpla;

  @Schema(
      description =
          "Seleciona pessoas com ao menos um indicador específico de deficiência registrado.")
  @UISchema(label = "Pessoas com deficiência", group = "Acessibilidade e PcD")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Critério de pesquisa sobre dado sensível de saúde ou identificação étnica.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean somentePcD;

  @Schema(description = "Trecho do nome civil para localizar a pessoa no cadastro único.")
  @UISchema(label = "Nome", group = "Pesquisa", order = 10)
  @Filterable(operation = FilterOperation.LIKE)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String nome;

  @Schema(
      description = "Intervalo inclusivo de nascimento usado para localizar pessoas por período.")
  @UISchema(
      label = "Período de nascimento",
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_RANGE,
      group = "Pesquisa",
      order = 20,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtnasc")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private List<LocalDate> nascimento;

  @CustomFields(resourceKey = "administracao-pessoal.funcionarios")
  @Schema(description = "Predicados sobre os atributos civis adicionais publicados para o órgão.")
  @UISchema(label = "Características adicionais", group = "Pesquisa", order = 30)
  private Map<String, Object> customFields;

  @Schema(description = "Trecho do nome social adotado pela pessoa.")
  @UISchema(label = "Nome social", group = "Pesquisa avançada", order = 200)
  @Filterable(operation = FilterOperation.LIKE, relation = "nomeSocial")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String nomeSocial;

  @Schema(description = "CPF exato da pessoa consultada.")
  @UISchema(label = "CPF exato", group = "Pesquisa avançada", order = 201)
  @Filterable(operation = FilterOperation.EQUAL, relation = "cpf")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String cpf;

  @Schema(description = "Prefixo do CPF da pessoa.")
  @UISchema(label = "CPF começa com", group = "Pesquisa avançada", order = 202)
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "cpf")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String cpfPrefixo;

  @Schema(description = "Inscrição PIS/PASEP exata.")
  @UISchema(label = "PIS/PASEP", group = "Pesquisa avançada", order = 203)
  @Filterable(operation = FilterOperation.EQUAL, relation = "pispasep")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String pispasep;

  @Schema(description = "Número exato da identidade civil.")
  @UISchema(label = "Identidade civil", group = "Pesquisa avançada", order = 204)
  @Filterable(operation = FilterOperation.EQUAL, relation = "numrg")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String numrg;

  @Schema(description = "Sexo registral incluído na consulta.")
  @UISchema(label = "Sexo registral", group = "Pesquisa avançada", order = 205)
  @Filterable(operation = FilterOperation.EQUAL, relation = "sexo")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String sexo;

  @Schema(description = "Estados civis incluídos na consulta.")
  @UISchema(label = "Estados civis", group = "Pesquisa avançada", order = 206)
  @Filterable(operation = FilterOperation.IN, relation = "estcivil")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private List<String> estadosCivis;

  @Schema(description = "Graus de instrução das pessoas consultadas.")
  @UISchema(label = "Escolaridades", group = "Pesquisa avançada", order = 207)
  @Filterable(operation = FilterOperation.IN, relation = "escolaridade")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private List<String> escolaridades;

  @Schema(description = "Raças ou cores autodeclaradas incluídas.")
  @UISchema(label = "Raças/cores", group = "Pesquisa avançada", order = 208)
  @Filterable(operation = FilterOperation.IN, relation = "racaCor")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Critério de pesquisa sobre dado sensível de saúde ou identificação étnica.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private List<String> racasCor;

  @Schema(description = "Grupos sanguíneos incluídos na consulta.")
  @UISchema(label = "Grupos sanguíneos", group = "Pesquisa avançada", order = 209)
  @Filterable(operation = FilterOperation.IN, relation = "gSanguineo")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Critério de pesquisa sobre dado sensível de saúde ou identificação étnica.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private List<String> gruposSanguineos;

  @Schema(description = "Data civil exata de nascimento.")
  @UISchema(
      label = "Nascimento na data",
      group = "Pesquisa avançada",
      order = 210,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  @Filterable(operation = FilterOperation.ON_DATE, relation = "dtnasc")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private LocalDate dataNascimentoExata;

  @Schema(description = "UF de residência das pessoas consultadas.")
  @UISchema(label = "UF de residência", group = "Pesquisa avançada", order = 211)
  @Filterable(operation = FilterOperation.EQUAL, relation = "ufender")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String ufender;

  @Schema(description = "Trecho do município de residência.")
  @UISchema(label = "Município de residência", group = "Pesquisa avançada", order = 212)
  @Filterable(operation = FilterOperation.LIKE, relation = "cidadeender")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String cidadeender;

  @Schema(description = "Trecho do endereço de correio eletrônico.")
  @UISchema(label = "E-mail", group = "Pesquisa avançada", order = 213)
  @Filterable(operation = FilterOperation.LIKE, relation = "eMail")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @Getter(onMethod_ = @com.fasterxml.jackson.annotation.JsonProperty("eMail"))
  @Setter(onMethod_ = @com.fasterxml.jackson.annotation.JsonProperty("eMail"))
  private String eMail;

  @Schema(description = "Indicador de deficiência física registrado.")
  @UISchema(label = "Deficiência física", group = "Pesquisa avançada", order = 214)
  @Filterable(operation = FilterOperation.EQUAL, relation = "deffisica")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Critério de pesquisa sobre dado sensível de saúde ou identificação étnica.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean deffisica;

  @Schema(description = "Indicador de deficiência visual registrado.")
  @UISchema(label = "Deficiência visual", group = "Pesquisa avançada", order = 215)
  @Filterable(operation = FilterOperation.EQUAL, relation = "defvisual")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Critério de pesquisa sobre dado sensível de saúde ou identificação étnica.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean defvisual;

  @Schema(description = "Indicador de deficiência auditiva registrado.")
  @UISchema(label = "Deficiência auditiva", group = "Pesquisa avançada", order = 216)
  @Filterable(operation = FilterOperation.EQUAL, relation = "defauditiva")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Critério de pesquisa sobre dado sensível de saúde ou identificação étnica.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean defauditiva;

  @Schema(description = "Indicador de deficiência mental registrado.")
  @UISchema(label = "Deficiência mental", group = "Pesquisa avançada", order = 217)
  @Filterable(operation = FilterOperation.EQUAL, relation = "defmental")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Critério de pesquisa sobre dado sensível de saúde ou identificação étnica.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean defmental;

  @Schema(description = "Indicador de deficiência intelectual registrado.")
  @UISchema(label = "Deficiência intelectual", group = "Pesquisa avançada", order = 218)
  @Filterable(operation = FilterOperation.EQUAL, relation = "defintelectual")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Critério de pesquisa sobre dado sensível de saúde ou identificação étnica.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean defintelectual;

  @Schema(description = "Seleciona pessoas com nome social registrado.")
  @UISchema(label = "Com nome social", group = "Pesquisa avançada", order = 219)
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "nomeSocial")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean possuiNomeSocial;

  @Schema(description = "Janela em dias até o vencimento da CNH.")
  @UISchema(label = "CNH vencendo nos próximos dias", group = "Pesquisa avançada", order = 220)
  @Filterable(operation = FilterOperation.IN_NEXT_DAYS, relation = "validcnh")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Integer cnhVencendoNosProximosDias;

  @Schema(description = "Janela em dias desde a inclusão cadastral.")
  @UISchema(label = "Cadastro nos últimos dias", group = "Pesquisa avançada", order = 221)
  @Filterable(operation = FilterOperation.IN_LAST_DAYS, relation = "auditoria.createdAt")
  private Integer cadastradoNosUltimosDias;
}
