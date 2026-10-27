package br.com.praxis.ergonshowcase.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.praxisplatform.uischema.id.CompositeIdJacksonModule;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/** Usa os defaults e propriedades Jackson do Boot, mantendo o módulo canônico de IDs compostos. */
@Configuration
public class JsonConfig {
  @Bean
  @Primary
  public ObjectMapper ergonObjectMapper(Jackson2ObjectMapperBuilder builder) {
    return builder.modulesToInstall(new CompositeIdJacksonModule()).build();
  }
}
