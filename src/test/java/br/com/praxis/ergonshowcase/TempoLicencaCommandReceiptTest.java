package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.dto.*;
import org.praxisplatform.uischema.command.JdbcResourceCommandReceiptStore;
import org.praxisplatform.uischema.command.ResourceCommandIdempotencyStore;
import org.praxisplatform.uischema.command.ResourceCommandFingerprint;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.praxisplatform.uischema.concurrency.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;

/** Prova de orquestração/replay; o armazenamento é simulado, sem alegar corrida PostgreSQL. */
@SuppressWarnings({"rawtypes","unchecked"})
class TempoLicencaCommandReceiptTest {
  final Connection connection=mock(Connection.class);
  final DataSource source=mock(DataSource.class);
  final JdbcTemplate jdbc=mock(JdbcTemplate.class);
  final Map<String,Object[]> receipts=new HashMap<>();
  DataSourceTransactionManager tx;
  JdbcResourceCommandReceiptStore gateway;
  ObjectMapper json=new ObjectMapper().findAndRegisterModules();
  @BeforeEach void setup() throws Exception {
    when(source.getConnection()).thenReturn(connection);
    var autoCommit=new AtomicBoolean(true);
    when(connection.getAutoCommit()).thenAnswer(i->autoCommit.get());
    doAnswer(i->{autoCommit.set(i.getArgument(0));return null;}).when(connection).setAutoCommit(anyBoolean());
    when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
    when(jdbc.getDataSource()).thenReturn(source);
    when(jdbc.execute(any(ConnectionCallback.class))).thenAnswer(i->((ConnectionCallback)i.getArgument(0)).doInConnection(connection));
    when(jdbc.query(startsWith("select fingerprint"),any(RowMapper.class),any(Object[].class))).thenAnswer(i->{
      String key=i.getArgument(2);Object[] receipt=receipts.get(key);
      if(receipt==null)return List.of();
      ResultSet rs=mock(ResultSet.class);when(rs.getString(1)).thenReturn((String)receipt[0]);
      when(rs.getString(2)).thenReturn((String)receipt[1]);when(rs.getLong(3)).thenReturn(((Number)receipt[2]).longValue());
      return List.of(((RowMapper)i.getArgument(1)).mapRow(rs,0));
    });
    when(jdbc.update(startsWith("insert into"),any(Object[].class))).thenAnswer(i->{
      receipts.put(i.getArgument(1),new Object[]{i.getArgument(2),i.getArgument(3),i.getArgument(4)});return 1;
    });
    tx=spy(new DataSourceTransactionManager(source));
    gateway=new JdbcResourceCommandReceiptStore(jdbc,tx,json);clearInvocations(tx);
    authenticate("operator","ERGON_WRITE");
  }
  void authenticate(String subject,String role){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(subject,"ignored",List.of(new SimpleGrantedAuthority(role))));}
  @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
  ResourceRepresentationResult<VendaLicencaEspecialResponseDTO> execute(String key,String token,Object payload,java.util.function.Supplier<ResourceRepresentationResult<VendaLicencaEspecialResponseDTO>> mutation) {
    return execute("administracao-pessoal.vendas-licenca-especial","/api/ergon/vendas-licenca-especial",91L,"autorizar-venda-licenca-especial",key,token,payload,VendaLicencaEspecialResponseDTO.class,mutation);
  }
  <T> ResourceRepresentationResult<T> execute(String resource,String path,Long id,String action,String key,String token,Object payload,Class<T> type,java.util.function.Supplier<ResourceRepresentationResult<T>> mutation) {
    var auth=SecurityContextHolder.getContext().getAuthentication();
    if(auth==null || auth.getAuthorities().stream().noneMatch(a->"ERGON_WRITE".equals(a.getAuthority())))
      throw new org.springframework.security.access.AccessDeniedException("Autoridade de escrita obrigatória.");
    if(key==null || key.isBlank()) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Idempotency-Key obrigatório.");
    var identity=new ResourceCommandIdempotencyStore.Identity("orgao=1",auth.getName(),resource,id.toString(),action,key);
    var input=new LinkedHashMap<String,Object>();input.put("payload",payload);input.put("ifMatch",token);
    return gateway.execute(identity,ResourceCommandFingerprint.of(json,input),type,()->{},mutation);
  }

  @Test void replayPreservesReadonlyOriginBodyAndOriginalVersionWithoutMutatingAgain(){
    var count=new AtomicInteger();var body=new VendaLicencaEspecialResponseDTO();body.setId(91L);body.setLicencaEspecialGozoId(23L);body.setSituacao("AUTORIZADA");
    var first=execute("same","etag-v0",Map.of("ato","deferimento"),()->{count.incrementAndGet();return ResourceRepresentationResult.versioned(body,1);});
    var replay=execute("same","etag-v0",Map.of("ato","deferimento"),()->{throw new AssertionError("Replay não pode reabrir a mutação");});
    assertThat(count.get()).isEqualTo(1);assertThat(replay.requirePersistedVersion()).isEqualTo(first.requirePersistedVersion());assertThat(replay.body().getLicencaEspecialGozoId()).isEqualTo(23L);assertThat(replay.body().isAbonoVinculado()).isTrue();assertThat(receipts).hasSize(1);verify(tx,times(2)).commit(any());
  }
  @Test void changedPayloadOrPreconditionConflictsAndScopesAreIsolated(){
    var body=new VendaLicencaEspecialResponseDTO();execute("key","etag",Map.of("dias",5),()->ResourceRepresentationResult.versioned(body,2));
    assertThatThrownBy(()->execute("key","etag",Map.of("dias",6),()->{throw new AssertionError();})).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(409));
    assertThatThrownBy(()->execute("key","changed",Map.of("dias",5),()->{throw new AssertionError();})).hasMessageContaining("409");
    authenticate("other","ERGON_WRITE");execute("key","etag",Map.of("dias",5),()->ResourceRepresentationResult.versioned(body,3));assertThat(receipts).hasSize(2);
  }
  @Test void failuresRollbackWithoutReceiptAndMissingVersionRetains428(){
    assertThatThrownBy(()->execute("invalid","etag",Map.of(),()->{throw new ResponseStatusException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,"Saldo insuficiente.");})).hasMessageContaining("422").hasMessageContaining("Saldo insuficiente");
    assertThat(receipts).isEmpty();verify(tx).rollback(any());
    assertThatThrownBy(()->execute("version",null,Map.of(),()->{throw ResourceVersionPreconditionException.required();})).isInstanceOfSatisfying(ResourceVersionPreconditionException.class,e->assertThat(e.status().value()).isEqualTo(428));assertThat(receipts).isEmpty();
  }
  @Test void authorityAndKeyAreCheckedBeforePersistence(){
    authenticate("reader","ERGON_READ");assertThatThrownBy(()->execute("x","tag",Map.of(),()->{throw new AssertionError();})).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);verifyNoInteractions(tx);
    authenticate("operator","ERGON_WRITE");assertThatThrownBy(()->execute(" ","tag",Map.of(),()->{throw new AssertionError();})).hasMessageContaining("400");verifyNoInteractions(tx);
  }
}
