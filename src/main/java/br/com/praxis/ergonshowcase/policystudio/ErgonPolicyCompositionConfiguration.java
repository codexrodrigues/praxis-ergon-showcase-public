package br.com.praxis.ergonshowcase.policystudio;

import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import java.util.List;
import org.praxisplatform.config.service.DomainRuleImplementationCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ErgonPolicyCompositionConfiguration {
  @Bean
  DomainRuleImplementationCatalog ergonRuleImplementationCatalog(ErgonRuleEngineFacade rules) {
    var implementations = rules.governedImplementations();
    return scope -> ErgonPolicyComposition.OWNER.equals(scope.ownerServiceKey())
        ? implementations : List.of();
  }
}
