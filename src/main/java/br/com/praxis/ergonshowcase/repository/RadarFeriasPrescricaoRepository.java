package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.RadarFeriasPrescricao;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

/** Repositório somente-leitura para a projeção analítica executiva de prescrição e saldo de férias. */
public interface RadarFeriasPrescricaoRepository
    extends BaseCrudRepository<RadarFeriasPrescricao, Long> {}
