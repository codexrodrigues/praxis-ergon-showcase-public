package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.RadarOrganogramaChefiaOcupacao;
import br.com.praxis.ergonshowcase.dto.RadarOrganogramaChefiaOcupacaoFilterDTO;
import java.time.LocalDate;
import java.util.Optional;
import org.praxisplatform.uischema.customfields.*;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
import org.praxisplatform.uischema.filter.specification.*;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Um snapshot por data/hierarquia, com comparação de segmentos completos da árvore. */
public final class OrganogramaRadarQuery extends GenericSpecificationsBuilder<RadarOrganogramaChefiaOcupacao> {
  @Override public <F extends GenericFilterDTO> GenericSpecification<RadarOrganogramaChefiaOcupacao> buildSpecification(F f,Pageable p) {
    return buildSpecification(f,p,new NoOpCustomFieldDefinitionProvider(),Optional::empty,null);
  }
  @Override public <F extends GenericFilterDTO> GenericSpecification<RadarOrganogramaChefiaOcupacao> buildSpecification(F f,Pageable p,
      CustomFieldDefinitionProvider definitions,ResourceTenantContextResolver tenant,String key) {
    var copy=new RadarOrganogramaChefiaOcupacaoFilterDTO();
    if(f!=null)BeanUtils.copyProperties(f,copy);
    String subtreeRoot = copy.getRaizSubarvore();
    var reference = SetorFichaReference.parse(subtreeRoot);
    if (reference != null) {
      copy.setDataReferencia(reference.date());
      copy.setTipoHierarquia(reference.hierarchy());
    }
    if(copy.getDataReferencia()==null)copy.setDataReferencia(LocalDate.now());
    if(copy.getTipoHierarquia()==null)copy.setTipoHierarquia(0);
    if(copy.getTipoHierarquia()<0||copy.getTipoHierarquia()>4)throw invalid("Hierarquia deve estar entre zero e quatro.");
    String path=copy.getCaminhoSubarvore();
    if(path!=null&&!path.matches("/(?:[0-9a-f]+/)+"))throw invalid("Informe o caminho completo delimitado da subárvore.");
    boolean exact=Boolean.FALSE.equals(copy.getIncluirSubordinados());
    if(exact)copy.setCaminhoSubarvore(null);
    var result=new GenericSpecificationsBuilder<RadarOrganogramaChefiaOcupacao>().buildSpecification(copy,p,definitions,tenant,key);
    var specification = result.spec();
    if (path != null && exact) {
      specification = specification.and((root, query, cb) -> cb.equal(root.get("caminhoHierarquico"), path));
    }
    if (subtreeRoot != null) {
      if (reference == null && (subtreeRoot.isBlank() || subtreeRoot.length() > 30)) {
        throw invalid("Informe o código da unidade que inicia a subárvore.");
      }
      specification = specification.and((node, query, cb) -> {
        var branch = query.subquery(Integer.class);
        var ancestor = branch.from(RadarOrganogramaChefiaOcupacao.class);
        var date = copy.getDataReferencia();
        branch.select(cb.literal(1)).where(
            reference == null ? cb.equal(ancestor.get("setor"), subtreeRoot)
                : cb.equal(ancestor.get("periodoId"), reference.period()),
            cb.equal(ancestor.get("empCodigo"), node.get("empCodigo")),
            cb.equal(ancestor.get("tipoHierarquia"), node.get("tipoHierarquia")),
            cb.lessThanOrEqualTo(ancestor.get("dtini"), date),
            cb.or(cb.isNull(ancestor.get("dtfim")), cb.greaterThanOrEqualTo(ancestor.get("dtfim"), date)),
            cb.like(node.get("caminhoHierarquico"), cb.concat(ancestor.get("caminhoHierarquico"), "%")));
        return cb.exists(branch);
      });
    }
    return new GenericSpecification<>(specification,result.pageable());
  }
  private ResponseStatusException invalid(String message) {return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
