package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.rules.ProvaHorariosAcumulacao;
import java.time.LocalDate;
import java.util.Optional;

/** Fonte institucional de escalas efetivas para a finalidade restrita da ERG-00300. */
public interface VinculoScheduleEvidenceSource {
  record Assignment(int orgao, VinculoId vinculo) {}

  /**
   * Resolve os dois vínculos em contextos de empresa independentes. A cobertura declarada
   * deve refletir escala e exceções efetivas por dia, inclusive folgas confirmadas.
   * Cobertura parcial pode demonstrar um conflito, mas não comprova compatibilidade.
   * `LocalDate.MAX` indica concomitância sem termo. A fonte só deve consultar um
   * período finito para o qual possa fornecer evidência.
   */
  Optional<ProvaHorariosAcumulacao> load(Assignment candidato, Assignment concomitante,
      LocalDate inicioConcomitancia, LocalDate fimConcomitancia);
}
