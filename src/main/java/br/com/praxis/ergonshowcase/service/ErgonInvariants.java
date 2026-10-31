package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Invariantes cadastrais e de provimento sobre o schema Ergon existente. */
@Component
public class ErgonInvariants {
  @PersistenceContext private EntityManager em;

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
    if (!blank(d.getTipodepen())) {
      TipoDependente tipo = em.find(TipoDependente.class, d.getTipodepen());
      if (tipo == null) fail("Modalidade de tipo de dependência inexistente.");
      if (Boolean.TRUE.equals(d.getDepIr()) && !Boolean.TRUE.equals(tipo.getGeraIr()))
        fail("A modalidade de dependência selecionada não permite dedução de IRRF.");
      if (Boolean.TRUE.equals(d.getDepSalarioFamilia())
          && !Boolean.TRUE.equals(tipo.getGeraSalarioFamilia()))
        fail("A modalidade de dependência selecionada não gera cota de salário-família.");
    }
    if (d.getDocumentoLegalId() != null
        && em.find(DocumentoLegal.class, d.getDocumentoLegalId()) == null)
      fail("Documento comprobatório inexistente.");
    if (d.getDtfim() != null
        && (Boolean.TRUE.equals(d.getDepIr())
            || Boolean.TRUE.equals(d.getDepSalarioFamilia())
            || Boolean.TRUE.equals(d.getDepPlanoSaude())))
      fail("Dependência encerrada não pode manter benefícios ativos de IRRF, salário-família ou saúde.");
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
  }

  public void campoCustomizado(DefinicaoCampoCustomizado d, boolean creating) {
    if (d.getEmpCodigo() == null || blank(d.getResourceKey()) || blank(d.getFieldKey()))
      fail("Órgão, recurso alvo e chave técnica são obrigatórios para o campo customizado.");
    Orgao orgao = em.find(Orgao.class, d.getEmpCodigo(), LockModeType.PESSIMISTIC_READ);
    if (orgao == null) fail("Órgão inexistente para a definição de campo customizado.");
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
    var duplicados =
        em.createQuery(
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
          "Já existe definição de campo customizado para o mesmo órgão, recurso e chave técnica.");
    }
  }

  public void vinculo(Vinculo v, boolean creating) {
    if (creating) {
      Orgao orgao = em.find(Orgao.class, v.getEmpCodigo(), LockModeType.PESSIMISTIC_READ);
      if (orgao == null || !Boolean.TRUE.equals(orgao.getAtiva()))
        fail("Nova admissão exige órgão ativo.");
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
    setor(v.getSetor(), v.getEmpCodigo());
    setor(v.getSetorExercicio(), v.getEmpCodigo());
    if (v.getCargoComissao() != null)
      validarProvimento(v, checkCargo(v.getCargoComissao(), v.getEmpCodigo()));
    if (v.getCargo() != null) {
      Cargo c = checkCargo(v.getCargo(), v.getEmpCodigo());
      validarProvimento(v, c);
    }
    if ("ATIVO".equals(v.getSituacao())) {
      var others =
          em.createQuery(
                  "select v from Vinculo v where v.id.numfunc=:p and v.id.numero<>:n and v.situacao='ATIVO'",
                  Vinculo.class)
              .setParameter("p", v.getId().numfunc())
              .setParameter("n", v.getId().numero())
              .setFlushMode(FlushModeType.COMMIT)
              .getResultList();
      if (!others.isEmpty()
          && (others.size() >= 2
              || !acumulavel(v.getCategoria())
              || others.stream().anyMatch(x -> !acumulavel(x.getCategoria()))))
        fail("Acumulação limitada a dois vínculos nas categorias MAGISTERIO ou SAUDE.");
    }
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

  private void setor(String id, Integer orgao) {
    if (id == null) return;
    Setor s = em.find(Setor.class, id);
    if (s == null || !Objects.equals(s.getEmpCodigo(), orgao))
      fail("Unidade de lotação/exercício pertence a outro órgão ou não existe.");
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
