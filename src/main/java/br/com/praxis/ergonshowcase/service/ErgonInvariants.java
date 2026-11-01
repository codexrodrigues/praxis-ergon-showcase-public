package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
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
  @PersistenceContext private EntityManager em;

  @Autowired(required = false)
  private ErgonRuleEngineFacade ruleEngineFacade;

  @Autowired(required = false)
  private TenantContext tenant;

  public ErgonRuleEngineFacade ruleEngine() {
    if (ruleEngineFacade == null) {
      ruleEngineFacade = new ErgonRuleEngineFacade();
    }
    return ruleEngineFacade;
  }

  public long ocupadas(Integer codigo, VinculoId excluir) {
    return em.createQuery(
            "select count(v) from Vinculo v where (v.cargo=:cargo or v.cargoComissao=:cargo) and v.situacao in ('ATIVO','AFASTADO','CEDIDO') and (:pessoa is null or v.id.numfunc<>:pessoa or v.id.numero<>:numero)",
            Long.class)
        .setParameter("cargo", codigo)
        .setParameter("pessoa", excluir == null ? null : excluir.numfunc())
        .setParameter("numero", excluir == null ? null : excluir.numero())
        .setFlushMode(FlushModeType.COMMIT)
        .getSingleResult();
  }

  public void cargo(Cargo c) {
    if (!Boolean.TRUE.equals(c.getEAglutinador()) && em.createQuery(
        "select count(c) from Cargo c where c.cargoAglutinador=:pai", Long.class)
        .setParameter("pai", c.getCargo()).setFlushMode(FlushModeType.COMMIT).getSingleResult() > 0)
      fail("Cargo com dependentes não pode deixar de ser aglutinador.");
    if (Boolean.TRUE.equals(c.getControleVaga())
        && (c.getVagasAutorizadas() == null || c.getVagasAutorizadas() <= 0))
      fail("Controle de vagas exige quantidade autorizada positiva.");
    if (c.getVagasAutorizadas() != null && c.getVagasAutorizadas() < ocupadas(c.getCargo(), null))
      fail("Vagas autorizadas inferiores à ocupação existente.");
    if (c.getCargoAglutinador() != null) {
      if (c.getCargoAglutinador().equals(c.getCargo()))
        fail("Cargo não pode aglutinar a si próprio.");
      // O compartilhamento do lock com a manutenção do pai impede desabilitação concorrente.
      Cargo parent = em.find(Cargo.class, c.getCargoAglutinador(), LockModeType.PESSIMISTIC_READ);
      if (parent != null) em.refresh(parent, LockModeType.PESSIMISTIC_READ);
      if (parent == null
          || !Objects.equals(parent.getEmpCodigo(), c.getEmpCodigo())
          || !Boolean.TRUE.equals(parent.getEAglutinador()))
        fail("Cargo aglutinador deve existir no mesmo órgão e estar habilitado.");
    }
    customFields("administracao-pessoal.cargos", c.getEmpCodigo(), c.getCustomFields(), c);
  }

  public void pessoa(ServidorPessoa p) {
    long duplicates =
        em.createQuery(
                "select count(p) from ServidorPessoa p where p.cpf=:cpf and p.numero<>:id",
                Long.class)
            .setParameter("cpf", p.getCpf())
            .setParameter("id", p.getNumero())
            .setFlushMode(FlushModeType.COMMIT)
            .getSingleResult();
    if (duplicates > 0)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já utilizado por outra pessoa.");
    if (blank(p.getIdentprof()) != blank(p.getTipoidprof()))
      fail("Registro profissional exige número e tipo correspondentes.");
    Integer emp = tenant != null ? tenant.orgao() : 1;
    customFields("administracao-pessoal.funcionarios", emp, p.getCustomFields(), p);
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
    if (d.getId() == null || d.getId().numfunc() == null || d.getId().numdep() == null)
      fail("Identidade composta (numfunc, numdep) é obrigatória para o dependente.");
    ServidorPessoa titular =
        em.find(ServidorPessoa.class, d.getId().numfunc(), LockModeType.PESSIMISTIC_READ);
    if (titular == null) fail("Servidor titular inexistente para o dependente.");
    chronology(
        d.getDtnasc(),
        d.getDtini(),
        "O início da dependência não pode anteceder a data de nascimento do dependente.");
    chronology(
        d.getDtini(),
        d.getDtfim(),
        "O encerramento da dependência não pode anteceder a data de início.");
    chronology(
        d.getDtnasc(),
        d.getExpedrg(),
        "A expedição do RG do dependente não pode anteceder a data de nascimento.");
    if (blank(d.getNumrg()) != blank(d.getOrgaorg()) || blank(d.getNumrg()) != blank(d.getUfrg()))
      fail("Registro de RG do dependente exige número, órgão emissor e UF correspondentes.");
    if (blank(d.getNumCert()) != blank(d.getTipodocCert())
        || blank(d.getNumCert()) != blank(d.getCartorioCert()))
      fail("Certidão civil do dependente exige tipo, termo e cartório correspondentes.");
    if (!blank(d.getCpf())) {
      if (d.getCpf().equals(titular.getCpf()))
        fail("O CPF do dependente não pode coincidir com o CPF do servidor titular.");
      long dupCpf =
          em.createQuery(
                  "select count(x) from Dependente x where x.id.numfunc=:f and x.cpf=:cpf and x.id.numdep<>:n",
                  Long.class)
              .setParameter("f", d.getId().numfunc())
              .setParameter("cpf", d.getCpf())
              .setParameter("n", d.getId().numdep())
              .setFlushMode(FlushModeType.COMMIT)
              .getSingleResult();
      if (dupCpf > 0)
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "CPF já utilizado por outro dependente vinculado ao mesmo servidor.");
    } else if (Boolean.TRUE.equals(d.getDepIr())) {
      fail("O CPF do dependente é obrigatório quando habilitado para dedução de IRRF.");
    }
    TipoDependente tipo = null;
    if (!blank(d.getTipodepen())) {
      tipo = em.find(TipoDependente.class, d.getTipodepen());
      if (tipo == null) fail("Modalidade de tipo de dependência inexistente.");
      if (Boolean.TRUE.equals(d.getDepIr()) && !Boolean.TRUE.equals(tipo.getGeraIr()))
        fail("A modalidade de dependência selecionada não permite dedução de IRRF.");
      if (Boolean.TRUE.equals(d.getDepSalarioFamilia())
          && !Boolean.TRUE.equals(tipo.getGeraSalarioFamilia()))
        fail("A modalidade de dependência selecionada não gera cota de salário-família.");
    }
    DocumentoLegal doc = null;
    if (d.getDocumentoLegalId() != null) {
      doc = em.find(DocumentoLegal.class, d.getDocumentoLegalId());
      if (doc == null) fail("Documento comprobatório inexistente.");
    }
    if (d.getDtfim() != null
        && (Boolean.TRUE.equals(d.getDepIr())
            || Boolean.TRUE.equals(d.getDepSalarioFamilia())
            || Boolean.TRUE.equals(d.getDepPlanoSaude())))
      fail("Dependência encerrada não pode manter benefícios ativos de IRRF, salário-família ou saúde.");
    Integer emp = tenant != null ? tenant.orgao() : 1;
    ruleEngine()
        .enforceDependente(
            new ErgonRuleEngineFacade.DependenteRuleContext(
                d, emp, titular, tipo, doc, List.of(), null));
    customFields("administracao-pessoal.dependentes", emp, d.getCustomFields(), d);
  }

  public void afastamento(Afastamento a, boolean creating) {
    if (a.getNumfunc() == null || a.getNumvinc() == null || a.getEmpCodigo() == null)
      fail("Órgão, servidor e vínculo são obrigatórios para o registro de afastamento.");
    Vinculo titular =
        em.find(
            Vinculo.class,
            new VinculoId(a.getNumfunc(), a.getNumvinc()),
            LockModeType.PESSIMISTIC_READ);
    if (titular == null || !Objects.equals(titular.getEmpCodigo(), a.getEmpCodigo()))
      fail("Vínculo funcional inexistente ou pertence a outro órgão.");
    ServidorPessoa pessoa =
        em.find(ServidorPessoa.class, a.getNumfunc(), LockModeType.PESSIMISTIC_READ);
    if (pessoa == null) fail("Servidor titular inexistente para o afastamento.");
    chronology(
        titular.getDtexerc(),
        a.getDtini(),
        "O início do afastamento não pode anteceder a data de exercício do vínculo.");
    chronology(
        a.getDtini(),
        a.getDtprevfim(),
        "O término previsto do afastamento não pode anteceder a data de início.");
    chronology(
        a.getDtini(),
        a.getDtfim(),
        "O término efetivo do afastamento não pode anteceder a data de início.");
    if (blank(a.getTipofreq()) || a.getCodfreq() == null)
      fail("Tipo e código de frequência são obrigatórios para o afastamento.");
    CodigoFrequencia codigo =
        em.find(CodigoFrequencia.class, new CodigoFrequenciaId(a.getTipofreq(), a.getCodfreq()));
    if (codigo == null || !Boolean.TRUE.equals(codigo.getAfastamento()))
      fail("A modalidade de frequência selecionada não existe ou não está habilitada como licença/afastamento.");
    if (codigo.getSexo() != null
        && Set.of("F", "M").contains(codigo.getSexo())
        && !Objects.equals(codigo.getSexo(), pessoa.getSexo()))
      fail("A modalidade de licença selecionada é exclusiva para servidores do sexo " + codigo.getSexo() + ".");
    Integer dias = a.getQuantidadeDias();
    if (dias == null && a.getDtini() != null) {
      LocalDate refFim = a.getDtfim() != null ? a.getDtfim() : a.getDtprevfim();
      if (refFim != null && !refFim.isBefore(a.getDtini())) {
        dias = Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(a.getDtini(), refFim) + 1);
      }
    }
    if (dias != null) {
      var diasDec = java.math.BigDecimal.valueOf(dias);
      if (codigo.getQtdmin() != null && diasDec.compareTo(codigo.getQtdmin()) < 0)
        fail("A duração do afastamento é inferior ao mínimo regulamentar da modalidade (" + codigo.getQtdmin() + " dias).");
      if (codigo.getQtdmax() != null && diasDec.compareTo(codigo.getQtdmax()) > 0)
        fail("A duração do afastamento excede o limite máximo regulamentar da modalidade (" + codigo.getQtdmax() + " dias).");
    }
    if (Boolean.TRUE.equals(a.getProrrogacao())) {
      if (a.getAfastamentoOrigemId() == null)
        fail("Afastamento marcado como prorrogação exige informar o afastamento de origem.");
      if (a.getId() != null && Objects.equals(a.getId(), a.getAfastamentoOrigemId()))
        fail("Um afastamento não pode prorrogar a si próprio.");
      Afastamento origem = em.find(Afastamento.class, a.getAfastamentoOrigemId());
      if (origem == null
          || !Objects.equals(origem.getNumfunc(), a.getNumfunc())
          || !Objects.equals(origem.getNumvinc(), a.getNumvinc()))
        fail("O afastamento de origem deve existir e pertencer ao mesmo vínculo funcional.");
    }
    if ((a.getNumfuncSubstituto() == null) != (a.getNumvincSubstituto() == null))
      fail("A designação de substituto exige informar simultaneamente o servidor e o sequencial do vínculo substituto.");
    if (a.getNumfuncSubstituto() != null) {
      if (Objects.equals(a.getNumfuncSubstituto(), a.getNumfunc()))
        fail("O servidor afastado não pode ser designado como substituto de si próprio.");
      Vinculo subst =
          em.find(
              Vinculo.class,
              new VinculoId(a.getNumfuncSubstituto(), a.getNumvincSubstituto()),
              LockModeType.PESSIMISTIC_READ);
      if (subst == null
          || !Objects.equals(subst.getEmpCodigo(), a.getEmpCodigo())
          || !"ATIVO".equals(subst.getSituacao()))
        fail("O vínculo do servidor substituto deve existir no mesmo órgão e estar em situação ATIVO.");
    }
    if ("HOMOLOGADO".equals(a.getSituacao()) && a.getDtini() != null) {
      var existentes =
          em.createQuery(
                  "select x from Afastamento x where x.numfunc=:f and x.numvinc=:v and x.situacao='HOMOLOGADO'",
                  Afastamento.class)
              .setParameter("f", a.getNumfunc())
              .setParameter("v", a.getNumvinc())
              .setFlushMode(FlushModeType.COMMIT)
              .getResultList();
      LocalDate startA = a.getDtini();
      LocalDate endA =
          a.getDtfim() != null
              ? a.getDtfim()
              : a.getDtprevfim() != null ? a.getDtprevfim() : LocalDate.MAX;
      for (Afastamento other : existentes) {
        if (a.getId() != null && Objects.equals(a.getId(), other.getId())) continue;
        if (other.getDtini() == null) continue;
        LocalDate startB = other.getDtini();
        LocalDate endB =
            other.getDtfim() != null
                ? other.getDtfim()
                : other.getDtprevfim() != null ? other.getDtprevfim() : LocalDate.MAX;
        if (!startA.isAfter(endB) && !startB.isAfter(endA))
          throw new ResponseStatusException(
              HttpStatus.CONFLICT,
              "Já existe afastamento homologado concomitante para o mesmo vínculo no período informado.");
      }
    }
    if (a.getDocumentoLegalId() != null) {
      DocumentoLegal doc = em.find(DocumentoLegal.class, a.getDocumentoLegalId());
      if (doc == null || !Objects.equals(doc.getEmpCodigo(), a.getEmpCodigo()))
        fail("Portaria/laudo legal inexistente ou pertence a outro órgão.");
    }
    if (a.getAtoPublicacaoId() != null) {
      DocumentoLegal pub = em.find(DocumentoLegal.class, a.getAtoPublicacaoId());
      if (pub == null || !Objects.equals(pub.getEmpCodigo(), a.getEmpCodigo()))
        fail("Ato de publicação oficial inexistente ou pertence a outro órgão.");
    }
    customFields("administracao-pessoal.afastamentos", a.getEmpCodigo(), a.getCustomFields(), a);
  }

  public void frequencia(Frequencia f, boolean creating) {
    if (f.getNumfunc() == null || f.getNumvinc() == null)
      fail("Servidor e vínculo funcional são obrigatórios para a ocorrência de frequência.");
    Vinculo vinculo =
        em.find(
            Vinculo.class,
            new VinculoId(f.getNumfunc(), f.getNumvinc()),
            LockModeType.PESSIMISTIC_READ);
    if (vinculo == null || !Objects.equals(vinculo.getEmpCodigo(), f.getEmpCodigo()))
      fail("Vínculo funcional inexistente ou pertence a outro órgão.");
    ServidorPessoa pessoa =
        em.find(ServidorPessoa.class, f.getNumfunc(), LockModeType.PESSIMISTIC_READ);
    if (pessoa == null) fail("Pessoa servidora titular inexistente.");
    chronology(
        vinculo.getDtexerc(),
        f.getDtini(),
        "A data de início da ocorrência de frequência não pode anteceder o início de exercício do vínculo.");
    chronology(
        f.getDtini(),
        f.getDtfim(),
        "A data final da ocorrência de frequência não pode anteceder a data de início.");
    if (f.getHoraEntrada() != null
        && f.getHoraSaida() != null
        && f.getHoraSaida().isBefore(f.getHoraEntrada()))
      fail("A hora de saída não pode anteceder a hora de entrada na mesma ocorrência.");
    if (blank(f.getTipofreq()) || f.getCodfreq() == null)
      fail("Tipo e código de frequência são obrigatórios.");
    CodigoFrequencia codigo =
        em.find(CodigoFrequencia.class, new CodigoFrequenciaId(f.getTipofreq(), f.getCodfreq()));
    if (codigo == null || !Boolean.TRUE.equals(codigo.getViaFrequencia()))
      fail("O código de frequência selecionado não existe ou não permite lançamento via frequência operacional.");
    if (Boolean.TRUE.equals(codigo.getHoraEntradaSaida())
        && (f.getHoraEntrada() == null || f.getHoraSaida() == null))
      fail("O código de frequência selecionado exige o preenchimento de hora de entrada e hora de saída.");
    if (codigo.getSexo() != null
        && Set.of("F", "M").contains(codigo.getSexo())
        && !Objects.equals(codigo.getSexo(), pessoa.getSexo()))
      fail("O código de frequência selecionado é exclusivo para servidores do sexo " + codigo.getSexo() + ".");
    if (f.getQuantidade() != null) {
      if (f.getQuantidade().signum() <= 0)
        fail("A quantidade apurada na frequência deve ser estritamente positiva.");
      if (codigo.getQtdmin() != null && f.getQuantidade().compareTo(codigo.getQtdmin()) < 0)
        fail("A quantidade informada é inferior ao mínimo permitido pelo código de frequência.");
      if (codigo.getQtdmax() != null && f.getQuantidade().compareTo(codigo.getQtdmax()) > 0)
        fail("A quantidade informada excede o limite máximo permitido pelo código de frequência.");
    }
    if (f.getDtini() != null && !"CANCELADO".equals(f.getSituacao())) {
      var duplicadas =
          em.createQuery(
                  "select x from Frequencia x where x.numfunc=:p and x.numvinc=:v and x.dtini=:d and x.tipofreq=:t and x.situacao<>'CANCELADO'",
                  Frequencia.class)
              .setParameter("p", f.getNumfunc())
              .setParameter("v", f.getNumvinc())
              .setParameter("d", f.getDtini())
              .setParameter("t", f.getTipofreq())
              .setFlushMode(FlushModeType.COMMIT)
              .getResultList();
      for (Frequencia other : duplicadas) {
        if (f.getId() != null && Objects.equals(f.getId(), other.getId())) continue;
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Já existe ocorrência ativa para o mesmo vínculo, data de início e tipo de frequência.");
      }
    }
    if (f.getDocumentoLegalId() != null) {
      DocumentoLegal doc = em.find(DocumentoLegal.class, f.getDocumentoLegalId());
      if (doc == null || !Objects.equals(doc.getEmpCodigo(), f.getEmpCodigo()))
        fail("Documento comprobatório inexistente ou pertence a outro órgão.");
    }
    customFields("administracao-pessoal.frequencias", f.getEmpCodigo(), f.getCustomFields(), f);
  }

  public void campoCustomizado(DefinicaoCampoCustomizado d, boolean creating) {
    if (blank(d.getResourceKey()) || blank(d.getFieldKey()))
      fail("Recurso alvo e chave técnica são obrigatórios para o campo customizado.");
    if (d.getEmpCodigo() != null) {
      Orgao orgao = em.find(Orgao.class, d.getEmpCodigo(), LockModeType.PESSIMISTIC_READ);
      if (orgao == null) fail("Órgão inexistente para a definição de campo customizado.");
    }
    if (blank(d.getFieldType())
        || !Set.of("STRING", "INTEGER", "NUMBER", "BOOLEAN", "DATE", "SELECT")
            .contains(d.getFieldType()))
      fail(
          "Tipo de dado inválido para o campo customizado; use STRING, INTEGER, NUMBER, BOOLEAN, DATE ou SELECT.");
    if (blank(d.getControlType()))
      fail("Controle visual é obrigatório para a definição de campo customizado.");
    if ("SELECT".equals(d.getFieldType()) && blank(d.getOptionTabela()))
      fail("Campo customizado do tipo SELECT exige informar a tabela geral de opções.");
    if (d.getFieldOrder() != null && (d.getFieldOrder() < 1 || d.getFieldOrder() > 999))
      fail("A ordem no grupo do campo customizado deve estar entre 1 e 999.");
    if (!blank(d.getOptionTabela())) {
      Tabela tab = em.find(Tabela.class, d.getOptionTabela());
      if (tab == null) fail("Tabela geral de opções (HADES.TABELA) inexistente: " + d.getOptionTabela());
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
    if (blank(p.getRegimeJuridico()) || blank(p.getTipoVinculo()) || blank(p.getCategoria()))
      fail("Regime jurídico, tipo de vínculo e categoria são obrigatórios na regra de férias.");
    if (p.getDtini() == null)
      fail("Data de início de vigência é obrigatória na regra de contagem de férias.");
    if (p.getEmpCodigo() != null) {
      Orgao orgao = em.find(Orgao.class, p.getEmpCodigo(), LockModeType.PESSIMISTIC_READ);
      if (orgao == null) fail("Órgão público inexistente para a regra de contagem de férias.");
    }
    chronology(
        p.getDtini(),
        p.getDtfim(),
        "O término de vigência da regra de férias não pode anteceder a data de início (ERG-01873).");
    if (Boolean.TRUE.equals(p.getTemFerias())) {
      if (p.getTotDiasFer() == null
          || p.getTotDiasFer() <= 0
          || p.getMinPerFer() == null
          || p.getMaxPerFer() == null
          || p.getMinDiasPerFer() == null
          || p.getMaxDiasPerFer() == null)
        fail(
            "Quando o direito a férias está habilitado, total de dias, parcelas mínima/máxima e dias mínimo/máximo por parcela são obrigatórios (ERG-01800).");
      if (p.getMinPerFer() > p.getMaxPerFer())
        fail(
            "A quantidade mínima de parcelas não pode exceder a quantidade máxima de parcelas (ERG-01809).");
      if (p.getMinDiasPerFer() > p.getMaxDiasPerFer())
        fail(
            "A quantidade mínima de dias por parcela não pode exceder o máximo de dias por parcela (ERG-01810).");
      if (p.getMaxDiasPerFer() > p.getTotDiasFer())
        fail("O máximo de dias por parcela não pode exceder o total de dias de direito a férias.");
      if (p.getMinDiasPerPrincFer() != null && p.getMinDiasPerPrincFer() < p.getMinDiasPerFer())
        fail(
            "O mínimo de dias na parcela principal não pode ser inferior ao mínimo geral por parcela.");
      if (p.getAbonoDirGozo() != null && !"N".equals(p.getAbonoDirGozo())) {
        if (!Set.of("D", "G").contains(p.getAbonoDirGozo()))
          fail("Base do abono pecuniário inválida; use D, G ou N (ERG-01801).");
        if (p.getNumDiasAbonoFer() == null
            || p.getDenDiasAbonoFer() == null
            || p.getDenDiasAbonoFer() <= 0
            || p.getNumDiasAbonoFer() > p.getDenDiasAbonoFer())
          fail(
              "A fração de abono pecuniário exige numerador válido menor ou igual ao denominador positivo (ERG-01802..01805).");
        if (p.getMinDiasPvendFer() != null
            && p.getMaxDiasVendFer() != null
            && p.getMinDiasPvendFer() > p.getMaxDiasVendFer())
          fail(
              "O mínimo de dias para venda de abono pecuniário não pode exceder o máximo vendável (ERG-01807).");
        if (p.getMaxDiasVendFer() != null && p.getMaxDiasVendFer() > p.getTotDiasFer())
          fail(
              "O máximo de dias vendidos em abono pecuniário não pode exceder o total de dias de direito (ERG-01808).");
      }
    }
    if (Boolean.TRUE.equals(p.getContDobroFerias())
        && p.getMinDiasPeraqfer() != null
        && p.getMaxDiasPeraqfer() != null
        && p.getMinDiasPeraqfer() > p.getMaxDiasPeraqfer())
      fail(
          "O mínimo de dias para contagem em dobro de férias não pode exceder o máximo permitido (ERG-00606).");
    if (Boolean.TRUE.equals(p.getTemLicEsp())
        && (p.getAnosLicEsp() == null
            || p.getAnosLicEsp() <= 0
            || p.getMesesLicEspPer() == null
            || p.getMesesLicEspPer() <= 0))
      fail(
          "Licença especial habilitada exige informar anos de interstício e meses de direito positivos (ERG-01813).");
    if (p.getDocumentoLegalFeriasId() != null) {
      DocumentoLegal doc = em.find(DocumentoLegal.class, p.getDocumentoLegalFeriasId());
      if (doc == null || (p.getEmpCodigo() != null && !Objects.equals(doc.getEmpCodigo(), p.getEmpCodigo())))
        fail("Ato legal de férias inexistente ou pertence a outro órgão.");
    }
    if (p.getDocumentoLegalProgressaoId() != null) {
      DocumentoLegal doc = em.find(DocumentoLegal.class, p.getDocumentoLegalProgressaoId());
      if (doc == null || (p.getEmpCodigo() != null && !Objects.equals(doc.getEmpCodigo(), p.getEmpCodigo())))
        fail("Ato legal de progressão inexistente ou pertence a outro órgão.");
    }
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
        var candidatos =
            (flushed != null ? flushed : r3) != null
                ? (flushed != null ? flushed : r3).getResultList()
                : List.<ParametroContagemFerias>of();
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
                "Já existe regra ativa de contagem de férias com vigência sobreposta para o mesmo escopo estatutário (ERG-01874).");
          }
        }
      }
    }
    Integer emp = p.getEmpCodigo() != null ? p.getEmpCodigo() : (tenant != null ? tenant.orgao() : 1);
    customFields(
        "administracao-pessoal.parametros-contagem-ferias", emp, p.getCustomFields(), p);
  }

  public void periodoAquisitivoFerias(PeriodoAquisitivoFerias pa, boolean creating) {
    if (pa.getEmpCodigo() == null
        || pa.getNumfunc() == null
        || pa.getNumvinc() == null
        || pa.getChave() == null)
      fail(
          "Órgão, servidor, vínculo e chave sequencial são obrigatórios para o período aquisitivo.");
    Vinculo vinculo =
        em.find(
            Vinculo.class,
            new VinculoId(pa.getNumfunc(), pa.getNumvinc()),
            LockModeType.PESSIMISTIC_READ);
    if (vinculo == null || !Objects.equals(vinculo.getEmpCodigo(), pa.getEmpCodigo()))
      fail("Vínculo funcional inexistente ou pertence a outro órgão.");
    ServidorPessoa pessoa =
        em.find(ServidorPessoa.class, pa.getNumfunc(), LockModeType.PESSIMISTIC_READ);
    if (pessoa == null) fail("Servidor titular inexistente para o período aquisitivo.");
    chronology(
        vinculo.getDtexerc(),
        pa.getDtini(),
        "O início do período aquisitivo não pode anteceder a data de exercício do vínculo (ERG-01850).");
    chronology(
        pa.getDtini(),
        pa.getDtfim(),
        "O término do período aquisitivo não pode anteceder a data de início (ERG-01851).");
    chronology(
        pa.getDtfim(),
        pa.getDtPrescricao(),
        "A data de prescrição não pode anteceder o término do período aquisitivo (ERG-00592).");
    chronology(
        pa.getDtfim(),
        pa.getDtPgtoComMulta(),
        "A data limite sem multa não pode anteceder o término do período aquisitivo.");
    int direito = pa.getTotDiasDireito() != null ? pa.getTotDiasDireito() : 30;
    int gozados = pa.getDiasGozados() != null ? pa.getDiasGozados() : 0;
    int vendidos = pa.getDiasVendidosAbono() != null ? pa.getDiasVendidosAbono() : 0;
    int dobro = pa.getDiasConvertidosDobro() != null ? pa.getDiasConvertidosDobro() : 0;
    if (gozados + vendidos + dobro > direito)
      fail(
          "A soma de dias gozados, vendidos em abono e convertidos em dobro não pode exceder o total de dias de direito do período aquisitivo.");
    if (pa.getMinPerFer() != null && pa.getMaxPerFer() != null && pa.getMinPerFer() > pa.getMaxPerFer())
      fail(
          "O override individual de mínimo de parcelas não pode exceder o máximo de parcelas (ERG-01809).");
    if (pa.getMinDiasPerFer() != null
        && pa.getMaxDiasPerFer() != null
        && pa.getMinDiasPerFer() > pa.getMaxDiasPerFer())
      fail(
          "O override individual de mínimo de dias por parcela não pode exceder o máximo (ERG-01810).");
    var existentes =
        em.createQuery(
                "select x from PeriodoAquisitivoFerias x where x.numfunc=:f and x.numvinc=:v and x.chave=:c",
                PeriodoAquisitivoFerias.class)
            .setParameter("f", pa.getNumfunc())
            .setParameter("v", pa.getNumvinc())
            .setParameter("c", pa.getChave())
            .setFlushMode(FlushModeType.COMMIT)
            .getResultList();
    for (PeriodoAquisitivoFerias other : existentes) {
      if (pa.getId() != null && Objects.equals(pa.getId(), other.getId())) continue;
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Já existe período aquisitivo com a mesma chave sequencial para este vínculo funcional.");
    }
    if (pa.getDocumentoLegalId() != null) {
      DocumentoLegal doc = em.find(DocumentoLegal.class, pa.getDocumentoLegalId());
      if (doc == null || !Objects.equals(doc.getEmpCodigo(), pa.getEmpCodigo()))
        fail("Documento comprobatório inexistente ou pertence a outro órgão.");
    }
    if (pa.getAtoPublicacaoId() != null) {
      DocumentoLegal pub = em.find(DocumentoLegal.class, pa.getAtoPublicacaoId());
      if (pub == null || !Objects.equals(pub.getEmpCodigo(), pa.getEmpCodigo()))
        fail("Ato de publicação inexistente ou pertence a outro órgão.");
    }
    customFields(
        "administracao-pessoal.periodos-aquisitivos-ferias",
        pa.getEmpCodigo(),
        pa.getCustomFields(),
        pa);
  }

  public void feriasGozo(FeriasGozo fg, boolean creating) {
    if (fg.getEmpCodigo() == null
        || fg.getNumfunc() == null
        || fg.getNumvinc() == null
        || fg.getPeriodoAquisitivoId() == null)
      fail(
          "Órgão, servidor, vínculo e período aquisitivo são obrigatórios para a parcela de férias.");
    Vinculo vinculo =
        em.find(
            Vinculo.class,
            new VinculoId(fg.getNumfunc(), fg.getNumvinc()),
            LockModeType.PESSIMISTIC_READ);
    if (vinculo == null || !Objects.equals(vinculo.getEmpCodigo(), fg.getEmpCodigo()))
      fail("Vínculo funcional inexistente ou pertence a outro órgão.");
    ServidorPessoa pessoa =
        em.find(ServidorPessoa.class, fg.getNumfunc(), LockModeType.PESSIMISTIC_READ);
    if (pessoa == null) fail("Servidor titular inexistente para a parcela de férias.");
    PeriodoAquisitivoFerias pa =
        em.find(
            PeriodoAquisitivoFerias.class,
            fg.getPeriodoAquisitivoId(),
            LockModeType.PESSIMISTIC_READ);
    if (pa == null
        || !Objects.equals(pa.getEmpCodigo(), fg.getEmpCodigo())
        || !Objects.equals(pa.getNumfunc(), fg.getNumfunc())
        || !Objects.equals(pa.getNumvinc(), fg.getNumvinc()))
      fail("Período aquisitivo inexistente ou não pertence ao mesmo vínculo funcional e órgão.");
    if (fg.getChavePeraqfer() == null) {
      fg.setChavePeraqfer(pa.getChave());
    } else if (!Objects.equals(fg.getChavePeraqfer(), pa.getChave())) {
      fail("A chave do período aquisitivo informada na parcela diverge do período aquisitivo selecionado.");
    }
    if (creating && Set.of("PRESCRITO", "CANCELADO", "SUSPENSO").contains(pa.getSituacao()))
      fail("Não é permitido programar férias sobre período aquisitivo em situação " + pa.getSituacao() + ".");
    chronology(
        fg.getDtini(),
        fg.getDtprevfim(),
        "O término previsto da parcela de férias não pode anteceder a data de início (ERG-01851).");
    chronology(
        fg.getDtini(),
        fg.getDtfim(),
        "O término efetivo da parcela de férias não pode anteceder a data de início.");
    chronology(
        fg.getDtini(),
        fg.getDtInterrupcao(),
        "A data de interrupção das férias não pode anteceder a data de início do gozo.");
    ParametroContagemFerias param =
        resolveEffectiveParametroFerias(
            vinculo, pa.getDtini() != null ? pa.getDtini() : fg.getDtini());
    int maxParcelas =
        pa.getMaxPerFer() != null
            ? pa.getMaxPerFer()
            : (param != null && param.getMaxPerFer() != null ? param.getMaxPerFer() : 3);
    if (fg.getOrdemParcela() == null || fg.getOrdemParcela() < 1 || fg.getOrdemParcela() > maxParcelas)
      fail(
          "A ordem da parcela ("
              + fg.getOrdemParcela()
              + ") deve estar entre 1 e o limite máximo aplicável ao período aquisitivo ("
              + maxParcelas
              + ").");
    int minDiasParcela =
        pa.getMinDiasPerFer() != null
            ? pa.getMinDiasPerFer()
            : (param != null && param.getMinDiasPerFer() != null ? param.getMinDiasPerFer() : 5);
    int maxDiasParcela =
        pa.getMaxDiasPerFer() != null
            ? pa.getMaxDiasPerFer()
            : (param != null && param.getMaxDiasPerFer() != null ? param.getMaxDiasPerFer() : 30);
    boolean interrompida = "INTERROMPIDA".equals(fg.getSituacao());
    if (fg.getDiasGozo() == null
        || fg.getDiasGozo() <= 0
        || (!interrompida && fg.getDiasGozo() < minDiasParcela)
        || fg.getDiasGozo() > maxDiasParcela)
      fail(
          "A duração da parcela de férias ("
              + fg.getDiasGozo()
              + " dias) deve respeitar os limites aplicáveis ao período aquisitivo ("
              + minDiasParcela
              + " a "
              + maxDiasParcela
              + " dias).");
    if (fg.getDtini() != null && pa.getDtfim() != null && fg.getDtini().isBefore(pa.getDtfim())) {
      boolean proibeGozoAntesFimPa =
          "N".equalsIgnoreCase(pa.getPermiteFerEstePer())
              || ((pa.getPermiteFerEstePer() == null || "I".equalsIgnoreCase(pa.getPermiteFerEstePer()))
                  && param != null
                  && Boolean.FALSE.equals(param.getPermiteGozoAntesPaFer()));
      if (proibeGozoAntesFimPa)
        fail(
            "A regra aplicável ao período aquisitivo não permite início de gozo antes do término do período aquisitivo ("
                + pa.getDtfim()
                + ").");
    }
    if (Boolean.TRUE.equals(fg.getAbonoPecuniario())) {
      String effectiveAbonoDirGozo =
          (pa.getAbonoDirGozo() != null && !"I".equalsIgnoreCase(pa.getAbonoDirGozo()))
              ? pa.getAbonoDirGozo()
              : (param != null && param.getAbonoDirGozo() != null ? param.getAbonoDirGozo() : "D");
      if ("N".equalsIgnoreCase(effectiveAbonoDirGozo))
        fail("A regra aplicável ao período aquisitivo veda conversão em abono pecuniário.");
      if (fg.getDiasAbonoVendidos() == null || fg.getDiasAbonoVendidos() <= 0)
        fail(
            "Parcela com abono pecuniário habilitado exige quantidade estritamente positiva de dias vendidos.");
      int maxDiasVend =
          pa.getMaxDiasVendFer() != null
              ? pa.getMaxDiasVendFer()
              : (param != null && param.getMaxDiasVendFer() != null
                  ? param.getMaxDiasVendFer()
                  : 30);
      if (fg.getDiasAbonoVendidos() > maxDiasVend)
        fail(
            "A quantidade de dias vendidos em abono pecuniário ("
                + fg.getDiasAbonoVendidos()
                + ") excede o máximo aplicável ao período aquisitivo ("
                + maxDiasVend
                + " dias).");
      if (fg.getIniFimAbono() == null || !Set.of("I", "F").contains(fg.getIniFimAbono()))
        fail(
            "Parcela com abono pecuniário exige indicar a posição do abono (I = início ou F = final).");
    }
    if ((fg.getNumfuncSubstituto() == null) != (fg.getNumvincSubstituto() == null))
      fail(
          "A designação de substituto nas férias exige informar simultaneamente o servidor e o vínculo substituto (ERG-00502).");
    if (fg.getNumfuncSubstituto() != null) {
      if (Objects.equals(fg.getNumfuncSubstituto(), fg.getNumfunc()))
        fail("O servidor em gozo de férias não pode ser designado como substituto de si próprio (ERG-00503).");
      Vinculo subst =
          em.find(
              Vinculo.class,
              new VinculoId(fg.getNumfuncSubstituto(), fg.getNumvincSubstituto()),
              LockModeType.PESSIMISTIC_READ);
      if (subst == null
          || !Objects.equals(subst.getEmpCodigo(), fg.getEmpCodigo())
          || !"ATIVO".equals(subst.getSituacao()))
        fail(
            "O vínculo do servidor substituto deve existir no mesmo órgão e estar em situação ATIVO (ERG-00504).");
    }
    if (!"CANCELADA".equals(fg.getSituacao()) && fg.getDtini() != null) {
      var existentes =
          em.createQuery(
                  "select x from FeriasGozo x where x.numfunc=:f and x.numvinc=:v and x.situacao<>'CANCELADA'",
                  FeriasGozo.class)
              .setParameter("f", fg.getNumfunc())
              .setParameter("v", fg.getNumvinc())
              .setFlushMode(FlushModeType.COMMIT)
              .getResultList();
      LocalDate startA = fg.getDtini();
      LocalDate endA = fg.getDtfim() != null ? fg.getDtfim() : fg.getDtprevfim();
      int diasConsumidosPa =
          (fg.getDiasGozo() != null ? fg.getDiasGozo() : 0)
              + (Boolean.TRUE.equals(fg.getAbonoPecuniario()) && fg.getDiasAbonoVendidos() != null
                  ? fg.getDiasAbonoVendidos()
                  : 0);
      for (FeriasGozo other : existentes) {
        if (fg.getId() != null && Objects.equals(fg.getId(), other.getId())) continue;
        if (Objects.equals(fg.getPeriodoAquisitivoId(), other.getPeriodoAquisitivoId())) {
          if (Objects.equals(fg.getOrdemParcela(), other.getOrdemParcela())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Já existe parcela ativa com a mesma ordem sequencial ("
                    + fg.getOrdemParcela()
                    + ") para este período aquisitivo.");
          }
          diasConsumidosPa +=
              (other.getDiasGozo() != null ? other.getDiasGozo() : 0)
                  + (Boolean.TRUE.equals(other.getAbonoPecuniario())
                          && other.getDiasAbonoVendidos() != null
                      ? other.getDiasAbonoVendidos()
                      : 0);
        }
        if (other.getDtini() == null) continue;
        LocalDate startB = other.getDtini();
        LocalDate endB = other.getDtfim() != null ? other.getDtfim() : other.getDtprevfim();
        if (endA != null && endB != null && !startA.isAfter(endB) && !startB.isAfter(endA))
          throw new ResponseStatusException(
              HttpStatus.CONFLICT,
              "Já existe parcela de férias ativa concomitante para o mesmo vínculo no período informado.");
      }
      int tetoLiquidoPa =
          (pa.getTotDiasDireito() != null ? pa.getTotDiasDireito() : 30)
              - (pa.getDiasConvertidosDobro() != null ? pa.getDiasConvertidosDobro() : 0);
      if (diasConsumidosPa > tetoLiquidoPa)
        fail(
            "A soma de dias de gozo e abono pecuniário das parcelas ativas ("
                + diasConsumidosPa
                + " dias) excede o total líquido disponível no período aquisitivo ("
                + tetoLiquidoPa
                + " dias).");
    }
    if (fg.getDocumentoLegalId() != null) {
      DocumentoLegal doc = em.find(DocumentoLegal.class, fg.getDocumentoLegalId());
      if (doc == null || !Objects.equals(doc.getEmpCodigo(), fg.getEmpCodigo()))
        fail("Documento de concessão inexistente ou pertence a outro órgão.");
    }
    if (fg.getAtoPublicacaoId() != null) {
      DocumentoLegal pub = em.find(DocumentoLegal.class, fg.getAtoPublicacaoId());
      if (pub == null || !Objects.equals(pub.getEmpCodigo(), fg.getEmpCodigo()))
        fail("Ato de publicação inexistente ou pertence a outro órgão.");
    }
    customFields("administracao-pessoal.ferias-gozo", fg.getEmpCodigo(), fg.getCustomFields(), fg);
  }

  private ParametroContagemFerias resolveEffectiveParametroFerias(
      Vinculo vinculo, LocalDate refDate) {
    return ruleEngine().getVacationParameterResolver().resolveActiveParameter(em, vinculo, refDate);
  }

  public void vinculo(Vinculo v, boolean creating) {
    Orgao orgao = null;
    if (creating) {
      orgao = em.find(Orgao.class, v.getEmpCodigo(), LockModeType.PESSIMISTIC_READ);
      if (orgao == null || !Boolean.TRUE.equals(orgao.getAtiva()))
        fail("Nova admissão exige órgão ativo.");
    } else if (v.getEmpCodigo() != null) {
      orgao = em.find(Orgao.class, v.getEmpCodigo());
      if (orgao == null) {
        orgao = new Orgao();
        orgao.setEmpresa(v.getEmpCodigo());
        orgao.setAtiva(true);
      }
    }
    if (v.getJornadaSemanal() != null
        && (v.getJornadaSemanal() <= 0 || v.getJornadaSemanal() > 168))
      fail("Jornada semanal deve estar entre 1 e 168 horas.");
    ServidorPessoa pessoa =
        em.find(ServidorPessoa.class, v.getId().numfunc(), LockModeType.PESSIMISTIC_WRITE);
    if (pessoa == null) fail("Pessoa inexistente.");
    chronology(v.getDtnom(), v.getDtposse(), "Nomeação posterior à posse.");
    chronology(v.getDtposse(), v.getDtexerc(), "Posse posterior ao exercício.");
    chronology(v.getDtnom(), v.getDtexerc(), "Nomeação posterior ao exercício.");
    chronology(v.getDtinicontr(), v.getDtfimcontr(), "Início contratual posterior ao término.");
    chronology(
        v.getDtfimcontr(), v.getDtprorrogcontr(), "Prorrogação anterior ao término contratual.");
    chronology(
        v.getDtinicontr(), v.getDtprorrogcontr(), "Prorrogação anterior ao início contratual.");
    Setor lotacao = setor(v.getSetor(), v.getEmpCodigo());
    Setor exercicio = setor(v.getSetorExercicio(), v.getEmpCodigo());
    Cargo cargoComissao = null;
    long comissaoOcupadas = 0L;
    if (v.getCargoComissao() != null) {
      cargoComissao = checkCargo(v.getCargoComissao(), v.getEmpCodigo());
      validarProvimento(v, cargoComissao);
      comissaoOcupadas = ocupadas(cargoComissao.getCargo(), v.getId());
    }
    Cargo cargo = null;
    long cargoOcupadas = 0L;
    if (v.getCargo() != null) {
      cargo = checkCargo(v.getCargo(), v.getEmpCodigo());
      validarProvimento(v, cargo);
      cargoOcupadas = ocupadas(cargo.getCargo(), v.getId());
    }
    List<Vinculo> others = List.of();
    if ("ATIVO".equals(v.getSituacao())) {
      others =
          em.createQuery(
                  "select v from Vinculo v where v.id.numfunc=:p and v.id.numero<>:n and v.situacao='ATIVO'",
                  Vinculo.class)
              .setParameter("p", v.getId().numfunc())
              .setParameter("n", v.getId().numero())
              .setFlushMode(FlushModeType.COMMIT)
              .getResultList();
      if (!others.isEmpty()
          && (others.size() >= 2
              || others.stream()
                  .anyMatch(
                      x ->
                          !ErgonRuleEngineFacade.isConstitutionalAccumulationPair(
                              v.getCategoria(), x.getCategoria()))))
        fail("Acumulação limitada a dois vínculos nas categorias MAGISTERIO ou SAUDE.");
    }
    long openAfastamentosCount = 0L;
    if (v.getDtvac() != null && v.getId() != null && v.getId().numfunc() != null && v.getId().numero() != null) {
      var qa =
          em.createQuery(
              "select count(a) from Afastamento a where a.numfunc=:p and a.numvinc=:v and a.situacao='HOMOLOGADO' and a.dtfim is null",
              Long.class);
      if (qa != null) {
        var p1 = qa.setParameter("p", v.getId().numfunc());
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
                others,
                openAfastamentosCount,
                creating));
    customFields("administracao-pessoal.vinculos", v.getEmpCodigo(), v.getCustomFields(), v);
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

  private boolean acumulavel(String categoria) {
    return Set.of("MAGISTERIO", "SAUDE").contains(categoria);
  }

  private void validarProvimento(Vinculo v, Cargo c) {
    if (!Set.of("ATIVO", "AFASTADO", "CEDIDO").contains(v.getSituacao())) return;
    if (c.getDtExtincao() != null) fail("Provimento em cargo extinto é proibido.");
    if (Boolean.TRUE.equals(c.getControleVaga())
        && (c.getVagasAutorizadas() == null
            || ocupadas(c.getCargo(), v.getId()) >= c.getVagasAutorizadas()))
      fail("Saldo de vagas insuficiente para provimento.");
  }

  private Cargo checkCargo(Integer id, Integer orgao) {
    Cargo c = em.find(Cargo.class, id, LockModeType.PESSIMISTIC_WRITE);
    if (c == null || !Objects.equals(c.getEmpCodigo(), orgao))
      fail("Cargo pertence a outro órgão ou não existe.");
    return c;
  }

  private Setor setor(String id, Integer orgao) {
    if (id == null) return null;
    Setor s = em.find(Setor.class, id);
    if (s == null || !Objects.equals(s.getEmpCodigo(), orgao))
      fail("Unidade de lotação/exercício pertence a outro órgão ou não existe.");
    return s;
  }

  public static boolean blank(String s) {
    return s == null || s.isBlank();
  }

  public static void chronology(LocalDate a, LocalDate b, String message) {
    if (a != null && b != null && a.isAfter(b)) fail(message);
  }

  public static void fail(String message) {
    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
  }
}
