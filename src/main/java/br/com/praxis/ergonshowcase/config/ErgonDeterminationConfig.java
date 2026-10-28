package br.com.praxis.ergonshowcase.config;

import java.util.*;
import org.praxisplatform.uischema.determination.*;
import org.springframework.context.annotation.*;

/** Bindings estáticos sem tenant, URLs ou decisões aplicadas no schema cacheável. */
@Configuration
public class ErgonDeterminationConfig {
  @Bean
  ReactiveDeterminationDefinitionProvider ergonDeterminations() {
    return () ->
        List.of(
            definition(
                "ergon.pessoa.pcd",
                "ServidorPessoa",
                "determineErgonPcd",
                List.of("deffisica", "defvisual", "defauditiva", "defmental", "defintelectual"),
                List.of("deffisica", "defvisual", "defauditiva", "defmental", "defintelectual"),
                List.of(
                    new ReactiveDeterminationOutputBinding(
                        "/possuiDeficienciaPcD", "/possuiDeficienciaPcDPrevisto"))),
            definition(
                "ergon.vinculo.provimento",
                "Vinculo",
                "determineErgonProvimento",
                List.of("cargo"),
                List.of("cargo"),
                List.of(
                    new ReactiveDeterminationOutputBinding("/cargoNome", "/cargoNomePrevisto"),
                    new ReactiveDeterminationOutputBinding(
                        "/saldoVagasDisponiveis", "/saldoVagasPrevisto"))),
            definition(
                "ergon.cargo.aglutinador",
                "Cargo",
                "determineErgonAglutinador",
                List.of("cargoAglutinador"),
                List.of("cargo"),
                List.of(
                    new ReactiveDeterminationOutputBinding(
                        "/nome", "/cargoAglutinadorNomePrevisto"))));
  }

  private ReactiveDeterminationDefinition definition(
      String id,
      String resource,
      String operation,
      List<String> source,
      List<String> request,
      List<ReactiveDeterminationOutputBinding> outputs) {
    var inputs = new ArrayList<ReactiveDeterminationInputBinding>();
    for (int i = 0; i < source.size(); i++)
      inputs.add(new ReactiveDeterminationInputBinding("/" + source.get(i), "/" + request.get(i)));
    return new ReactiveDeterminationDefinition(
        id,
        List.of(
            new ReactiveDeterminationScope(
                "createErgon" + resource, ReactiveDeterminationFormMode.CREATE),
            new ReactiveDeterminationScope(
                "updateErgon" + resource, ReactiveDeterminationFormMode.EDIT)),
        operation,
        ReactiveDeterminationTriggerMode.ON_CHANGE,
        source.stream().map(v -> "/" + v).toList(),
        inputs,
        outputs,
        new ReactiveDeterminationProvenance(
            ReactiveDeterminationProvenanceKind.HOST, "ergon-derived-facts", "1"));
  }
}
