package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Plano de leitura das relações necessárias à projeção de regras estatutárias de frequência sem N+1. */
public interface RegraFrequenciaEstatutariaRepository
    extends BaseCrudRepository<RegraFrequenciaEstatutaria, Long> {
  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "documentoLegal"
      })
  Page<RegraFrequenciaEstatutaria> findAll(
      Specification<RegraFrequenciaEstatutaria> spec, Pageable pageable);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "documentoLegal"
      })
  List<RegraFrequenciaEstatutaria> findAll(
      Specification<RegraFrequenciaEstatutaria> spec, Sort sort);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "documentoLegal"
      })
  List<RegraFrequenciaEstatutaria> findAll(Specification<RegraFrequenciaEstatutaria> spec);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "documentoLegal"
      })
  List<RegraFrequenciaEstatutaria> findAll();

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "documentoLegal"
      })
  List<RegraFrequenciaEstatutaria> findAllById(Iterable<Long> ids);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "documentoLegal"
      })
  Optional<RegraFrequenciaEstatutaria> findById(Long id);

  /** Consulta candidatos do mesmo escopo funcional para validação de concomitância da RN-013 (ERG-08393). */
  List<RegraFrequenciaEstatutaria> findByEmpCodigoAndRegimeJuridicoAndTipofreqAndCodfreq(
      Integer empCodigo, String regimeJuridico, String tipofreq, Integer codfreq);

  /**
   * Consulta candidatos pela dupla obrigatória {@code (tipofreq, codfreq)} quando {@code empCodigo}
   * ou {@code regimeJuridico} for {@code null} (regra geral do ente público em {@code PCK_REGRAS_FREQ.MAIN_POS}).
   */
  List<RegraFrequenciaEstatutaria> findByTipofreqAndCodfreq(String tipofreq, Integer codfreq);
}
