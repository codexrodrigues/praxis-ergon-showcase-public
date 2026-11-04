package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.praxis.ergonshowcase.bulk.*;
import br.com.praxis.ergonshowcase.config.*;
import br.com.praxis.ergonshowcase.controller.*;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.repository.*;
import br.com.praxis.ergonshowcase.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.bulk.*;
import org.praxisplatform.uischema.exporting.*;
import org.praxisplatform.uischema.extension.CustomOpenApiResolver;
import org.praxisplatform.uischema.stats.*;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;

/** Compila os contratos consumidos pelo SDK e executa os engines de exportação reais. */
@SuppressWarnings({"rawtypes","unchecked"})
class TempoLicencaBulkAnalyticsExportTest {
  final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
  final TempoLicencaCrudContractTest h=new TempoLicencaCrudContractTest();
  BulkEditableFields fields(Class<?> dto,Class<?> controller){
    var converter=new ModelConverters();converter.addConverter(new CustomOpenApiResolver(json));
    var schema=converter.resolveAsResolvedSchema(new AnnotatedType(dto).resolveAsRef(false)).schema;
    return BulkEditableFields.compile(json,json.constructType(dto),io.swagger.v3.core.util.Json.mapper().valueToTree(schema),io.swagger.v3.oas.models.SpecVersion.V30,Set.of(controller.getAnnotation(BulkResourceOperations.class).protectedUpdateFields()));
  }
  @TestFactory Stream<DynamicTest> realUpdateSchemasCompileBothModesAndProtectWorkflowFields(){
    var types=List.of(UpdateAverbacaoTempoServicoDTO.class,UpdatePeriodoAquisitivoLicencaEspecialDTO.class,UpdateLicencaEspecialGozoDTO.class,UpdateVendaLicencaEspecialDTO.class);
    return java.util.stream.IntStream.range(0,4).mapToObj(i->DynamicTest.dynamicTest(types.get(i).getSimpleName(),()->{
      var compiled=fields(types.get(i),TempoLicencaSlice6Phase2Test.CONTROLLERS.get(i));
      for(var mode:List.of(BulkMode.UNIFORM_UPDATE,BulkMode.PER_ITEM_UPDATE)){
        assertThat(compiled.writableFields(mode)).contains("descricao","observacoes").doesNotContain("situacao","id","empCodigo","numfunc","numvinc","dtPagamento","diasDesaverbados","saldoDias","diasConvertidosDobro");
        assertThat(compiled.writableFields(mode).size()).isGreaterThanOrEqualTo(7);
      }
      if(i==2)assertThat(compiled.clearableFields(BulkMode.PER_ITEM_UPDATE)).contains("numfuncSubstituto","iniFimAbono");
      if(i==3)assertThat(compiled.clearableFields(BulkMode.PER_ITEM_UPDATE)).contains("valorIndenizacao");
    }));
  }
  @Test void bothBulkModesPreserveOmittedFieldsAndRejectWrongTypesBeforeAnyMutation(){
    var f=new TempoLicencaCrudContractTest.Fixture();var s=(LicencaEspecialGozoService)f.service(2)[0];s.create((CreateLicencaEspecialGozoDTO)h.create(2));var original=f.gozos.getFirst();
    var domain=new ErgonBulkDomain();ReflectionTestUtils.setField(domain,"em",f.em);ReflectionTestUtils.setField(domain,"tenant",f.tenant);ReflectionTestUtils.setField(domain,"json",json);ReflectionTestUtils.setField(domain,"licencaEspecialGozo",Mappers.getMapper(LicencaEspecialGozoMapper.class));ReflectionTestUtils.setField(domain,"licencaEspecialGozoService",s);
    try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()){
      ReflectionTestUtils.setField(domain,"validator",factory.getValidator());var compiled=fields(UpdateLicencaEspecialGozoDTO.class,LicencaEspecialGozoController.class);
      for(var mode:List.of(BulkMode.UNIFORM_UPDATE,BulkMode.PER_ITEM_UPDATE)){
        var dto=(UpdateLicencaEspecialGozoDTO)BulkCandidateFixture.candidate(s,original,List.of(BulkFieldChange.set("observacoes",json.valueToTree("Revisão coletiva"))),compiled,mode);
        assertThat(dto.getDiasGozo()).isEqualTo(10);assertThat(dto.getAbonoPecuniario()).isFalse();assertThat(dto.getPeriodoAquisitivoLicencaId()).isEqualTo(20L);assertThat(original.getObservacoes()).isNull();
        assertThatThrownBy(()->BulkCandidateFixture.candidate(s,original,List.of(BulkFieldChange.set("diasGozo",json.valueToTree("10"))),compiled,mode)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->BulkCandidateFixture.candidate(s,original,List.of(BulkFieldChange.set("situacao",json.valueToTree("HOMOLOGADA"))),compiled,mode)).isInstanceOf(IllegalArgumentException.class);
        original.setAbonoPecuniario(true);original.setDiasAbono(5);original.setIniFimAbono("F");
        var withoutAbono=(UpdateLicencaEspecialGozoDTO)BulkCandidateFixture.candidate(s,original,List.of(BulkFieldChange.set("abonoPecuniario",json.valueToTree(false)),BulkFieldChange.set("diasAbono",json.valueToTree(0)),BulkFieldChange.clear("iniFimAbono")),compiled,mode);
        assertThat(withoutAbono.getAbonoPecuniario()).isFalse();assertThat(withoutAbono.getDiasAbono()).isZero();assertThat(withoutAbono.getIniFimAbono()).isNull();assertThat(original.getAbonoPecuniario()).isTrue();
        original.setAbonoPecuniario(false);original.setDiasAbono(0);original.setIniFimAbono(null);
      }
    }
  }
  @Test void domainCommandCandidateDoesNotChangeManagedItemAndExecutionSynchronizesParent(){
    var f=new TempoLicencaCrudContractTest.Fixture();var s=(LicencaEspecialGozoService)f.service(2)[0];s.create((CreateLicencaEspecialGozoDTO)h.create(2));var original=f.gozos.getFirst();
    var domain=new ErgonBulkDomain();ReflectionTestUtils.setField(domain,"em",f.em);ReflectionTestUtils.setField(domain,"tenant",f.tenant);ReflectionTestUtils.setField(domain,"json",json);ReflectionTestUtils.setField(domain,"licencaEspecialGozo",Mappers.getMapper(LicencaEspecialGozoMapper.class));ReflectionTestUtils.setField(domain,"licencaEspecialGozoService",s);ReflectionTestUtils.setField(domain,"periodoAquisitivoLicencaEspecialService",(PeriodoAquisitivoLicencaEspecialService)f.service(1)[0]);
    try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()){
      ReflectionTestUtils.setField(domain, "invariants", new ErgonInvariants());
      ReflectionTestUtils.setField(domain, "validator", factory.getValidator());
      String action = "ergon.licencas-especiais-gozo.homologar-licencas-especiais-lote";
      f.pa.setSituacao("PRESCRITO");
      assertThatThrownBy(() -> domain.commandCandidate(original, action, json.valueToTree(h.command())))
          .isInstanceOfSatisfying(br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException.class, rejection -> {
            assertThat(rejection.getHttpStatus()).isEqualTo(422);
            assertThat(rejection.getReasonCodes()).containsExactly("CERG-06303");
            assertThat(rejection.getViolations().getFirst().slotKey()).isNotBlank();
            assertThat(rejection.getViolations().getFirst().targetField()).isEqualTo("periodoAquisitivoLicencaId");
            assertThat(rejection.getViolations().getFirst().lineage()).isNotBlank();
          });
      assertThat(original.getSituacao()).isEqualTo("PROGRAMADA");
      f.pa.setSituacao("DISPONIVEL");
      var candidate = (LicencaEspecialGozo) domain.commandCandidate(original, action, json.valueToTree(h.command()));
      assertThat(candidate.getSituacao()).isEqualTo("HOMOLOGADA");assertThat(original.getSituacao()).isEqualTo("PROGRAMADA");assertThat(f.pa.getSaldoDias()).isEqualTo(80);
      assertThat(domain.applyCommand(original,candidate)).isTrue();assertThat(original.getSituacao()).isEqualTo("HOMOLOGADA");assertThat(f.pa.getSaldoDias()).isEqualTo(80);
    }
  }
  List<Object> services(){
    var f=new TempoLicencaCrudContractTest.Fixture();var all=new ArrayList<Object>();for(int i=0;i<4;i++)all.add(f.service(i)[0]);
    all.add(new RadarTempoServicoLicencaEspecialService(mock(RadarTempoServicoLicencaEspecialRepository.class),Mappers.getMapper(RadarTempoServicoLicencaEspecialMapper.class),f.tenant));all.forEach(CanonicalResourceTestContext::exports);return all;
  }
  @TestFactory Stream<DynamicTest> allFiveRegistriesUseRealEntityPropertiesAndEnableEveryStatsFamily(){
    var services=services();var entities=List.of(AverbacaoTempoServico.class,PeriodoAquisitivoLicencaEspecial.class,LicencaEspecialGozo.class,VendaLicencaEspecial.class,RadarTempoServicoLicencaEspecial.class);
    return java.util.stream.IntStream.range(0,5).mapToObj(i->DynamicTest.dynamicTest(entities.get(i).getSimpleName(),()->{
      var service=services.get(i);var registry=(StatsFieldRegistry)ReflectionTestUtils.invokeMethod(service,"getStatsFieldRegistry");var entity=new BeanWrapperImpl(entities.get(i).getConstructor().newInstance());
      for(var descriptor:registry.descriptors()){assertThat(entity.isReadableProperty(descriptor.field())).isTrue();assertThat(entity.isReadableProperty(descriptor.keyPropertyPath())).isTrue();}
      for(var operation:List.of("getGroupByStatsSupportMode","getTimeSeriesStatsSupportMode","getDistributionStatsSupportMode","getComparisonStatsSupportMode"))assertThat((StatsSupportMode)ReflectionTestUtils.invokeMethod(service,operation)).isEqualTo(StatsSupportMode.AUTO);
      var capability=(Optional<CollectionExportCapability>)ReflectionTestUtils.invokeMethod(service,"getCollectionExportCapability");assertThat(capability.orElseThrow().formats()).containsExactly(CollectionExportFormat.CSV,CollectionExportFormat.JSON,CollectionExportFormat.EXCEL);assertThat(capability.get().scopes()).contains(CollectionExportScope.SELECTED,CollectionExportScope.FILTERED,CollectionExportScope.CURRENT_PAGE,CollectionExportScope.ALL);
    }));
  }
  @Test void exportEnginesProduceCsvJsonExcelAndRejectRestrictedFieldSelection(){
    var service=services().get(3);var fields=(List<CollectionExportField>)ReflectionTestUtils.invokeMethod(service,"governedExportFields");assertThat(fields).extracting(CollectionExportField::key).doesNotContain("valorIndenizacao","observacoes","customFields");
    var executor=new DefaultCollectionExportExecutor(List.of(new CsvCollectionExportEngine(),new JsonCollectionExportEngine(json),new ExcelCollectionExportEngine()));
    var row=new VendaLicencaEspecialResponseDTO();row.setId(91L);row.setDescricao("=SUM(A1:A3)");row.setDiasVendidos(10);row.setDtini(LocalDate.of(2026,1,1));
    for(var format:List.of(CollectionExportFormat.CSV,CollectionExportFormat.JSON,CollectionExportFormat.EXCEL)){
      var request=new CollectionExportRequest<>(null,null,null,format,CollectionExportScope.FILTERED,null,List.of(),new VendaLicencaEspecialFilterDTO(),null,null,null,true,true,100,"vendas",Map.of());
      var result=executor.export(request,List.of(row),fields,CollectionExportValueResolver.reflective(),Map.of());assertThat(result.content()).isNotEmpty();assertThat(result.rowCount()).isEqualTo(1L);
      var denied=new CollectionExportRequest<>(null,null,null,format,CollectionExportScope.FILTERED,null,List.of(new CollectionExportField("valorIndenizacao","Valor",true,true,"number","valorIndenizacao")),new VendaLicencaEspecialFilterDTO(),null,null,null,true,true,100,"vendas",Map.of());
      assertThatThrownBy(()->executor.export(denied,List.of(row),fields,CollectionExportValueResolver.reflective(),Map.of())).hasMessageContaining("not supported");
    }
  }

  @Test void metadataUsesCorrectCollectiveBusinessMeaning() {
    var period=Arrays.stream(PeriodoAquisitivoLicencaEspecialController.class.getDeclaredMethods()).map(m->m.getAnnotation(WorkflowAction.class)).filter(Objects::nonNull).filter(a->a.scope()==org.praxisplatform.uischema.action.ActionScope.COLLECTION).findFirst().orElseThrow();
    assertThat(period.title()).isEqualTo("Reconciliar saldos em lote");assertThat(period.description()).contains("parcelas persistidas").doesNotContain("Homologa");
    var sales=Arrays.stream(VendaLicencaEspecialController.class.getDeclaredMethods()).map(m->m.getAnnotation(WorkflowAction.class)).filter(Objects::nonNull).filter(a->a.scope()==org.praxisplatform.uischema.action.ActionScope.COLLECTION).findFirst().orElseThrow();assertThat(sales.title()).isEqualTo("Autorizar vendas em lote");
  }
}
