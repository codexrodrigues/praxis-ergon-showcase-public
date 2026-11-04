package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.HistoricoSetor;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.praxisplatform.uischema.customfields.CustomFieldDefinitionProvider;
import org.praxisplatform.uischema.customfields.ResourceTenantContextResolver;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
import org.praxisplatform.uischema.filter.specification.GenericSpecification;
import org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Identidade da ficha e predicado de relacionamento para os filtros enviados pelo outlet 360°. */
record SetorFichaReference(long period, LocalDate date, int hierarchy) {
  // O prefixo excede o limite do código físico de setor.
  private static final String PREFIX = "periodo-organizacional-em-vigencia:";

  String encode() {
    return PREFIX + period + ":" + date + ":" + hierarchy;
  }

  static SetorFichaReference parse(String value) {
    if (value == null || !value.startsWith(PREFIX)) {
      return null;
    }
    String[] parts = value.substring(PREFIX.length()).split(":", -1);
    if (parts.length == 3 && parts[2].matches("[0-4]")) {
      try {
        return new SetorFichaReference(Long.parseLong(parts[0]), LocalDate.parse(parts[1]), Integer.parseInt(parts[2]));
      } catch (NumberFormatException | DateTimeParseException ignored) {
        // A referência malformada recebe o mesmo resultado de uma ficha indisponível.
      }
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Referência da unidade indisponível.");
  }

  <E> Specification<E> matchesSector(String sectorField) {
    return (child, query, cb) -> {
      var periodQuery = query.subquery(Integer.class);
      var history = periodQuery.from(HistoricoSetor.class);
      periodQuery.select(cb.literal(1)).where(
          cb.equal(history.get("id"), period),
          cb.equal(history.get("empCodigo"), child.get("empCodigo")),
          cb.equal(history.get("setor"), child.get(sectorField)),
          cb.lessThanOrEqualTo(history.get("dtini"), date),
          cb.or(cb.isNull(history.get("dtfim")), cb.greaterThanOrEqualTo(history.get("dtfim"), date)));
      return cb.exists(periodQuery);
    };
  }

  static <E, F extends GenericFilterDTO> GenericSpecificationsBuilder<E> relatedFilter(
      Class<F> type, Supplier<F> factory, Function<F, String> parentValue,
      BiConsumer<F, String> bindParent, String sectorField) {
    return new GenericSpecificationsBuilder<>() {
      @Override
      public <Q extends GenericFilterDTO> GenericSpecification<E> buildSpecification(Q filter, Pageable pageable,
          CustomFieldDefinitionProvider definitions, ResourceTenantContextResolver tenant, String key) {
        if (!type.isInstance(filter)) {
          return super.buildSpecification(filter, pageable, definitions, tenant, key);
        }
        F typed = type.cast(filter);
        var reference = parse(parentValue.apply(typed));
        if (reference == null) {
          return super.buildSpecification(filter, pageable, definitions, tenant, key);
        }
        F copy = factory.get();
        BeanUtils.copyProperties(typed, copy);
        bindParent.accept(copy, null);
        var ordinary = super.buildSpecification(copy, pageable, definitions, tenant, key);
        return new GenericSpecification<>(ordinary.spec().and(reference.matchesSector(sectorField)), ordinary.pageable());
      }
    };
  }
}
