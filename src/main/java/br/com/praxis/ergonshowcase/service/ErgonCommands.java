package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import jakarta.persistence.*;
import java.util.Objects;
import org.praxisplatform.uischema.concurrency.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Comandos especializados preservam identidade, versão, organização e invariantes do assentamento.
 */
@Service
@Transactional
public class ErgonCommands {
  @PersistenceContext private EntityManager em;
  @Autowired private VinculoMapper vinculos;
  @Autowired private CargoMapper cargos;
  @Autowired private ServidorPessoaMapper pessoas;
  @Autowired private ErgonInvariants rules;
  @Autowired private TenantContext tenant;
  @Autowired private OrgaoMapper orgaos;
  @Autowired private ErgonReadFacts readFacts;

  @Autowired private AfastamentoService afastamentoService;

  public ResourceRepresentationResult<OrgaoResponseDTO> ativarOrgao(
      Integer id, AlterarSituacaoOrgaoDTO dto, ResourceVersionUpdatePrecondition<Integer> p) {
    Orgao e = locked(Orgao.class, id);
    p.requireMatch(e.getVersion());
    if (Boolean.TRUE.equals(e.getAtiva()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Órgão já ativo.");
    e.setAtiva(true);
    em.flush();
    return ResourceRepresentationResult.versioned(orgaos.toResponse(e), e.getVersion());
  }

  public ResourceRepresentationResult<OrgaoResponseDTO> inativarOrgao(
      Integer id, AlterarSituacaoOrgaoDTO dto, ResourceVersionUpdatePrecondition<Integer> p) {
    Orgao e = locked(Orgao.class, id);
    p.requireMatch(e.getVersion());
    if (!Boolean.TRUE.equals(e.getAtiva()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Órgão já inativo.");
    e.setAtiva(false);
    em.flush();
    return ResourceRepresentationResult.versioned(orgaos.toResponse(e), e.getVersion());
  }

  private void documentoDoOrgao(Long id, Integer orgao) {
    if (id == null) return;
    DocumentoLegal doc = em.find(DocumentoLegal.class, id);
    if (doc == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento legal inexistente.");
    ErgonTenantAccess.requireReferencesInOrganization(orgao, doc);
  }

  public ResourceRepresentationResult<VinculoResponseDTO> registrarAfastamento(
      VinculoId id, RegistrarAfastamentoDTO dto, ResourceVersionUpdatePrecondition<VinculoId> p) {
    Vinculo e = vinculo(id, p, null);
    if (!java.util.Set.of("ATIVO", "AFASTADO", "CEDIDO").contains(e.getSituacao()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Vínculo encerrado não recebe novo afastamento.");
    documentoDoOrgao(dto.documentoLegalId(), e.getEmpCodigo());
    documentoDoOrgao(dto.atoPublicacaoId(), e.getEmpCodigo());
    CodigoFrequencia codigo =
        em.find(CodigoFrequencia.class, new CodigoFrequenciaId(dto.tipofreq(), dto.codfreq()));
    if (codigo == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Código de frequência inexistente.");
    Afastamento a = new Afastamento();
    a.setNumfunc(id.numfunc());
    a.setNumvinc(id.numero());
    a.setEmpCodigo(e.getEmpCodigo());
    a.setDtini(dto.dtini());
    a.setDtprevfim(dto.dtprevfim());
    a.setTipofreq(dto.tipofreq());
    a.setCodfreq(dto.codfreq());
    a.setMotivo(dto.motivo());
    a.setDocumentoLegalId(dto.documentoLegalId());
    a.setAtoPublicacaoId(dto.atoPublicacaoId());
    a.setSituacao("HOMOLOGADO");
    a.setCodigoFrequenciaEntidade(codigo);
    afastamentoService.validateAfastamentoConcessao(a, false);
    em.persist(a);
    if ("ATIVO".equals(e.getSituacao())) e.setSituacao("AFASTADO");
    if (dto.documentoLegalId() != null) e.setDocumentoLegalId(dto.documentoLegalId());
    return finish(e);
  }

  public ResourceRepresentationResult<VinculoResponseDTO> encerrarAfastamento(
      VinculoId id, EncerrarAfastamentoDTO dto, ResourceVersionUpdatePrecondition<VinculoId> p) {
    Vinculo e = vinculo(id, p, null);
    if (!java.util.Set.of("AFASTADO", "CEDIDO").contains(e.getSituacao()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Estado funcional incompatível com o retorno.");
    Afastamento a = locked(Afastamento.class, dto.afastamentoId());
    if (!Objects.equals(a.getNumfunc(), id.numfunc())
        || !Objects.equals(a.getNumvinc(), id.numero())
        || !Objects.equals(a.getEmpCodigo(), e.getEmpCodigo()))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Afastamento não pertence ao vínculo selecionado.");
    if (!"HOMOLOGADO".equals(a.getSituacao()) || a.getDtfim() != null)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Afastamento não está homologado e aberto.");
    long abertos =
        em.createQuery(
                "select count(a) from Afastamento a where a.empCodigo=:org and a.numfunc=:p and a.numvinc=:v and a.situacao='HOMOLOGADO' and a.dtfim is null",
                Long.class)
            .setParameter("org", e.getEmpCodigo())
            .setParameter("p", id.numfunc())
            .setParameter("v", id.numero())
            .getSingleResult();
    documentoDoOrgao(dto.documentoLegalId(), e.getEmpCodigo());
    boolean prorrogado = AfastamentoService.prorrogadoNoMesmoRegistro(a);
    a.setDtfim(dto.dtfim());
    a.setDocumentoLegalId(dto.documentoLegalId());
    afastamentoService.validateAfastamentoConcessao(a, prorrogado);
    if (abertos == 1 && "AFASTADO".equals(e.getSituacao())) e.setSituacao("ATIVO");
    e.setDocumentoLegalId(dto.documentoLegalId());
    return finish(e);
  }

  private <E> E locked(Class<E> type, Object id) {
    E e = em.find(type, id);
    if (e == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assentamento inexistente.");
    if (e instanceof Vinculo vinculo) org(vinculo.getEmpCodigo());
    ErgonTenantAccess.assertAllowed(e, tenant, em, false);
    em.lock(e, LockModeType.PESSIMISTIC_WRITE);
    em.refresh(e);
    ErgonTenantAccess.assertAllowed(e, tenant, em, false);
    return e;
  }

  private void org(Integer id) {
    if (!Objects.equals(id, tenant.orgao())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Órgão diverge de X-Tenant.");
  }

  private Vinculo vinculo(
      VinculoId id, ResourceVersionUpdatePrecondition<VinculoId> p, String state) {
    Vinculo e = locked(Vinculo.class, id);
    p.requireMatch(e.getVersion());
    org(e.getEmpCodigo());
    if (state != null && !state.equals(e.getSituacao()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Estado funcional incompatível com o comando.");
    return e;
  }

  private ResourceRepresentationResult<VinculoResponseDTO> finish(Vinculo e) {
    rules.vinculo(e, false);
    em.flush();
    em.refresh(e);
    return ResourceRepresentationResult.versioned(
        readFacts.enrichOne(vinculos.toResponse(e)), e.getVersion());
  }

  public ResourceRepresentationResult<VinculoResponseDTO> registrarCessao(
      VinculoId id, RegistrarCessaoDTO dto, ResourceVersionUpdatePrecondition<VinculoId> p) {
    Vinculo e = vinculo(id, p, "ATIVO");
    e.setTipoReq(dto.tipoReq());
    e.setOrgaoReq(dto.orgaoReq());
    e.setFuncaoReq(dto.funcaoReq());
    e.setTipoOnusReq(dto.tipoOnusReq());
    e.setTipoRessarcReq(dto.tipoRessarcReq());
    e.setDtiniCessao(dto.dtiniCessao());
    e.setDtfimCessao(dto.dtfimCessao());
    e.setSituacao("CEDIDO");
    return finish(e);
  }

  public ResourceRepresentationResult<VinculoResponseDTO> encerrarCessao(
      VinculoId id, EncerrarCessaoDTO dto, ResourceVersionUpdatePrecondition<VinculoId> p) {
    Vinculo e = vinculo(id, p, "CEDIDO");
    e.setDtfimCessao(dto.dtfimCessao());
    e.setSituacao("ATIVO");
    return finish(e);
  }

  public ResourceRepresentationResult<VinculoResponseDTO> concederAposentadoria(
      VinculoId id, ConcederAposentadoriaDTO dto, ResourceVersionUpdatePrecondition<VinculoId> p) {
    Vinculo e = vinculo(id, p, "ATIVO");
    documentoDoOrgao(dto.documentoLegalId(), e.getEmpCodigo());
    e.setDtaposent(dto.dtaposent());
    e.setTipoapos(dto.tipoapos());
    e.setFatorProporcAposent(dto.fatorProporcAposent());
    e.setDocumentoLegalId(dto.documentoLegalId());
    e.setSituacao("APOSENTADO");
    return finish(e);
  }

  public ResourceRepresentationResult<VinculoResponseDTO> homologarEstagio(
      VinculoId id, HomologarEstagioDTO dto, ResourceVersionUpdatePrecondition<VinculoId> p) {
    Vinculo e = vinculo(id, p, "ATIVO");
    if (e.getDtHomologEstagio() != null)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Estágio já homologado.");
    documentoDoOrgao(dto.documentoLegalId(), e.getEmpCodigo());
    e.setDtHomologEstagio(dto.dtHomologEstagio());
    e.setDocumentoLegalId(dto.documentoLegalId());
    return finish(e);
  }

  public ResourceRepresentationResult<CargoResponseDTO> extinguirCargo(
      Integer id, ExtinguirCargoDTO dto, ResourceVersionUpdatePrecondition<Integer> p) {
    Cargo e = locked(Cargo.class, id);
    p.requireMatch(e.getVersion());
    org(e.getEmpCodigo());
    if (e.getDtExtincao() != null)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cargo já extinto.");
    if (dto.documentoLegalId() != null) e.setDocumentoLegalId(dto.documentoLegalId());
    e.setDtExtincao(dto.dtExtincao());
    rules.cargo(e, true);
    em.flush();
    return ResourceRepresentationResult.versioned(
        readFacts.enrichOne(cargos.toResponse(e)), e.getVersion());
  }

  public ResourceRepresentationResult<VinculoResponseDTO> alterarLotacaoExercicio(
      VinculoId id,
      AlterarLotacaoExercicioDTO dto,
      ResourceVersionUpdatePrecondition<VinculoId> p) {
    Vinculo e = vinculo(id, p, null);
    if (dto.wasSupplied("setor")) e.setSetor(dto.getSetor());
    if (dto.wasSupplied("setorExercicio")) e.setSetorExercicio(dto.getSetorExercicio());
    if (dto.wasSupplied("cargoComissao")) e.setCargoComissao(dto.getCargoComissao());
    if (dto.wasSupplied("jornadaSemanal")) e.setJornadaSemanal(dto.getJornadaSemanal());
    if (dto.wasSupplied("documentoLegalId")) e.setDocumentoLegalId(dto.getDocumentoLegalId());
    documentoDoOrgao(e.getDocumentoLegalId(), e.getEmpCodigo());
    return finish(e);
  }

  public ResourceRepresentationResult<VinculoResponseDTO> atualizarPrevidenciaPagamento(
      VinculoId id,
      AtualizarPrevidenciaPagamentoDTO dto,
      ResourceVersionUpdatePrecondition<VinculoId> p) {
    Vinculo e = vinculo(id, p, null);
    if (dto.wasSupplied("regimeprev")) e.setRegimeprev(dto.getRegimeprev());
    if (dto.wasSupplied("planoprev")) e.setPlanoprev(dto.getPlanoprev());
    if (dto.wasSupplied("descontaIr")) e.setDescontaIr(dto.getDescontaIr());
    if (dto.wasSupplied("correio")) e.setCorreio(dto.getCorreio());
    if (dto.wasSupplied("banco")) e.setBanco(dto.getBanco());
    if (dto.wasSupplied("agencia")) e.setAgencia(dto.getAgencia());
    if (dto.wasSupplied("conta")) e.setConta(dto.getConta());
    if (dto.wasSupplied("tipopag")) e.setTipopag(dto.getTipopag());
    documentoDoOrgao(e.getDocumentoLegalId(), e.getEmpCodigo());
    return finish(e);
  }

  public ResourceRepresentationResult<ServidorPessoaResponseDTO> atualizarDadosBancariosContato(
      Long id, AtualizarDadosBancariosContatoDTO dto, ResourceVersionUpdatePrecondition<Long> p) {
    ServidorPessoa e = locked(ServidorPessoa.class, id);
    p.requireMatch(e.getVersion());
    if (dto.wasSupplied("eMail")) e.setEMail(dto.getEMail());
    if (dto.wasSupplied("telefone")) e.setTelefone(dto.getTelefone());
    if (dto.wasSupplied("numtelCelular")) e.setNumtelCelular(dto.getNumtelCelular());
    if (dto.wasSupplied("tipologender")) e.setTipologender(dto.getTipologender());
    if (dto.wasSupplied("nomelogender")) e.setNomelogender(dto.getNomelogender());
    if (dto.wasSupplied("numender")) e.setNumender(dto.getNumender());
    if (dto.wasSupplied("complender")) e.setComplender(dto.getComplender());
    if (dto.wasSupplied("bairroender")) e.setBairroender(dto.getBairroender());
    if (dto.wasSupplied("cidadeender")) e.setCidadeender(dto.getCidadeender());
    if (dto.wasSupplied("ufender")) e.setUfender(dto.getUfender());
    if (dto.wasSupplied("cepender")) e.setCepender(dto.getCepender());
    if (dto.wasSupplied("banco")) e.setBanco(dto.getBanco());
    if (dto.wasSupplied("agencia")) e.setAgencia(dto.getAgencia());
    if (dto.wasSupplied("conta")) e.setConta(dto.getConta());
    if (dto.wasSupplied("tipopag")) e.setTipopag(dto.getTipopag());
    rules.pessoa(e);
    em.flush();
    em.refresh(e);
    return ResourceRepresentationResult.versioned(pessoas.toResponse(e), e.getVersion());
  }
}
