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
    if (Boolean.TRUE.equals(c.getControleVaga())
        && (c.getVagasAutorizadas() == null || c.getVagasAutorizadas() <= 0))
      fail("Controle de vagas exige quantidade autorizada positiva.");
    if (c.getVagasAutorizadas() != null && c.getVagasAutorizadas() < ocupadas(c.getCargo(), null))
      fail("Vagas autorizadas inferiores à ocupação existente.");
    if (c.getCargoAglutinador() != null) {
      if (c.getCargoAglutinador().equals(c.getCargo()))
        fail("Cargo não pode aglutinar a si próprio.");
      Cargo parent = em.find(Cargo.class, c.getCargoAglutinador());
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
