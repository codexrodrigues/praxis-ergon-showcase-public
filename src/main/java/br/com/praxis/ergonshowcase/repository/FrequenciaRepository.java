package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.Frequencia;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Plano de leitura das relações necessárias à projeção de ocorrências de frequência sem N+1. */
public interface FrequenciaRepository extends BaseCrudRepository<Frequencia, Long> {
  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "documentoLegal"
      })
  Page<Frequencia> findAll(Specification<Frequencia> spec, Pageable pageable);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "documentoLegal"
      })
  List<Frequencia> findAll(Specification<Frequencia> spec, Sort sort);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "documentoLegal"
      })
  List<Frequencia> findAll(Specification<Frequencia> spec);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "documentoLegal"
      })
  List<Frequencia> findAll();

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "documentoLegal"
      })
  List<Frequencia> findAllById(Iterable<Long> ids);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "documentoLegal"
      })
  Optional<Frequencia> findById(Long id);
}
