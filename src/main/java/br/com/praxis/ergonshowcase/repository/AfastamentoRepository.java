package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.Afastamento;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Plano de leitura das relações necessárias à projeção de licenças e afastamentos sem N+1. */
public interface AfastamentoRepository extends BaseCrudRepository<Afastamento, Long> {
  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "afastamentoOrigem",
        "pessoaSubstituta",
        "documentoLegal",
        "atoPublicacao"
      })
  Page<Afastamento> findAll(Specification<Afastamento> spec, Pageable pageable);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "afastamentoOrigem",
        "pessoaSubstituta",
        "documentoLegal",
        "atoPublicacao"
      })
  List<Afastamento> findAll(Specification<Afastamento> spec, Sort sort);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "afastamentoOrigem",
        "pessoaSubstituta",
        "documentoLegal",
        "atoPublicacao"
      })
  List<Afastamento> findAll(Specification<Afastamento> spec);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "afastamentoOrigem",
        "pessoaSubstituta",
        "documentoLegal",
        "atoPublicacao"
      })
  List<Afastamento> findAll();

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "afastamentoOrigem",
        "pessoaSubstituta",
        "documentoLegal",
        "atoPublicacao"
      })
  List<Afastamento> findAllById(Iterable<Long> ids);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "tipoFrequenciaEntidade",
        "codigoFrequenciaEntidade",
        "afastamentoOrigem",
        "pessoaSubstituta",
        "documentoLegal",
        "atoPublicacao"
      })
  Optional<Afastamento> findById(Long id);
}
