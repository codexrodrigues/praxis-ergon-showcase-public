package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Manutenção especializada; propriedades ausentes preservam o valor persistido. */
@Getter
@Setter
public class AlterarLotacaoExercicioDTO {
  @Schema(description = "Código da unidade de lotação do vínculo no mesmo órgão.")
  @UISchema(label = "Lotação", group = "Enquadramento e lotação")
  private String setor;

  @Schema(description = "Unidade de efetivo exercício do vínculo no mesmo órgão.")
  @UISchema(label = "Exercício", group = "Enquadramento e lotação")
  private String setorExercicio;

  @Schema(description = "Cargo em comissão ocupado no mesmo órgão do vínculo.")
  @UISchema(label = "Cargo em comissão", group = "Enquadramento e lotação")
  private Integer cargoComissao;

  @Schema(description = "Quantidade de horas semanais de trabalho do vínculo.")
  @UISchema(label = "Jornada semanal", group = "Enquadramento e lotação")
  private Integer jornadaSemanal;

  @Schema(description = "Documento legal que fundamenta o assentamento funcional.")
  @UISchema(label = "Documento legal", group = "Enquadramento e lotação")
  private Long documentoLegalId;
}
