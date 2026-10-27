package br.com.praxis.ergonshowcase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Host de demonstração do cadastro civil e funcional da administração pública. */
@SpringBootApplication(
    scanBasePackages = {"br.com.praxis.ergonshowcase", "org.praxisplatform.uischema"})
public class ErgonShowcaseApplication {
  public static void main(String[] args) {
    SpringApplication.run(ErgonShowcaseApplication.class, args);
  }
}
