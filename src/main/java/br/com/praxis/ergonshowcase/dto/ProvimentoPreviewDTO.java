package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Consulta informativa do cargo; a decisão de provimento permanece no submit transacional. */
@Schema(description = "Fatos atuais do cargo selecionado, sem reserva ou promessa de vaga.")
public record ProvimentoPreviewDTO(
    @Schema(description = "Nomenclatura oficial do cargo selecionado.", nullable = true)
        String cargoNome,
    @Schema(
            description =
                "Vagas autorizadas menos ocupantes ativos, afastados ou cedidos, contando titular e comissão uma vez por vínculo.",
            nullable = true)
        Long saldoVagasDisponiveis) {}
