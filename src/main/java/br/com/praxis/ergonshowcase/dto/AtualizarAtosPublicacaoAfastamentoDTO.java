package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.dto.PartialUpdateInput;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Manutenção parcial dos atos formais de concessão, publicação oficial e vínculo de prorrogação. */
@Getter
@NoArgsConstructor
@Schema(
    description =
        "Comando de manutenção parcial da portaria de concessão, publicação em Diário Oficial e referência de prorrogação do afastamento.")
public class AtualizarAtosPublicacaoAfastamentoDTO extends PartialUpdateInput {
  @Positive
  @Schema(
      nullable = true,
      description = "Portaria de concessão, despacho decisório ou laudo pericial oficial.")
  @UISchema(
      label = "Portaria ou laudo legal",
      helpText = "Ato concessório ou laudo pericial que ampara o afastamento.",
      group = "Atos e publicação",
      order = 10,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalId;

  @Positive
  @Schema(
      nullable = true,
      description = "Ato de publicação oficial do afastamento em Diário Oficial.")
  @UISchema(
      label = "Ato de publicação oficial",
      helpText = "Publicação oficial vinculada à concessão ou homologação.",
      group = "Atos e publicação",
      order = 20,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "newspaper",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long atoPublicacaoId;

  @Schema(
      nullable = true,
      description = "Indica se este lançamento representa prorrogação de afastamento anterior.")
  @UISchema(
      label = "É prorrogação",
      helpText = "Marque quando a licença estender afastamento imediatamente anterior.",
      group = "Atos e publicação",
      order = 30,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "update")
  private Boolean prorrogacao;

  @Positive
  @Schema(
      nullable = true,
      description = "Referência ao afastamento original quando o registro configura prorrogação.")
  @UISchema(
      label = "Afastamento de origem",
      helpText = "Licença anterior prorrogada por este lançamento.",
      group = "Atos e publicação",
      order = 40,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "history",
      endpoint = ApiPaths.AFASTAMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long afastamentoOrigemId;

  @JsonSetter(value = "documentoLegalId", nulls = Nulls.SET)
  public void setDocumentoLegalId(Long v) {
    supplied("documentoLegalId");
    this.documentoLegalId = v;
  }

  @JsonSetter(value = "atoPublicacaoId", nulls = Nulls.SET)
  public void setAtoPublicacaoId(Long v) {
    supplied("atoPublicacaoId");
    this.atoPublicacaoId = v;
  }

  @JsonSetter(value = "prorrogacao", nulls = Nulls.SET)
  public void setProrrogacao(Boolean v) {
    supplied("prorrogacao");
    this.prorrogacao = v;
  }

  @JsonSetter(value = "afastamentoOrigemId", nulls = Nulls.SET)
  public void setAfastamentoOrigemId(Long v) {
    supplied("afastamentoOrigemId");
    this.afastamentoOrigemId = v;
  }
}
