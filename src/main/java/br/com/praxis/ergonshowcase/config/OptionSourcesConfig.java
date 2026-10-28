package br.com.praxis.ergonshowcase.config;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import java.util.*;
import org.praxisplatform.uischema.options.*;
import org.springframework.context.annotation.*;

/** Fontes governadas de seleção de identidades de órgão, cargo, pessoa e vínculo. */
@Configuration
public class OptionSourcesConfig {
  @Bean
  OptionSourceRegistry options() {
    return OptionSourceRegistry.builder()
        .add(
            Orgao.class,
            source("orgao", ApiPaths.ORGAOS, "empresa", "nome", "administracao-pessoal.empresas"))
        .add(
            Cargo.class,
            source("cargo", ApiPaths.CARGOS, "cargo", "nome", "administracao-pessoal.cargos"))
        .add(
            ServidorPessoa.class,
            source(
                "pessoa", ApiPaths.PESSOAS, "numero", "nome", "administracao-pessoal.funcionarios"))
        .add(
            Vinculo.class,
            source("vinculo", ApiPaths.VINCULOS, "id", "matric", "administracao-pessoal.vinculos"))
        .add(
            Setor.class,
            source("setor", "/api/ergon/setores", "setor", "nome", "administracao-pessoal.setores"))
        .add(
            DocumentoLegal.class,
            source(
                "documento",
                "/api/ergon/documentos-legais",
                "id",
                "numeroDocumento",
                "administracao-pessoal.documentos-legais"))
        .build();
  }

  private OptionSourceDescriptor source(
      String key, String path, String id, String label, String entityKey) {
    var lookup =
        new EntityLookupDescriptor(
            entityKey,
            null,
            List.of(),
            null,
            null,
            null,
            List.of(label),
            Map.of(),
            null,
            new LookupCapabilities(
                true, true, false, false, false, false, true, false, false, false),
            null);
    return new OptionSourceDescriptor(
        key,
        OptionSourceType.RESOURCE_ENTITY,
        path,
        null,
        null,
        label,
        id,
        List.of(),
        Map.of(),
        OptionSourcePolicy.defaults(),
        lookup);
  }
}
