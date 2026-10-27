package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Integração real com PostgreSQL Neon; mutações de cada teste são revertidas. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@WithMockUser(authorities = {"ERGON_READ", "ERGON_WRITE"})
class NeonIntegrationTest {
  @Test
  void revisionResponsesCoverEveryPersistentBusinessField() throws Exception {
    for (String name : new String[] {"Orgao", "Cargo", "ServidorPessoa", "Vinculo"}) {
      Class<?> entity = Class.forName("br.com.praxis.ergonshowcase.domain." + name);
      Class<?> response = Class.forName("br.com.praxis.ergonshowcase.dto." + name + "ResponseDTO");
      java.util.Set<String> fields = new java.util.HashSet<>();
      for (Class<?> cursor = response; cursor != Object.class; cursor = cursor.getSuperclass())
        for (var f : cursor.getDeclaredFields()) fields.add(f.getName());
      for (var field : entity.getDeclaredFields())
        if (field.isAnnotationPresent(jakarta.persistence.Column.class)
            && !field.isAnnotationPresent(jakarta.persistence.Id.class)
            && !field.isAnnotationPresent(jakarta.persistence.Version.class))
          assertThat(fields).as(name + "." + field.getName()).contains(field.getName());
    }
  }

  @Test
  void revisionVacancyBalancePreventsOverProvision() {
    var rules = context.getBean(br.com.praxis.ergonshowcase.service.ErgonInvariants.class);
    Integer id =
        em.createQuery(
                "select min(v.cargo) from Vinculo v where v.empCodigo=1 and v.situacao in ('ATIVO','AFASTADO','CEDIDO')",
                Integer.class)
            .getSingleResult();
    var cargo = em.find(br.com.praxis.ergonshowcase.domain.Cargo.class, id);
    cargo.setDtExtincao(null);
    cargo.setControleVaga(true);
    cargo.setVagasAutorizadas((int) rules.ocupadas(id, null));
    var v = revisionNewVinculo();
    v.setCargo(id);
    assertThatThrownBy(() -> rules.vinculo(v, true)).hasMessageContaining("Saldo de vagas");
  }

  @Test
  void revisionBusinessRulesRejectInvalidVacanciesAndAggregation() {
    var rules = context.getBean(br.com.praxis.ergonshowcase.service.ErgonInvariants.class);
    var cargo = new br.com.praxis.ergonshowcase.domain.Cargo();
    cargo.setCargo(9405);
    cargo.setEmpCodigo(1);
    cargo.setControleVaga(true);
    cargo.setVagasAutorizadas(0);
    assertThatThrownBy(() -> rules.cargo(cargo))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("positiva");
    cargo.setControleVaga(false);
    cargo.setVagasAutorizadas(null);
    cargo.setCargoAglutinador(999999);
    assertThatThrownBy(() -> rules.cargo(cargo)).hasMessageContaining("aglutinador");
    Integer occupied =
        em.createQuery(
                "select min(v.cargo) from Vinculo v where v.empCodigo=1 and v.situacao in ('ATIVO','AFASTADO','CEDIDO')",
                Integer.class)
            .getSingleResult();
    var existing = em.find(br.com.praxis.ergonshowcase.domain.Cargo.class, occupied);
    existing.setVagasAutorizadas(0);
    existing.setControleVaga(false);
    assertThatThrownBy(() -> rules.cargo(existing)).hasMessageContaining("ocupação");
  }

  private br.com.praxis.ergonshowcase.domain.Vinculo revisionNewVinculo() {
    var v = new br.com.praxis.ergonshowcase.domain.Vinculo();
    v.setId(new br.com.praxis.ergonshowcase.domain.VinculoId(1001L, 99));
    v.setEmpCodigo(1);
    v.setSituacao("ATIVO");
    v.setCategoria("GERAL");
    v.setDtexerc(java.time.LocalDate.of(2020, 1, 1));
    return v;
  }

  @Test
  void revisionBusinessRulesRejectChronologyAndAccumulation() {
    var rules = context.getBean(br.com.praxis.ergonshowcase.service.ErgonInvariants.class);
    var v = revisionNewVinculo();
    v.setDtnom(java.time.LocalDate.of(2021, 1, 1));
    v.setDtposse(java.time.LocalDate.of(2020, 1, 1));
    assertThatThrownBy(() -> rules.vinculo(v, true)).hasMessageContaining("Nomeação");
    v.setDtnom(null);
    v.setDtposse(null);
    assertThatThrownBy(() -> rules.vinculo(v, true)).hasMessageContaining("Acumulação");
    v.setSituacao("CEDIDO");
    v.setDtinicontr(java.time.LocalDate.of(2026, 5, 1));
    v.setDtfimcontr(java.time.LocalDate.of(2026, 4, 1));
    assertThatThrownBy(() -> rules.vinculo(v, true)).hasMessageContaining("contratual");
  }

  @Test
  void revisionBusinessRulesRejectCrossOrgAndExtinctCargo() {
    var rules = context.getBean(br.com.praxis.ergonshowcase.service.ErgonInvariants.class);
    var v = revisionNewVinculo();
    v.setSituacao("CEDIDO");
    String foreign =
        em.createQuery("select min(s.setor) from Setor s where s.empCodigo<>1", String.class)
            .getSingleResult();
    v.setSetor(foreign);
    assertThatThrownBy(() -> rules.vinculo(v, true)).hasMessageContaining("outro órgão");
    v.setSetor(null);
    v.setSetorExercicio(foreign);
    assertThatThrownBy(() -> rules.vinculo(v, true)).hasMessageContaining("outro órgão");
    v.setSetorExercicio(null);
    Integer other =
        em.createQuery("select min(c.cargo) from Cargo c where c.empCodigo<>1", Integer.class)
            .getSingleResult();
    v.setCargoComissao(other);
    assertThatThrownBy(() -> rules.vinculo(v, true)).hasMessageContaining("outro órgão");
    v.setCargoComissao(null);
    Integer own =
        em.createQuery("select min(c.cargo) from Cargo c where c.empCodigo=1", Integer.class)
            .getSingleResult();
    var cargo = em.find(br.com.praxis.ergonshowcase.domain.Cargo.class, own);
    cargo.setDtExtincao(java.time.LocalDate.now().plusDays(1));
    v.setCargo(own);
    assertThatThrownBy(() -> rules.vinculo(v, true)).hasMessageContaining("extinto");
  }

  @Test
  void revisionProfessionalRegistrationAndUniqueness() throws Exception {
    var rules = context.getBean(br.com.praxis.ergonshowcase.service.ErgonInvariants.class);
    var p = new br.com.praxis.ergonshowcase.domain.ServidorPessoa();
    p.setNumero(9499L);
    p.setCpf("00000009499");
    p.setIdentprof("123");
    assertThatThrownBy(() -> rules.pessoa(p)).hasMessageContaining("número e tipo");
    var existing = em.find(br.com.praxis.ergonshowcase.domain.ServidorPessoa.class, 1001L);
    p.setTipoidprof("CRM");
    p.setCpf(existing.getCpf());
    assertThatThrownBy(() -> rules.pessoa(p)).hasMessageContaining("CPF");
    var o = new br.com.praxis.ergonshowcase.domain.Orgao();
    o.setEmpresa(9499);
    o.setCnpj(em.find(br.com.praxis.ergonshowcase.domain.Orgao.class, 1).getCnpj());
    assertThatThrownBy(() -> rules.orgao(o)).hasMessageContaining("CNPJ");
    revisionCommand(
            "/api/ergon/servidor-pessoas/1001/dados-bancarios-contato",
            "{\"eMail\":\"prova@example.invalid\"}")
        .andExpect(status().isOk());
  }

  @Test
  void revisionDiscoveryAndRichSchemas() throws Exception {
    for (String resource : new String[] {"orgaos", "cargos", "servidor-pessoas", "vinculos"}) {
      String schema =
          mvc.perform(
                  get("/schemas/filtered")
                      .param("resourcePath", "/api/ergon/" + resource)
                      .param("operation", "post")
                      .param("schemaType", "response"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();
      assertThat(schema).contains("quickFilters");
      if (resource.equals("servidor-pessoas"))
        assertThat(schema)
            .contains(
                "numcartpro",
                "numtitel",
                "defintelectual",
                "nomelogender",
                "identprof",
                "idadeAnos",
                "possuiDeficienciaPcD");
      if (resource.equals("vinculos"))
        assertThat(schema)
            .contains(
                "regimeprev",
                "dtaposent",
                "dtiniCessao",
                "emCessaoAtiva",
                "setorExercicio",
                "cargoComissao");
      if (resource.equals("cargos"))
        assertThat(schema).contains("cargoAglutinador", "saldoVagasDisponiveis", "cargoExtinto");
    }
    for (String key : new String[] {"vinculos", "cargos", "funcionarios"}) {
      String domain =
          mvc.perform(get("/schemas/domain").param("resource", "administracao-pessoal." + key))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();
      assertThat(domain)
          .contains(
              key.equals("vinculos")
                  ? "emEstagioProbatorio"
                  : key.equals("cargos") ? "saldoVagasDisponiveis" : "possuiDeficienciaPcD");
    }
    String actions =
        mvc.perform(get("/schemas/actions").param("resource", "administracao-pessoal.vinculos"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(actions)
        .contains(
            "registrar-cessao",
            "encerrar-cessao",
            "conceder-aposentadoria",
            "homologar-estagio-probatorio");
    String surfaces =
        mvc.perform(get("/schemas/surfaces").param("resource", "administracao-pessoal.vinculos"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(surfaces).contains("lotacao-exercicio", "previdencia-pagamento");
  }

  @Test
  void revisionFiltersExecuteTheirDeclaredPredicates() throws Exception {
    String[][] cases = {
      {"orgaos", "{\"cnpj\":\"00\",\"somenteAtivos\":true}"},
      {"orgaos", "{\"semPortalWeb\":true,\"criadoNosUltimosDias\":30}"},
      {"cargos", "{\"apenasVigentes\":true,\"vagasMinimas\":0}"},
      {"cargos", "{\"apenasExtintos\":true,\"vagasAutorizadasRange\":[0,99999]}"},
      {"servidor-pessoas", "{\"cpfPrefixo\":\"0\",\"possuiNomeSocial\":true}"},
      {"servidor-pessoas", "{\"dataNascimentoExata\":\"1990-01-01\"}"},
      {"servidor-pessoas", "{\"cnhVencendoNosProximosDias\":90,\"somentePcD\":true}"},
      {"vinculos", "{\"pessoaNome\":\"a\",\"cargoNome\":\"a\",\"excluirSituacoes\":[\"VACANTE\"]}"},
      {"vinculos", "{\"exercicioNaData\":\"2020-01-01\",\"emExercicioSemVacancia\":true}"},
      {
        "vinculos",
        "{\"fimContratoNosProximosDias\":90,\"estagioHomologado\":true,\"infoCota\":true}"
      }
    };
    for (var item : cases)
      mvc.perform(
              post("/api/ergon/" + item[0] + "/filter")
                  .contentType("application/json")
                  .content(item[1]))
          .andExpect(status().isOk());
    var expected =
        em.createQuery(
                "select count(p) from ServidorPessoa p where p.deffisica=true or p.defvisual=true or p.defauditiva=true or p.defmental=true or p.defintelectual=true",
                Long.class)
            .getSingleResult();
    mvc.perform(
            post("/api/ergon/servidor-pessoas/filter")
                .contentType("application/json")
                .content("{\"somentePcD\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(expected.intValue()));
  }

  private String revisionEtag(String uri) throws Exception {
    return mvc.perform(get(uri))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getHeader("ETag");
  }

  private org.springframework.test.web.servlet.ResultActions revisionCommand(
      String uri, String body) throws Exception {
    String base =
        uri.contains("/actions/")
            ? uri.substring(0, uri.indexOf("/actions/"))
            : uri.substring(0, uri.lastIndexOf('/'));
    return mvc.perform(
        (uri.contains("/actions/") ? post(uri) : patch(uri))
            .header("If-Match", revisionEtag(base))
            .contentType("application/json")
            .content(body));
  }

  @Test
  void revisionPartialFormsPreserveIdentityAndUntouchedValues() throws Exception {
    String uri = "/api/ergon/vinculos/1001:1";
    Integer bank =
        em.find(
                br.com.praxis.ergonshowcase.domain.Vinculo.class,
                new br.com.praxis.ergonshowcase.domain.VinculoId(1001L, 1))
            .getBanco();
    revisionCommand(uri + "/previdencia-pagamento", "{\"correio\":true}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("1001:1"));
    assertThat(
            em.find(
                    br.com.praxis.ergonshowcase.domain.Vinculo.class,
                    new br.com.praxis.ergonshowcase.domain.VinculoId(1001L, 1))
                .getBanco())
        .isEqualTo(bank);
    revisionCommand(uri + "/lotacao-exercicio", "{\"jornadaSemanal\":35}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.jornadaSemanal").value(35));
    revisionCommand(
            "/api/ergon/servidor-pessoas/1001/dados-bancarios-contato",
            "{\"telefone\":\"1130503050\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.telefone").value("1130503050"));
    mvc.perform(patch(uri + "/lotacao-exercicio").contentType("application/json").content("{}"))
        .andExpect(status().isPreconditionRequired());
  }

  @Test
  void revisionCessaoAndRetirementTransitions() throws Exception {
    String uri = "/api/ergon/vinculos/1001:1";
    revisionCommand(
            uri + "/actions/registrar-cessao",
            "{\"tipoReq\":\"CESSAO\",\"orgaoReq\":\"Órgão de prova\",\"funcaoReq\":\"APOIO\",\"tipoOnusReq\":\"CEDENTE\",\"dtiniCessao\":\"2026-09-01\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.situacao").value("CEDIDO"));
    revisionCommand(uri + "/actions/encerrar-cessao", "{\"dtfimCessao\":\"2026-10-01\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.situacao").value("ATIVO"));
    Long doc =
        em.createQuery("select min(d.id) from DocumentoLegal d", Long.class).getSingleResult();
    revisionCommand(
            uri + "/actions/conceder-aposentadoria",
            "{\"dtaposent\":\"2026-10-01\",\"tipoapos\":\"VOLUNTARIA\",\"fatorProporcAposent\":1,\"documentoLegalId\":"
                + doc
                + "}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.situacao").value("APOSENTADO"));
    revisionCommand(
            uri + "/actions/registrar-cessao",
            "{\"tipoReq\":\"CESSAO\",\"orgaoReq\":\"Órgão de prova\",\"funcaoReq\":\"APOIO\",\"tipoOnusReq\":\"CEDENTE\",\"dtiniCessao\":\"2026-09-01\"}")
        .andExpect(status().isConflict());
  }

  @Test
  void revisionHomologacaoAndCargoExtinction() throws Exception {
    var vinculo =
        em.find(
            br.com.praxis.ergonshowcase.domain.Vinculo.class,
            new br.com.praxis.ergonshowcase.domain.VinculoId(1001L, 1));
    vinculo.setDtHomologEstagio(null);
    em.flush();
    Long doc =
        em.createQuery("select min(d.id) from DocumentoLegal d", Long.class).getSingleResult();
    revisionCommand(
            "/api/ergon/vinculos/1001:1/actions/homologar-estagio-probatorio",
            "{\"dtHomologEstagio\":\"2026-10-01\",\"documentoLegalId\":" + doc + "}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emEstagioProbatorio").value(false));
    mvc.perform(
            post("/api/ergon/cargos")
                .contentType("application/json")
                .content(
                    "{\"cargo\":9405,\"empCodigo\":1,\"nome\":\"Cargo para extinção\",\"categoria\":\"GERAL\"}"))
        .andExpect(status().isCreated());
    revisionCommand(
            "/api/ergon/cargos/9405/actions/extinguir-cargo", "{\"dtExtincao\":\"2026-10-01\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cargoExtinto").value(true));
    Integer occupied =
        em.createQuery(
                "select min(v.cargo) from Vinculo v where v.empCodigo=1 and v.situacao in ('ATIVO','AFASTADO','CEDIDO')",
                Integer.class)
            .getSingleResult();
    revisionCommand(
            "/api/ergon/cargos/" + occupied + "/actions/extinguir-cargo",
            "{\"dtExtincao\":\"2026-10-01\"}")
        .andExpect(status().isUnprocessableEntity());
  }

  @Autowired MockMvc mvc;
  @Autowired EntityManager em;
  @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
  @org.springframework.boot.test.web.server.LocalServerPort int port;
  @Autowired org.springframework.web.context.WebApplicationContext context;

  @org.springframework.beans.factory.annotation.Value("${showcase.security.username}")
  String username;

  @org.springframework.beans.factory.annotation.Value("${showcase.security.password}")
  String password;

  @BeforeEach
  void configureRealOpenApiOrigin() {
    mvc =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
            .apply(
                org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                    .springSecurity())
            .alwaysDo(
                result -> {
                  if (result.getResponse().getStatus() >= 400)
                    System.out.println(
                        "HTTP "
                            + result.getRequest().getRequestURI()
                            + " "
                            + result.getResponse().getStatus()
                            + " "
                            + result.getResponse().getContentAsString());
                })
            .defaultRequest(
                get("/")
                    .with(
                        request -> {
                          request.setServerPort(port);
                          request.setLocalPort(port);
                          return request;
                        })
                    .with(
                        org.springframework.security.test.web.servlet.request
                            .SecurityMockMvcRequestPostProcessors.httpBasic(username, password)))
            .build();
  }

  @Test
  void allSixteenEntitiesMatchPostgres17() {
    assertThat(em.getMetamodel().getEntities()).hasSize(16);
    assertThat(em.createNativeQuery("show server_version").getSingleResult().toString())
        .startsWith("17.");
    for (var entity : em.getMetamodel().getEntities())
      assertThat(
              em.createQuery("select count(e) from " + entity.getName() + " e", Long.class)
                  .getSingleResult())
          .isNotNegative();
  }

  @Test
  void fourResourcesExposeDataAndCapabilities() throws Exception {
    for (String resource : new String[] {"orgaos", "cargos", "servidor-pessoas", "vinculos"}) {
      mvc.perform(get("/api/ergon/" + resource + "/capabilities")).andExpect(status().isOk());
      mvc.perform(
              post("/api/ergon/" + resource + "/filter")
                  .contentType("application/json")
                  .content("{}"))
          .andExpect(status().isOk());
    }
  }

  @Test
  void compositeIdentityAndSurface() throws Exception {
    mvc.perform(get("/api/ergon/vinculos/1001:1"))
        .andExpect(status().isOk())
        .andExpect(header().exists("ETag"));
    mvc.perform(get("/api/ergon/vinculos/1001:1/resumo-funcional"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.situacao").value("ATIVO"));
  }

  @Test
  void crudFourPilotsAndImmutableIdentifiers() throws Exception {
    String[][] fixtures = {
      {
        "orgaos",
        "9305",
        "{\"empresa\":9305,\"nome\":\"Órgão de prova\",\"razao\":\"Ente de prova\",\"cnpj\":\"00000000930500\",\"ufSigla\":\"SP\",\"municipio\":\"São Paulo\"}",
        "empresa"
      },
      {
        "cargos",
        "9305",
        "{\"cargo\":9305,\"empCodigo\":1,\"nome\":\"Cargo de prova\",\"categoria\":\"ADMINISTRATIVO\"}",
        "cargo"
      },
      {
        "servidor-pessoas",
        "9305",
        "{\"numero\":9305,\"nome\":\"Pessoa de prova\",\"cpf\":\"00000009305\",\"dtnasc\":\"1990-01-01\"}",
        "numero"
      },
      {
        "vinculos",
        "9305:1",
        "{\"numfunc\":9305,\"numero\":1,\"empCodigo\":1,\"matric\":\"PROVA-305\",\"regimejur\":\"ESTATUTARIO\",\"tipovinc\":\"EFETIVO\",\"categoria\":\"ADMINISTRATIVO\",\"dtexerc\":\"2020-01-01\",\"customFields\":{\"processo_sei_nomeacao\":\"SEI-2026.000305\"}}",
        "numfunc"
      }
    };
    for (var f : fixtures) {
      String uri = "/api/ergon/" + f[0];
      mvc.perform(post(uri).contentType("application/json").content(f[2]))
          .andExpect(status().isCreated());
      String etag =
          mvc.perform(get(uri + "/" + f[1]))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getHeader("ETag");
      var update = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(f[2]);
      update.remove(f[3]);
      update.remove("empCodigo");
      if (f[0].equals("vinculos")) {
        update.remove("numero");
        update.remove("dtexerc");
        update.put("matric", "PROVA-305-EDITADA");
      } else update.put("nome", "Nome alterado na prova");
      mvc.perform(put(uri + "/" + f[1]).contentType("application/json").content(update.toString()))
          .andExpect(status().isPreconditionRequired());
      mvc.perform(
              put(uri + "/" + f[1])
                  .header("If-Match", etag)
                  .contentType("application/json")
                  .content(update.toString()))
          .andExpect(status().isOk());
      mvc.perform(
              put(uri + "/" + f[1])
                  .header("If-Match", etag)
                  .contentType("application/json")
                  .content(update.toString()))
          .andExpect(status().isPreconditionFailed());
    }
    for (int i = fixtures.length - 1; i >= 0; i--)
      mvc.perform(delete("/api/ergon/" + fixtures[i][0] + "/" + fixtures[i][1]))
          .andExpect(status().isNoContent());
  }

  @Test
  void customFieldsAreTenantScopedAndValidated() throws Exception {
    var provider =
        context.getBean(br.com.praxis.ergonshowcase.config.CamposCustomizadosProvider.class);
    assertThat(
            provider.findDefinitions("administracao-pessoal.vinculos", "1", "request").toString())
        .contains("processo_sei_nomeacao")
        .doesNotContain("porte_arma_funcional");
    assertThat(
            provider.findDefinitions("administracao-pessoal.vinculos", "2", "request").toString())
        .contains("porte_arma_funcional");
    String invalid =
        "{\"numfunc\":1001,\"numero\":99,\"empCodigo\":1,\"matric\":\"PROVA-INVALIDA\",\"regimejur\":\"ESTATUTARIO\",\"tipovinc\":\"EFETIVO\",\"categoria\":\"ADMINISTRATIVO\",\"dtexerc\":\"2020-01-01\",\"customFields\":{}}";
    mvc.perform(post("/api/ergon/vinculos").contentType("application/json").content(invalid))
        .andExpect(status().isBadRequest());
  }

  @Test
  void optionsCursorStatsAndExportExecuteOnNeon() throws Exception {
    for (String resource : new String[] {"orgaos", "cargos", "servidor-pessoas", "vinculos"}) {
      String uri = "/api/ergon/" + resource;
      mvc.perform(post(uri + "/options/filter").contentType("application/json").content("{}"))
          .andExpect(status().isOk());
      mvc.perform(post(uri + "/filter/cursor").contentType("application/json").content("{}"))
          .andExpect(status().isOk());
      mvc.perform(
              post(uri + "/export")
                  .contentType("application/json")
                  .content("{\"format\":\"CSV\",\"maxRows\":10}"))
          .andExpect(status().isOk());
    }
    mvc.perform(get("/api/ergon/vinculos/options/by-ids").param("ids", "1001:1"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/ergon/vinculos/stats/group-by")
                .contentType("application/json")
                .content(
                    "{\"field\":\"situacao\",\"metric\":{\"operation\":\"COUNT\"},\"filter\":{}}"))
        .andExpect(status().isOk());
  }

  @Test
  void structuralAndSemanticMetadataArePublished() throws Exception {
    for (String resource : new String[] {"orgaos", "cargos", "servidor-pessoas", "vinculos"}) {
      for (String type : new String[] {"request", "response"}) {
        var response =
            mvc.perform(
                    get("/schemas/filtered")
                        .param("resourcePath", "/api/ergon/" + resource)
                        .param("operation", "post")
                        .param("schemaType", type))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(header().exists("X-Schema-Hash"))
                .andReturn()
                .getResponse();
        var folder = java.nio.file.Path.of("target", "proof");
        java.nio.file.Files.createDirectories(folder);
        java.nio.file.Files.writeString(
            folder.resolve(resource + "-" + type + ".json"),
            response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(response.getContentAsString()).contains("x-ui", "description");
      }
      mvc.perform(get("/api/ergon/" + resource + "/schemas"))
          .andExpect(status().isFound())
          .andExpect(
              header()
                  .string("Location", org.hamcrest.Matchers.containsString("/schemas/filtered")));
    }
    mvc.perform(get("/schemas/actions").param("resource", "administracao-pessoal.vinculos"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("registrar-vacancia")));
    mvc.perform(get("/schemas/surfaces").param("resource", "administracao-pessoal.vinculos"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("resumo-funcional")));
    mvc.perform(get("/schemas/domain").param("resource", "administracao-pessoal.vinculos"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("diasDesdeExercicio")));
  }

  @Test
  void realHttpOpenApiAndAuthentication() {
    var rest = new org.springframework.web.client.RestTemplate();
    assertThat(
            rest.getForEntity("http://localhost:" + port + "/v3/api-docs", String.class)
                .getStatusCode()
                .value())
        .isEqualTo(200);
    assertThatThrownBy(
            () ->
                rest.getForEntity("http://localhost:" + port + "/api/ergon/orgaos/1", String.class))
        .isInstanceOf(org.springframework.web.client.HttpClientErrorException.Unauthorized.class);
    var headers = new org.springframework.http.HttpHeaders();
    headers.setBasicAuth(username, password);
    assertThat(
            rest.exchange(
                    "http://localhost:" + port + "/api/ergon/orgaos/1",
                    org.springframework.http.HttpMethod.GET,
                    new org.springframework.http.HttpEntity<>(headers),
                    String.class)
                .getStatusCode()
                .value())
        .isEqualTo(200);
  }

  @Test
  void cursorContinuityLocateAndCompositeOptions() throws Exception {
    String base = "/api/ergon/vinculos";
    var first =
        json.readTree(
                mvc.perform(
                        post(base + "/filter/cursor")
                            .param("size", "1")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("data");
    String next = first.path("next").asText();
    assertThat(next).isNotBlank();
    var second =
        json.readTree(
                mvc.perform(
                        post(base + "/filter/cursor")
                            .param("size", "1")
                            .param("after", next)
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .path("data");
    assertThat(second.findValuesAsText("id"))
        .doesNotContainAnyElementsOf(first.findValuesAsText("id"));
    mvc.perform(
            post(base + "/locate")
                .param("id", "1001:1")
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isOk());
    mvc.perform(
            post(base + "/option-sources/vinculo/options/filter")
                .contentType("application/json")
                .content("{\"search\":\"100\"}"))
        .andExpect(status().isOk());
    mvc.perform(get(base + "/option-sources/vinculo/options/by-ids").param("ids", "1001:1"))
        .andExpect(status().isOk());
  }

  @Test
  void filtersAndExportPolicyAreEffective() throws Exception {
    var result =
        json.readTree(
            mvc.perform(
                    post("/api/ergon/vinculos/filter")
                        .contentType("application/json")
                        .content("{\"numfunc\":1001,\"situacoes\":[\"ATIVO\"]}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(result.findValuesAsText("numfunc")).isNotEmpty().containsOnly("1001");
    mvc.perform(
            post("/api/ergon/vinculos/filter")
                .contentType("application/json")
                .content("{\"customFields\":{\"processo_sei_nomeacao\":\"SEI-2026.000305\"}}"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/ergon/servidor-pessoas/export")
                .contentType("application/json")
                .content("{\"fields\":[{\"key\":\"cpf\"}]}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/ergon/vinculos/export")
                .contentType("application/json")
                .content("{\"format\":\"json\",\"maxRows\":1}"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/ergon/cargos/export")
                .contentType("application/json")
                .content("{\"format\":\"excel\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void schemaShowsCompositeKeyGovernanceAndTenantFields() throws Exception {
    var schema =
        json.readTree(
            mvc.perform(
                    get("/schemas/filtered")
                        .param("resourcePath", "/api/ergon/vinculos")
                        .param("operation", "post")
                        .param("schemaType", "response")
                        .header("X-Tenant", "2"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(schema.at("/properties/id/format").asText()).isEqualTo("composite-id");
    assertThat(schema.at("/properties/id/x-ui/compositeIdComponents").toString())
        .isEqualTo("[\"numfunc\",\"numero\"]");
    assertThat(schema.at("/properties/customFields/properties").has("porte_arma_funcional"))
        .isTrue();
    var pessoa =
        json.readTree(
            mvc.perform(
                    get("/schemas/filtered")
                        .param("resourcePath", "/api/ergon/servidor-pessoas")
                        .param("operation", "post")
                        .param("schemaType", "response"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(pessoa.at("/properties/cpf").toString()).contains("x-domain-governance");
    mvc.perform(
            post("/api/ergon/servidor-pessoas/filter")
                .contentType("application/json")
                .content(
                    "{\"customFields\":{\"curriculo_lattes_url\":\"https://example.invalid/prova\"}}"))
        .andExpect(status().isOk());
  }

  @Test
  void statisticsExecuteAndRejectIdentifierMeasures() throws Exception {
    String base = "/api/ergon/vinculos/stats/";
    mvc.perform(
            post(base + "timeseries")
                .contentType("application/json")
                .content(
                    "{\"filter\":{},\"field\":\"dtexerc\",\"granularity\":\"MONTH\",\"from\":\"2026-01-01\",\"to\":\"2026-10-01\",\"metric\":{\"operation\":\"COUNT\"}}"))
        .andExpect(status().isOk());
    mvc.perform(
            post(base + "distribution")
                .contentType("application/json")
                .content(
                    "{\"filter\":{},\"field\":\"jornadaSemanal\",\"mode\":\"HISTOGRAM\",\"bucketSize\":10,\"metric\":{\"operation\":\"COUNT\"}}"))
        .andExpect(status().isOk());
    mvc.perform(
            post(base + "comparison")
                .contentType("application/json")
                .content(
                    "{\"filter\":{},\"field\":\"situacao\",\"periodField\":\"dtexerc\",\"metrics\":[{\"operation\":\"COUNT\"}],\"period\":{\"from\":\"2026-01-01\",\"to\":\"2026-03-31\",\"timezone\":\"UTC\",\"mode\":\"PREVIOUS_ALIGNED\"}}"))
        .andExpect(status().isOk());
    mvc.perform(
            post(base + "group-by")
                .contentType("application/json")
                .content(
                    "{\"filter\":{},\"field\":\"situacao\",\"metric\":{\"operation\":\"SUM\",\"field\":\"numfunc\"}}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void mappingCoversEveryDocumentedColumn() {
    int total = 0;
    for (var entity : em.getMetamodel().getEntities()) {
      var type = entity.getJavaType();
      var columns = columns(type);
      total += columns.size();
      String table = type.getAnnotation(jakarta.persistence.Table.class).name();
      var actual =
          em.createNativeQuery(
                  "select column_name from information_schema.columns where table_schema='public' and table_name=:table",
                  String.class)
              .setParameter("table", table)
              .getResultList();
      assertThat(columns).containsExactlyInAnyOrderElementsOf(actual);
    }
    assertThat(total).isEqualTo(392);
  }

  private java.util.Set<String> columns(Class<?> type) {
    var names = new java.util.HashSet<String>();
    for (var field : type.getDeclaredFields()) {
      var column = field.getAnnotation(jakarta.persistence.Column.class);
      if (column != null) {
        names.add(column.name());
        assertThat(field.getAnnotation(org.hibernate.annotations.Comment.class)).isNotNull();
      }
      if (field.isAnnotationPresent(jakarta.persistence.Embedded.class)
          || field.isAnnotationPresent(jakarta.persistence.EmbeddedId.class))
        names.addAll(columns(field.getType()));
    }
    return names;
  }

  @Test
  void vacanciaEnforcesVersionAndPersistsTransition() throws Exception {
    String uri = "/api/ergon/vinculos/1001:1";
    String etag =
        mvc.perform(get(uri))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader("ETag");
    String body =
        "{\"data\":\"2026-10-01\",\"forma\":\"EXONERACAO\",\"motivo\":\"Ato de teste de integração com rollback obrigatório.\"}";
    mvc.perform(
            post(uri + "/actions/registrar-vacancia").contentType("application/json").content(body))
        .andExpect(status().isPreconditionRequired());
    mvc.perform(
            post(uri + "/actions/registrar-vacancia")
                .header("If-Match", etag)
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.situacao").value("VACANTE"));
    mvc.perform(
            post(uri + "/actions/registrar-vacancia")
                .header("If-Match", etag)
                .contentType("application/json")
                .content(body))
        .andExpect(status().isPreconditionFailed());
  }
}
