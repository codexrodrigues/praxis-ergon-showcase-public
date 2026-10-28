package br.com.praxis.ergonshowcase.bulk;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.service.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.*;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.*;
import org.praxisplatform.uischema.bulk.*;
import org.praxisplatform.uischema.id.CompositeIdSupport;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Adapter do domínio: candidato completo e validação sem modificar entidades gerenciadas. */
@Component
@SuppressWarnings({"rawtypes", "unchecked"})
public class ErgonBulkDomain {
  public enum Resource {
    VINCULOS("administracao-pessoal.vinculos", "vinculos", "updateErgonVinculo", Vinculo.class, UpdateVinculoDTO.class),
    PESSOAS("administracao-pessoal.funcionarios", "funcionarios", "updateErgonServidorPessoa", ServidorPessoa.class, UpdateServidorPessoaDTO.class),
    CARGOS("administracao-pessoal.cargos", "cargos", "updateErgonCargo", Cargo.class, UpdateCargoDTO.class);
    public final String key, name, updateOperation;
    public final Class<?> entity, update;
    Resource(String key, String name, String op, Class<?> entity, Class<?> update) { this.key=key;this.name=name;this.updateOperation=op;this.entity=entity;this.update=update; }
    public Set<String> protectedFields() {
      return switch(this) {
        case VINCULOS -> Set.of("cargo", "dtnom", "dtposse");
        case PESSOAS -> Set.of("cpf", "nome", "dtnasc");
        case CARGOS -> Set.of();
      };
    }
  }
  @PersistenceContext EntityManager em;
  @Autowired ObjectMapper json;
  @Autowired Validator validator;
  @Autowired TenantContext tenant;
  @Autowired ErgonInvariants invariants;
  @Autowired VinculoMapper vinculos;
  @Autowired ServidorPessoaMapper pessoas;
  @Autowired CargoMapper cargos;
  @Autowired VinculoService vinculoService;
  @Autowired ServidorPessoaService pessoaService;
  @Autowired CargoService cargoService;

  ResourceMapper mapper(Resource r) { return switch(r) { case VINCULOS -> vinculos; case PESSOAS -> pessoas; case CARGOS -> cargos; }; }
  public BulkIdentityCodec codec(Resource r) { return switch(r) { case CARGOS -> BulkIdentityCodecs.integers(); case PESSOAS -> BulkIdentityCodecs.longs(); case VINCULOS -> BulkIdentityCodecs.strings(); }; }
  public Object identity(Resource r, Object wire) {
    return switch(r) {
      case CARGOS -> BulkIdentityCodecs.integers().decode((Integer) wire);
      case PESSOAS -> BulkIdentityCodecs.longs().decode((String) wire);
      case VINCULOS -> {
        VinculoId id=CompositeIdSupport.decode((String)wire,VinculoId.class);
        if(!wire.equals(CompositeIdSupport.encode(id))) throw new IllegalArgumentException("Identidade composta não canônica.");
        yield id;
      }
    };
  }
  public Object wire(Resource r, Object e) {
    return switch(r) { case CARGOS -> ((Cargo)e).getCargo(); case PESSOAS -> ((ServidorPessoa)e).getNumero().toString(); case VINCULOS -> CompositeIdSupport.encode(((Vinculo)e).getId()); };
  }
  public long version(Object e) {
    return e instanceof Vinculo v ? v.getVersion() : e instanceof Cargo c ? c.getVersion() : ((ServidorPessoa)e).getVersion();
  }
  public Object load(Resource r, Object wire, boolean lock) {
    if (em.unwrap(org.hibernate.Session.class).isDirty()) throw new IllegalStateException("Contexto de persistência deve estar limpo antes do lote.");
    Object e = lock ? em.find(r.entity, identity(r,wire), LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout",1000)) : em.find(r.entity,identity(r,wire));
    if (e != null && !scope(r,e)) throw new SecurityException("Registro fora do órgão selecionado.");
    return e;
  }
  public boolean scope(Resource r, Object e) {
    return r == Resource.PESSOAS || Objects.equals(r == Resource.CARGOS ? ((Cargo)e).getEmpCodigo() : ((Vinculo)e).getEmpCodigo(), tenant.orgao());
  }
  public ObjectNode prior(Resource r, Object e) {
    ObjectNode response=json.valueToTree(mapper(r).toResponse(e));
    var allowed=new HashSet<String>();
    json.getDeserializationConfig().introspect(json.constructType(r.update)).findProperties().stream()
        .filter(p -> p.couldDeserialize()).forEach(p -> allowed.add(p.getName()));
    allowed.removeAll(Set.of("cargoNomePrevisto","saldoVagasPrevisto","cargoAglutinadorNomePrevisto","possuiDeficienciaPcDPrevisto"));
    response.retain(allowed);
    return response;
  }
  public void validate(Object dto) {
    var violations=validator.validate(dto);
    if(violations.stream().anyMatch(v->v.getPropertyPath().toString().equals("jornadaSemanal")))
      throw new BulkInputViolation("JORNADA_FORA_INTERVALO");
    if (!violations.isEmpty()) throw new IllegalArgumentException("Parâmetros incompatíveis com o cadastro.");
  }
  private void types(Resource r, List<BulkFieldChange> changes) {
    var bean=json.getDeserializationConfig().introspect(json.constructType(r.update));
    for (var change:changes) {
      if (change.operator()==BulkChangeOperator.CLEAR) continue;
      var property=bean.findProperties().stream().filter(p -> p.getName().equals(change.field())).findFirst()
          .orElseThrow(()->new IllegalArgumentException("Campo não pertence ao contrato cadastral de atualização."));
      Class<?> type=property.getPrimaryType().getRawClass(); var value=change.value();
      boolean valid=type==String.class ? value.isTextual() : type==Boolean.class ? value.isBoolean()
          : type==Integer.class ? value.isIntegralNumber() && value.canConvertToInt()
          : type==Long.class ? value.isIntegralNumber() && value.canConvertToLong() : false;
      if (!valid) throw new IllegalArgumentException("Tipo de valor incompatível com o campo do cadastro.");
    }
  }
  public Object candidate(Resource r, Object e, List<BulkFieldChange> changes, BulkEditableFields fields, BulkMode mode) {
    types(r,changes);
    ObjectNode value=BulkFieldChanges.applyTo(prior(r,e),changes,fields.writableFields(mode),fields.clearableFields(mode));
    Object dto=json.convertValue(value,r.update); validate(dto);
    try {
      Object copy=r.entity.getDeclaredConstructor().newInstance(); BeanUtils.copyProperties(e,copy);
      mapper(r).applyUpdate(copy,dto); validateEntity(r,copy); return dto;
    } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Tipo de entidade não construível."); }
  }
  public void validateEntity(Resource r, Object e) {
    switch(r) { case CARGOS -> invariants.cargo((Cargo)e); case PESSOAS -> invariants.pessoa((ServidorPessoa)e); case VINCULOS -> invariants.vinculo((Vinculo)e,false); }
  }
  public boolean apply(Resource r, Object e, Object dto) {
    ObjectNode before=prior(r,e); mapper(r).applyUpdate(e,dto);
    boolean changed=!before.equals(prior(r,e));
    validateEntity(r,e); em.flush(); return changed;
  }
  public List<Object> query(Resource r, Object filter, int excluded) {
    validate(filter);
    int limit=Math.addExact(201,excluded);
    int orgao=tenant.orgao();
    return switch(r) {
      case VINCULOS -> vinculoService.selectBulkIdentities((VinculoFilterDTO)filter,
          (root,q,cb)->cb.equal(root.get("empCodigo"),orgao),limit).stream()
          .map(id->(Object)CompositeIdSupport.encode(id)).toList();
      case PESSOAS -> pessoaService.selectBulkIdentities((ServidorPessoaFilterDTO)filter,
          (root,q,cb)->cb.conjunction(),limit).stream().map(id->(Object)id.toString()).toList();
      case CARGOS -> cargoService.selectBulkIdentities((CargoFilterDTO)filter,
          (root,q,cb)->cb.equal(root.get("empCodigo"),orgao),limit).stream().map(id->(Object)id).toList();
    };
  }
  public Object commandCandidate(Vinculo current, String operation, JsonNode parameters) {
    Vinculo copy=new Vinculo(); BeanUtils.copyProperties(current,copy);
    if (operation.endsWith("realocar-setor-lote")) {
      var dto=json.convertValue(parameters,RealocarSetorLoteDTO.class); validate(dto);
      if (!Set.of("ATIVO","CEDIDO").contains(copy.getSituacao())) throw new IllegalArgumentException("Estado incompatível com realocação.");
      document(dto.documentoLegalId(),copy.getEmpCodigo());
      copy.setSetor(dto.setor()); if(dto.setorExercicio()!=null) copy.setSetorExercicio(dto.setorExercicio()); copy.setDocumentoLegalId(dto.documentoLegalId());
    } else {
      var dto=json.convertValue(parameters,HomologarEstagioLoteDTO.class); validate(dto);
      if (!"ATIVO".equals(copy.getSituacao()) || copy.getDtHomologEstagio()!=null || copy.getDtexerc()==null) throw new IllegalArgumentException("Estágio não disponível para homologação.");
      ErgonInvariants.chronology(copy.getDtexerc(),dto.dtHomologEstagio(),"Homologação anterior ao exercício.");
      document(dto.documentoLegalId(),copy.getEmpCodigo()); copy.setDtHomologEstagio(dto.dtHomologEstagio()); copy.setDocumentoLegalId(dto.documentoLegalId());
    }
    invariants.vinculo(copy,false); return copy;
  }
  private void document(Long id, Integer owner) {
    var document=em.find(DocumentoLegal.class,id);
    if (document==null || !Objects.equals(document.getEmpCodigo(),owner)) throw new IllegalArgumentException("Ato coletivo deve pertencer ao órgão do vínculo.");
  }
  public boolean applyCommand(Vinculo entity, Vinculo candidate) {
    boolean changed=!Objects.equals(entity.getSetor(),candidate.getSetor()) || !Objects.equals(entity.getSetorExercicio(),candidate.getSetorExercicio())
        || !Objects.equals(entity.getDtHomologEstagio(),candidate.getDtHomologEstagio()) || !Objects.equals(entity.getDocumentoLegalId(),candidate.getDocumentoLegalId());
    entity.setSetor(candidate.getSetor());entity.setSetorExercicio(candidate.getSetorExercicio());
    entity.setDtHomologEstagio(candidate.getDtHomologEstagio());entity.setDocumentoLegalId(candidate.getDocumentoLegalId());em.flush();return changed;
  }
}
