package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.HistoricoSetor;
import br.com.praxis.ergonshowcase.dto.HistoricoSetorFilterDTO;
import br.com.praxis.ergonshowcase.dto.HistoricoSetorResponseDTO;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.*;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
import org.praxisplatform.uischema.filter.specification.*;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Travessia do grafo do host; os predicados escalares continuam no construtor canônico. */
public final class OrganogramaTemporalQuery extends GenericSpecificationsBuilder<HistoricoSetor> {
  private final EntityManager em;
  private final TenantContext tenant;
  public OrganogramaTemporalQuery(EntityManager em, TenantContext tenant) {this.em=em;this.tenant=tenant;}

  public record Node(long period, String code, String parent, String path, int level, long children) {}

  @SuppressWarnings("unchecked")
  public Map<Long,Node> nodes(HistoricoSetorFilterDTO filter) {
    int hierarchy=filter.getTipoHierarquia()==null?0:filter.getTipoHierarquia();
    if(hierarchy<0||hierarchy>4) throw invalid("Hierarquia deve estar entre zero e quatro.");
    String path=filter.getCaminhoSubarvore();
    if(path!=null&&!path.matches("/(?:[0-9a-f]+/)+")) throw invalid("Informe o caminho completo delimitado da subárvore.");
    LocalDate date=filter.getDataReferencia()==null?LocalDate.now():filter.getDataReferencia();
    List<Object[]> rows=em.createNativeQuery("select periodo_id,setor,pai,caminho,nivel,filhos from organograma_em(:org,:data,:hier)")
        .setParameter("org",tenant.orgao()).setParameter("data",date).setParameter("hier",hierarchy).getResultList();
    Map<Long,Node> result=new LinkedHashMap<>();
    for(Object[] row:rows) {
      Node n=new Node(((Number)row[0]).longValue(),(String)row[1],(String)row[2],(String)row[3],((Number)row[4]).intValue(),((Number)row[5]).longValue());
      if(path!=null && !(Boolean.FALSE.equals(filter.getIncluirSubordinados())?n.path().equals(path):n.path().startsWith(path)))continue;
      if(filter.getNivelHierarquico()!=null && n.level()<filter.getNivelHierarquico())continue;
      if(filter.getSetorPai()!=null && !filter.getSetorPai().equals(n.parent()))continue;
      result.put(n.period(),n);
    }
    return result;
  }

  public boolean temporal(HistoricoSetorFilterDTO f) {
    return f!=null&&(f.getDataReferencia()!=null||f.getTipoHierarquia()!=null||f.getCaminhoSubarvore()!=null||f.getNivelHierarquico()!=null);
  }

  @Override public <F extends GenericFilterDTO> GenericSpecification<HistoricoSetor> buildSpecification(F filter,Pageable pageable) {
    return buildSpecification(filter,pageable,new org.praxisplatform.uischema.customfields.NoOpCustomFieldDefinitionProvider(),java.util.Optional::empty,null);
  }

  @Override public <F extends GenericFilterDTO> GenericSpecification<HistoricoSetor> buildSpecification(F filter,Pageable pageable,
      org.praxisplatform.uischema.customfields.CustomFieldDefinitionProvider definitions,
      org.praxisplatform.uischema.customfields.ResourceTenantContextResolver context,String key) {
    var delegate=new GenericSpecificationsBuilder<HistoricoSetor>();
    if(!(filter instanceof HistoricoSetorFilterDTO f)||!temporal(f))return delegate.buildSpecification(filter,pageable,definitions,context,key);
    var copy=new HistoricoSetorFilterDTO();BeanUtils.copyProperties(f,copy);
    copy.setCaminhoSubarvore(null);copy.setNivelHierarquico(null);copy.setSetorPai(null);
    if(copy.getDataReferencia()==null)copy.setDataReferencia(LocalDate.now());
    var canonical=delegate.buildSpecification(copy,pageable,definitions,context,key);
    Set<Long> ids=nodes(f).keySet();
    return new GenericSpecification<>(canonical.spec().and((root,query,cb)->ids.isEmpty()?cb.disjunction():root.get("id").in(ids)),canonical.pageable());
  }

  public void enrich(List<HistoricoSetorResponseDTO> responses,HistoricoSetorFilterDTO filter) {
    if(responses.isEmpty())return;
    if(!temporal(filter)) {
      responses.stream().collect(java.util.stream.Collectors.groupingBy(HistoricoSetorResponseDTO::getDtini)).forEach((date,group)->{
        var atStart=new HistoricoSetorFilterDTO();atStart.setDataReferencia(date);enrich(group,atStart);
      });return;
    }
    var shape=new HistoricoSetorFilterDTO();shape.setDataReferencia(filter.getDataReferencia());shape.setTipoHierarquia(filter.getTipoHierarquia());
    var nodes=nodes(shape);
    for(var dto:responses) {
      Node n=nodes.get(dto.getId());
      if(n==null) {dto.setCaminhoHierarquico(null);dto.setNivelHierarquico(null);dto.setQuantidadeSubordinadosDiretos(null);dto.setDiagnosticoHierarquia("CICLO_OU_SUPERIOR_AUSENTE");continue;}
      dto.setSetorPai(n.parent());dto.setCaminhoHierarquico(n.path());dto.setNivelHierarquico(n.level());
      dto.setQuantidadeSubordinadosDiretos(Math.toIntExact(n.children()));
      dto.setDiagnosticoHierarquia("INTEGRA");
    }
  }
  private ResponseStatusException invalid(String message) {return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
