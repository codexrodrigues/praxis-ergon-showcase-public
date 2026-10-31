package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.FeriasGozo;
import java.util.List;
import java.util.Optional;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Plano de leitura das relações necessárias à projeção de parcelas de gozo de férias sem N+1. */
public interface FeriasGozoRepository extends BaseCrudRepository<FeriasGozo, Long> {
  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "periodoAquisitivo",
        "pessoaSubstituta",
        "documentoLegal",
        "atoPublicacao"
      })
  Page<FeriasGozo> findAll(Specification<FeriasGozo> spec, Pageable pageable);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "periodoAquisitivo",
        "pessoaSubstituta",
        "documentoLegal",
        "atoPublicacao"
      })
  List<FeriasGozo> findAll(Specification<FeriasGozo> spec, Sort sort);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "periodoAquisitivo",
        "pessoaSubstituta",
        "documentoLegal",
        "atoPublicacao"
      })
  List<FeriasGozo> findAll(Specification<FeriasGozo> spec);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "periodoAquisitivo",
        "pessoaSubstituta",
        "documentoLegal",
        "atoPublicacao"
      })
  List<FeriasGozo> findAll();

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "periodoAquisitivo",
        "pessoaSubstituta",
        "documentoLegal",
        "atoPublicacao"
      })
  List<FeriasGozo> findAllById(Iterable<Long> ids);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "vinculo",
        "periodoAquisitivo",
        "pessoaSubstituta",
        "documentoLegal",
        "atoPublicacao"
      })
  Optional<FeriasGozo> findById(Long id);
}
