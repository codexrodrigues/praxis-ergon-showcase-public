package br.com.praxis.ergonshowcase.dto;
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
@io.swagger.v3.oas.annotations.media.Schema(description = "Rascunho de novo assentamento, sem identidade, vínculo, datas, atos, efeitos contábeis nem autorização copiados. Revise e complete o contrato antes da criação.")
public class AverbacaoTempoServicoDuplicateDraftDTO extends CreateAverbacaoTempoServicoDTO {}
