package br.com.praxis.ergonshowcase;

import br.com.praxis.ergonshowcase.config.CamposCustomizadosProvider;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.DefinicaoCampoCustomizadoMapper;
import br.com.praxis.ergonshowcase.repository.DefinicaoCampoCustomizadoRepository;
import br.com.praxis.ergonshowcase.rules.*;
import br.com.praxis.ergonshowcase.service.DefinicaoCampoCustomizadoService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.customfields.*;
import org.praxisplatform.uischema.service.base.BaseResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class P4CustomFieldAdoptionRegressionTest {
    static CustomFieldDescriptor descriptor(Map<String,Object> expression) {
        return new CustomFieldDescriptor("extra","Extra","Validation used by adoption regression",CustomFieldDataType.INTEGER,
                null,null,0,false,false,false,false,false,null,null,null,null,null,null,null,null,
                List.of(),List.of(),null,null,Map.of(),CamposCustomizadosProvider.jsonLogicRules(expression));
    }
    @Test void optionalOmissionHasTheSameResultInTheHostFacadeAndTheNativeLifecycle() {
        var expression=Map.<String,Object>of(">=",List.of(Map.of("var","value"),4));
        var definition=new DefinicaoCampoCustomizado();definition.setFieldKey("extra");definition.setFieldType("INTEGER");
        definition.setResourceKey("administracao-pessoal.vinculos");definition.setActive(true);definition.setRequired(false);definition.setValidationRules(expression);
        var facade=new ErgonRuleEngineFacade();
        assertThatCode(()->facade.evaluateCustomFieldsJsonLogic(definition.getResourceKey(),1,Map.of(),new Vinculo(),List.of(definition))).doesNotThrowAnyException();
        var nativeValidator=new CustomFieldValidator((r,t,s)->List.of(descriptor(expression)));
        assertThatCode(()->nativeValidator.validateValues(Map.of(),List.of(descriptor(expression)))).doesNotThrowAnyException();
        assertThatThrownBy(()->nativeValidator.validateValues(Map.of("extra",0),List.of(descriptor(expression)))).isInstanceOf(CustomFieldValidationException.class);
        definition.setRequired(true);
        assertThatThrownBy(()->facade.evaluateCustomFieldsJsonLogic(definition.getResourceKey(),1,Map.of(),new Vinculo(),List.of(definition)))
                .isInstanceOfSatisfying(ErgonRuleViolationException.class,error->assertThat(error.getReasonCodes()).contains("ERG-21502"));
    }
    @Test void aTemporalDefinitionIsRejectedWithItsDomainCodeBeforeItCanBeSaved() {
        var repository=mock(DefinicaoCampoCustomizadoRepository.class);
        var service=new DefinicaoCampoCustomizadoService(repository,new DefinicaoCampoCustomizadoMapper(),new TenantContext(),new ErgonRuleEngineFacade());
        var input=new CreateDefinicaoCampoCustomizadoDTO();input.setEmpCodigo(1);input.setResourceKey("administracao-pessoal.vinculos");
        input.setFieldKey("extra");input.setFieldType("INTEGER");input.setControlType("NUMERIC_TEXT_BOX");input.setLabel("Extra");
        input.setValidationRules(Map.of("<",List.of(Map.of("var","value"),Map.of("now",List.of()))));
        assertThatThrownBy(()->service.create(input)).isInstanceOfSatisfying(ErgonRuleViolationException.class,error->{
            assertThat(error.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(error.getReasonCodes()).contains("ERG-21501");
        });
        verify(repository,never()).save(any());
        assertThatThrownBy(()->descriptor(input.getValidationRules())).isInstanceOf(IllegalArgumentException.class);
        var pure=Map.<String,Object>of("<=",List.of(Map.of("var","value"),10));
        assertThatCode(()->new ErgonRuleEngineFacade().validateCustomFieldValidationRules(pure)).doesNotThrowAnyException();
        assertThatCode(()->descriptor(pure)).doesNotThrowAnyException();
    }
    @Test @SuppressWarnings({"rawtypes","unchecked"}) void hydratedLicenseAndSaleDraftsAreValidatedWithOneNativeDefinitionSnapshot() {
        for(int kind:List.of(2,3)) {
            var fixture=new TempoLicencaCrudContractTest.Fixture();
            var service=(BaseResourceService)fixture.service(kind)[0];
            Object input=new TempoLicencaCrudContractTest().create(kind);
            var bean=new org.springframework.beans.BeanWrapperImpl(input);
            bean.setPropertyValue("empCodigo",null);bean.setPropertyValue("numfunc",null);bean.setPropertyValue("numvinc",null);
            bean.setPropertyValue("customFields",Map.of("extra",1));
            var reads=new java.util.concurrent.atomic.AtomicInteger();
            ReflectionTestUtils.setField(service,"customFieldValidator",new CustomFieldValidator((r,t,s)->{
                reads.incrementAndGet();return List.of(descriptor(Map.of("and",List.of(
                        Map.of("==",List.of(Map.of("var","draft.empCodigo"),1)),
                        Map.of("==",List.of(Map.of("var","draft.numfunc"),1001)),
                        Map.of("==",List.of(Map.of("var","draft.numvinc"),1))))));
            }));
            var result=service.create(input);
            var response=new org.springframework.beans.BeanWrapperImpl(result.body());
            assertThat(response.getPropertyValue("empCodigo")).isEqualTo(1);
            assertThat(response.getPropertyValue("numfunc")).isEqualTo(1001L);
            assertThat(reads.get()).isEqualTo(1);
        }
    }
    @Test @SuppressWarnings({"rawtypes","unchecked"}) void aDeniedHydratedDraftAndAForeignParentCannotBeSaved() {
        for(boolean foreign:List.of(false,true)) {
            var fixture=new TempoLicencaCrudContractTest.Fixture();
            var parts=fixture.service(2);var service=(BaseResourceService)parts[0];
            var repository=(org.praxisplatform.uischema.repository.base.BaseCrudRepository)parts[1];
            var input=(CreateLicencaEspecialGozoDTO)new TempoLicencaCrudContractTest().create(2);
            input.setEmpCodigo(null);input.setNumfunc(null);input.setNumvinc(null);input.setCustomFields(Map.of("extra",1));
            if(foreign)fixture.pa.setEmpCodigo(2);
            ReflectionTestUtils.setField(service,"customFieldValidator",new CustomFieldValidator((r,t,s)->List.of(
                    descriptor(Map.of("==",List.of(Map.of("var","draft.empCodigo"),foreign?1:2))))));
            if(foreign)assertThatThrownBy(()->service.create(input)).isInstanceOfSatisfying(ResponseStatusException.class,error->assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
            else assertThatThrownBy(()->service.create(input)).isInstanceOf(CustomFieldValidationException.class);
            verify(repository,never()).save(any());
        }
    }
}
