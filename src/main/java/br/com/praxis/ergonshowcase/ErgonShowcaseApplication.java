package br.com.praxis.ergonshowcase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Host de demonstração do cadastro civil e funcional da administração pública. */
@SpringBootApplication(
    scanBasePackages = {"br.com.praxis.ergonshowcase", "org.praxisplatform.uischema"})
@io.swagger.v3.oas.annotations.OpenAPIDefinition(
    info = @io.swagger.v3.oas.annotations.info.Info(
        title = "Ergon Showcase — Administração de Pessoal", version = "0.1.0",
        description = "Cadastro institucional, civil e funcional com operações administrativas governadas."),
    servers = @io.swagger.v3.oas.annotations.servers.Server(url = "/"))
public class ErgonShowcaseApplication {
  public static void main(String[] args) {
    SpringApplication.run(ErgonShowcaseApplication.class, args);
  }
}
