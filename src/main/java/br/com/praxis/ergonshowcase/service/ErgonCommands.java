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

  private <E> E locked(Class<E> type, Object id) {
    E e = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
    if (e == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assentamento inexistente.");
    em.refresh(e);
    return e;
  }

  private void org(Integer id) {
    if (!Objects.equals(id, tenant.orgao())) ErgonInvariants.fail("Órgão diverge de X-Tenant.");
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
    return ResourceRepresentationResult.versioned(vinculos.toResponse(e), e.getVersion());
  }

  public ResourceRepresentationResult<VinculoResponseDTO> registrarCessao(
      VinculoId id, RegistrarCessaoDTO dto, ResourceVersionUpdatePrecondition<VinculoId> p) {
    Vinculo e = vinculo(id, p, "ATIVO");
    ErgonInvariants.chronology(e.getDtexerc(), dto.dtiniCessao(), "Cessão anterior ao exercício.");
    ErgonInvariants.chronology(
        dto.dtiniCessao(), dto.dtfimCessao(), "Fim da cessão anterior ao início.");
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
    ErgonInvariants.chronology(
        e.getDtiniCessao(), dto.dtfimCessao(), "Encerramento anterior ao início da cessão.");
    e.setDtfimCessao(dto.dtfimCessao());
    e.setSituacao("ATIVO");
    return finish(e);
  }

  private void documento(Long id) {
    if (id != null && em.find(DocumentoLegal.class, id) == null)
      ErgonInvariants.fail("Documento legal inexistente.");
  }

  public ResourceRepresentationResult<VinculoResponseDTO> concederAposentadoria(
      VinculoId id, ConcederAposentadoriaDTO dto, ResourceVersionUpdatePrecondition<VinculoId> p) {
    Vinculo e = vinculo(id, p, "ATIVO");
    documento(dto.documentoLegalId());
    ErgonInvariants.chronology(
        e.getDtexerc(), dto.dtaposent(), "Aposentadoria anterior ao exercício.");
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
    documento(dto.documentoLegalId());
    ErgonInvariants.chronology(
        e.getDtexerc(), dto.dtHomologEstagio(), "Homologação anterior ao exercício.");
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
    if (rules.ocupadas(id, null) > 0)
      ErgonInvariants.fail("Cargo ocupado por vínculo ativo, afastado ou cedido.");
    ErgonInvariants.chronology(
        e.getDtInicioContrVaga(), dto.dtExtincao(), "Extinção anterior à vigência do cargo.");
    e.setDtExtincao(dto.dtExtincao());
    em.flush();
    return ResourceRepresentationResult.versioned(cargos.toResponse(e), e.getVersion());
  }

  public ResourceRepresentationResult<VinculoResponseDTO> alterarLotacaoExercicio(
      VinculoId id,
      AlterarLotacaoExercicioDTO dto,
      ResourceVersionUpdatePrecondition<VinculoId> p) {
    Vinculo e = vinculo(id, p, null);
    if (dto.getSetor() != null) e.setSetor(dto.getSetor());
    if (dto.getSetorExercicio() != null) e.setSetorExercicio(dto.getSetorExercicio());
    if (dto.getCargoComissao() != null) e.setCargoComissao(dto.getCargoComissao());
    if (dto.getJornadaSemanal() != null) e.setJornadaSemanal(dto.getJornadaSemanal());
    if (dto.getDocumentoLegalId() != null) e.setDocumentoLegalId(dto.getDocumentoLegalId());
    documento(e.getDocumentoLegalId());
    return finish(e);
  }

  public ResourceRepresentationResult<VinculoResponseDTO> atualizarPrevidenciaPagamento(
      VinculoId id,
      AtualizarPrevidenciaPagamentoDTO dto,
      ResourceVersionUpdatePrecondition<VinculoId> p) {
    Vinculo e = vinculo(id, p, null);
    if (dto.getRegimeprev() != null) e.setRegimeprev(dto.getRegimeprev());
    if (dto.getPlanoprev() != null) e.setPlanoprev(dto.getPlanoprev());
    if (dto.getDescontaIr() != null) e.setDescontaIr(dto.getDescontaIr());
    if (dto.getCorreio() != null) e.setCorreio(dto.getCorreio());
    if (dto.getBanco() != null) e.setBanco(dto.getBanco());
    if (dto.getAgencia() != null) e.setAgencia(dto.getAgencia());
    if (dto.getConta() != null) e.setConta(dto.getConta());
    if (dto.getTipopag() != null) e.setTipopag(dto.getTipopag());
    documento(e.getDocumentoLegalId());
    return finish(e);
  }

  public ResourceRepresentationResult<ServidorPessoaResponseDTO> atualizarDadosBancariosContato(
      Long id, AtualizarDadosBancariosContatoDTO dto, ResourceVersionUpdatePrecondition<Long> p) {
    ServidorPessoa e = locked(ServidorPessoa.class, id);
    p.requireMatch(e.getVersion());
    if (dto.getEMail() != null) e.setEMail(dto.getEMail());
    if (dto.getTelefone() != null) e.setTelefone(dto.getTelefone());
    if (dto.getNumtelCelular() != null) e.setNumtelCelular(dto.getNumtelCelular());
    if (dto.getTipologender() != null) e.setTipologender(dto.getTipologender());
    if (dto.getNomelogender() != null) e.setNomelogender(dto.getNomelogender());
    if (dto.getNumender() != null) e.setNumender(dto.getNumender());
    if (dto.getComplender() != null) e.setComplender(dto.getComplender());
    if (dto.getBairroender() != null) e.setBairroender(dto.getBairroender());
    if (dto.getCidadeender() != null) e.setCidadeender(dto.getCidadeender());
    if (dto.getUfender() != null) e.setUfender(dto.getUfender());
    if (dto.getCepender() != null) e.setCepender(dto.getCepender());
    if (dto.getBanco() != null) e.setBanco(dto.getBanco());
    if (dto.getAgencia() != null) e.setAgencia(dto.getAgencia());
    if (dto.getConta() != null) e.setConta(dto.getConta());
    if (dto.getTipopag() != null) e.setTipopag(dto.getTipopag());
    rules.pessoa(e);
    em.flush();
    em.refresh(e);
    return ResourceRepresentationResult.versioned(pessoas.toResponse(e), e.getVersion());
  }
}
