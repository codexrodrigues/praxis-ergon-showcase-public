package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/** Contratos das prévias não persistentes do cadastro de chefias. */
public final class ChefiaDeterminationDTO {
  private ChefiaDeterminationDTO() {}

  public record FuncaoDraft(
      @Schema(description = "Primeiro dia de vigência cadastral proposto.") LocalDate dtini,
      @Schema(description = "Último dia proposto, quando a vigência tem termo.") LocalDate dtfim) {}

  public record FuncaoPreview(
      @Schema(description = "Duração civil inclusiva da vigência delimitada.") Integer diasVigenciaPrevistos) {}

  public record VagaDraft(
      @Schema(description = "Função que fundamenta a posição numerada.") @Positive Long funcaoId) {}

  public record VagaPreview(
      @Schema(description = "Indica se a função está sujeita a controle de vagas.") Boolean controleVagaPrevisto) {}

  public record DesignacaoDraft(
      @Schema(description = "Posição numerada indicada no rascunho do ato.") @Positive Long vagaFuncaoId) {}

  public record DesignacaoPreview(
      @Schema(description = "Situação cadastrada da posição na consulta. A prévia não reserva a vaga.")
      String situacaoVagaPrevista) {}

  public record SubstituicaoDraft(
      @Schema(description = "Ato de designação do titular impedido.") @Positive Long designacaoId,
      @Schema(description = "Primeiro dia de exercício interino proposto.") LocalDate dtini,
      @Schema(description = "Último dia proposto para o exercício interino.") LocalDate dtfim) {}

  public record SubstituicaoPreview(
      @Schema(description = "Duração civil inclusiva do exercício interino proposto.") Integer diasSubstituicaoPrevistos,
      @Schema(description = "Mínimo cadastrado na função para remuneração de substituição.")
      Integer minimoRemuneradoPrevisto) {}
}
