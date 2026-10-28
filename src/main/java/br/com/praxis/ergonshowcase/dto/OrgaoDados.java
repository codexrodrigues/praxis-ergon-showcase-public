package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

/** Dados institucionais editáveis do ente responsável pela administração de pessoal. */
@Getter
@Setter
public abstract class OrgaoDados {
  @NotBlank
  @Size(max = 100)
  @Schema(
      description =
          "Nome oficial resumido do ente público usado em cabeçalhos e consultas de seus quadros funcionais.")
  @UISchema(
      label = "Nome do órgão",
      preset = UISchemaPreset.ENTERPRISE_NAME,
      group = "Identificação",
      order = 10,
      icon = "apartment")
  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "id",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  private String nome;

  @Size(max = 60)
  @Schema(
      description =
          "Sigla institucional que permite distinguir o ente em consultas e seleções de lotação.")
  @UISchema(label = "Sigla", group = "Identificação", order = 20, icon = "short_text")
  private String fantasia;

  @NotBlank
  @Size(max = 200)
  @Schema(
      description =
          "Denominação jurídica completa do ente responsável pelas obrigações fiscais e previdenciárias.")
  @UISchema(label = "Razão social", group = "Identificação", order = 30, icon = "account_balance")
  private String razao;

  @NotBlank
  @Size(max = 18)
  @Schema(
      description =
          "Inscrição do ente no CNPJ; identifica a pessoa jurídica responsável pelos vínculos e deve ser única no cadastro.")
  @UISchema(
      label = "CNPJ",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.CPF_CNPJ_INPUT,
      group = "Identificação",
      order = 40,
      icon = "badge",
      quickViewVisible = true,
      quickViewOrder = 3)
  private String cnpj;

  @NotBlank
  @Pattern(regexp = "[A-Z]{2}")
  @Schema(description = "Sigla da unidade federativa onde está sediado o órgão.")
  @UISchema(
      label = "UF",
      group = "Sede e contato",
      order = 50,
      icon = "map",
      quickViewVisible = true,
      quickViewOrder = 2)
  @jakarta.validation.constraints.Size(max = 2)
  private String ufSigla;

  @NotBlank
  @Size(max = 60)
  @Schema(
      description =
          "Município sede da entidade pública, usado para identificação territorial institucional.")
  @UISchema(
      label = "Município",
      group = "Sede e contato",
      order = 60,
      icon = "location_city",
      quickViewVisible = true,
      quickViewOrder = 1)
  private String municipio;

  @Size(max = 10)
  @Schema(
      description =
          "Código postal da sede administrativa; preserva zeros à esquerda e eventual formatação.")
  @UISchema(
      label = "CEP",
      type = FieldDataType.TEXT,
      group = "Sede e contato",
      order = 70,
      icon = "mail")
  private String cep;

  @Size(max = 150)
  @Schema(description = "Logradouro, número e complemento da sede administrativa do órgão.")
  @UISchema(
      label = "Endereço",
      group = "Sede e contato",
      order = 80,
      icon = "place",
      quickViewVisible = true,
      quickViewOrder = 4)
  private String logradouro;

  @Size(max = 20)
  @Schema(
      description = "Telefone institucional principal para contato com a administração do órgão.")
  @UISchema(
      label = "Telefone",
      type = FieldDataType.TEXT,
      group = "Sede e contato",
      order = 90,
      icon = "phone")
  private String telefone;

  @Email
  @Size(max = 100)
  @Schema(
      description = "Caixa de correio eletrônico institucional para contato com o ente público.")
  @UISchema(
      label = "E-mail",
      controlType = FieldControlType.EMAIL_INPUT,
      group = "Sede e contato",
      order = 100,
      icon = "email")
  private String email;

  @Size(max = 120)
  @Schema(description = "Endereço do portal oficial do órgão na internet.")
  @UISchema(
      label = "Portal",
      controlType = FieldControlType.URL_INPUT,
      group = "Sede e contato",
      order = 110,
      icon = "public")
  private String web;
}
