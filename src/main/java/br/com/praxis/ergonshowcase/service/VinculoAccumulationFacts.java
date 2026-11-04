package br.com.praxis.ergonshowcase.service;

import static br.com.praxis.ergonshowcase.config.SecurityConfig.ACCUMULATION_CHECK;
import static br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.VINCULO_EM_EXERCICIO_OU_AFASTADO;

import br.com.praxis.ergonshowcase.domain.ServidorPessoa;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.VinculoConcomitante;
import br.com.praxis.ergonshowcase.rules.ProvaHorariosAcumulacao;
import br.com.praxis.ergonshowcase.service.VinculoScheduleEvidenceSource.Assignment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Tuple;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Consulta de finalidade específica para a decisão ERG-00300 na base local do Showcase. */
@Component
class VinculoAccumulationFacts {
  private final Set<Integer> authorizedOrganizations;
  private final Optional<VinculoScheduleEvidenceSource> schedules;

  VinculoAccumulationFacts(@Value("${showcase.security.accumulation-orgs:}") String organizations,
      Optional<VinculoScheduleEvidenceSource> schedules) {
    this.schedules = schedules;
    authorizedOrganizations = organizations.isBlank() ? Set.of() : Arrays.stream(organizations.split(",", -1))
        .map(String::trim).map(Integer::valueOf).collect(Collectors.toUnmodifiableSet());
    if (authorizedOrganizations.stream().anyMatch(org -> org <= 0)) {
      throw new IllegalArgumentException("Órgãos autorizados para acumulação devem ser positivos.");
    }
  }

  /** Ausência de autorização ou de fonte habilitada produz contexto inconclusivo. */
  List<VinculoConcomitante> load(EntityManager em, Vinculo draft) {
    if (draft.getSituacao() == null || !VINCULO_EM_EXERCICIO_OU_AFASTADO.contains(draft.getSituacao())) {
      return List.of();
    }
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (draft.getEmpCodigo() == null || !authorizedOrganizations.contains(draft.getEmpCodigo())
        || authentication == null || !authentication.isAuthenticated()
        || authentication.getAuthorities().stream().noneMatch(role -> ACCUMULATION_CHECK.equals(role.getAuthority()))
        || em == null || !em.isJoinedToTransaction()
        || draft.getId() == null || draft.getId().numfunc() == null) {
      return null;
    }
    var id = draft.getId();
    // O titular é a chave comum aos provimentos de todos os órgãos da base.
    if (em.find(ServidorPessoa.class, id.numfunc(), LockModeType.PESSIMISTIC_WRITE) == null) return null;
    return em.createQuery("""
            select coalesce(v.dtexerc, v.dtposse, v.dtnom), v.dtvac, v.categoria, v.empCodigo, v.id.numero
            from Vinculo v
            where v.id.numfunc = :titular and (:numero is null or v.id.numero <> :numero)
              and v.situacao in :situacoes
            """, Tuple.class)
        .setParameter("titular", id.numfunc())
        .setParameter("numero", id.numero())
        .setParameter("situacoes", VINCULO_EM_EXERCICIO_OU_AFASTADO)
        .setFlushMode(FlushModeType.COMMIT)
        .getResultList().stream()
        .map(row -> {
          var inicio = row.get(0, LocalDate.class);
          var fim = row.get(1, LocalDate.class);
          return new VinculoConcomitante(inicio, fim, row.get(2, String.class),
              loadSchedules(draft, inicio, fim, row.get(3, Integer.class), row.get(4, Integer.class)));
        })
        .toList();
  }

  private ProvaHorariosAcumulacao loadSchedules(Vinculo draft, LocalDate inicio, LocalDate fim,
      Integer orgao, Integer numero) {
    if (schedules.isEmpty() || orgao == null || !authorizedOrganizations.contains(orgao)
        || inicio == null || numero == null || draft.getId().numero() == null) return null;
    var draftStart = draft.getDtexerc() != null ? draft.getDtexerc()
        : draft.getDtposse() != null ? draft.getDtposse() : draft.getDtnom();
    if (draftStart == null) return null;
    var draftEnd = draft.getDtvac() == null ? LocalDate.MAX : draft.getDtvac();
    var existingEnd = fim == null ? LocalDate.MAX : fim;
    var from = draftStart.isAfter(inicio) ? draftStart : inicio;
    var to = draftEnd.isBefore(existingEnd) ? draftEnd : existingEnd;
    if (from.isAfter(to)) return null;
    return schedules.get().load(new Assignment(draft.getEmpCodigo(), draft.getId()),
        new Assignment(orgao, new VinculoId(draft.getId().numfunc(), numero)), from, to).orElse(null);
  }
}
