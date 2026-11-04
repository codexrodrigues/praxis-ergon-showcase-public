package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.praxis.ergonshowcase.config.*;
import br.com.praxis.ergonshowcase.controller.*;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.repository.*;
import br.com.praxis.ergonshowcase.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** Prova local de contratos e hidratação; não constitui execução Flyway ou prova HTTP/Neon. */
class TempoLicencaSlice6Phase1ContractTest {
  private final TenantContext tenant = mock(TenantContext.class);
  private final EntityManager em = mock(EntityManager.class);

  @Test void allBusinessColumnsRoundTripAndUpdateKeepsIdentity() throws Exception {
    roundTrip(AverbacaoTempoServico.class, CreateAverbacaoTempoServicoDTO.class,
        UpdateAverbacaoTempoServicoDTO.class, Mappers.getMapper(AverbacaoTempoServicoMapper.class));
    roundTrip(PeriodoAquisitivoLicencaEspecial.class, CreatePeriodoAquisitivoLicencaEspecialDTO.class,
        UpdatePeriodoAquisitivoLicencaEspecialDTO.class, Mappers.getMapper(PeriodoAquisitivoLicencaEspecialMapper.class));
    roundTrip(LicencaEspecialGozo.class, CreateLicencaEspecialGozoDTO.class,
        UpdateLicencaEspecialGozoDTO.class, Mappers.getMapper(LicencaEspecialGozoMapper.class));
    roundTrip(VendaLicencaEspecial.class, CreateVendaLicencaEspecialDTO.class,
        UpdateVendaLicencaEspecialDTO.class, Mappers.getMapper(VendaLicencaEspecialMapper.class));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void roundTrip(Class<?> entityType, Class<?> createType, Class<?> updateType, ResourceMapper mapper) throws Exception {
    var create = createType.getConstructor().newInstance();
    var update = updateType.getConstructor().newInstance();
    var input = new BeanWrapperImpl(create);
    var replacement = new BeanWrapperImpl(update);
    var business = new ArrayList<String>();
    var expected = new LinkedHashMap<String,Object>();
    Set<String> derived = entityType == PeriodoAquisitivoLicencaEspecial.class
        ? Set.of("diasGozados", "diasVendidos", "saldoDias") : Set.of();
    for (Class<?> type = entityType; type != null; type = type.getSuperclass()) {
      for (var field : type.getDeclaredFields()) {
        if (Set.of("id", "version", "auditoria").contains(field.getName())
            || field.isAnnotationPresent(jakarta.persistence.ManyToOne.class)) continue;
        Object value = sample(field.getType());
        expected.put(field.getName(), value);
        if (derived.contains(field.getName())) {
          assertThat(input.isWritableProperty(field.getName())).isFalse();
          assertThat(replacement.isWritableProperty(field.getName())).isFalse();
        } else {
          input.setPropertyValue(field.getName(), value);
          replacement.setPropertyValue(field.getName(), value);
        }
        business.add(field.getName());
      }
    }
    var entity = mapper.newEntity(create);
    var managed = new BeanWrapperImpl(entity);
    // Totais pertencem ao servidor: aplicar o DTO não os altera e o response os conserva.
    derived.forEach(name -> managed.setPropertyValue(name, expected.get(name)));
    managed.setPropertyValue("id", 812L);
    managed.setPropertyValue("version", 7L);
    mapper.applyUpdate(entity, update);
    var response = new BeanWrapperImpl(mapper.toResponse(entity));
    for (String field : business) {
      assertThat(managed.getPropertyValue(field)).as(entityType.getSimpleName() + "." + field).isEqualTo(expected.get(field));
      assertThat(response.getPropertyValue(field)).as("response " + field).isEqualTo(expected.get(field));
    }
    assertThat(managed.getPropertyValue("id")).isEqualTo(812L);
    assertThat(managed.getPropertyValue("version")).isEqualTo(7L);
  }

  private Object sample(Class<?> type) {
    if (type == String.class) return "Referência funcional deliberada";
    if (type == Integer.class) return 17;
    if (type == Long.class) return 1001L;
    if (type == Boolean.class) return true;
    if (type == PoliticaAntecipacaoLicenca.class) return PoliticaAntecipacaoLicenca.PERMITIR;
    if (type == LocalDate.class) return LocalDate.of(2026, 11, 1);
    if (type == BigDecimal.class) return new BigDecimal("1475.35");
    if (Map.class.isAssignableFrom(type)) return Map.of("origem", "certidao");
    throw new AssertionError("Sem amostra deliberada para " + type);
  }

  private PeriodoAquisitivoLicencaEspecial parent() {
    when(tenant.explicitOrgao()).thenReturn(OptionalInt.of(1));
    when(tenant.orgao()).thenReturn(1);
    var parent = new PeriodoAquisitivoLicencaEspecial();
    parent.setId(91L); parent.setEmpCodigo(1); parent.setNumfunc(1001L); parent.setNumvinc(2); parent.setChave(3);
    when(em.find(PeriodoAquisitivoLicencaEspecial.class, 91L)).thenReturn(parent);
    return parent;
  }

  @Test void gozoHydratesAllMirroredKeysFromOnlyParentIdentity() {
    parent();
    var service = new LicencaEspecialGozoService(mock(LicencaEspecialGozoRepository.class), Mappers.getMapper(LicencaEspecialGozoMapper.class), tenant);
    CanonicalResourceTestContext.wire(service, em);
    var entity = new LicencaEspecialGozo(); entity.setPeriodoAquisitivoLicencaId(91L);
    service.hydrateParentPeriodoAquisitivo(entity);
    assertMirrors(entity);
    entity.setNumfunc(9999L);
    assertThatThrownBy(() -> service.hydrateParentPeriodoAquisitivo(entity)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
  }

  @Test void vendaHydratesAndRejectsMissingOrForeignParent() {
    var parent = parent();
    var service = new VendaLicencaEspecialService(mock(VendaLicencaEspecialRepository.class), Mappers.getMapper(VendaLicencaEspecialMapper.class), tenant);
    CanonicalResourceTestContext.wire(service, em);
    var entity = new VendaLicencaEspecial(); entity.setPeriodoAquisitivoLicencaId(91L);
    service.hydrateParentPeriodoAquisitivo(entity);
    assertMirrors(entity);
    entity.setPeriodoAquisitivoLicencaId(92L);
    assertThatThrownBy(() -> service.hydrateParentPeriodoAquisitivo(entity)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
    entity.setPeriodoAquisitivoLicencaId(91L); parent.setEmpCodigo(2);
    assertThatThrownBy(() -> service.hydrateParentPeriodoAquisitivo(entity)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
    entity.setPeriodoAquisitivoLicencaId(null);
    assertThatThrownBy(() -> service.hydrateParentPeriodoAquisitivo(entity)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
  }

  private void assertMirrors(ParcelaLicencaEspecial entity) {
    assertThat(entity.getEmpCodigo()).isEqualTo(1);
    assertThat(entity.getNumfunc()).isEqualTo(1001L);
    assertThat(entity.getNumvinc()).isEqualTo(2);
    assertThat(entity.getChavePeraqlicesp()).isEqualTo(3);
  }

  @Test void quickFiltersOnlyUseRealFilterableWireFields() throws Exception {
    var bindings = Map.of(AverbacaoTempoServicoController.class, AverbacaoTempoServicoFilterDTO.class,
        PeriodoAquisitivoLicencaEspecialController.class, PeriodoAquisitivoLicencaEspecialFilterDTO.class,
        LicencaEspecialGozoController.class, LicencaEspecialGozoFilterDTO.class,
        VendaLicencaEspecialController.class, VendaLicencaEspecialFilterDTO.class,
        RadarTempoServicoLicencaEspecialController.class, RadarTempoServicoLicencaEspecialFilterDTO.class);
    for (var entry : bindings.entrySet()) {
      Set<String> names = new HashSet<>();
      for (Class<?> type = entry.getValue(); type != null; type = type.getSuperclass())
        for (var field : type.getDeclaredFields()) if (field.isAnnotationPresent(Filterable.class)) names.add(field.getName());
      assertThat(names)
          .hasSize(entry.getValue() == VendaLicencaEspecialFilterDTO.class ? 24 : 21)
          .contains("setorLotacaoNome", "tipoSetorLotacao", "categoriaVinculo");
      for (var quick : entry.getKey().getAnnotation(ApiResource.class).quickFilters())
        new ObjectMapper().readTree(quick.filter()).fieldNames().forEachRemaining(name -> assertThat(names).contains(name));
    }
  }

  @Test void newLookupsDeclareFunctionalCascadeAndCanonicalReload() {
    var registry = new OptionSourcesConfig().options();
    for (var source : registry.descriptors().stream().filter(d -> Set.of("averbacaoTempoServico", "periodoAquisitivoLicencaEspecial", "licencaEspecialGozo", "vendaLicencaEspecial").contains(d.key())).toList()) {
      assertThat(source.dependsOn()).containsExactly("empCodigo", "numfunc", "numvinc");
      assertThat(source.dependencyFilterMap()).containsEntry("empCodigo", "empCodigo").containsEntry("numfunc", "numfunc").containsEntry("numvinc", "numvinc");
      assertThat(source.runtimeContract().byIdsEndpoint()).endsWith("/options/by-ids");
      assertThat(source.entityLookup().detail().previewSurfaceId()).isEqualTo("detail");
      if(source.key().equals("averbacaoTempoServico")) assertThat(source.entityLookup().selectionPolicy().allowedStatuses()).containsExactly("HOMOLOGADA");
      if(source.key().equals("periodoAquisitivoLicencaEspecial")) assertThat(source.entityLookup().selectionPolicy().blockedStatuses()).contains("QUITADO","SUSPENSO","PRESCRITO","CANCELADO");
    }
  }

  @Test void anticipationParameterDefaultsFalseAndMapsBothDirections() {
    var mapper = new ParametroContagemFeriasMapper(new TenantContext());
    var create = new CreateParametroContagemFeriasDTO();
    assertThat(create.getPermGozoLicEspAntFimPe()).isFalse();
    create.setPermGozoLicEspAntFimPe(true);
    var entity = mapper.newEntity(create);
    assertThat(mapper.toResponse(entity).getPermGozoLicEspAntFimPe()).isTrue();
    var update = new UpdateParametroContagemFeriasDTO(); update.setPermGozoLicEspAntFimPe(false);
    mapper.applyUpdate(entity, update);
    assertThat(entity.getPermGozoLicEspAntFimPe()).isFalse();
  }

  @Test void itemReadAndDeleteCannotBypassExplicitTenant() {
    when(tenant.explicitOrgao()).thenReturn(OptionalInt.of(1));
    when(tenant.orgao()).thenReturn(1);
    var repository = mock(VendaLicencaEspecialRepository.class);
    var foreign = new VendaLicencaEspecial(); foreign.setId(91L); foreign.setEmpCodigo(2);
    when(repository.findById(91L)).thenReturn(Optional.of(foreign));
    var service = new VendaLicencaEspecialService(repository, Mappers.getMapper(VendaLicencaEspecialMapper.class), tenant);
    assertThatThrownBy(() -> service.findById(91L)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
    assertThatThrownBy(() -> service.deleteById(91L)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
    assertThatThrownBy(() -> service.deleteAllById(List.of(91L))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
    verify(repository, never()).delete(any(VendaLicencaEspecial.class));
    verify(repository, never()).deleteAllById(any());
  }

  @Test void financialAmountStaysOutOfExportAndRadarKeepsUnknownPrescription() {
    when(tenant.explicitOrgao()).thenReturn(OptionalInt.of(1));
    when(tenant.orgao()).thenReturn(1);
    var service = new VendaLicencaEspecialService(mock(VendaLicencaEspecialRepository.class), Mappers.getMapper(VendaLicencaEspecialMapper.class), tenant);
    @SuppressWarnings("unchecked")
    var fields = (List<org.praxisplatform.uischema.exporting.CollectionExportField>) ReflectionTestUtils.invokeMethod(service, "exportFields");
    assertThat(fields).extracting(org.praxisplatform.uischema.exporting.CollectionExportField::key).doesNotContain("valorIndenizacao", "observacoes");
    assertThat(service.supportsCursorPagination()).isTrue();
    CanonicalResourceTestContext.exports(service);
    assertThat(service.getCollectionExportCapability().orElseThrow().scopes()).hasSize(5);
    var mapper = Mappers.getMapper(RadarTempoServicoLicencaEspecialMapper.class);
    var radar = new RadarTempoServicoLicencaEspecial();
    assertThat(mapper.toResponse(radar).getDiasAtePrescricao()).isNull();
    assertThat(mapper.toResponse(radar).getDtPrescricao()).isNull();
  }
}
