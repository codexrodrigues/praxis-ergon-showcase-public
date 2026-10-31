package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.praxis.ergonshowcase.config.ErgonDeterminationConfig;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.config.VinculoStateProvider;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.repository.DefinicaoCampoCustomizadoRepository;
import br.com.praxis.ergonshowcase.repository.VinculoRepository;
import br.com.praxis.ergonshowcase.service.ErgonDeterminationService;
import br.com.praxis.ergonshowcase.service.ErgonInvariants;
import br.com.praxis.ergonshowcase.service.ErgonReadFacts;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Testes determinísticos de contrato, invariantes, mapeamento, atualização parcial e determinações
 * reativas de Tabela, ItemTabela e DefinicaoCampoCustomizado (Slice 4).
 */
class CampoCustomizadoContractTest {

  @Test
  void tabelaAndItemTabelaMappersStayReadOnlyAndMapAllColumns() {
    var tabela = new Tabela();
    tabela.setTab("ERG_PARENTESCO");
    tabela.setDescr("Graus de Parentesco");
    tabela.setSis("Ergon");
    tabela.setMultiEmp(false);
    tabela.setTipo("A");
    tabela.setTaminfo(20);
    tabela.setLabelItem1("Código eSocial");
    tabela.setLabelItem2("Sigla");
    tabela.setLabelItem3("Observação");

    var tabelaMapper = new TabelaMapper();
    TabelaResponseDTO tabelaDto = tabelaMapper.toResponse(tabela);
    assertThat(tabelaDto.getId()).isEqualTo("ERG_PARENTESCO");
    assertThat(tabelaDto.getTab()).isEqualTo("ERG_PARENTESCO");
    assertThat(tabelaDto.getDescr()).isEqualTo("Graus de Parentesco");
    assertThat(tabelaDto.getSis()).isEqualTo("Ergon");
    assertThat(tabelaDto.getTaminfo()).isEqualTo(20);
    assertThat(tabelaDto.getLabelItem1()).isEqualTo("Código eSocial");
    assertThatThrownBy(() -> tabelaMapper.newEntity(null))
        .isInstanceOf(UnsupportedOperationException.class);

    var orgao = new Orgao();
    orgao.setEmpresa(1);
    orgao.setNome("Secretaria de Administração");

    var item = new ItemTabela();
    item.setId(new ItemTabelaId("ERG_PARENTESCO", "CONJUGE"));
    item.setDescr("Cônjuge ou Companheiro(a)");
    item.setItem1("01");
    item.setItem2("CJ");
    item.setItem3("Cônjuge com comunhão ou união estável");
    item.setItem4("ESOCIAL_01");
    item.setOrdem(10);
    item.setAtivo(true);
    item.setEmpCodigo(1);
    item.setTabelaEntidade(tabela);
    item.setOrgao(orgao);

    var itemMapper = new ItemTabelaMapper();
    ItemTabelaResponseDTO itemDto = itemMapper.toResponse(item);
    assertThat(itemDto.getId()).isEqualTo(new ItemTabelaId("ERG_PARENTESCO", "CONJUGE"));
    assertThat(itemDto.getTab()).isEqualTo("ERG_PARENTESCO");
    assertThat(itemDto.getItem()).isEqualTo("CONJUGE");
    assertThat(itemDto.getTabelaDescricao()).isEqualTo("Graus de Parentesco");
    assertThat(itemDto.getOrgaoNome()).isEqualTo("Secretaria de Administração");
    assertThat(itemDto.getOrdem()).isEqualTo(10);
    assertThat(itemDto.getItem1()).isEqualTo("01");
    assertThatThrownBy(() -> itemMapper.newEntity(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void definicaoCampoCustomizadoMapperDerivesFactsAndMapsMutableFields() {
    var mapper = new DefinicaoCampoCustomizadoMapper();
    var orgao = new Orgao();
    orgao.setEmpresa(1);
    orgao.setNome("Secretaria de Administração");

    var tabela = new Tabela();
    tabela.setTab("ERG_TIPO_SANGUINEO");
    tabela.setDescr("Tipos Sanguíneos");

    var entity = new DefinicaoCampoCustomizado();
    entity.setId(900L);
    entity.setEmpCodigo(1);
    entity.setOrgao(orgao);
    entity.setResourceKey("administracao-pessoal.funcionarios");
    entity.setFieldKey("tipo_sanguineo");
    entity.setFieldType("SELECT");
    entity.setControlType("SELECT");
    entity.setLabel("Tipo sanguíneo");
    entity.setHelpText("Informe o grupo sanguíneo e fator Rh.");
    entity.setGroupName("Saúde Ocupacional");
    entity.setFieldOrder(15);
    entity.setRequired(true);
    entity.setOptionTabela("ERG_TIPO_SANGUINEO");
    entity.setOptionTabelaEntidade(tabela);
    entity.setValidationRules(Map.of("maxLength", 5));
    entity.setGovernancePolicy(Map.of("sensitivity", "PERSONAL_SENSITIVE"));
    entity.setActive(true);

    DefinicaoCampoCustomizadoResponseDTO dto = mapper.toResponse(entity);
    assertThat(dto.getId()).isEqualTo(900L);
    assertThat(dto.getOrgaoNome()).isEqualTo("Secretaria de Administração");
    assertThat(dto.getOptionTabelaDescricao()).isEqualTo("Tipos Sanguíneos");
    assertThat(dto.getPossuiTabelaOpcoes()).isTrue();
    assertThat(dto.getPossuiRegrasValidacao()).isTrue();
    assertThat(dto.getObrigatorioAtivo()).isTrue();

    entity.setActive(false);
    entity.setOptionTabela(null);
    entity.setValidationRules(Map.of());
    DefinicaoCampoCustomizadoResponseDTO inativo = mapper.toResponse(entity);
    assertThat(inativo.getPossuiTabelaOpcoes()).isFalse();
    assertThat(inativo.getPossuiRegrasValidacao()).isFalse();
    assertThat(inativo.getObrigatorioAtivo()).isFalse();
  }

  @Test
  void reactiveDeterminationConfigAndServiceComputeCampoCustomizadoPreview() {
    var defs = new ErgonDeterminationConfig().ergonDeterminations().definitions();
    var ccDef =
        defs.stream()
            .filter(d -> "ergon.campoCustomizado.previa".equals(d.id()))
            .findFirst()
            .orElseThrow();
    assertThat(ccDef.operationId()).isEqualTo("determineErgonCampoCustomizadoPrevia");
    assertThat(ccDef.sourcePaths())
        .containsExactly("/empCodigo", "/fieldType", "/controlType", "/optionTabela");

    EntityManager em = mock(EntityManager.class);
    var tabela = new Tabela();
    tabela.setTab("ERG_TIPO_SANGUINEO");
    tabela.setDescr("Tipos Sanguíneos");
    when(em.find(Tabela.class, "ERG_TIPO_SANGUINEO")).thenReturn(tabela);

    var service = new ErgonDeterminationService(new TenantContext(), new ErgonReadFacts());
    ReflectionTestUtils.setField(service, "em", em);

    var draft = new CampoCustomizadoPreviaDraftDTO();
    draft.setEmpCodigo(null);
    draft.setFieldType("SELECT");
    draft.setControlType("SELECT");
    draft.setOptionTabela("ERG_TIPO_SANGUINEO");

    var preview = service.previaCampoCustomizado(draft);
    assertThat(preview.getEscopoGlobalPrevisto()).isTrue();
    assertThat(preview.getRequerTabelaOpcoesPrevisto()).isTrue();
    assertThat(preview.getOptionTabelaDescricaoPrevista()).isEqualTo("Tipos Sanguíneos");

    var invalidDraft = new CampoCustomizadoPreviaDraftDTO();
    invalidDraft.setEmpCodigo(1);
    invalidDraft.setFieldType("SELECT");
    invalidDraft.setOptionTabela("TABELA_INEXISTENTE");
    assertThatThrownBy(() -> service.previaCampoCustomizado(invalidDraft))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Tabela geral de opções (HADES.TABELA) inexistente");
  }

  @Test
  @SuppressWarnings("unchecked")
  void ergonInvariantsEnforcesCampoCustomizadoTypesTableAndUniqueness() {
    EntityManager em = mock(EntityManager.class);
    var invariants = new ErgonInvariants();
    ReflectionTestUtils.setField(invariants, "em", em);

    var orgao = new Orgao();
    orgao.setEmpresa(1);
    orgao.setAtiva(true);
    when(em.find(eq(Orgao.class), eq(1), any(LockModeType.class))).thenReturn(orgao);

    var tabela = new Tabela();
    tabela.setTab("ERG_TIPO_SANGUINEO");
    tabela.setDescr("Tipos Sanguíneos");
    when(em.find(Tabela.class, "ERG_TIPO_SANGUINEO")).thenReturn(tabela);

    TypedQuery<DefinicaoCampoCustomizado> dupQuery = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(DefinicaoCampoCustomizado.class))).thenReturn(dupQuery);
    when(dupQuery.setParameter(anyString(), any())).thenReturn(dupQuery);
    when(dupQuery.setFlushMode(any(FlushModeType.class))).thenReturn(dupQuery);
    when(dupQuery.getResultList()).thenReturn(List.of());

    var cc = new DefinicaoCampoCustomizado();
    cc.setId(900L);
    cc.setEmpCodigo(1);
    cc.setResourceKey("administracao-pessoal.funcionarios");
    cc.setFieldKey("tipo_sanguineo");
    cc.setFieldType("SELECT");
    cc.setControlType("SELECT");
    cc.setLabel("Tipo sanguíneo");
    cc.setFieldOrder(10);
    cc.setOptionTabela("ERG_TIPO_SANGUINEO");
    cc.setActive(true);

    // Caso válido de escopo do órgão passa
    invariants.campoCustomizado(cc, true);

    // Caso válido de escopo global (empCodigo == null) também passa
    cc.setEmpCodigo(null);
    invariants.campoCustomizado(cc, true);
    cc.setEmpCodigo(1);

    // 1. SELECT sem optionTabela -> 422
    cc.setOptionTabela(null);
    assertThatThrownBy(() -> invariants.campoCustomizado(cc, false))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("exige informar a tabela geral de opções");
    cc.setOptionTabela("ERG_TIPO_SANGUINEO");

    // 2. Tipo de dado inválido -> 422
    cc.setFieldType("XML");
    assertThatThrownBy(() -> invariants.campoCustomizado(cc, false))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Tipo de dado inválido");
    cc.setFieldType("SELECT");

    // 3. Chave duplicada para o mesmo órgão e recurso -> 409 CONFLICT
    var existente = new DefinicaoCampoCustomizado();
    existente.setId(899L);
    existente.setEmpCodigo(1);
    existente.setResourceKey("administracao-pessoal.funcionarios");
    existente.setFieldKey("tipo_sanguineo");
    when(dupQuery.getResultList()).thenReturn(List.of(existente));

    assertThatThrownBy(() -> invariants.campoCustomizado(cc, false))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  void partialUpdateDtosAndStateProviderTrackCampoCustomizadoLifecycle() {
    var apresDto = new AtualizarApresentacaoCampoCustomizadoDTO();
    apresDto.setLabel("Grupo Sanguíneo e Fator Rh");
    assertThat(apresDto.wasSupplied("label")).isTrue();
    assertThat(apresDto.wasSupplied("groupName")).isFalse();

    var govDto = new AtualizarGovernancaCampoCustomizadoDTO();
    govDto.setRequired(true);
    assertThat(govDto.wasSupplied("required")).isTrue();
    assertThat(govDto.wasSupplied("optionTabela")).isFalse();

    DefinicaoCampoCustomizadoRepository repo = mock(DefinicaoCampoCustomizadoRepository.class);
    var cc = new DefinicaoCampoCustomizado();
    cc.setId(900L);
    cc.setEmpCodigo(1);
    cc.setRequired(true);
    cc.setActive(true);
    when(repo.findById(900L)).thenReturn(Optional.of(cc));

    var stateProvider = new VinculoStateProvider(mock(VinculoRepository.class));
    ReflectionTestUtils.setField(stateProvider, "camposCustomizados", repo);
    ReflectionTestUtils.setField(stateProvider, "tenant", new TenantContext());

    var ativoSnap =
        stateProvider.resolve("administracao-pessoal.campos-customizados", "900").orElseThrow();
    assertThat(ativoSnap.state()).isEqualTo("ATIVO");
    assertThat(ativoSnap.metadata()).containsEntry("required", true);

    cc.setActive(false);
    var inativoSnap =
        stateProvider.resolve("administracao-pessoal.campos-customizados", "900").orElseThrow();
    assertThat(inativoSnap.state()).isEqualTo("INATIVO");

    // Escopo global (empCodigo == null) também resolve estado para qualquer órgão do tenant
    cc.setEmpCodigo(null);
    cc.setActive(true);
    var globalSnap =
        stateProvider.resolve("administracao-pessoal.campos-customizados", "900").orElseThrow();
    assertThat(globalSnap.state()).isEqualTo("ATIVO");
  }
}
