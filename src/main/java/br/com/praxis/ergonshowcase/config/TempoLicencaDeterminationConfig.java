package br.com.praxis.ergonshowcase.config;

import java.util.List;
import org.praxisplatform.uischema.determination.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Prévias não persistentes para criação e edição dos assentamentos de tempo e licença. */
@Configuration
public class TempoLicencaDeterminationConfig {
  @Bean
  public ReactiveDeterminationDefinitionProvider tempoLicencaDeterminations() {
    return () -> List.of(
        definition("tempo-licenca.averbacao.preview", "AverbacaoTempoServico",
            "determineAverbacaoTempoServico", "/totalDias", "/diasDesaverbados"),
        definition("tempo-licenca.periodo.preview", "PeriodoAquisitivoLicencaEspecial",
            "determinePeriodoAquisitivoLicencaEspecial", "/diasDireito", "/diasConvertidosDobro"),
        definition("tempo-licenca.gozo.preview", "LicencaEspecialGozo",
            "determineLicencaEspecialGozo", "/dtini", "/dtprevfim"),
        definition("tempo-licenca.venda.preview", "VendaLicencaEspecial",
            "determineVendaLicencaEspecial", "/periodoAquisitivoLicencaId", "/diasVendidos",
            new ReactiveDeterminationInputBinding("/id", "/vendaId")));
  }

  private ReactiveDeterminationDefinition definition(String id, String resource, String operation,
      String first, String second, ReactiveDeterminationInputBinding... context) {
    var builder = ReactiveDeterminationDefinition.forCrudResource(id, "createErgon" + resource,
        "updateErgon" + resource, operation)
        .triggerMode(ReactiveDeterminationTriggerMode.ON_INIT_AND_CHANGE)
        .source(first).source(second).input(first, first).input(second, second)
        .output("/diasPrevistos", "/diasPrevistos")
        .provenance(new ReactiveDeterminationProvenance(ReactiveDeterminationProvenanceKind.HOST,
            "tempo-licenca-preview", "2"));
    for (var binding : context) builder.input(binding.fieldPath(), binding.requestPath());
    return builder.build();
  }
}
