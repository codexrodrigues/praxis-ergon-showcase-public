package br.com.praxis.ergonshowcase.bulk;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
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
    FERIAS_GOZO("administracao-pessoal.ferias-gozo", "ferias-gozo", "updateErgonFeriasGozo", FeriasGozo.class, UpdateFeriasGozoDTO.class),
    AVERBACOES_TEMPO_SERVICO("administracao-pessoal.averbacoes-tempo-servico","averbacoes-tempo-servico","updateErgonAverbacaoTempoServico",AverbacaoTempoServico.class,UpdateAverbacaoTempoServicoDTO.class),
    PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL("administracao-pessoal.periodos-aquisitivos-licenca-especial","periodos-aquisitivos-licenca-especial","updateErgonPeriodoAquisitivoLicencaEspecial",PeriodoAquisitivoLicencaEspecial.class,UpdatePeriodoAquisitivoLicencaEspecialDTO.class),
    LICENCAS_ESPECIAIS_GOZO("administracao-pessoal.licencas-especiais-gozo","licencas-especiais-gozo","updateErgonLicencaEspecialGozo",LicencaEspecialGozo.class,UpdateLicencaEspecialGozoDTO.class),
    VENDAS_LICENCA_ESPECIAL("administracao-pessoal.vendas-licenca-especial","vendas-licenca-especial","updateErgonVendaLicencaEspecial",VendaLicencaEspecial.class,UpdateVendaLicencaEspecialDTO.class),
    DESIGNACOES(ApiPaths.KEY_DESIGNACOES, "designacoes", "updateErgonDesignacaoFuncao", DesignacaoFuncao.class, UpdateDesignacaoFuncaoDTO.class),
    FUNCOES(ApiPaths.KEY_FUNCOES, "funcoes", "updateErgonFuncaoConfianca", FuncaoConfianca.class, UpdateFuncaoConfiancaDTO.class),
    VAGAS_FUNCAO(ApiPaths.KEY_VAGAS_FUNCAO, "vagas-funcao", "updateErgonVagaFuncao", VagaFuncao.class, UpdateVagaFuncaoDTO.class),
    SUBSTITUICOES(ApiPaths.KEY_SUBSTITUICOES, "substituicoes", "updateErgonSubstituicaoFuncao", SubstituicaoFuncao.class, UpdateSubstituicaoFuncaoDTO.class);
    public final String key, name, updateOperation;
    /** Nome físico declarado pela entidade; a rota pública nunca identifica uma tabela. */
    public final String table;
    public final Class<?> entity, update;
    Resource(String key, String name, String op, Class<?> entity, Class<?> update) {
      this.key=key;this.name=name;this.updateOperation=op;this.entity=entity;this.update=update;
      this.table=Objects.requireNonNull(entity.getAnnotation(Table.class), "Entidade de lote exige @Table explícita").name();
      if(!table.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("Tabela de lote deve ser um identificador físico simples");
    }
    public Set<String> protectedFields() {
      return switch(this) {
        case FUNCOES -> Set.of("empCodigo", "codigo", "situacao", "dtini", "dtfim", "controleVaga", "dtInicioContrVaga", "permiteSubstituicao", "tipoSubstituicao", "diasMinimosSubstituicao", "valorGratificacao", "percentualOpcaoEfetivo", "exigeVinculoEfetivo", "atoPublicacaoId");
        case VAGAS_FUNCAO -> Set.of("empCodigo", "numeroVaga", "funcaoId", "situacao", "dtini", "dtfim", "setorId", "origemVaga", "motivoBloqueio", "atoPublicacaoId");
        case SUBSTITUICOES -> Set.of("empCodigo", "designacaoId", "substitutoId", "situacao", "dtini", "dtfim", "motivo", "afastamentoId", "feriasGozoId", "dtiniRemunerada", "dtfimRemunerada", "competenciaFolha", "valorSubstituicao", "atoPublicacaoId");
        case DESIGNACOES -> Set.of("empCodigo", "titularId", "funcaoId", "vagaFuncaoId", "situacao",
            "dtini", "dtfim", "dtExercicio", "atoDispensaId", "motivoDispensa");
        case VINCULOS -> Set.of("cargo", "dtnom", "dtposse");
        case PESSOAS -> Set.of("cpf", "nome", "dtnasc");
        case CARGOS -> Set.of();
        case DEPENDENTES -> Set.of("nome", "dtnasc", "invalido");
        case AFASTAMENTOS, FREQUENCIAS -> Set.of("tipofreq", "codfreq", "dtini");
        case CAMPOS_CUSTOMIZADOS -> Set.of("empCodigo", "resourceKey", "fieldKey", "fieldType", "validationRules", "governancePolicy");
        case PARAMETROS_CONTAGEM_FERIAS -> Set.of("dtini");
        case PERIODOS_AQUISITIVOS_FERIAS -> Set.of("empCodigo", "numfunc", "numvinc", "chave", "dtini", "diasGozados", "diasVendidosAbono", "diasConvertidosDobro", "geradoPorAverbacao");
        case AVERBACOES_TEMPO_SERVICO -> Set.of("empCodigo", "numfunc", "numvinc", "chave", "situacao", "diasDesaverbados", "dataDesaverbacao", "motivoDesaverbacao", "dataRemocao", "motivoRemocao");
        case PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL -> Set.of("empCodigo", "numfunc", "numvinc", "chave", "situacao", "diasConvertidosDobro", "dtCntDobro", "averbacaoOrigemId");
        case LICENCAS_ESPECIAIS_GOZO -> Set.of("empCodigo", "numfunc", "numvinc", "periodoAquisitivoLicencaId", "chavePeraqlicesp", "ordemParcela", "situacao", "dtfim");
        case VENDAS_LICENCA_ESPECIAL -> Set.of("empCodigo", "numfunc", "numvinc", "periodoAquisitivoLicencaId", "chavePeraqlicesp", "situacao", "dtPagamento", "dtEstorno", "modalidade");
        case FERIAS_GOZO -> Set.of("empCodigo", "numfunc", "numvinc", "periodoAquisitivoId", "chavePeraqfer", "ordemParcela", "dtini", "totDiasDireitoPa");
      };
    }
  }
  @PersistenceContext EntityManager em;
  @Autowired ObjectMapper json;
  @Autowired Validator validator;
  @Autowired TenantContext tenant;
  @Autowired ErgonInvariants invariants;
  @Autowired VinculoMapper vinculos;
  @Autowired CargoMapper cargos;
  @Autowired DependenteMapper dependentes;
  @Autowired AfastamentoMapper afastamentos;
  @Autowired FrequenciaMapper frequencias;
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

  @Autowired AverbacaoTempoServicoMapper averbacaoTempoServico;
  @Autowired AverbacaoTempoServicoService averbacaoTempoServicoService;
  @Autowired PeriodoAquisitivoLicencaEspecialMapper periodoAquisitivoLicencaEspecial;
  @Autowired PeriodoAquisitivoLicencaEspecialService periodoAquisitivoLicencaEspecialService;
  @Autowired LicencaEspecialGozoMapper licencaEspecialGozo;
  @Autowired LicencaEspecialGozoService licencaEspecialGozoService;
  @Autowired VendaLicencaEspecialMapper vendaLicencaEspecial;
  @Autowired VendaLicencaEspecialService vendaLicencaEspecialService;
  @Autowired FuncaoConfiancaService funcaoConfiancaService;
  @Autowired VagaFuncaoService vagaFuncaoService;
  @Autowired SubstituicaoFuncaoService substituicaoFuncaoService;
  @Autowired DesignacaoFuncaoService designacaoFuncaoService;

  public ErgonResourceService service(Resource resource) {
    return switch (resource) {
      case FUNCOES -> funcaoConfiancaService;
      case VAGAS_FUNCAO -> vagaFuncaoService;
      case SUBSTITUICOES -> substituicaoFuncaoService;
      case DESIGNACOES -> designacaoFuncaoService;
      case VINCULOS -> vinculoService; case PESSOAS -> pessoaService; case CARGOS -> cargoService;
      case DEPENDENTES -> dependenteService; case AFASTAMENTOS -> afastamentoService; case FREQUENCIAS -> frequenciaService;
      case CAMPOS_CUSTOMIZADOS -> campoCustomizadoService; case PARAMETROS_CONTAGEM_FERIAS -> parametroContagemFeriasService;
      case PERIODOS_AQUISITIVOS_FERIAS -> periodoAquisitivoFeriasService; case FERIAS_GOZO -> feriasGozoService;
      case AVERBACOES_TEMPO_SERVICO -> averbacaoTempoServicoService;
      case PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL -> periodoAquisitivoLicencaEspecialService;
      case LICENCAS_ESPECIAIS_GOZO -> licencaEspecialGozoService; case VENDAS_LICENCA_ESPECIAL -> vendaLicencaEspecialService;
    };
  }
  public BulkIdentityCodec codec(Resource r) {
    return switch(r) {
      case CARGOS -> BulkIdentityCodecs.integers();
      case FUNCOES, VAGAS_FUNCAO, SUBSTITUICOES, DESIGNACOES, PESSOAS, AFASTAMENTOS, FREQUENCIAS, CAMPOS_CUSTOMIZADOS,
          PARAMETROS_CONTAGEM_FERIAS, PERIODOS_AQUISITIVOS_FERIAS, AVERBACOES_TEMPO_SERVICO, PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL, LICENCAS_ESPECIAIS_GOZO, VENDAS_LICENCA_ESPECIAL, FERIAS_GOZO -> BulkIdentityCodecs.longs();
      case VINCULOS -> BulkIdentityCodecs.composite(VinculoId.class);
      case DEPENDENTES -> BulkIdentityCodecs.composite(DependenteId.class);
    };
  }
  public Object identity(Resource r, Object wire) {
    return switch(r) {
      case CARGOS -> BulkIdentityCodecs.integers().decode((Integer) wire);
      case FUNCOES, VAGAS_FUNCAO, SUBSTITUICOES, DESIGNACOES, PESSOAS, AFASTAMENTOS, FREQUENCIAS, CAMPOS_CUSTOMIZADOS,
          PARAMETROS_CONTAGEM_FERIAS, PERIODOS_AQUISITIVOS_FERIAS, AVERBACOES_TEMPO_SERVICO, PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL, LICENCAS_ESPECIAIS_GOZO, VENDAS_LICENCA_ESPECIAL, FERIAS_GOZO -> BulkIdentityCodecs.longs().decode((String) wire);
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

  public long version(Object e) {
    return e instanceof DesignacaoFuncao d ? d.getVersion()
        : e instanceof AssentamentoTempoLicenca t ? t.getVersion()
        : e instanceof Vinculo v ? v.getVersion()
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
    Object e = em.find(r.entity, identity(r,wire));
    if (e != null && !scope(r,e)) throw new SecurityException("Registro fora do órgão selecionado.");
    if (lock && e instanceof Afastamento afastamento) {
      AfastamentoService.lockVinculo(em, afastamento);
    }
    if (lock && e != null) {
      em.lock(e, LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout",1000));
      em.refresh(e);
      if (!scope(r,e)) throw new SecurityException("Registro fora do órgão selecionado.");
    }
    if (lock && e instanceof FeriasGozo fg && fg.getPeriodoAquisitivoId() != null) {
      em.find(
          PeriodoAquisitivoFerias.class,
          fg.getPeriodoAquisitivoId(),
          LockModeType.PESSIMISTIC_WRITE,
          Map.of("jakarta.persistence.lock.timeout", 1000));
    }
    if(lock && e instanceof AssentamentoTempoLicenca) { em.refresh(e); if(!scope(r,e)) throw new SecurityException("Registro fora do órgão selecionado."); }
    return e;
  }
  public boolean scope(Resource resource, Object entity) {
    try {
      ErgonTenantAccess.assertAllowed(entity, tenant, em,
          resource == Resource.CAMPOS_CUSTOMIZADOS || resource == Resource.PARAMETROS_CONTAGEM_FERIAS);
      return true;
    } catch (org.springframework.web.server.ResponseStatusException denied) {
      if (denied.getStatusCode().value() == 403) return false;
      throw denied;
    }
  }

  public void validate(Object dto) {
    var violations=validator.validate(dto);
    if(violations.stream().anyMatch(v->v.getPropertyPath().toString().equals("jornadaSemanal")))
      throw new BulkInputViolation("JORNADA_FORA_INTERVALO");
    if (!violations.isEmpty()) throw new IllegalArgumentException("Parâmetros incompatíveis com o cadastro.");
  }






  public Object commandCandidate(Object current, String operation, JsonNode parameters) {
    if (current instanceof DesignacaoFuncao source) {
      var input = json.convertValue(parameters, HomologarDesignacaoLoteDTO.class);
      validate(input);
      return designacaoFuncaoService.previewHomologacao(source, input);
    }
    if(current instanceof AverbacaoTempoServico source) {
      var copy = new AverbacaoTempoServico(); BeanUtils.copyProperties(source,copy);
      var input=json.convertValue(parameters,TempoLicencaTransicaoDTO.class);validate(input);
      TempoLicencaLifecycle.transition(copy,"homologar-averbacao",input,tenant,em,invariants.ruleEngine());
      averbacaoTempoServicoService.validateBulkCandidate(copy,source);
      return copy;
    }
    if(current instanceof PeriodoAquisitivoLicencaEspecial source) {
      var copy = new PeriodoAquisitivoLicencaEspecial(); BeanUtils.copyProperties(source,copy);
      var input=json.convertValue(parameters,TempoLicencaTransicaoDTO.class);validate(input);
      TempoLicencaLifecycle.transition(copy,"recalcular-saldo-licenca-especial",input,tenant,em,invariants.ruleEngine());
      periodoAquisitivoLicencaEspecialService.validateBulkCandidate(copy,source);
      return copy;
    }
    if(current instanceof LicencaEspecialGozo source) {
      var copy = new LicencaEspecialGozo(); BeanUtils.copyProperties(source,copy);
      var input=json.convertValue(parameters,TempoLicencaTransicaoDTO.class);validate(input);
      TempoLicencaLifecycle.transition(copy,"homologar-licenca-especial",input,tenant,em,invariants.ruleEngine());
      licencaEspecialGozoService.validateBulkCandidate(copy,source);
      return copy;
    }
    if(current instanceof VendaLicencaEspecial source) {
      var copy = new VendaLicencaEspecial(); BeanUtils.copyProperties(source,copy);
      var input=json.convertValue(parameters,TempoLicencaTransicaoDTO.class);validate(input);
      TempoLicencaLifecycle.transition(copy,"autorizar-venda-licenca-especial",input,tenant,em,invariants.ruleEngine());
      vendaLicencaEspecialService.validateBulkCandidate(copy,source);
      return copy;
    }

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
      invariants.parametroContagemFerias(copy, false, false);
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
      invariants.periodoAquisitivoFerias(copy, false, false);
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
      invariants.feriasGozo(copy, false, false);
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
      invariants.frequencia(copy, false, false); return copy;
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
      invariants.afastamento(copy, false, false, false); return copy;
    }
    if (current instanceof Dependente dep) {
      Dependente copy=new Dependente(); BeanUtils.copyProperties(dep,copy);
      var dto=json.convertValue(parameters,EncerrarDependenciaLoteDTO.class); validate(dto);
      if (copy.getDtfim()!=null) throw new IllegalArgumentException("Dependência já encerrada.");
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
  private void synchronizeTempo(Long id) {
    // Service delegates to the same parent lock and recalculation used by CRUD.
    periodoAquisitivoLicencaEspecialService.synchronizeAccounting(id);
  }
  public boolean applyCommand(Object entity, Object candidate) {
    if (entity instanceof DesignacaoFuncao current && candidate instanceof DesignacaoFuncao next) {
      designacaoFuncaoService.applyHomologacao(current, new HomologarDesignacaoLoteDTO(next.getDtExercicio()));
      return true;
    }
    if(entity instanceof AssentamentoTempoLicenca current && candidate instanceof AssentamentoTempoLicenca next) {
      boolean changed=!json.valueToTree(mapperForTempo(current).toResponse(current)).equals(json.valueToTree(mapperForTempo(next).toResponse(next)));
      BeanUtils.copyProperties(next,current,"id","version","auditoria","orgao","pessoa","vinculo","documentoLegal","atoPublicacao","averbacaoOrigem","periodoAquisitivoLicenca");
      em.flush();
      if(current instanceof AverbacaoTempoServico a) averbacaoTempoServicoService.synchronizeOrigin(a);
      if(current instanceof LicencaEspecialGozo g) licencaEspecialGozoService.synchronizeAbono(g);
      if(current instanceof ParcelaLicencaEspecial t) synchronizeTempo(t.getPeriodoAquisitivoLicencaId());
      return changed;
    }
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
  private ResourceMapper mapperForTempo(AssentamentoTempoLicenca e) {
    if(e instanceof AverbacaoTempoServico) return averbacaoTempoServico;
    if(e instanceof PeriodoAquisitivoLicencaEspecial) return periodoAquisitivoLicencaEspecial;
    if(e instanceof LicencaEspecialGozo) return licencaEspecialGozo;
    if(e instanceof VendaLicencaEspecial) return vendaLicencaEspecial;
    throw new IllegalArgumentException("Tipo desconhecido.");
  }
}
