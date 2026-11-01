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
    CARGOS("administracao-pessoal.cargos", "cargos", "updateErgonCargo", Cargo.class, UpdateCargoDTO.class),
    DEPENDENTES("administracao-pessoal.dependentes", "dependentes", "updateErgonDependente", Dependente.class, UpdateDependenteDTO.class),
    AFASTAMENTOS("administracao-pessoal.afastamentos", "afastamentos", "updateErgonAfastamento", Afastamento.class, UpdateAfastamentoDTO.class),
    FREQUENCIAS("administracao-pessoal.frequencias", "frequencias", "updateErgonFrequencia", Frequencia.class, UpdateFrequenciaDTO.class),
    CAMPOS_CUSTOMIZADOS("administracao-pessoal.campos-customizados", "campos-customizados", "updateErgonCampoCustomizado", DefinicaoCampoCustomizado.class, UpdateDefinicaoCampoCustomizadoDTO.class),
    PARAMETROS_CONTAGEM_FERIAS("administracao-pessoal.parametros-contagem-ferias", "parametros-contagem-ferias", "updateErgonParametroContagemFerias", ParametroContagemFerias.class, UpdateParametroContagemFeriasDTO.class),
    PERIODOS_AQUISITIVOS_FERIAS("administracao-pessoal.periodos-aquisitivos-ferias", "periodos-aquisitivos-ferias", "updateErgonPeriodoAquisitivoFerias", PeriodoAquisitivoFerias.class, UpdatePeriodoAquisitivoFeriasDTO.class),
    FERIAS_GOZO("administracao-pessoal.ferias-gozo", "ferias-gozo", "updateErgonFeriasGozo", FeriasGozo.class, UpdateFeriasGozoDTO.class);
    public final String key, name, updateOperation;
    public final Class<?> entity, update;
    Resource(String key, String name, String op, Class<?> entity, Class<?> update) { this.key=key;this.name=name;this.updateOperation=op;this.entity=entity;this.update=update; }
    public Set<String> protectedFields() {
      return switch(this) {
        case VINCULOS -> Set.of("cargo", "dtnom", "dtposse");
        case PESSOAS -> Set.of("cpf", "nome", "dtnasc");
        case CARGOS -> Set.of();
        case DEPENDENTES -> Set.of("nome", "dtnasc", "invalido");
        case AFASTAMENTOS, FREQUENCIAS -> Set.of("tipofreq", "codfreq", "dtini");
        case CAMPOS_CUSTOMIZADOS -> Set.of("empCodigo", "resourceKey", "fieldKey", "fieldType", "validationRules", "governancePolicy");
        case PARAMETROS_CONTAGEM_FERIAS -> Set.of("dtini", "active");
        case PERIODOS_AQUISITIVOS_FERIAS -> Set.of("empCodigo", "numfunc", "numvinc", "chave", "dtini", "diasGozados", "diasVendidosAbono", "diasConvertidosDobro", "saldoDiasDisponivel", "geradoPorAverbacao", "situacao");
        case FERIAS_GOZO -> Set.of("empCodigo", "numfunc", "numvinc", "periodoAquisitivoId", "chavePeraqfer", "ordemParcela", "dtini", "totDiasDireitoPa", "situacao");
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
  @Autowired DependenteMapper dependentes;
  @Autowired AfastamentoMapper afastamentos;
  @Autowired FrequenciaMapper frequencias;
  @Autowired DefinicaoCampoCustomizadoMapper camposCustomizados;
  @Autowired ParametroContagemFeriasMapper parametrosContagemFerias;
  @Autowired PeriodoAquisitivoFeriasMapper periodosAquisitivosFerias;
  @Autowired FeriasGozoMapper feriasGozo;
  @Autowired VinculoService vinculoService;
  @Autowired ServidorPessoaService pessoaService;
  @Autowired CargoService cargoService;
  @Autowired DependenteService dependenteService;
  @Autowired AfastamentoService afastamentoService;
  @Autowired FrequenciaService frequenciaService;
  @Autowired DefinicaoCampoCustomizadoService campoCustomizadoService;
  @Autowired ParametroContagemFeriasService parametroContagemFeriasService;
  @Autowired PeriodoAquisitivoFeriasService periodoAquisitivoFeriasService;
  @Autowired FeriasGozoService feriasGozoService;

  ResourceMapper mapper(Resource r) {
    return switch(r) {
      case VINCULOS -> vinculos;
      case PESSOAS -> pessoas;
      case CARGOS -> cargos;
      case DEPENDENTES -> dependentes;
      case AFASTAMENTOS -> afastamentos;
      case FREQUENCIAS -> frequencias;
      case CAMPOS_CUSTOMIZADOS -> camposCustomizados;
      case PARAMETROS_CONTAGEM_FERIAS -> parametrosContagemFerias;
      case PERIODOS_AQUISITIVOS_FERIAS -> periodosAquisitivosFerias;
      case FERIAS_GOZO -> feriasGozo;
    };
  }
  public BulkIdentityCodec codec(Resource r) {
    return switch(r) {
      case CARGOS -> BulkIdentityCodecs.integers();
      case PESSOAS, AFASTAMENTOS, FREQUENCIAS, CAMPOS_CUSTOMIZADOS,
          PARAMETROS_CONTAGEM_FERIAS, PERIODOS_AQUISITIVOS_FERIAS, FERIAS_GOZO -> BulkIdentityCodecs.longs();
      case VINCULOS, DEPENDENTES -> BulkIdentityCodecs.strings();
    };
  }
  public Object identity(Resource r, Object wire) {
    return switch(r) {
      case CARGOS -> BulkIdentityCodecs.integers().decode((Integer) wire);
      case PESSOAS, AFASTAMENTOS, FREQUENCIAS, CAMPOS_CUSTOMIZADOS,
          PARAMETROS_CONTAGEM_FERIAS, PERIODOS_AQUISITIVOS_FERIAS, FERIAS_GOZO -> BulkIdentityCodecs.longs().decode((String) wire);
      case VINCULOS -> {
        VinculoId id=CompositeIdSupport.decode((String)wire,VinculoId.class);
        if(!wire.equals(CompositeIdSupport.encode(id))) throw new IllegalArgumentException("Identidade composta não canônica.");
        yield id;
      }
      case DEPENDENTES -> {
        DependenteId id=CompositeIdSupport.decode((String)wire,DependenteId.class);
        if(!wire.equals(CompositeIdSupport.encode(id))) throw new IllegalArgumentException("Identidade composta não canônica.");
        yield id;
      }
    };
  }
  public Object wire(Resource r, Object e) {
    return switch(r) {
      case CARGOS -> ((Cargo)e).getCargo();
      case PESSOAS -> ((ServidorPessoa)e).getNumero().toString();
      case AFASTAMENTOS -> ((Afastamento)e).getId().toString();
      case FREQUENCIAS -> ((Frequencia)e).getId().toString();
      case CAMPOS_CUSTOMIZADOS -> ((DefinicaoCampoCustomizado)e).getId().toString();
      case PARAMETROS_CONTAGEM_FERIAS -> ((ParametroContagemFerias)e).getId().toString();
      case PERIODOS_AQUISITIVOS_FERIAS -> ((PeriodoAquisitivoFerias)e).getId().toString();
      case FERIAS_GOZO -> ((FeriasGozo)e).getId().toString();
      case VINCULOS -> CompositeIdSupport.encode(((Vinculo)e).getId());
      case DEPENDENTES -> CompositeIdSupport.encode(((Dependente)e).getId());
    };
  }
  public long version(Object e) {
    return e instanceof Vinculo v ? v.getVersion()
        : e instanceof Cargo c ? c.getVersion()
        : e instanceof Dependente d ? d.getVersion()
        : e instanceof Afastamento a ? a.getVersion()
        : e instanceof Frequencia f ? f.getVersion()
        : e instanceof DefinicaoCampoCustomizado cc ? cc.getVersion()
        : e instanceof ParametroContagemFerias pcf ? pcf.getVersion()
        : e instanceof PeriodoAquisitivoFerias paf ? paf.getVersion()
        : e instanceof FeriasGozo fg ? fg.getVersion()
        : ((ServidorPessoa)e).getVersion();
  }
  public Object load(Resource r, Object wire, boolean lock) {
    if (em.unwrap(org.hibernate.Session.class).isDirty()) throw new IllegalStateException("Contexto de persistência deve estar limpo antes do lote.");
    Object e = lock ? em.find(r.entity, identity(r,wire), LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout",1000)) : em.find(r.entity,identity(r,wire));
    if (e != null && !scope(r,e)) throw new SecurityException("Registro fora do órgão selecionado.");
    if (lock && e instanceof FeriasGozo fg && fg.getPeriodoAquisitivoId() != null) {
      em.find(
          PeriodoAquisitivoFerias.class,
          fg.getPeriodoAquisitivoId(),
          LockModeType.PESSIMISTIC_WRITE,
          Map.of("jakarta.persistence.lock.timeout", 1000));
    }
    return e;
  }
  public boolean scope(Resource r, Object e) {
    if (r == Resource.CAMPOS_CUSTOMIZADOS) {
      Integer emp = ((DefinicaoCampoCustomizado) e).getEmpCodigo();
      return emp == null || Objects.equals(emp, tenant.orgao());
    }
    if (r == Resource.PARAMETROS_CONTAGEM_FERIAS) {
      Integer emp = ((ParametroContagemFerias) e).getEmpCodigo();
      return emp == null || Objects.equals(emp, tenant.orgao());
    }
    return r == Resource.PESSOAS || r == Resource.DEPENDENTES
        || Objects.equals(
            r == Resource.CARGOS ? ((Cargo)e).getEmpCodigo()
                : r == Resource.AFASTAMENTOS ? ((Afastamento)e).getEmpCodigo()
                : r == Resource.FREQUENCIAS ? ((Frequencia)e).getEmpCodigo()
                : r == Resource.PERIODOS_AQUISITIVOS_FERIAS ? ((PeriodoAquisitivoFerias)e).getEmpCodigo()
                : r == Resource.FERIAS_GOZO ? ((FeriasGozo)e).getEmpCodigo()
                : ((Vinculo)e).getEmpCodigo(),
            tenant.orgao());
  }
  public ObjectNode prior(Resource r, Object e) {
    ObjectNode response=json.valueToTree(mapper(r).toResponse(e));
    var allowed=new HashSet<String>();
    json.getDeserializationConfig().introspect(json.constructType(r.update)).findProperties().stream()
        .filter(p -> p.couldDeserialize()).forEach(p -> allowed.add(p.getName()));
    allowed.removeAll(Set.of("cargoNomePrevisto","saldoVagasPrevisto","cargoAglutinadorNomePrevisto",
        "possuiDeficienciaPcDPrevisto","tipoDependenteDescricaoPrevista","idadeCalculadaAnos","elegivelIrrfPrevisto",
        "codigoFrequenciaNomePrevisto","quantidadeDiasPrevista","comOnusPrevisto",
        "formatoQuantidadePrevisto","exigeHorarioPrevisto",
        "optionTabelaDescricaoPrevista","requerTabelaOpcoesPrevisto","escopoGlobalPrevisto",
        "orgaoNome","escopoGlobal","resumoParametro",
        "pessoaNome","vinculoSituacao","periodoResumo","saldoDiasDisponivel",
        "parcelaResumo","totalDiasConsumidosPa","saldoPaAposParcela"));
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
      boolean valid=type==String.class || type==java.time.LocalDate.class || type==java.time.LocalTime.class ? value.isTextual()
          : type==Boolean.class ? value.isBoolean()
          : type==Integer.class ? value.isIntegralNumber() && value.canConvertToInt()
          : type==Long.class ? value.isIntegralNumber() && value.canConvertToLong()
          : type==java.math.BigDecimal.class ? value.isNumber() : false;
      if (!valid) throw new IllegalArgumentException("Tipo de valor incompatível com o campo do cadastro.");
    }
  }
  public Object candidate(Resource r, Object e, List<BulkFieldChange> changes, BulkEditableFields fields, BulkMode mode) {
    types(r,changes);
    ObjectNode value=BulkFieldChanges.applyTo(prior(r,e),changes,fields.writableFields(mode),fields.clearableFields(mode));
    Object dto=json.convertValue(value,r.update); validate(dto);
    try {
      Object copy=r.entity.getDeclaredConstructor().newInstance(); BeanUtils.copyProperties(e,copy);
      mapper(r).applyUpdate(copy,dto);
      if (copy instanceof FeriasGozo fg && feriasGozoService != null) {
        feriasGozoService.hydrateParentPeriodoAquisitivo(fg, false);
      }
      validateEntity(r,copy,e); return dto;
    } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Tipo de entidade não construível."); }
  }
  public void validateEntity(Resource r, Object e) {
    validateEntity(r, e, null);
  }
  public void validateEntity(Resource r, Object e, Object previousSnapshot) {
    switch(r) {
      case CARGOS -> invariants.cargo((Cargo)e);
      case PESSOAS -> invariants.pessoa((ServidorPessoa)e);
      case VINCULOS -> {
        invariants.vinculo((Vinculo) e, false, false);
        if (vinculoService != null) {
          vinculoService.validateVinculoProvimento((Vinculo) e, false);
        }
      }
      case DEPENDENTES -> {
        invariants.dependente((Dependente) e, false, false);
        if (dependenteService != null) {
          dependenteService.validateDependenteElegibilidade((Dependente) e);
        }
      }
      case AFASTAMENTOS -> {
        if (afastamentoService != null) {
          afastamentoService.validateAfastamentoConcessao((Afastamento) e, false);
        }
        invariants.afastamento((Afastamento) e, false);
      }
      case FREQUENCIAS -> {
        if (frequenciaService != null) {
          frequenciaService.validateFrequenciaLancamento((Frequencia) e);
        }
        invariants.frequencia((Frequencia) e, false);
      }
      case CAMPOS_CUSTOMIZADOS -> invariants.campoCustomizado((DefinicaoCampoCustomizado)e,false);
      case PARAMETROS_CONTAGEM_FERIAS -> {
        if (parametroContagemFeriasService != null) {
          parametroContagemFeriasService.validateParametroContagemFerias((ParametroContagemFerias) e);
        }
        invariants.parametroContagemFerias((ParametroContagemFerias) e, false);
      }
      case PERIODOS_AQUISITIVOS_FERIAS -> {
        if (periodoAquisitivoFeriasService != null) {
          periodoAquisitivoFeriasService.validatePeriodoAquisitivoFerias(
              (PeriodoAquisitivoFerias) e,
              previousSnapshot instanceof PeriodoAquisitivoFerias paf ? paf : null);
        }
        invariants.periodoAquisitivoFerias((PeriodoAquisitivoFerias) e, false);
      }
      case FERIAS_GOZO -> {
        if (feriasGozoService != null) {
          feriasGozoService.validateFeriasGozo(
              (FeriasGozo) e, previousSnapshot instanceof FeriasGozo fg ? fg : null);
        }
        invariants.feriasGozo((FeriasGozo) e, false);
      }
    }
  }
  public boolean apply(Resource r, Object e, Object dto) {
    Object previousSnapshot = null;
    try {
      previousSnapshot = r.entity.getDeclaredConstructor().newInstance();
      BeanUtils.copyProperties(e, previousSnapshot);
    } catch (ReflectiveOperationException ignored) {
    }
    ObjectNode before=prior(r,e); mapper(r).applyUpdate(e,dto);
    if (e instanceof PeriodoAquisitivoFerias paf && previousSnapshot instanceof PeriodoAquisitivoFerias prevPa) {
      if (prevPa.getDiasGozados() != null) paf.setDiasGozados(prevPa.getDiasGozados());
      if (prevPa.getDiasVendidosAbono() != null) paf.setDiasVendidosAbono(prevPa.getDiasVendidosAbono());
      if (prevPa.getDiasConvertidosDobro() != null) paf.setDiasConvertidosDobro(prevPa.getDiasConvertidosDobro());
      if (prevPa.getGeradoPorAverbacao() != null) paf.setGeradoPorAverbacao(prevPa.getGeradoPorAverbacao());
      PeriodoAquisitivoFeriasMapper.recomputeSaldoAndSituacao(paf);
    }
    if (e instanceof FeriasGozo fg && feriasGozoService != null) {
      feriasGozoService.hydrateParentPeriodoAquisitivo(fg, false);
    }
    boolean changed=!before.equals(prior(r,e));
    validateEntity(r,e,previousSnapshot);
    if (e instanceof FeriasGozo fg && feriasGozoService != null) {
      feriasGozoService.sincronizarContabilidadePeriodoAquisitivo(fg.getPeriodoAquisitivoId());
    }
    em.flush(); return changed;
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
      case DEPENDENTES -> dependenteService.selectBulkIdentities((DependenteFilterDTO)filter,
          (root,q,cb)->cb.conjunction(),limit).stream()
          .map(id->(Object)CompositeIdSupport.encode(id)).toList();
      case AFASTAMENTOS -> afastamentoService.selectBulkIdentities((AfastamentoFilterDTO)filter,
          (root,q,cb)->cb.equal(root.get("empCodigo"),orgao),limit).stream()
          .map(id->(Object)id.toString()).toList();
      case FREQUENCIAS -> frequenciaService.selectBulkIdentities((FrequenciaFilterDTO)filter,
          (root,q,cb)->cb.equal(root.get("empCodigo"),orgao),limit).stream()
          .map(id->(Object)id.toString()).toList();
      case CAMPOS_CUSTOMIZADOS -> campoCustomizadoService.selectBulkIdentities((DefinicaoCampoCustomizadoFilterDTO)filter,
          (root,q,cb)->cb.or(cb.isNull(root.get("empCodigo")), cb.equal(root.get("empCodigo"),orgao)),limit).stream()
          .map(id->(Object)id.toString()).toList();
      case PARAMETROS_CONTAGEM_FERIAS -> parametroContagemFeriasService.selectBulkIdentities((ParametroContagemFeriasFilterDTO)filter,
          (root,q,cb)->cb.or(cb.isNull(root.get("empCodigo")), cb.equal(root.get("empCodigo"),orgao)),limit).stream()
          .map(id->(Object)id.toString()).toList();
      case PERIODOS_AQUISITIVOS_FERIAS -> periodoAquisitivoFeriasService.selectBulkIdentities((PeriodoAquisitivoFeriasFilterDTO)filter,
          (root,q,cb)->cb.equal(root.get("empCodigo"),orgao),limit).stream()
          .map(id->(Object)id.toString()).toList();
      case FERIAS_GOZO -> feriasGozoService.selectBulkIdentities((FeriasGozoFilterDTO)filter,
          (root,q,cb)->cb.equal(root.get("empCodigo"),orgao),limit).stream()
          .map(id->(Object)id.toString()).toList();
    };
  }
  public Object commandCandidate(Object current, String operation, JsonNode parameters) {
    if (current instanceof ParametroContagemFerias pcf) {
      ParametroContagemFerias copy = new ParametroContagemFerias(); BeanUtils.copyProperties(pcf, copy);
      var dto = json.convertValue(parameters, AlterarVigenciaParametroFeriasDTO.class); validate(dto);
      if (Boolean.TRUE.equals(copy.getActive()))
        throw new IllegalArgumentException("Parâmetro de contagem de férias já se encontra ativo.");
      copy.setActive(true);
      if (dto.getDtfim() != null) copy.setDtfim(dto.getDtfim());
      if (parametroContagemFeriasService != null) {
        parametroContagemFeriasService.validateParametroContagemFerias(copy);
      }
      invariants.parametroContagemFerias(copy, false);
      return copy;
    }
    if (current instanceof PeriodoAquisitivoFerias paf) {
      PeriodoAquisitivoFerias copy = new PeriodoAquisitivoFerias(); BeanUtils.copyProperties(paf, copy);
      var dto = json.convertValue(parameters, TransicaoPeriodoAquisitivoFeriasDTO.class); validate(dto);
      if (!Set.of("EM_AQUISICAO", "SUSPENSO").contains(copy.getSituacao()))
        throw new IllegalArgumentException("Período aquisitivo não está em aquisição ou suspenso para homologação.");
      if (dto.getDocumentoLegalId() != null) document(dto.getDocumentoLegalId(), copy.getEmpCodigo());
      copy.setSituacao("DISPONIVEL");
      PeriodoAquisitivoFeriasMapper.recomputeSaldoAndSituacao(copy);
      if (dto.getDocumentoLegalId() != null) copy.setDocumentoLegalId(dto.getDocumentoLegalId());
      copy.setObservacoes((copy.getObservacoes() == null ? "" : copy.getObservacoes() + " | ") + "Homologação em lote: " + dto.getObservacoes());
      if (periodoAquisitivoFeriasService != null) {
        periodoAquisitivoFeriasService.validatePeriodoAquisitivoFerias(copy, paf);
      }
      invariants.periodoAquisitivoFerias(copy, false);
      return copy;
    }
    if (current instanceof FeriasGozo fg) {
      FeriasGozo copy = new FeriasGozo(); BeanUtils.copyProperties(fg, copy);
      var dto = json.convertValue(parameters, TransicaoFeriasGozoDTO.class); validate(dto);
      if (!"PROGRAMADA".equals(copy.getSituacao()))
        throw new IllegalArgumentException("Parcela de férias não está em estado PROGRAMADA para homologação.");
      if (dto.getDocumentoLegalId() != null) document(dto.getDocumentoLegalId(), copy.getEmpCodigo());
      if (dto.getAtoPublicacaoId() != null) document(dto.getAtoPublicacaoId(), copy.getEmpCodigo());
      copy.setSituacao("HOMOLOGADA");
      if (dto.getDocumentoLegalId() != null) copy.setDocumentoLegalId(dto.getDocumentoLegalId());
      if (dto.getAtoPublicacaoId() != null) copy.setAtoPublicacaoId(dto.getAtoPublicacaoId());
      copy.setObservacoes((copy.getObservacoes() == null ? "" : copy.getObservacoes() + " | ") + "Homologação em lote: " + dto.getObservacoes());
      if (feriasGozoService != null) {
        feriasGozoService.validateFeriasGozo(copy, fg);
      }
      invariants.feriasGozo(copy, false);
      return copy;
    }
    if (current instanceof DefinicaoCampoCustomizado cc) {
      DefinicaoCampoCustomizado copy=new DefinicaoCampoCustomizado(); BeanUtils.copyProperties(cc,copy);
      var dto=json.convertValue(parameters,AtivarCampoCustomizadoLoteDTO.class); validate(dto);
      if (Boolean.TRUE.equals(copy.getActive()))
        throw new IllegalArgumentException("Campo customizado já se encontra ativo.");
      copy.setActive(true);
      invariants.campoCustomizado(copy,false); return copy;
    }
    if (current instanceof Frequencia freq) {
      Frequencia copy=new Frequencia(); BeanUtils.copyProperties(freq,copy);
      var dto=json.convertValue(parameters,HomologarFrequenciaLoteDTO.class); validate(dto);
      if (!"PENDENTE_HOMOLOGACAO".equals(copy.getSituacao()))
        throw new IllegalArgumentException("Ocorrência de frequência não está pendente de homologação.");
      if (dto.getDocumentoLegalId()!=null) document(dto.getDocumentoLegalId(),copy.getEmpCodigo());
      copy.setSituacao("HOMOLOGADO");
      if (dto.getDocumentoLegalId()!=null) copy.setDocumentoLegalId(dto.getDocumentoLegalId());
      copy.setObs((copy.getObs()==null ? "" : copy.getObs()+" | ") + "Homologação em lote: " + dto.getObservacoes());
      if (frequenciaService != null) {
        frequenciaService.validateFrequenciaLancamento(copy);
      }
      invariants.frequencia(copy,false); return copy;
    }
    if (current instanceof Afastamento afast) {
      Afastamento copy=new Afastamento(); BeanUtils.copyProperties(afast,copy);
      var dto=json.convertValue(parameters,HomologarAfastamentoLoteDTO.class); validate(dto);
      if (!"PENDENTE_HOMOLOGACAO".equals(copy.getSituacao()))
        throw new IllegalArgumentException("Afastamento não está pendente de homologação.");
      document(dto.documentoLegalId(),copy.getEmpCodigo());
      if (dto.atoPublicacaoId()!=null) document(dto.atoPublicacaoId(),copy.getEmpCodigo());
      copy.setSituacao("HOMOLOGADO");
      copy.setDocumentoLegalId(dto.documentoLegalId());
      if (dto.atoPublicacaoId()!=null) copy.setAtoPublicacaoId(dto.atoPublicacaoId());
      if (afastamentoService != null) {
        afastamentoService.validateAfastamentoConcessao(copy, false);
      }
      invariants.afastamento(copy,false); return copy;
    }
    if (current instanceof Dependente dep) {
      Dependente copy=new Dependente(); BeanUtils.copyProperties(dep,copy);
      var dto=json.convertValue(parameters,EncerrarDependenciaLoteDTO.class); validate(dto);
      if (copy.getDtfim()!=null) throw new IllegalArgumentException("Dependência já encerrada.");
      ErgonInvariants.chronology(copy.getDtini(),dto.dtfim(),"Encerramento anterior ao início da dependência.");
      document(dto.documentoLegalId(),tenant.orgao());
      copy.setDtfim(dto.dtfim()); copy.setDepIr(false); copy.setDepSalarioFamilia(false); copy.setDepPlanoSaude(false);
      copy.setDocumentoLegalId(dto.documentoLegalId());
      invariants.dependente(copy, false, false);
      if (dependenteService != null) {
        dependenteService.validateDependenteElegibilidade(copy);
      }
      return copy;
    }
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
    invariants.vinculo(copy, false, false);
    if (vinculoService != null) {
      vinculoService.validateVinculoProvimento(copy, false);
    }
    return copy;
  }
  private void document(Long id, Integer owner) {
    var document=em.find(DocumentoLegal.class,id);
    if (document==null || !Objects.equals(document.getEmpCodigo(),owner)) throw new IllegalArgumentException("Ato coletivo deve pertencer ao órgão do vínculo.");
  }
  public boolean applyCommand(Object entity, Object candidate) {
    if (entity instanceof ParametroContagemFerias current && candidate instanceof ParametroContagemFerias next) {
      boolean changed = !Objects.equals(current.getActive(), next.getActive())
          || !Objects.equals(current.getDtfim(), next.getDtfim());
      current.setActive(next.getActive());
      current.setDtfim(next.getDtfim());
      em.flush(); return changed;
    }
    if (entity instanceof PeriodoAquisitivoFerias current && candidate instanceof PeriodoAquisitivoFerias next) {
      boolean changed = !Objects.equals(current.getSituacao(), next.getSituacao())
          || !Objects.equals(current.getSaldoDiasDisponivel(), next.getSaldoDiasDisponivel())
          || !Objects.equals(current.getDocumentoLegalId(), next.getDocumentoLegalId())
          || !Objects.equals(current.getObservacoes(), next.getObservacoes());
      current.setSituacao(next.getSituacao());
      current.setSaldoDiasDisponivel(next.getSaldoDiasDisponivel());
      current.setDocumentoLegalId(next.getDocumentoLegalId());
      current.setObservacoes(next.getObservacoes());
      em.flush(); return changed;
    }
    if (entity instanceof FeriasGozo current && candidate instanceof FeriasGozo next) {
      boolean changed = !Objects.equals(current.getSituacao(), next.getSituacao())
          || !Objects.equals(current.getDocumentoLegalId(), next.getDocumentoLegalId())
          || !Objects.equals(current.getAtoPublicacaoId(), next.getAtoPublicacaoId())
          || !Objects.equals(current.getObservacoes(), next.getObservacoes());
      current.setSituacao(next.getSituacao());
      current.setDocumentoLegalId(next.getDocumentoLegalId());
      current.setAtoPublicacaoId(next.getAtoPublicacaoId());
      current.setObservacoes(next.getObservacoes());
      if (feriasGozoService != null) {
        feriasGozoService.sincronizarContabilidadePeriodoAquisitivo(current.getPeriodoAquisitivoId());
      }
      em.flush(); return changed;
    }
    if (entity instanceof DefinicaoCampoCustomizado current && candidate instanceof DefinicaoCampoCustomizado next) {
      boolean changed=!Objects.equals(current.getActive(),next.getActive());
      current.setActive(next.getActive());
      em.flush(); return changed;
    }
    if (entity instanceof Frequencia current && candidate instanceof Frequencia next) {
      boolean changed=!Objects.equals(current.getSituacao(),next.getSituacao())
          || !Objects.equals(current.getDocumentoLegalId(),next.getDocumentoLegalId())
          || !Objects.equals(current.getObs(),next.getObs());
      current.setSituacao(next.getSituacao());
      current.setDocumentoLegalId(next.getDocumentoLegalId());
      current.setObs(next.getObs());
      em.flush(); return changed;
    }
    if (entity instanceof Afastamento current && candidate instanceof Afastamento next) {
      boolean changed=!Objects.equals(current.getSituacao(),next.getSituacao())
          || !Objects.equals(current.getDocumentoLegalId(),next.getDocumentoLegalId())
          || !Objects.equals(current.getAtoPublicacaoId(),next.getAtoPublicacaoId());
      current.setSituacao(next.getSituacao());
      current.setDocumentoLegalId(next.getDocumentoLegalId());
      current.setAtoPublicacaoId(next.getAtoPublicacaoId());
      var vinculo=em.find(Vinculo.class,new VinculoId(current.getNumfunc(),current.getNumvinc()));
      if (vinculo!=null && "ATIVO".equals(vinculo.getSituacao()) && current.getDtfim()==null) {
        vinculo.setSituacao("AFASTADO");
        vinculo.setDocumentoLegalId(current.getDocumentoLegalId());
      }
      em.flush(); return changed;
    }
    if (entity instanceof Dependente current && candidate instanceof Dependente next) {
      boolean changed=!Objects.equals(current.getDtfim(),next.getDtfim())
          || !Objects.equals(current.getDepIr(),next.getDepIr())
          || !Objects.equals(current.getDepSalarioFamilia(),next.getDepSalarioFamilia())
          || !Objects.equals(current.getDepPlanoSaude(),next.getDepPlanoSaude())
          || !Objects.equals(current.getDocumentoLegalId(),next.getDocumentoLegalId());
      current.setDtfim(next.getDtfim());current.setDepIr(next.getDepIr());
      current.setDepSalarioFamilia(next.getDepSalarioFamilia());current.setDepPlanoSaude(next.getDepPlanoSaude());
      current.setDocumentoLegalId(next.getDocumentoLegalId());em.flush();return changed;
    }
    Vinculo v=(Vinculo)entity; Vinculo c=(Vinculo)candidate;
    boolean changed=!Objects.equals(v.getSetor(),c.getSetor()) || !Objects.equals(v.getSetorExercicio(),c.getSetorExercicio())
        || !Objects.equals(v.getDtHomologEstagio(),c.getDtHomologEstagio()) || !Objects.equals(v.getDocumentoLegalId(),c.getDocumentoLegalId());
    v.setSetor(c.getSetor());v.setSetorExercicio(c.getSetorExercicio());
    v.setDtHomologEstagio(c.getDtHomologEstagio());v.setDocumentoLegalId(c.getDocumentoLegalId());em.flush();return changed;
  }
}
