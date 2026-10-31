package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.ItemTabela;
import br.com.praxis.ergonshowcase.domain.ItemTabelaId;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Consulta dos itens das tabelas gerais de domínio (HADES.ITEMTABELA_) sem N+1. */
public interface ItemTabelaRepository extends BaseCrudRepository<ItemTabela, ItemTabelaId> {
  @Override
  @EntityGraph(attributePaths = {"tabelaEntidade", "orgao"})
  Page<ItemTabela> findAll(Specification<ItemTabela> spec, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"tabelaEntidade", "orgao"})
  List<ItemTabela> findAll(Specification<ItemTabela> spec, Sort sort);

  @Override
  @EntityGraph(attributePaths = {"tabelaEntidade", "orgao"})
  List<ItemTabela> findAll(Specification<ItemTabela> spec);

  @Override
  @EntityGraph(attributePaths = {"tabelaEntidade", "orgao"})
  List<ItemTabela> findAll();

  @Override
  @EntityGraph(attributePaths = {"tabelaEntidade", "orgao"})
  List<ItemTabela> findAllById(Iterable<ItemTabelaId> ids);

  @Override
  @EntityGraph(attributePaths = {"tabelaEntidade", "orgao"})
  Optional<ItemTabela> findById(ItemTabelaId id);
}
