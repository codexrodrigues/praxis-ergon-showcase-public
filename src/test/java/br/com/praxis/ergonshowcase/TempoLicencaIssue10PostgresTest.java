package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.praxis.ergonshowcase.bulk.*;
import br.com.praxis.ergonshowcase.config.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.praxisplatform.uischema.bulk.*;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** R1/R2: PostgreSQL real, sem Neon; callback de autorização e migração, sem prova HTTP/kernel. */
class TempoLicencaIssue10PostgresTest {
  @TempDir static Path temporary;
  static EmbeddedPostgres postgres;
  @BeforeAll static void start() throws Exception {
    var root=temporary.toAbsolutePath().normalize();var data=root.resolve("postgres").normalize();
    if(!data.startsWith(root))throw new IllegalStateException("Diretório de teste inválido");
    postgres=EmbeddedPostgres.builder().setDataDirectory(data).setCleanDataDirectory(true)
        .setPGStartupWait(Duration.ofMinutes(2))
        .setServerConfig("listen_addresses","127.0.0.1").setPort(0).start();
  }
  @AfterAll static void stop() throws Exception { if(postgres!=null)postgres.close(); }
  static DataSource database() throws Exception {
    String name="slice6_"+UUID.randomUUID().toString().replace("-","");
    try(var c=postgres.getPostgresDatabase().getConnection();var st=c.createStatement()) { st.execute("create database "+name); }
    var ds=postgres.getDatabase("postgres",name);
    try(var c=ds.getConnection();var st=c.createStatement()) {
      st.execute("create table empresas(empresa integer primary key);create table funcionarios(numero bigint primary key);"
          +"create table vinculos(numfunc bigint,numero integer,primary key(numfunc,numero));"
          +"create table documentos_legais(id bigint primary key);create table parametros_contagem_ferias(id bigint primary key)");
      String v5=resource("V5__averbacoes_e_licenca_especial.sql");
      st.execute(v5.substring(0,v5.indexOf("-- Seed demonstrativo")));
      st.execute("insert into empresas values(1);insert into funcionarios values(1001);insert into vinculos values(1001,1)");
      st.execute("insert into averbacoes_tempo_servico(id,emp_codigo,numfunc,numvinc,descricao,dtini,chave,tipo_tempo,finalidade,total_dias,dias_finalidade) values(91,1,1001,1,'Teste','2000-01-01',1,'PUBLICO','APOSENTADORIA',5000,5000)");
      st.execute("insert into periodos_aquisitivos_licenca_especial(id,emp_codigo,numfunc,numvinc,descricao,dtini,dtfim,chave,dias_gozados,dias_vendidos,saldo_dias) values(91,1,1001,1,'Teste','2015-01-01','2019-12-31',1,10,10,70)");
      st.execute("insert into licencas_especiais_gozo(id,emp_codigo,numfunc,numvinc,descricao,dtini,dtfim,periodo_aquisitivo_licenca_id,chave_peraqlicesp,ordem_parcela,dtprevfim,dias_gozo) values(91,1,1001,1,'Teste','2026-01-01','2026-01-10',91,1,1,'2026-01-10',10)");
      st.execute("insert into vendas_licenca_especial(id,emp_codigo,numfunc,numvinc,descricao,dtini,periodo_aquisitivo_licenca_id,chave_peraqlicesp,dias_vendidos,dt_pagamento,valor_indenizacao,mes_ano_folha,num_folha) values(91,1,1001,1,'Teste','2026-01-01',91,1,10,'2026-01-05',1234.56,'2026-01-01',1)");
    }
    return ds;
  }
  static String resource(String name) throws Exception {
    try(var input=TempoLicencaIssue10PostgresTest.class.getResourceAsStream("/db/migration/"+name)) {
      if(input==null)throw new IllegalStateException("Migração ausente: "+name);
      return new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
    }
  }
  @Test void s1CompleteV5SeedAndV6InstallWithoutOrphanAbonoOrDoubleDebit() throws Exception {
    String name="seed_"+UUID.randomUUID().toString().replace("-","");
    try(var c=postgres.getPostgresDatabase().getConnection();var st=c.createStatement()) { st.execute("create database "+name); }
    try(var c=postgres.getDatabase("postgres",name).getConnection();var st=c.createStatement()) {
      st.execute("create table empresas(empresa integer primary key);create table funcionarios(numero bigint primary key);"
          +"create table vinculos(numfunc bigint,numero integer,emp_codigo integer,primary key(numfunc,numero));"
          +"create table documentos_legais(id bigint primary key);create table parametros_contagem_ferias(id bigint primary key)");
      st.execute("insert into empresas values(1);insert into funcionarios values(1001);insert into vinculos values(1001,1,1)");
      st.execute(resource("V5__averbacoes_e_licenca_especial.sql"));
      try(var r=st.executeQuery("select count(*) from vendas_licenca_especial where modalidade='ABONO'")) {
        assertThat(r.next()).isTrue();assertThat(r.getInt(1)).isZero();
      }
      st.execute(resource("V6__tempo_licenca_ciclo_de_vida.sql"));
      try(var r=st.executeQuery("select a.situacao,p.situacao,p.dias_gozados,p.dias_vendidos,p.saldo_dias from averbacoes_tempo_servico a join periodos_aquisitivos_licenca_especial p on p.averbacao_origem_id=a.id")) {
        assertThat(r.next()).isTrue();assertThat(r.getString(1)).isEqualTo("HOMOLOGADA");assertThat(r.getString(2)).isEqualTo("PARCIALMENTE_GOZADO");
        assertThat(r.getInt(3)).isEqualTo(45);assertThat(r.getInt(4)).isEqualTo(15);assertThat(r.getInt(5)).isEqualTo(30);
      }
      try(var r=st.executeQuery("select count(*) from vendas_licenca_especial where situacao='PAGA' and modalidade='PECUNIA'")) {
        assertThat(r.next()).isTrue();assertThat(r.getInt(1)).isEqualTo(1);
      }
      try(var r=st.executeQuery("select count(*) from periodos_aquisitivos_licenca_especial p where saldo_dias<>dias_direito-dias_convertidos_dobro-coalesce((select sum(g.dias_gozo+g.dias_abono) from licencas_especiais_gozo g where g.periodo_aquisitivo_licenca_id=p.id and g.situacao<>'CANCELADO'),0)-coalesce((select sum(v.dias_vendidos) from vendas_licenca_especial v where v.periodo_aquisitivo_licenca_id=p.id and v.situacao<>'ESTORNADA' and v.licenca_especial_gozo_id is null),0)")) {
        assertThat(r.next()).isTrue();assertThat(r.getInt(1)).isZero();
      }
    }
  }
  @Test void s2ConsumedOriginIsMigratedAsHomologatedInsteadOfReturningToAnalysis() throws Exception {
    try(var c=database().getConnection();var st=c.createStatement()) {
      st.execute("update averbacoes_tempo_servico set finalidade='LICENCA_ESPECIAL',dias_finalidade=90 where id=91");
      st.execute("update periodos_aquisitivos_licenca_especial set averbacao_origem_id=91 where id=91");
      st.execute(resource("V6__tempo_licenca_ciclo_de_vida.sql"));
      assertState(st,"averbacoes_tempo_servico",91,"HOMOLOGADA");
      assertState(st,"periodos_aquisitivos_licenca_especial",91,"PARCIALMENTE_GOZADO");
    }
  }
  @ParameterizedTest @ValueSource(strings={"situacao='CANCELADO'","data_remocao='2026-01-05'","finalidade='APOSENTADORIA'","dias_desaverbados=5000"})
  void s2IncompatibleOriginRequiresReconciliationBeforeAcceptingItsActivePeriod(String incompatible) throws Exception {
    try(var c=database().getConnection();var st=c.createStatement()) {
      st.execute("update averbacoes_tempo_servico set finalidade='LICENCA_ESPECIAL',dias_finalidade=90 where id=91");
      st.execute("update periodos_aquisitivos_licenca_especial set averbacao_origem_id=91 where id=91");
      st.execute("update averbacoes_tempo_servico set "+incompatible+" where id=91");
      String v6=resource("V6__tempo_licenca_ciclo_de_vida.sql");
      assertThatThrownBy(()->st.execute(v6)).isInstanceOf(SQLException.class).hasMessageContaining("origem removida ou incompatível");
      assertState(st,"periodos_aquisitivos_licenca_especial",91,"REGISTRADO");
    }
  }
  @Test void s3FinancialHistoryAllowsReissueButOnlyOneActiveAbonoPerGozo() throws Exception {
    try(var c=database().getConnection();var st=c.createStatement()) {
      st.execute(resource("V6__tempo_licenca_ciclo_de_vida.sql"));
      st.execute("update vendas_licenca_especial set modalidade='ABONO_JUNTO_GOZO',licenca_especial_gozo_id=91,situacao='ESTORNADA',dt_estorno='2026-01-06' where id=91");
      String insert="insert into vendas_licenca_especial(id,emp_codigo,numfunc,numvinc,descricao,dtini,periodo_aquisitivo_licenca_id,chave_peraqlicesp,dias_vendidos,modalidade,licenca_especial_gozo_id,situacao) values(%d,1,1001,1,'Reemissão','2026-01-01',91,1,2,'ABONO_JUNTO_GOZO',91,'AUTORIZADA')";
      st.execute(insert.formatted(92));
      assertThatThrownBy(()->st.execute(insert.formatted(93))).isInstanceOfSatisfying(SQLException.class,e->assertThat(e.getSQLState()).isEqualTo("23505"));
      try(var r=st.executeQuery("select dt_pagamento,dt_estorno,valor_indenizacao from vendas_licenca_especial where id=91")) {
        assertThat(r.next()).isTrue();assertThat(r.getDate(1).toLocalDate()).isEqualTo(LocalDate.of(2026,1,5));
        assertThat(r.getDate(2).toLocalDate()).isEqualTo(LocalDate.of(2026,1,6));assertThat(r.getBigDecimal(3)).isEqualByComparingTo("1234.56");
      }
      assertState(st,"vendas_licenca_especial",91,"ESTORNADA");assertState(st,"vendas_licenca_especial",92,"AUTORIZADA");
      assertThatThrownBy(()->st.execute("update vendas_licenca_especial set situacao='AUTORIZADA' where id=91"))
          .isInstanceOfSatisfying(SQLException.class,e->assertThat(e.getSQLState()).isEqualTo("23514"));
      assertThatThrownBy(()->st.execute("update vendas_licenca_especial set dt_estorno='2026-01-04' where id=91"))
          .isInstanceOfSatisfying(SQLException.class,e->assertThat(e.getSQLState()).isEqualTo("23514"));
    }
  }
  @ParameterizedTest @EnumSource(BulkMode.class)
  void r1AllSlice6ResourcesAuthorizeReadsForEveryBulkMode(BulkMode mode) throws Exception {
    var ds=database();var infrastructure=mock(BulkExecutionInfrastructure.class);when(infrastructure.dataSource()).thenReturn(ds);
    var tenant=mock(TenantContext.class);when(tenant.orgao()).thenReturn(1);
    var ops=new ErgonBulkOperations();ReflectionTestUtils.setField(ops,"infrastructure",infrastructure);ReflectionTestUtils.setField(ops,"tenant",tenant);
    var json=new ObjectMapper();
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("operator","unused",
        List.of(new SimpleGrantedAuthority("ERGON_READ"),new SimpleGrantedAuthority("ERGON_WRITE"))));
    try(var c=ds.getConnection()) {
      TransactionSynchronizationManager.bindResource(ds,new ConnectionHolder(c));
      try {
        for(var r:List.of(ErgonBulkDomain.Resource.AVERBACOES_TEMPO_SERVICO,ErgonBulkDomain.Resource.PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL,
            ErgonBulkDomain.Resource.LICENCAS_ESPECIAIS_GOZO,ErgonBulkDomain.Resource.VENDAS_LICENCA_ESPECIAL)) {
          String operation="regression."+mode.name();
          BulkReadAuthorizationProvider auth=ReflectionTestUtils.invokeMethod(ops,"authorization",r,operation);
          var context=new BulkReadAuthorizationProvider.Context("test",r.key,operation,"operator|orgao=1");
          var target=new BulkReadAuthorizationProvider.Target(0,"91",json.createObjectNode().put("orgao",1));
          assertThat(auth.preAuthorize(context,Duration.ofSeconds(2))).isEqualTo(BulkReadAuthorizationProvider.GlobalDecision.ALLOWED);
          assertThat(auth.authorize(context,"operator|orgao=1",List.of(target),Duration.ofSeconds(2)).state())
              .as("%s / %s",r,mode).isEqualTo(BulkReadAuthorizationProvider.State.AUTHORIZED_ALL);
          assertThat(auth.authorize(context,"another|orgao=1",List.of(target),Duration.ofSeconds(2)).state()).isEqualTo(BulkReadAuthorizationProvider.State.DENIED_OR_REDUCED);
          var other=new BulkReadAuthorizationProvider.Target(0,"91",json.createObjectNode().put("orgao",2));
          assertThat(auth.authorize(context,"operator|orgao=1",List.of(other),Duration.ofSeconds(2)).state()).isEqualTo(BulkReadAuthorizationProvider.State.DENIED_OR_REDUCED);
          var missing=new BulkReadAuthorizationProvider.Target(0,"999",json.createObjectNode().put("orgao",1));
          assertThat(auth.authorize(context,"operator|orgao=1",List.of(missing),Duration.ofSeconds(2)).state()).isEqualTo(BulkReadAuthorizationProvider.State.DENIED_OR_REDUCED);
        }
      } finally { TransactionSynchronizationManager.unbindResource(ds);SecurityContextHolder.clearContext(); }
    }
  }
  @Test void r1EveryBulkResourceUsesTheJpaPhysicalTableRatherThanItsPublicRoute() {
    for(var r:ErgonBulkDomain.Resource.values())
      assertThat(r.table).isEqualTo(r.entity.getAnnotation(jakarta.persistence.Table.class).name()).matches("[a-z][a-z0-9_]*");
  }
  @Test void r2MigrationPreservesPaidClosedAndCanceledHistoryAndInitializesOnlyPendingRecords() throws Exception {
    try(var c=database().getConnection();var st=c.createStatement()) {
      st.execute("insert into licencas_especiais_gozo(id,emp_codigo,numfunc,numvinc,descricao,dtini,periodo_aquisitivo_licenca_id,chave_peraqlicesp,ordem_parcela,dtprevfim,dias_gozo) values(92,1,1001,1,'Pendente','2026-03-01',91,1,2,'2026-03-10',10)");
      st.execute("insert into vendas_licenca_especial(id,emp_codigo,numfunc,numvinc,descricao,dtini,periodo_aquisitivo_licenca_id,chave_peraqlicesp,dias_vendidos) values(92,1,1001,1,'Pendente','2026-03-01',91,1,2)");
      st.execute("insert into vendas_licenca_especial(id,emp_codigo,numfunc,numvinc,descricao,dtini,periodo_aquisitivo_licenca_id,chave_peraqlicesp,dias_vendidos,situacao,dt_pagamento) values(93,1,1001,1,'Cancelada','2026-03-01',91,1,2,'CANCELADO','2026-03-05')");
      st.execute(resource("V6__tempo_licenca_ciclo_de_vida.sql"));
      try(var rows=st.executeQuery("select situacao,dt_pagamento,valor_indenizacao,version from vendas_licenca_especial where id=91")) {
        assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("PAGA");
        assertThat(rows.getDate(2).toLocalDate()).isEqualTo(LocalDate.of(2026,1,5));assertThat(rows.getBigDecimal(3)).isEqualByComparingTo("1234.56");assertThat(rows.getLong(4)).isEqualTo(1);
      }
      assertState(st,"licencas_especiais_gozo",91,"GOZADA");assertState(st,"licencas_especiais_gozo",92,"PROGRAMADA");
      assertState(st,"vendas_licenca_especial",92,"SOLICITADA");assertState(st,"vendas_licenca_especial",93,"ESTORNADA");
      assertState(st,"periodos_aquisitivos_licenca_especial",91,"PARCIALMENTE_GOZADO");
      try(var rows=st.executeQuery("select dtfim from licencas_especiais_gozo where id=91")) {
        assertThat(rows.next()).isTrue();assertThat(rows.getDate(1).toLocalDate()).isEqualTo(LocalDate.of(2026,1,10));
      }
    }
  }
  static void assertState(Statement st,String table,int id,String state) throws Exception {
    try(var rows=st.executeQuery("select situacao from "+table+" where id="+id)) { assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo(state); }
  }
  @ParameterizedTest @ValueSource(strings={
      "update vendas_licenca_especial set valor_indenizacao=null where id=91",
      "update vendas_licenca_especial set num_folha=null where id=91",
      "update vendas_licenca_especial set modalidade='ABONO' where id=91",
      "update licencas_especiais_gozo set dtfim='2026-01-09' where id=91",
      "update periodos_aquisitivos_licenca_especial set dias_convertidos_dobro=10,saldo_dias=60 where id=91"})
  void r2MigrationFailsBeforeReclassifyingAmbiguousHistory(String ambiguous) throws Exception {
    try(var c=database().getConnection();var st=c.createStatement()) {
      st.execute(ambiguous);c.setAutoCommit(false);String migration=resource("V6__tempo_licenca_ciclo_de_vida.sql");
      assertThatThrownBy(()->st.execute(migration)).isInstanceOf(SQLException.class).hasMessageContaining("reconcilie");c.rollback();
      assertState(st,"vendas_licenca_especial",91,"REGISTRADO");assertState(st,"licencas_especiais_gozo",91,"REGISTRADO");
      try(var rows=st.executeQuery("select count(*) from information_schema.columns where table_name='vendas_licenca_especial' and column_name='licenca_especial_gozo_id'")) {
        assertThat(rows.next()).isTrue();assertThat(rows.getInt(1)).isZero();
      }
      c.rollback();
    }
  }
}
