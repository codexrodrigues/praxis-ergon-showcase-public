package br.com.praxis.ergonshowcase;

import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.controller.PeriodoAquisitivoFeriasController;
import br.com.praxis.ergonshowcase.controller.FeriasGozoController;
import br.com.praxis.ergonshowcase.mapper.FeriasGozoMapper;
import br.com.praxis.ergonshowcase.mapper.PeriodoAquisitivoFeriasMapper;
import br.com.praxis.ergonshowcase.service.ErgonDeterminationService;
import br.com.praxis.ergonshowcase.service.PeriodoAquisitivoFeriasService;
import br.com.praxis.ergonshowcase.service.FeriasGozoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.concurrency.ResourceRepresentationResult;
import org.praxisplatform.uischema.concurrency.ResourceVersionEtagService;
import org.praxisplatform.uischema.service.base.BaseCreateUpdateResourceCommandService.SavedResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Actual Spring MVC/Jackson/Bean Validation for the published DTOs; no database or domain parity claim. */
class P5BPreviewTransportTest {
  final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
      .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @Test void periodDeterminationCanBeSubmittedToTheRealPostAndPutMappings() throws Exception {
    var tenant = new TenantContext();
    var mapper = new PeriodoAquisitivoFeriasMapper(tenant);
    var service = mock(PeriodoAquisitivoFeriasService.class);
    var officialDate = java.time.LocalDate.of(2028, 1, 31);
    var entity = new br.com.praxis.ergonshowcase.domain.PeriodoAquisitivoFerias();
    entity.setId(20L);
    when(service.create(any(CreatePeriodoAquisitivoFeriasDTO.class))).thenAnswer(call -> {
      CreatePeriodoAquisitivoFeriasDTO dto = call.getArgument(0);
      assertNull(dto.getDiasDireitoPrevistos());
      assertNull(dto.getFaltasApuradasPrevistas());
      assertNull(dto.getDataPrescricaoPrevista());
      mapper.applyUpdate(entity, json.convertValue(dto, UpdatePeriodoAquisitivoFeriasDTO.class));
      assertEquals(officialDate, entity.getDtPrescricao());
      assertEquals(30, entity.getTotDiasDireito());
      var response = new PeriodoAquisitivoFeriasResponseDTO();
      response.setId(20L);
      return new SavedResult<>(20L, response);
    });
    when(service.update(eq(20L), any(UpdatePeriodoAquisitivoFeriasDTO.class), any())).thenAnswer(call -> {
      UpdatePeriodoAquisitivoFeriasDTO dto = call.getArgument(1);
      assertNull(dto.getDiasDireitoPrevistos());
      assertNull(dto.getFaltasApuradasPrevistas());
      assertNull(dto.getDataPrescricaoPrevista());
      mapper.applyUpdate(entity, dto);
      assertEquals(officialDate, entity.getDtPrescricao());
      assertEquals(30, entity.getTotDiasDireito());
      var response = new PeriodoAquisitivoFeriasResponseDTO();
      response.setId(20L);
      return ResourceRepresentationResult.versioned(response, 2L);
    });
    var etags = new ResourceVersionEtagService("p5c-preview-transport-secret");
    var scope = new org.praxisplatform.uischema.concurrency.ResourceVersionScope("ergon|org:1");
    var controller = new PeriodoAquisitivoFeriasController(service, etags, () -> scope);
    ReflectionTestUtils.setField(controller, "determinations", new ErgonDeterminationService(tenant, null));
    ReflectionTestUtils.invokeMethod(controller, "initializeBasePath");
    var header = etags.create(scope, "administracao-pessoal.periodos-aquisitivos-ferias", 20L, 1L);
    var request = mock(jakarta.servlet.http.HttpServletRequest.class);
    when(request.getHeader("If-Match")).thenReturn(header);
    ReflectionTestUtils.setField(controller, "request", request);
    var mvc = MockMvcBuilders.standaloneSetup(controller)
        .setMessageConverters(new MappingJackson2HttpMessageConverter(json)).build();
    var body = json.createObjectNode().put("empCodigo", 1).put("numfunc", 1001).put("numvinc", 1)
        .put("dtini", "2025-01-01").put("dtfim", "2025-12-31");
    var computed = mvc.perform(post(ApiPaths.PERIODOS_AQUISITIVOS_FERIAS + "/determinations/previa")
        .contentType(MediaType.APPLICATION_JSON).content(body.toString())).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    var preview = json.readTree(computed);
    assertEquals("2027-12-31", preview.path("dtPrescricao").asText());
    body.put("chave", 1).put("exercicio", 2026);
    body.set("dataPrescricaoPrevista", preview.get("dtPrescricao"));
    body.set("diasDireitoPrevistos", preview.get("totDiasDireito"));
    body.set("faltasApuradasPrevistas", preview.get("faltasApuradas"));
    body.put("dtPrescricao", officialDate.toString());
    mvc.perform(post(ApiPaths.PERIODOS_AQUISITIVOS_FERIAS).contentType(MediaType.APPLICATION_JSON)
        .content(body.toString())).andExpect(status().isCreated());
    mvc.perform(put(ApiPaths.PERIODOS_AQUISITIVOS_FERIAS + "/20").header("If-Match", header)
        .contentType(MediaType.APPLICATION_JSON).content(body.toString())).andExpect(status().isOk());
  }

  @Test void populatedDeterminationOutputsAreIgnoredOnCreateAndEditWhileWritableInputsRemainValidated() throws Exception {
    var mvc = MockMvcBuilders.standaloneSetup(new FormBoundary())
        .setMessageConverters(new MappingJackson2HttpMessageConverter(json)).build();
    var gozo = json.createObjectNode().put("periodoAquisitivoId", 20).put("ordemParcela", 1)
        .put("dtini", "2026-07-01").put("dtprevfim", "2026-07-10").put("diasGozo", 10)
        .put("diasGozoPrevistos", 999).put("dtprevfimPrevista", "2099-01-01")
        .put("diasAbonoPrevistos", 999).put("saldoPaAposParcela", 999);
    var periodo = json.createObjectNode().put("empCodigo", 1).put("numfunc", 1001).put("numvinc", 1)
        .put("chave", 1).put("exercicio", 2026).put("dtini", "2025-01-01").put("dtfim", "2025-12-31")
        .put("dtfimPrevista", "2099-01-01").put("diasDireitoPrevistos", 999)
        .put("saldoDiasDisponivelPrevisto", 999).put("faltasApuradasPrevistas", 999).put("diasSuspensivosPrevistos", 999);
    for (var request : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[]{post("/form/gozo"), put("/form/gozo")})
      mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(gozo.toString()))
          .andExpect(status().is2xxSuccessful()).andExpect(jsonPath("$.days").value(10))
          .andExpect(jsonPath("$.previewsIgnored").value(true));
    for (var request : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[]{post("/form/periodo"), put("/form/periodo")})
      mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(periodo.toString()))
          .andExpect(status().is2xxSuccessful()).andExpect(jsonPath("$.days").value(30))
          .andExpect(jsonPath("$.previewsIgnored").value(true));
    gozo.put("diasGozo", -1);
    mvc.perform(post("/form/gozo").contentType(MediaType.APPLICATION_JSON).content(gozo.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test void gozoDeterminationCanBeSubmittedAndCannotReplaceTheWritableDays() throws Exception {
    var tenant = new TenantContext();
    var mapper = new FeriasGozoMapper(tenant);
    var service = mock(FeriasGozoService.class);
    when(service.create(any(CreateFeriasGozoDTO.class))).thenAnswer(call -> {
      CreateFeriasGozoDTO dto = call.getArgument(0);
      assertNull(dto.getDiasGozoPrevistos());
      assertNull(dto.getDiasAbonoPrevistos());
      var entity = mapper.newEntity(dto);
      assertEquals(10, entity.getDiasGozo());
      assertEquals(0, entity.getDiasAbonoVendidos());
      var response = new FeriasGozoResponseDTO();
      response.setId(30L);
      return new SavedResult<>(30L, response);
    });
    when(service.update(eq(30L), any(UpdateFeriasGozoDTO.class), any())).thenAnswer(call -> {
      UpdateFeriasGozoDTO dto = call.getArgument(1);
      assertNull(dto.getDiasGozoPrevistos());
      assertNull(dto.getDiasAbonoPrevistos());
      var entity = new br.com.praxis.ergonshowcase.domain.FeriasGozo();
      mapper.applyUpdate(entity, dto);
      assertEquals(10, entity.getDiasGozo());
      assertEquals(0, entity.getDiasAbonoVendidos());
      var response = new FeriasGozoResponseDTO();
      response.setId(30L);
      return ResourceRepresentationResult.versioned(response, 2L);
    });
    var etags = new ResourceVersionEtagService("p5c-preview-transport-secret");
    var scope = new org.praxisplatform.uischema.concurrency.ResourceVersionScope("ergon|org:1");
    var controller = new FeriasGozoController(service, etags, () -> scope);
    ReflectionTestUtils.setField(controller, "determinations", new ErgonDeterminationService(tenant, null));
    ReflectionTestUtils.invokeMethod(controller, "initializeBasePath");
    var header = etags.create(scope, "administracao-pessoal.ferias-gozo", 30L, 1L);
    var request = mock(jakarta.servlet.http.HttpServletRequest.class);
    when(request.getHeader("If-Match")).thenReturn(header);
    ReflectionTestUtils.setField(controller, "request", request);
    var mvc = MockMvcBuilders.standaloneSetup(controller)
        .setMessageConverters(new MappingJackson2HttpMessageConverter(json)).build();
    var body = json.createObjectNode().put("dtini", "2026-07-01").put("dtprevfim", "2026-07-10")
        .put("abonoPecuniario", false);
    var previewResponse = mvc.perform(post(ApiPaths.FERIAS_GOZO + "/determinations/previa")
        .contentType(MediaType.APPLICATION_JSON).content(body.toString())).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    var preview = json.readTree(previewResponse);
    assertEquals(10, preview.path("diasGozo").asInt());
    body.put("periodoAquisitivoId", 20).put("ordemParcela", 1).put("diasGozo", 10);
    body.set("diasGozoPrevistos", preview.get("diasGozo"));
    body.set("diasAbonoPrevistos", preview.get("diasAbonoVendidos"));
    mvc.perform(post(ApiPaths.FERIAS_GOZO).contentType(MediaType.APPLICATION_JSON)
        .content(body.toString())).andExpect(status().isCreated());
    body.put("diasGozoPrevistos", 999).put("diasAbonoPrevistos", 999);
    mvc.perform(put(ApiPaths.FERIAS_GOZO + "/30").header("If-Match", header)
        .contentType(MediaType.APPLICATION_JSON).content(body.toString())).andExpect(status().isOk());
  }

  @Test void saleIdentityIsHydratedForTheFormButNeverBoundFromAWriteRequest() throws Exception {
    var input = json.readValue("{\"id\":987,\"diasVendidos\":10}", UpdateVendaLicencaEspecialDTO.class);
    org.junit.jupiter.api.Assertions.assertNull(input.getId());
    input.setId(91L);
    org.junit.jupiter.api.Assertions.assertEquals(91L, json.valueToTree(input).path("id").longValue());
  }

  @RestController static class FormBoundary {
    @PostMapping("/form/gozo") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> createGozo(@Valid @RequestBody CreateFeriasGozoDTO d) { return gozo(d); }
    @PutMapping("/form/gozo") Map<String,Object> updateGozo(@Valid @RequestBody UpdateFeriasGozoDTO d) { return gozo(d); }
    @PostMapping("/form/periodo") @ResponseStatus(HttpStatus.CREATED)
    Map<String,Object> createPeriodo(@Valid @RequestBody CreatePeriodoAquisitivoFeriasDTO d) { return periodo(d); }
    @PutMapping("/form/periodo") Map<String,Object> updatePeriodo(@Valid @RequestBody UpdatePeriodoAquisitivoFeriasDTO d) { return periodo(d); }
    static Map<String,Object> gozo(FeriasGozoDados d) {
      return Map.of("days", d.getDiasGozo(), "previewsIgnored", d.getDiasGozoPrevistos()==null
          && d.getDtprevfimPrevista()==null && d.getDiasAbonoPrevistos()==null && d.getSaldoPaAposParcela()==null);
    }
    static Map<String,Object> periodo(PeriodoAquisitivoFeriasDados d) {
      return Map.of("days", d.getTotDiasDireito(), "previewsIgnored", d.getDtfimPrevista()==null
          && d.getDiasDireitoPrevistos()==null && d.getSaldoDiasDisponivelPrevisto()==null
          && d.getFaltasApuradasPrevistas()==null && d.getDiasSuspensivosPrevistos()==null);
    }
  }
}
