package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.praxis.ergonshowcase.config.*;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.repository.*;
import br.com.praxis.ergonshowcase.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.*;
import jakarta.persistence.criteria.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.capability.*;
import org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition;
import org.praxisplatform.uischema.customfields.*;
import org.praxisplatform.uischema.extension.CustomOpenApiResolver;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder;
import org.praxisplatform.uischema.options.*;
import org.praxisplatform.uischema.options.service.OptionSourceQueryExecutor;
import org.praxisplatform.uischema.service.base.*;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** Executa os caminhos que a revisão demonstrou não estarem cobertos pelo scorecard. */
class TempoLicencaCorrectionsContractTest {
  private List<TempoLicencaFilterDTO> filters() {
    return List.of(new AverbacaoTempoServicoFilterDTO(), new PeriodoAquisitivoLicencaEspecialFilterDTO(),
        new LicencaEspecialGozoFilterDTO(), new VendaLicencaEspecialFilterDTO(), new RadarTempoServicoLicencaEspecialFilterDTO());
  }

  @Test @SuppressWarnings({"unchecked", "rawtypes"})
  void selectedExecutorBuildsRealPredicatesForCommonFieldsInAllFiveFilters() {
    for (var filter : filters()) {
      assertThat(Arrays.stream(filter.getClass().getDeclaredFields())
          .filter(f -> f.isAnnotationPresent(Filterable.class)).map(java.lang.reflect.Field::getName))
          .contains("empCodigo", "numfunc", "numvinc", "situacao");
      var bean = new BeanWrapperImpl(filter);
      bean.setPropertyValue("empCodigo", 2); bean.setPropertyValue("numfunc", 1001L);
      bean.setPropertyValue("numvinc", 3); bean.setPropertyValue("situacao", "DISPONIVEL");
      Root<Object> root = mock(Root.class, RETURNS_DEEP_STUBS);
      CriteriaBuilder cb = mock(CriteriaBuilder.class);
      new GenericSpecificationsBuilder<>().buildSpecification(filter, Pageable.unpaged())
          .spec().toPredicate(root, mock(CriteriaQuery.class), cb);
      verify(cb).equal(root.get("empCodigo"), 2);
      verify(cb).equal(root.get("numfunc"), 1001L);
      verify(cb).equal(root.get("numvinc"), 3);
      verify(cb).equal(root.get("situacao"), "DISPONIVEL");
    }
  }

  private TenantContext tenant() {
    var tenant = mock(TenantContext.class); when(tenant.orgao()).thenReturn(1); return tenant;
  }

  @Test @SuppressWarnings({"rawtypes", "unchecked"})
  void everyLookupPathReceivesExecutableTenantPredicateAndRejectsForeignContext() {
    var registry = new OptionSourcesConfig().options();
    var services = List.of(
        new AverbacaoTempoServicoService(mock(AverbacaoTempoServicoRepository.class), Mappers.getMapper(AverbacaoTempoServicoMapper.class), tenant()),
        new PeriodoAquisitivoLicencaEspecialService(mock(PeriodoAquisitivoLicencaEspecialRepository.class), Mappers.getMapper(PeriodoAquisitivoLicencaEspecialMapper.class), tenant()),
        new LicencaEspecialGozoService(mock(LicencaEspecialGozoRepository.class), Mappers.getMapper(LicencaEspecialGozoMapper.class), tenant()),
        new VendaLicencaEspecialService(mock(VendaLicencaEspecialRepository.class), Mappers.getMapper(VendaLicencaEspecialMapper.class), tenant()));
    var keys = List.of("averbacaoTempoServico", "periodoAquisitivoLicencaEspecial", "licencaEspecialGozo", "vendaLicencaEspecial");
    for (int i = 0; i < services.size(); i++) {
      AbstractBaseQueryResourceService service = services.get(i);
      String key = keys.get(i);
      var executor = mock(OptionSourceQueryExecutor.class);
      ObjectProvider provider = mock(ObjectProvider.class); when(provider.getIfAvailable()).thenReturn(registry);
      ReflectionTestUtils.setField(service, "optionSourceRegistryProvider", provider);
      ReflectionTestUtils.setField(service, "optionSourceQueryExecutor", executor);
      var dependent = filters().get(i);
      var dependentBean = new BeanWrapperImpl(dependent);
      dependentBean.setPropertyValue("numfunc", 1001L);
      dependentBean.setPropertyValue("numvinc", 3);
      service.byIdsOptionSourceOptions(key, List.of(91L));
      service.byIdsOptionSourceOptions(key, new OptionSourceByIdsRequest(dependent, List.of(), List.of(91L)));
      service.filterOptionSourceOptions(key, new OptionSourceFilterRequest(dependent, List.of(), null, null, null, List.of()), Pageable.unpaged());
      assertThat(mockingDetails(executor).getInvocations()).hasSize(3);
      for (var invocation : mockingDetails(executor).getInvocations()) {
        var effective = (TempoLicencaFilterDTO) invocation.getArgument(3);
        assertThat(effective.getEmpCodigo()).isEqualTo(1);
        org.springframework.data.jpa.domain.Specification spec = invocation.getArgument(2);
        assertThat(spec).isNotNull();
        Root root = mock(Root.class, RETURNS_DEEP_STUBS); CriteriaBuilder cb = mock(CriteriaBuilder.class);
        spec.toPredicate(root, mock(CriteriaQuery.class), cb);
        verify(cb,times(2)).equal(root.get("empCodigo"), 1);
        if (new BeanWrapperImpl(effective).getPropertyValue("numfunc") != null) {
          verify(cb).equal(root.get("numfunc"), 1001L);
          verify(cb).equal(root.get("numvinc"), 3);
        }
      }
      TempoLicencaFilterDTO foreign = filters().get(i); foreign.setEmpCodigo(2);
      assertThatThrownBy(() -> service.byIdsOptionSourceOptions(key,
          new OptionSourceByIdsRequest(foreign, List.of(), List.of(91L))))
          .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
      assertThat(mockingDetails(executor).getInvocations()).hasSize(3);
    }
  }

  @Test @SuppressWarnings({"unchecked", "rawtypes"})
  void declaredCustomContainersBuildJsonbPredicatesForAllFiveEntities() {
    var definition = new CustomFieldDescriptor("origem", "Origem", "Origem da certidão institucional",
        CustomFieldDataType.STRING, FieldControlType.INPUT, "Certidão", 1, false, false, true, false, false,
        null, null, null, null, null, null, null, null, List.of(), List.of(), null,
        Filterable.FilterOperation.EQUAL, Map.of());
    for (var entity : List.of(AverbacaoTempoServico.class, PeriodoAquisitivoLicencaEspecial.class,
        LicencaEspecialGozo.class, VendaLicencaEspecial.class, RadarTempoServicoLicencaEspecial.class)) {
      Root root = mock(Root.class, RETURNS_DEEP_STUBS); doReturn(entity).when(root).getJavaType();
      CriteriaBuilder cb = mock(CriteriaBuilder.class); Expression<String> text = mock(Expression.class);
      when(cb.function(eq("jsonb_extract_path_text"), eq(String.class), any(), any())).thenReturn(text);
      CustomFieldJpaCriteriaSupport.predicates(Map.of("origem", "certidao"), List.of(definition), root, cb);
      verify(cb).equal(text, "certidao");
      assertThatThrownBy(() -> CustomFieldJpaCriteriaSupport.predicates(Map.of("naoDeclarado", "x"), List.of(definition), root, cb))
          .hasMessageContaining("not filterable");
    }
  }

  @Test void hostAvailabilityNoLongerContainsTemporaryWriteDenial() {
    var host = new CargoDraftAvailability(mock(CargoRepository.class), tenant());
    org.springframework.security.core.context.SecurityContextHolder.clearContext();
    for(String key:List.of("averbacoes-tempo-servico","periodos-aquisitivos-licenca-especial","licencas-especiais-gozo","vendas-licenca-especial"))
      assertThat(host.evaluate(ResourceOperationAvailabilityContext.collection("administracao-pessoal."+key,"/proof","create")).allowed()).isFalse();
    try {
      org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
          new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("operator","test",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ERGON_WRITE"))));
      for(String key:List.of("averbacoes-tempo-servico","periodos-aquisitivos-licenca-especial","licencas-especiais-gozo","vendas-licenca-especial"))
        assertThat(host.evaluate(ResourceOperationAvailabilityContext.collection("administracao-pessoal."+key,"/proof","create")).allowed()).isTrue();
    } finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
  }

  @Test void anticipationChoicePreservesAllThreeMeaningsAndGeneralParameterStaysBoolean() throws Exception {
    var converter = new PoliticaAntecipacaoLicencaConverter(); var json = new ObjectMapper();
    for (var policy : PoliticaAntecipacaoLicenca.values()) {
      assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(policy))).isEqualTo(policy);
      var dto = new CreatePeriodoAquisitivoLicencaEspecialDTO(); dto.setPermGozoLicEspAntFimPe(policy);
      assertThat(json.readValue(json.writeValueAsString(dto), CreatePeriodoAquisitivoLicencaEspecialDTO.class).getPermGozoLicEspAntFimPe()).isEqualTo(policy);
    }
    assertThat(new CreateParametroContagemFeriasDTO().getPermGozoLicEspAntFimPe()).isFalse();
  }

  @Test void emittedUxProvidesThreeChoicesAndSaleDateAndPayment() {
    var converters = new ModelConverters(); var mapper = new ObjectMapper().findAndRegisterModules();
    converters.addConverter(new CustomOpenApiResolver(mapper));
    var period = converters.resolveAsResolvedSchema(new AnnotatedType(PeriodoAquisitivoLicencaEspecialResponseDTO.class).resolveAsRef(false)).schema;
    var policy = (io.swagger.v3.oas.models.media.Schema<?>) period.getProperties().get("permGozoLicEspAntFimPe");
    assertThat(policy.getEnum()).extracting(Object::toString).containsExactly("HERDAR", "PERMITIR", "VEDAR");
    assertThat(mapper.valueToTree(policy.getExtensions().get("x-ui")).path("controlType").asText()).isEqualTo("select");
    var sale = converters.resolveAsResolvedSchema(new AnnotatedType(VendaLicencaEspecialResponseDTO.class).resolveAsRef(false)).schema;
    var date = (io.swagger.v3.oas.models.media.Schema<?>) sale.getProperties().get("dtini");
    assertThat(mapper.valueToTree(date.getExtensions().get("x-ui")).path("label").asText()).isEqualTo("Data da venda");
    var payment = (io.swagger.v3.oas.models.media.Schema<?>) sale.getProperties().get("dtPagamento");
    assertThat(mapper.valueToTree(payment.getExtensions().get("x-ui")).path("quickViewVisible").asBoolean()).isTrue();
  }

  @Test void radarDoesNotCertifyIncompletePersistedRevision() {
    var repository = mock(RadarTempoServicoLicencaEspecialRepository.class);
    var row = new RadarTempoServicoLicencaEspecial(); row.setId(91L); row.setEmpCodigo(1); row.setVersion(7L);
    when(repository.findById(91L)).thenReturn(Optional.of(row));
    var service = new RadarTempoServicoLicencaEspecialService(repository, Mappers.getMapper(RadarTempoServicoLicencaEspecialMapper.class), tenant());
    assertThat(service.findById(91L).persistedVersion()).isEmpty();
  }

  @Test void radarFilterAcceptsCompositeVinculoIdAndReadFactsBatchHydratesPostWriteDisplayFields() throws Exception {
    var radarFilter = new RadarTempoServicoLicencaEspecialFilterDTO();
    radarFilter.setVinculoId(new VinculoId(1001L, 2));
    assertThat(radarFilter.getNumfunc()).isEqualTo(1001L);
    assertThat(radarFilter.getNumvinc()).isEqualTo(2);
    assertThat(radarFilter.getVinculoId()).isEqualTo(new VinculoId(1001L, 2));
    assertThat(VendaLicencaEspecialRepository.class.getMethod("findAll").getAnnotation(org.springframework.data.jpa.repository.EntityGraph.class).attributePaths())
        .contains("licencaEspecialGozo");

    var em = mock(jakarta.persistence.EntityManager.class);
    var qOrgao = mock(jakarta.persistence.TypedQuery.class);
    var qPessoa = mock(jakarta.persistence.TypedQuery.class);
    var qVinculo = mock(jakarta.persistence.TypedQuery.class);
    var qDoc = mock(jakarta.persistence.TypedQuery.class);
    when(em.createQuery(contains("from Orgao"), eq(Object[].class))).thenReturn(qOrgao);
    when(em.createQuery(contains("from ServidorPessoa"), eq(Object[].class))).thenReturn(qPessoa);
    when(em.createQuery(contains("from Vinculo"), eq(Object[].class))).thenReturn(qVinculo);
    when(em.createQuery(contains("from DocumentoLegal"), eq(Object[].class))).thenReturn(qDoc);
    when(qOrgao.setParameter(eq("ids"), any())).thenReturn(qOrgao);
    when(qPessoa.setParameter(eq("ids"), any())).thenReturn(qPessoa);
    when(qVinculo.setParameter(eq("ids"), any())).thenReturn(qVinculo);
    when(qDoc.setParameter(eq("ids"), any())).thenReturn(qDoc);
    when(qOrgao.getResultList()).thenReturn(List.<Object[]>of(new Object[]{1, "SEFAZ"}));
    when(qPessoa.getResultList()).thenReturn(List.<Object[]>of(new Object[]{1001L, "Maria"}));
    when(qVinculo.getResultList()).thenReturn(List.<Object[]>of(new Object[]{new VinculoId(1001L, 1), "MAT-01"}));
    when(qDoc.getResultList()).thenReturn(List.<Object[]>of(new Object[]{10L, "PORT-10"}, new Object[]{11L, "DOE-11"}));

    var facts = new ErgonReadFacts();
    ReflectionTestUtils.setField(facts, "em", em);
    var pa = new PeriodoAquisitivoLicencaEspecialResponseDTO();
    pa.setEmpCodigo(1); pa.setNumfunc(1001L); pa.setNumvinc(1);
    pa.setDocumentoLegalId(10L); pa.setAtoPublicacaoId(11L);
    facts.enrich(List.of(pa));

    assertThat(pa.getOrgaoNome()).isEqualTo("SEFAZ");
    assertThat(pa.getPessoaNome()).isEqualTo("Maria");
    assertThat(pa.getVinculoMatricula()).isEqualTo("MAT-01");
    assertThat(pa.getDocumentoLegalNumero()).isEqualTo("PORT-10");
    assertThat(pa.getAtoPublicacaoNumero()).isEqualTo("DOE-11");
  }

  private void unusedMarker() {
  }
}
