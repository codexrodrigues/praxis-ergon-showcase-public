package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.dto.PartialUpdateInput;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/**
 * Manutenção parcial dos dados de certidão civil e cartório de registro do dependente; propriedades
 * ausentes preservam o valor persistido.
 */
@Getter
@Schema(
    description =
        "Manutenção parcial dos dados de certidão civil e cartório de registro do dependente.")
public class AtualizarCertidaoDependenteDTO extends PartialUpdateInput {
  @Size(max = 30)
  @Schema(
      nullable = true,
      description =
          "Espécie da certidão de registro civil apresentada (ex.: NASCIMENTO, CASAMENTO, GUARDA JUDICIAL).")
  @UISchema(label = "Tipo de certidão", group = "Registro civil", order = 1, icon = "article")
  private String tipodocCert;

  @Size(max = 32)
  @Schema(
      nullable = true,
      description = "Número do termo da certidão civil lavrada em cartório.")
  @UISchema(label = "Termo da certidão", group = "Registro civil", order = 2, icon = "tag")
  private String numCert;

  @Size(max = 15)
  @Schema(
      nullable = true,
      description = "Número ou código do livro de registro da certidão civil.")
  @UISchema(label = "Livro da certidão", group = "Registro civil", order = 3, icon = "menu_book")
  private String livroACert;

  @Size(max = 15)
  @Schema(
      nullable = true,
      description = "Número da folha do livro de registro da certidão civil.")
  @UISchema(label = "Folha da certidão", group = "Registro civil", order = 4, icon = "description")
  private String folhaCert;

  @Size(max = 100)
  @Schema(
      nullable = true,
      description = "Denominação oficial do cartório de registro civil emissor da certidão.")
  @UISchema(
      label = "Cartório de registro",
      group = "Registro civil",
      order = 5,
      icon = "assured_workload")
  private String cartorioCert;

  @Size(max = 2)
  @Schema(
      nullable = true,
      description = "Sigla da Unidade da Federação do cartório de registro civil.")
  @UISchema(label = "UF do cartório", group = "Registro civil", order = 6, icon = "map")
  private String ufCart;

  @Size(max = 60)
  @Schema(
      nullable = true,
      description = "Município de localização do cartório de registro civil.")
  @UISchema(
      label = "Município do cartório",
      group = "Registro civil",
      order = 7,
      icon = "location_city")
  private String municipioCart;

  @Size(max = 40)
  @Schema(
      nullable = true,
      description = "Matrícula nacional unificada de 32 dígitos da certidão de registro civil.")
  @UISchema(label = "Matrícula da certidão", group = "Registro civil", order = 8, icon = "pin")
  private String matriculaCert;

  @Positive
  @Schema(
      nullable = true,
      description = "Documento digitalizado ou ato legal comprobatório da certidão.")
  @UISchema(
      label = "Documento comprobatório",
      group = "Registro civil",
      order = 9,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalId;

  @JsonSetter(value = "tipodocCert", nulls = Nulls.SET)
  public void setTipodocCert(String value) {
    supplied("tipodocCert");
    this.tipodocCert = value;
  }

  @JsonSetter(value = "numCert", nulls = Nulls.SET)
  public void setNumCert(String value) {
    supplied("numCert");
    this.numCert = value;
  }

  @JsonSetter(value = "livroACert", nulls = Nulls.SET)
  public void setLivroACert(String value) {
    supplied("livroACert");
    this.livroACert = value;
  }

  @JsonSetter(value = "folhaCert", nulls = Nulls.SET)
  public void setFolhaCert(String value) {
    supplied("folhaCert");
    this.folhaCert = value;
  }

  @JsonSetter(value = "cartorioCert", nulls = Nulls.SET)
  public void setCartorioCert(String value) {
    supplied("cartorioCert");
    this.cartorioCert = value;
  }

  @JsonSetter(value = "ufCart", nulls = Nulls.SET)
  public void setUfCart(String value) {
    supplied("ufCart");
    this.ufCart = value;
  }

  @JsonSetter(value = "municipioCart", nulls = Nulls.SET)
  public void setMunicipioCart(String value) {
    supplied("municipioCart");
    this.municipioCart = value;
  }

  @JsonSetter(value = "matriculaCert", nulls = Nulls.SET)
  public void setMatriculaCert(String value) {
    supplied("matriculaCert");
    this.matriculaCert = value;
  }

  @JsonSetter(value = "documentoLegalId", nulls = Nulls.SET)
  public void setDocumentoLegalId(Long value) {
    supplied("documentoLegalId");
    this.documentoLegalId = value;
  }
}
