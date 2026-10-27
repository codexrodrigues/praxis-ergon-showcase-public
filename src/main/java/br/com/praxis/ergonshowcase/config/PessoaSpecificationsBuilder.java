package br.com.praxis.ergonshowcase.config;

import br.com.praxis.ergonshowcase.domain.ServidorPessoa;
import br.com.praxis.ergonshowcase.dto.ServidorPessoaFilterDTO;
import org.praxisplatform.uischema.customfields.*;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
import org.praxisplatform.uischema.filter.specification.*;
import org.springframework.data.domain.Pageable;

/** Predicado PcD é a disjunção dos cinco indicadores reais, combinado aos filtros canônicos. */
public class PessoaSpecificationsBuilder extends GenericSpecificationsBuilder<ServidorPessoa> {
  @Override
  public <F extends GenericFilterDTO> GenericSpecification<ServidorPessoa> buildSpecification(
      F filter,
      Pageable pageable,
      CustomFieldDefinitionProvider definitions,
      ResourceTenantContextResolver tenant,
      String key) {
    var result = super.buildSpecification(filter, pageable, definitions, tenant, key);
    if (filter instanceof ServidorPessoaFilterDTO p && Boolean.TRUE.equals(p.getSomentePcD())) {
      return new GenericSpecification<>(
          result
              .spec()
              .and(
                  (root, q, cb) ->
                      cb.or(
                          cb.isTrue(root.get("deffisica")),
                          cb.isTrue(root.get("defvisual")),
                          cb.isTrue(root.get("defauditiva")),
                          cb.isTrue(root.get("defmental")),
                          cb.isTrue(root.get("defintelectual")))),
          result.pageable());
    }
    return result;
  }
}
