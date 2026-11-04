package br.com.praxis.ergonshowcase;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.util.*;
import org.praxisplatform.uischema.exporting.*;
import org.praxisplatform.uischema.service.base.AbstractBaseQueryResourceService;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;

/** Unit-fixture model of joined managed state. It does not certify real transaction or lock behavior. */
final class CanonicalResourceTestContext {
  private CanonicalResourceTestContext() { }
  @SuppressWarnings({"unchecked","rawtypes"})
  static void wire(Object service,EntityManager em) {
    ReflectionTestUtils.setField(service,"entityManager",em);
    if (service instanceof br.com.praxis.ergonshowcase.service.ErgonResourceService)
      ReflectionTestUtils.setField(service, "resourceObjectMapper", new ObjectMapper().findAndRegisterModules());
    if (!mockingDetails(em).isMock()) return;
    when(em.isJoinedToTransaction()).thenReturn(true);
    when(em.contains(any())).thenReturn(true);
    var session=em.unwrap(org.hibernate.Session.class);
    if(session==null)when(em.unwrap(org.hibernate.Session.class)).thenReturn(mock(org.hibernate.Session.class));
    if(service instanceof AbstractBaseQueryResourceService query) {
      Class type=query.getEntityClass();BaseCrudRepository repository=query.getRepository();
      if (type==null || repository==null) return;
      var initial = new HashMap<Object,Object>();
      // Keep explicitly modeled related rows when this EntityManager serves several resources.
      for (var stubbing : mockingDetails(em).getStubbings()) {
        var invocation = stubbing.getInvocation();
        if (invocation.getMethod().getName().equals("find") && invocation.getArguments().length == 2
            && invocation.getArgument(0) == type && invocation.getArgument(1) != null) {
          Object id = invocation.getArgument(1);
          Object row = em.find(type, id);
          if (row != null) initial.put(id, row);
        }
      }
      when(em.find(eq(type),any())).thenAnswer(i -> {
        var row=repository.findById(i.getArgument(1));
        return row.isPresent() ? row.get() : initial.get(i.getArgument(1));
      });
      when(em.find(eq(type),any(),eq(LockModeType.PESSIMISTIC_WRITE))).thenAnswer(i->{
        var row=repository.findById(i.getArgument(1));return row.isPresent()?row.get():em.find(type,i.getArgument(1));
      });
    }
    exports(service);
  }
  static void exports(Object service) {
    var json=new ObjectMapper().findAndRegisterModules();
    ReflectionTestUtils.setField(service,"collectionExportExecutor",new DefaultCollectionExportExecutor(
        List.of(new CsvCollectionExportEngine(),new JsonCollectionExportEngine(json),new ExcelCollectionExportEngine())));
  }
}
