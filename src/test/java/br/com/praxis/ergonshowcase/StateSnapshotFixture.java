package br.com.praxis.ergonshowcase;

import java.util.Arrays;
import java.util.Comparator;
import org.praxisplatform.uischema.capability.ResourceServiceDelegatingStateSnapshotProvider;
import org.praxisplatform.uischema.service.base.BaseResourceQueryService;
import org.springframework.beans.factory.ObjectProvider;
import static org.mockito.Mockito.*;

/** Exercises the real service projection and the starter's resource/identity delegation. */
final class StateSnapshotFixture {
  @SuppressWarnings({"rawtypes","unchecked"})
  static ResourceServiceDelegatingStateSnapshotProvider provider(BaseResourceQueryService... services) {
    ObjectProvider source = mock(ObjectProvider.class);
    when(source.orderedStream()).thenAnswer(invocation -> Arrays.stream(services));
    return new ResourceServiceDelegatingStateSnapshotProvider(source);
  }
  static BaseResourceQueryService<?,?,?> service(Class<?> type, Object repository) {
    var constructor = Arrays.stream(type.getConstructors()).min(Comparator.comparingInt(java.lang.reflect.Constructor::getParameterCount)).orElseThrow();
    Object[] arguments = Arrays.stream(constructor.getParameterTypes()).map(parameter -> {
      if (parameter.isInstance(repository)) return repository;
      if (parameter.isInterface()) return org.mapstruct.factory.Mappers.getMapper(parameter);
      return org.springframework.beans.BeanUtils.instantiateClass(parameter);
    }).toArray();
    return (BaseResourceQueryService<?,?,?>) org.springframework.beans.BeanUtils.instantiateClass(constructor,arguments);
  }
}
