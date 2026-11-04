package br.com.praxis.ergonshowcase.service;


import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.OperationalRuleSet;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Invariantes cadastrais e de provimento sobre o schema Ergon existente. */
@Component
public class ErgonInvariants {
  @org.springframework.beans.factory.annotation.Autowired private VinculoAccumulationFacts accumulationFacts;
  @PersistenceContext private EntityManager em;

  @Autowired(required = false)
  private ErgonRuleEngineFacade ruleEngineFacade;

  @Autowired @org.springframework.context.annotation.Lazy
  private AfastamentoService afastamentoService;

  @Autowired @org.springframework.context.annotation.Lazy
  private FrequenciaService frequenciaService;

  @Autowired @org.springframework.context.annotation.Lazy
  private PeriodoAquisitivoFeriasService periodoAquisitivoFeriasService;

  @Autowired @org.springframework.context.annotation.Lazy
  private FeriasGozoService feriasGozoService;

  @Autowired(required = false)
  private TenantContext tenant;

  public ErgonRuleEngineFacade ruleEngine() {
    if (ruleEngineFacade == null) {
      ruleEngineFacade = new ErgonRuleEngineFacade();
    }
    return ruleEngineFacade;
  }

  public long ocupadas(Integer codigo, VinculoId excluir, Integer org) {
    return em.createQuery(
            "select count(v) from Vinculo v where v.empCodigo=:org and (v.cargo=:cargo or v.cargoComissao=:cargo) and v.situacao in ('ATIVO','AFASTADO','CEDIDO') and (:pessoa is null or v.id.numfunc<>:pessoa or v.id.numero<>:numero)",
            Long.class)
        .setParameter("org", org)
        .setParameter("cargo", codigo)
        .setParameter("pessoa", excluir == null ? null : excluir.numfunc())
        .setParameter("numero", excluir == null ? null : excluir.numero())
        .setFlushMode(FlushModeType.COMMIT)
        .getSingleResult();
  }

  public void cargo(Cargo c) {
    cargo(c, false);
  }

  public void cargo(Cargo c, boolean extinguindo) {
    Cargo parent = null;
    if (c.getCargoAglutinador() != null && !Objects.equals(c.getCargoAglutinador(), c.getCargo())) {
      // O compartilhamento do lock impede desabilitação concorrente do cargo pai.
      parent = em.find(Cargo.class, c.getCargoAglutinador(), LockModeType.PESSIMISTIC_READ);
      if (parent == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo aglutinador inexistente.");
      em.refresh(parent, LockModeType.PESSIMISTIC_READ);
      ErgonTenantAccess.requireReferencesInOrganization(c.getEmpCodigo(), parent);
    }
    if (c.getDocumentoLegalId() != null) {
      DocumentoLegal documento = em.find(DocumentoLegal.class, c.getDocumentoLegalId());
      if (documento == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento legal inexistente.");
      ErgonTenantAccess.requireReferencesInOrganization(c.getEmpCodigo(), documento);
    }
    long dependentes = em.createQuery(
        "select count(c) from Cargo c where c.empCodigo=:org and c.cargoAglutinador=:pai", Long.class)
        .setParameter("org", c.getEmpCodigo()).setParameter("pai", c.getCargo())
        .setFlushMode(FlushModeType.COMMIT).getSingleResult();
    ruleEngine().enforceCargoQuadro(c, parent, ocupadas(c.getCargo(), null, c.getEmpCodigo()),
        dependentes, extinguindo);
  }

  public void pessoa(ServidorPessoa p) {
    long duplicates = em.createQuery(
        "select count(p) from ServidorPessoa p where p.cpf=:cpf and p.numero<>:id", Long.class)
        .setParameter("cpf", p.getCpf()).setParameter("id", p.getNumero())
        .setFlushMode(FlushModeType.COMMIT).getSingleResult();
    ruleEngine().enforceServidorPessoaCadastro(p, tenant.orgao(), duplicates,
        LocalDate.now(java.time.ZoneId.of(ErgonRuleEngineFacade.DEFAULT_TIME_ZONE)));
  }

  public void orgao(Orgao o) {
    if (em.createQuery(
                "select count(o) from Orgao o where o.cnpj=:cnpj and o.empresa<>:id", Long.class)
            .setParameter("cnpj", o.getCnpj())
            .setParameter("id", o.getEmpresa())
            .setFlushMode(FlushModeType.COMMIT)
            .getSingleResult()
        > 0)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "CNPJ já utilizado por outro órgão.");
  }

  public void dependente(Dependente d, boolean creating) {
    dependente(d, creating, true);
  }

  public void dependente(Dependente d, boolean creating, boolean runRuleEngine) {
    if (d.getId() == null || d.getId().numfunc() == null || d.getId().numdep() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identidade composta (numfunc, numdep) é obrigatória para o dependente.");
    ServidorPessoa titular =
        em.find(ServidorPessoa.class, d.getId().numfunc(), LockModeType.PESSIMISTIC_READ);
    if (titular == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Servidor titular inexistente para o dependente.");
    TipoDependente tipo = null;
    if (!blank(d.getTipodepen())) {
      tipo = em.find(TipoDependente.class, d.getTipodepen());
      if (tipo == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modalidade de tipo de dependência inexistente.");
    }
    DocumentoLegal doc = null;
    if (d.getDocumentoLegalId() != null) {
      doc = em.find(DocumentoLegal.class, d.getDocumentoLegalId());
      if (doc == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento comprobatório inexistente.");
      ErgonTenantAccess.requireReferencesInOrganization(tenant != null ? tenant.orgao() : 1, doc);
    }
    Integer emp = tenant != null ? tenant.orgao() : 1;
    if (runRuleEngine) {
      List<Dependente> existentes = em.createQuery(
              "select x from Dependente x where x.id.numfunc=:f and x.id.numdep<>:n",
              Dependente.class)
          .setParameter("f", d.getId().numfunc())
          .setParameter("n", d.getId().numdep())
          .setFlushMode(FlushModeType.COMMIT)
          .getResultList();
      ruleEngine()
          .enforceDependente(
              new ErgonRuleEngineFacade.DependenteRuleContext(
                  d, emp, titular, tipo, doc, existentes, null));
    }
  }

  public void afastamento(Afastamento a, boolean creating) {
    afastamento(a, creating, false, true);
  }

  public void afastamento(Afastamento a, boolean creating, boolean inPlaceProrrogacao) {
    afastamento(a, creating, inPlaceProrrogacao, true);
  }

  public void afastamento(
      Afastamento a, boolean creating, boolean inPlaceProrrogacao, boolean runRuleEngine) {
    if (a.getNumfunc() == null || a.getNumvinc() == null || a.getEmpCodigo() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Órgão, servidor e vínculo são obrigatórios para o registro de afastamento.");
    Vinculo titular = em.find(Vinculo.class, new VinculoId(a.getNumfunc(), a.getNumvinc()));
    if (titular == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo funcional inexistente.");
    ErgonTenantAccess.requireReferencesInOrganization(a.getEmpCodigo(), titular);
    ServidorPessoa pessoa =
        em.find(ServidorPessoa.class, a.getNumfunc(), LockModeType.PESSIMISTIC_READ);
    if (pessoa == null) {
      pessoa = em.find(ServidorPessoa.class, a.getNumfunc());
    }
    if (pessoa == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Servidor titular inexistente para o afastamento.");
    if (blank(a.getTipofreq()) || a.getCodfreq() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo e código de frequência são obrigatórios para o afastamento.");
    CodigoFrequencia codigo =
        em.find(CodigoFrequencia.class, new CodigoFrequenciaId(a.getTipofreq(), a.getCodfreq()));
    if (codigo == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "A modalidade de frequência selecionada não existe.");
    if (a.getAfastamentoOrigemId() != null) {
      Afastamento origem = em.find(Afastamento.class, a.getAfastamentoOrigemId());
      if (origem == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Afastamento de origem inexistente.");
      ErgonTenantAccess.requireReferencesInOrganization(a.getEmpCodigo(), origem);
    }
    if (a.getNumfuncSubstituto() != null && a.getNumvincSubstituto() != null
        && !Objects.equals(a.getNumfuncSubstituto(), a.getNumfunc())) {
      Vinculo substituto = em.find(Vinculo.class,
          new VinculoId(a.getNumfuncSubstituto(), a.getNumvincSubstituto()));
      if (substituto == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo substituto inexistente.");
      ErgonTenantAccess.requireReferencesInOrganization(a.getEmpCodigo(), substituto);
    }
    if (a.getDocumentoLegalId() != null) {
      DocumentoLegal doc = em.find(DocumentoLegal.class, a.getDocumentoLegalId());
      if (doc == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Portaria/laudo legal inexistente.");
      ErgonTenantAccess.requireReferencesInOrganization(a.getEmpCodigo(), doc);
    }
    if (a.getAtoPublicacaoId() != null) {
      DocumentoLegal pub = em.find(DocumentoLegal.class, a.getAtoPublicacaoId());
      if (pub == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ato de publicação oficial inexistente.");
      ErgonTenantAccess.requireReferencesInOrganization(a.getEmpCodigo(), pub);
    }
    if (runRuleEngine) {
      afastamentoService.validateAfastamentoConcessao(a, inPlaceProrrogacao);
    }
  }

  public void frequencia(Frequencia f, boolean creating) {
    frequencia(f, creating, true);
  }

  public void frequencia(Frequencia f, boolean creating, boolean runRuleEngine) {
    if (f.getNumfunc() == null || f.getNumvinc() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Servidor e vínculo funcional são obrigatórios para a ocorrência de frequência.");
    Vinculo vinculo =
        em.find(
            Vinculo.class,
            new VinculoId(f.getNumfunc(), f.getNumvinc()),
            LockModeType.PESSIMISTIC_READ);
    if (vinculo == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo funcional inexistente.");
    ErgonTenantAccess.requireReferencesInOrganization(f.getEmpCodigo(), vinculo);
    ServidorPessoa pessoa =
        em.find(ServidorPessoa.class, f.getNumfunc(), LockModeType.PESSIMISTIC_READ);
    if (pessoa == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa servidora titular inexistente.");
    if (blank(f.getTipofreq()) || f.getCodfreq() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo e código de frequência são obrigatórios.");
    CodigoFrequencia codigo =
        em.find(CodigoFrequencia.class, new CodigoFrequenciaId(f.getTipofreq(), f.getCodfreq()));
    if (codigo == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "O código de frequência selecionado não existe.");
    if (f.getDocumentoLegalId() != null) {
      DocumentoLegal doc = em.find(DocumentoLegal.class, f.getDocumentoLegalId());
      if (doc == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento comprobatório inexistente.");
      ErgonTenantAccess.requireReferencesInOrganization(f.getEmpCodigo(), doc);
    }
    if (runRuleEngine) {
      frequenciaService.validateFrequenciaLancamento(f);
    }
  }

  public void campoCustomizado(DefinicaoCampoCustomizado d, boolean creating) {
    if (blank(d.getResourceKey()) || blank(d.getFieldKey()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recurso alvo e chave técnica são obrigatórios para o campo customizado.");
    if (d.getEmpCodigo() != null) {
      Orgao orgao = em.find(Orgao.class, d.getEmpCodigo(), LockModeType.PESSIMISTIC_READ);
      if (orgao == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Órgão inexistente para a definição de campo customizado.");
    }
    if (blank(d.getFieldType())
        || !Set.of("STRING", "INTEGER", "NUMBER", "BOOLEAN", "DATE", "SELECT").contains(d.getFieldType()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de dado inválido para o campo customizado.");
    if (blank(d.getControlType()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Controle visual é obrigatório para a definição de campo customizado.");
    ruleEngine().enforceOperational(OperationalRuleSet.CAMPO_SELECT, d.getEmpCodigo(), d.getFieldType(), OperationalRuleSet.text(d.getOptionTabela()));
    ruleEngine().enforceOperational(OperationalRuleSet.CAMPO_ORDEM, d.getEmpCodigo(), d.getFieldOrder());
    if (!blank(d.getOptionTabela())) {
      Tabela tab = em.find(Tabela.class, d.getOptionTabela());
      if (tab == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tabela geral de opções (HADES.TABELA) inexistente: " + d.getOptionTabela());
    }
    ruleEngine().validateCustomFieldValidationRules(d.getValidationRules());
    var duplicados =
        d.getEmpCodigo() == null
            ? em.createQuery(
                    "select x from DefinicaoCampoCustomizado x where x.empCodigo is null and x.resourceKey=:r and x.fieldKey=:k",
                    DefinicaoCampoCustomizado.class)
                .setParameter("r", d.getResourceKey())
                .setParameter("k", d.getFieldKey())
                .setFlushMode(FlushModeType.COMMIT)
                .getResultList()
            : em.createQuery(
                    "select x from DefinicaoCampoCustomizado x where x.empCodigo=:e and x.resourceKey=:r and x.fieldKey=:k",
                    DefinicaoCampoCustomizado.class)
                .setParameter("e", d.getEmpCodigo())
                .setParameter("r", d.getResourceKey())
                .setParameter("k", d.getFieldKey())
                .setFlushMode(FlushModeType.COMMIT)
                .getResultList();
    for (DefinicaoCampoCustomizado other : duplicados) {
      if (d.getId() != null && Objects.equals(d.getId(), other.getId())) continue;
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Já existe definição de campo customizado para o mesmo escopo, recurso e chave técnica.");
    }
  }

  public void parametroContagemFerias(ParametroContagemFerias p, boolean creating) {
    parametroContagemFerias(p, creating, true);
  }

  public void parametroContagemFerias(
      ParametroContagemFerias p, boolean creating, boolean runRuleEngine) {
    if (blank(p.getRegimeJuridico()) || blank(p.getTipoVinculo()) || blank(p.getCategoria()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Regime jurídico, tipo de vínculo e categoria são obrigatórios na regra de férias.");
    if (p.getDtini() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de início de vigência é obrigatória na regra de contagem de férias.");
    if (p.getEmpCodigo() != null) {
      Orgao orgao = em.find(Orgao.class, p.getEmpCodigo(), LockModeType.PESSIMISTIC_READ);
      if (orgao == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Órgão público inexistente para a regra de contagem de férias.");
    }
    if (p.getDocumentoLegalFeriasId() != null) {
      DocumentoLegal doc = em.find(DocumentoLegal.class, p.getDocumentoLegalFeriasId());
      if (doc == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ato legal de férias inexistente.");
      if (p.getEmpCodigo() != null) ErgonTenantAccess.requireReferencesInOrganization(p.getEmpCodigo(), doc);
    }
    if (p.getDocumentoLegalProgressaoId() != null) {
      DocumentoLegal doc = em.find(DocumentoLegal.class, p.getDocumentoLegalProgressaoId());
      if (doc == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ato legal de progressão inexistente.");
      if (p.getEmpCodigo() != null) ErgonTenantAccess.requireReferencesInOrganization(p.getEmpCodigo(), doc);
    }
    List<ParametroContagemFerias> candidatos = List.of();
    if (!Boolean.FALSE.equals(p.getActive())) {
      var q =
          em.createQuery(
              "select x from ParametroContagemFerias x where x.active=true and x.regimeJuridico=:rj and x.tipoVinculo=:tv and x.categoria=:cat",
              ParametroContagemFerias.class);
      if (q != null) {
        var r1 = q.setParameter("rj", p.getRegimeJuridico());
        var r2 = r1 != null ? r1.setParameter("tv", p.getTipoVinculo()) : null;
        var r3 = r2 != null ? r2.setParameter("cat", p.getCategoria()) : null;
        var flushed = r3 != null ? r3.setFlushMode(FlushModeType.COMMIT) : null;
        var rows =
            (flushed != null ? flushed : r3) != null
                ? (flushed != null ? flushed : r3).getResultList()
                : List.<ParametroContagemFerias>of();
        candidatos = rows != null ? rows : List.of();
        LocalDate startA = p.getDtini();
        LocalDate endA = p.getDtfim() != null ? p.getDtfim() : LocalDate.MAX;
        for (ParametroContagemFerias other : candidatos) {
          if (p.getId() != null && Objects.equals(p.getId(), other.getId())) continue;
          if (!Objects.equals(p.getEmpCodigo(), other.getEmpCodigo())) continue;
          if (!Objects.equals(p.getSubcategoria(), other.getSubcategoria())) continue;
          if (other.getDtini() == null) continue;
          LocalDate startB = other.getDtini();
          LocalDate endB = other.getDtfim() != null ? other.getDtfim() : LocalDate.MAX;
          if (!startA.isAfter(endB) && !startB.isAfter(endA)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Já existe regra ativa de contagem de férias com vigência sobreposta para o mesmo escopo estatutário.");
          }
        }
      }
    }
    if (runRuleEngine) {
      ruleEngine()
          .enforceParametroContagemFerias(
              new ErgonRuleEngineFacade.ParametroContagemFeriasRuleContext(p, candidatos));
    }
  }

  public void periodoAquisitivoFerias(PeriodoAquisitivoFerias pa, boolean creating) {
    periodoAquisitivoFerias(pa, creating, true);
  }

  public void periodoAquisitivoFerias(
      PeriodoAquisitivoFerias pa, boolean creating, boolean runRuleEngine) {
    if (pa.getEmpCodigo() == null
        || pa.getNumfunc() == null
        || pa.getNumvinc() == null
        || pa.getChave() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Órgão, servidor, vínculo e chave sequencial são obrigatórios para o período aquisitivo.");
    Vinculo vinculo =
        em.find(
            Vinculo.class,
            new VinculoId(pa.getNumfunc(), pa.getNumvinc()),
            LockModeType.PESSIMISTIC_READ);
    if (vinculo == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo funcional inexistente.");
    ErgonTenantAccess.requireReferencesInOrganization(pa.getEmpCodigo(), vinculo);
    ServidorPessoa pessoa =
        em.find(ServidorPessoa.class, pa.getNumfunc(), LockModeType.PESSIMISTIC_READ);
    if (pessoa == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Servidor titular inexistente para o período aquisitivo.");
    if (pa.getDocumentoLegalId() != null) {
      DocumentoLegal doc = em.find(DocumentoLegal.class, pa.getDocumentoLegalId());
      if (doc == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento comprobatório inexistente.");
      ErgonTenantAccess.requireReferencesInOrganization(pa.getEmpCodigo(), doc);
    }
    if (pa.getAtoPublicacaoId() != null) {
      DocumentoLegal pub = em.find(DocumentoLegal.class, pa.getAtoPublicacaoId());
      if (pub == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ato de publicação inexistente.");
      ErgonTenantAccess.requireReferencesInOrganization(pa.getEmpCodigo(), pub);
    }
    if (runRuleEngine) periodoAquisitivoFeriasService.validatePeriodoAquisitivoFerias(pa);
  }

  public void feriasGozo(FeriasGozo fg, boolean creating) {
    feriasGozo(fg, creating, true);
  }

  public void feriasGozo(FeriasGozo fg, boolean creating, boolean runRuleEngine) {
    if (fg.getEmpCodigo() == null
        || fg.getNumfunc() == null
        || fg.getNumvinc() == null
        || fg.getPeriodoAquisitivoId() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Órgão, servidor, vínculo e período aquisitivo são obrigatórios para a parcela de férias.");
    Vinculo vinculo =
        em.find(
            Vinculo.class,
            new VinculoId(fg.getNumfunc(), fg.getNumvinc()),
            LockModeType.PESSIMISTIC_READ);
    if (vinculo == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo funcional inexistente.");
    ErgonTenantAccess.requireReferencesInOrganization(fg.getEmpCodigo(), vinculo);
    ServidorPessoa pessoa =
        em.find(ServidorPessoa.class, fg.getNumfunc(), LockModeType.PESSIMISTIC_READ);
    if (pessoa == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Servidor titular inexistente para a parcela de férias.");
    PeriodoAquisitivoFerias pa =
        em.find(
            PeriodoAquisitivoFerias.class,
            fg.getPeriodoAquisitivoId(),
            LockModeType.PESSIMISTIC_READ);
    if (pa == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Período aquisitivo inexistente.");
    ErgonTenantAccess.requireReferencesInOrganization(fg.getEmpCodigo(), pa);
    ruleEngine().enforceOperational(OperationalRuleSet.FERIAS_VINCULO, fg.getEmpCodigo(), pa.getNumfunc(), fg.getNumfunc(), pa.getNumvinc(), fg.getNumvinc());
    if (fg.getNumfuncSubstituto() != null && fg.getNumvincSubstituto() != null) {
      Vinculo substituto = em.find(Vinculo.class,
          new VinculoId(fg.getNumfuncSubstituto(), fg.getNumvincSubstituto()),
          LockModeType.PESSIMISTIC_READ);
      if (substituto == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo substituto inexistente.");
      ErgonTenantAccess.requireReferencesInOrganization(fg.getEmpCodigo(), substituto);
    }
    if (fg.getDocumentoLegalId() != null) {
      DocumentoLegal doc = em.find(DocumentoLegal.class, fg.getDocumentoLegalId());
      if (doc == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento de concessão inexistente.");
      ErgonTenantAccess.requireReferencesInOrganization(fg.getEmpCodigo(), doc);
    }
    if (fg.getAtoPublicacaoId() != null) {
      DocumentoLegal pub = em.find(DocumentoLegal.class, fg.getAtoPublicacaoId());
      if (pub == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ato de publicação inexistente.");
      ErgonTenantAccess.requireReferencesInOrganization(fg.getEmpCodigo(), pub);
    }
    if (runRuleEngine) {
      feriasGozoService.hydrateParentPeriodoAquisitivo(fg);
      feriasGozoService.validateFeriasGozo(fg);
    }
  }

  public void vinculo(Vinculo v, boolean creating) {
    vinculo(v, creating, true);
  }

  public void vinculo(Vinculo v, boolean creating, boolean runRuleEngine) {
    Orgao orgao = null;
    if (creating) {
      orgao = em.find(Orgao.class, v.getEmpCodigo(), LockModeType.PESSIMISTIC_READ);
      if (orgao == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Órgão do vínculo inexistente.");
    } else if (v.getEmpCodigo() != null) {
      orgao = em.find(Orgao.class, v.getEmpCodigo());
      if (orgao == null) {
        orgao = new Orgao();
        orgao.setEmpresa(v.getEmpCodigo());
        orgao.setAtiva(true);
      }
    }
    ServidorPessoa pessoa =
        em.find(ServidorPessoa.class, v.getId().numfunc(), LockModeType.PESSIMISTIC_WRITE);
    if (pessoa == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa inexistente.");
    Setor lotacao = setor(v.getSetor(), v.getEmpCodigo());
    Setor exercicio = setor(v.getSetorExercicio(), v.getEmpCodigo());
    Cargo cargoComissao = null;
    long comissaoOcupadas = 0L;
    if (v.getCargoComissao() != null) {
      cargoComissao = checkCargo(v.getCargoComissao(), v.getEmpCodigo());
      comissaoOcupadas = ocupadas(cargoComissao.getCargo(), v.getId(), v.getEmpCodigo());
    }
    Cargo cargo = null;
    long cargoOcupadas = 0L;
    if (v.getCargo() != null) {
      cargo = checkCargo(v.getCargo(), v.getEmpCodigo());
      cargoOcupadas = ocupadas(cargo.getCargo(), v.getId(), v.getEmpCodigo());
    }
    long openAfastamentosCount = 0L;
    if (v.getDtvac() != null && v.getId() != null && v.getId().numfunc() != null && v.getId().numero() != null) {
      var qa =
          em.createQuery(
              "select count(a) from Afastamento a where a.empCodigo=:org and a.numfunc=:p and a.numvinc=:v and a.situacao='HOMOLOGADO' and a.dtfim is null",
              Long.class);
      if (qa != null) {
        var p1 = qa.setParameter("org", v.getEmpCodigo()).setParameter("p", v.getId().numfunc());
        var p2 = p1 != null ? p1.setParameter("v", v.getId().numero()) : null;
        var flushed = p2 != null ? p2.setFlushMode(FlushModeType.COMMIT) : null;
        Long cnt = (flushed != null ? flushed : p2) != null ? (flushed != null ? flushed : p2).getSingleResult() : 0L;
        openAfastamentosCount = cnt != null ? cnt : 0L;
      }
    }
    DocumentoLegal doc =
        v.getDocumentoLegalId() != null
            ? em.find(DocumentoLegal.class, v.getDocumentoLegalId())
            : null;
    if (v.getDocumentoLegalId() != null && doc == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento legal do vínculo inexistente.");
    ErgonTenantAccess.requireReferencesInOrganization(v.getEmpCodigo(), doc);
    if (runRuleEngine) {
      ruleEngine()
          .enforceVinculo(
              new ErgonRuleEngineFacade.VinculoRuleContext(
                  v,
                  orgao,
                  pessoa,
                  cargo,
                  cargoOcupadas,
                  cargoComissao,
                  comissaoOcupadas,
                  lotacao,
                  exercicio,
                  doc,
                  accumulationFacts == null ? null : accumulationFacts.load(em, v),
                  openAfastamentosCount,
                  creating));
    }
  }

  public void customFields(
      String resourceKey, Integer empCodigo, Map<String, Object> values, Object draftEntity) {
    customFields(
        resourceKey,
        empCodigo,
        values,
        draftEntity,
        loadActiveCustomFieldDefinitions(resourceKey, empCodigo));
  }

  public void customFields(String resourceKey, Integer empCodigo, Map<String, Object> values) {
    customFields(resourceKey, empCodigo, values, null);
  }

  public void customFields(
      String resourceKey,
      Integer empCodigo,
      Map<String, Object> values,
      Object draftEntity,
      List<DefinicaoCampoCustomizado> activeDefinitions) {
    ruleEngine()
        .evaluateCustomFieldsJsonLogic(
            resourceKey, empCodigo, values, draftEntity, activeDefinitions);
  }

  private List<DefinicaoCampoCustomizado> loadActiveCustomFieldDefinitions(
      String resourceKey, Integer empCodigo) {
    if (em == null || blank(resourceKey)) {
      return List.of();
    }
    Integer effectiveOrg = empCodigo != null ? empCodigo : 1;
    var q =
        em.createQuery(
            "select d from DefinicaoCampoCustomizado d where d.active=true and d.resourceKey=:r and (d.empCodigo is null or d.empCodigo=:o) order by d.empCodigo nulls first, d.fieldOrder",
            DefinicaoCampoCustomizado.class);
    if (q == null) {
      return List.of();
    }
    var p1 = q.setParameter("r", resourceKey);
    if (p1 == null) {
      return List.of();
    }
    var p2 = p1.setParameter("o", effectiveOrg);
    if (p2 == null) {
      return List.of();
    }
    var flushed = p2.setFlushMode(FlushModeType.COMMIT);
    List<DefinicaoCampoCustomizado> rows = (flushed != null ? flushed : p2).getResultList();
    return rows != null ? rows : List.of();
  }

  private Cargo checkCargo(Integer id, Integer orgao) {
    Cargo c = em.find(Cargo.class, id, LockModeType.PESSIMISTIC_WRITE);
    if (c == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo inexistente.");
    ErgonTenantAccess.requireReferencesInOrganization(orgao, c);
    return c;
  }

  private Setor setor(String id, Integer orgao) {
    if (id == null) return null;
    Setor s = em.find(Setor.class, id);
    if (s == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidade de lotação/exercício inexistente.");
    ErgonTenantAccess.requireReferencesInOrganization(orgao, s);
    return s;
  }

  public static boolean blank(String s) {
    return s == null || s.isBlank();
  }

}
