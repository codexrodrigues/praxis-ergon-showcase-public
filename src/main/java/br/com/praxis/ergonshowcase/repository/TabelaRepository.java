package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.Tabela;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

/** Consulta do catálogo mestre de tabelas gerais de domínio do Ergon/Hades (HADES.TABELA). */
public interface TabelaRepository extends BaseCrudRepository<Tabela, String> {}
