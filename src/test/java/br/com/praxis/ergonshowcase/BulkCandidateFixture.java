package br.com.praxis.ergonshowcase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.praxisplatform.uischema.bulk.*;
import org.praxisplatform.uischema.concurrency.*;
import org.praxisplatform.uischema.service.base.AbstractBaseResourceService;

/** Domain unit tests now drive the starter adapter and the real versioned service update. */
@SuppressWarnings({"rawtypes","unchecked"})
final class BulkCandidateFixture {
  static final ResourceVersionEtagService ETAGS=new ResourceVersionEtagService("native-bulk-unit-fixture-secret-more-than-32-characters");
  static final ResourceVersionScope SCOPE=new ResourceVersionScope("bulk-unit-organization-1");
  static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();
  private BulkCandidateFixture() { }
  static Object candidate(AbstractBaseResourceService service,Object entity,java.util.List<BulkFieldChange> changes,BulkEditableFields fields,BulkMode mode) {
    Object id=new org.springframework.beans.BeanWrapperImpl(entity).getPropertyValue("id");
    String expected=ETAGS.create(SCOPE,service.getResourceKey(),id,service.findById(id).requirePersistedVersion());
    try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
      var adapter=service.bulkDomainAdapter(JSON,factory.getValidator(),ETAGS,()->SCOPE);
      var candidate=adapter.evaluate(id,expected,changes,mode,fields);
      Class<?> update=org.springframework.core.ResolvableType.forClass(service.getClass()).as(AbstractBaseResourceService.class).getGeneric(5).resolve();
      return JSON.convertValue(candidate.plan(),update);
    }
  }
  static void apply(AbstractBaseResourceService service,Object entity,Object dto) {
    Object id=new org.springframework.beans.BeanWrapperImpl(entity).getPropertyValue("id");
    String expected=ETAGS.create(SCOPE,service.getResourceKey(),id,service.findById(id).requirePersistedVersion());
    service.update(id,dto,new ResourceVersionUpdatePrecondition(ETAGS,expected,SCOPE,service.getResourceKey(),id));
  }
}
