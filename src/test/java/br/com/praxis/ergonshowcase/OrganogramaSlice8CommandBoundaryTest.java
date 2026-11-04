package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.praxis.ergonshowcase.config.OrganogramaPhaseOneAvailability;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.repository.*;
import br.com.praxis.ergonshowcase.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.praxisplatform.uischema.capability.ResourceOperationAvailabilityContext;
import org.springframework.security.access.AccessDeniedException;

class OrganogramaSlice8CommandBoundaryTest {
  @Test void discoveryDeniesEveryMutationForEveryPreparedResource() {
    var provider=new OrganogramaPhaseOneAvailability();
    for(var key:OrganogramaPhaseOneAvailability.RESOURCES)for(var op:List.of("create","update","delete","delete-all","duplicate-draft","bulk-update")) {
      var result=provider.evaluate(ResourceOperationAvailabilityContext.collection(key,"/unused",op));
      assertThat(result.allowed()).as(key+" "+op).isFalse();
      assertThat(result.reason()).isEqualTo("organograma-comandos-ainda-indisponiveis");
    }
  }
  @Test void servicesDenyCreationBeforePersistenceEvenWithoutTheDiscoveryGuard() {
    var hr=mock(HistoricoSetorRepository.class);var sr=mock(SetorParametrizacaoErgonRepository.class);
    var cr=mock(CargoSetorRepository.class);var ur=mock(SubempresaLocalidadeRepository.class);var gr=mock(GrupoSetorRepository.class);
    assertThatThrownBy(()->new HistoricoSetorService(hr,Mappers.getMapper(HistoricoSetorMapper.class)).create(new CreateHistoricoSetorDTO())).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(()->new SetorParametrizacaoErgonService(sr,Mappers.getMapper(SetorParametrizacaoErgonMapper.class)).create(new CreateSetorParametrizacaoErgonDTO())).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(()->new CargoSetorService(cr,Mappers.getMapper(CargoSetorMapper.class)).create(new CreateCargoSetorDTO())).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(()->new SubempresaLocalidadeService(ur,Mappers.getMapper(SubempresaLocalidadeMapper.class)).create(new CreateSubempresaLocalidadeDTO())).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(()->new GrupoSetorService(gr,Mappers.getMapper(GrupoSetorMapper.class)).create(new CreateGrupoSetorDTO())).isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(hr,sr,cr,ur,gr);
  }
  @Test void derivedValuesCannotBeSuppliedByClientJsonOrCopiedIntoANewEntity() throws Exception {
    var json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
    var history=json.readValue("{\"caminhoHierarquico\":\"/forjado/\",\"nivelHierarquico\":7}",CreateHistoricoSetorDTO.class);
    assertThat(history.getCaminhoHierarquico()).isNull();assertThat(history.getNivelHierarquico()).isNull();
    history.setCaminhoHierarquico("/forjado/");history.setNivelHierarquico(7);
    var entity=Mappers.getMapper(HistoricoSetorMapper.class).newEntity(history);
    assertThat(entity.getCaminhoHierarquico()).isNull();assertThat(entity.getNivelHierarquico()).isNull();
    var cargo=json.readValue("{\"lotacaoOcupadaAtual\":99}",CreateCargoSetorDTO.class);
    assertThat(cargo.getLotacaoOcupadaAtual()).isNull();
  }
}
