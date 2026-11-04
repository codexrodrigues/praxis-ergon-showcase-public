package br.com.praxis.ergonshowcase;

import br.com.praxis.ergonshowcase.bulk.*;
import br.com.praxis.ergonshowcase.config.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.*;
import java.time.Duration;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.praxisplatform.uischema.bulk.*;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real isolated PostgreSQL + production bulk read-authorizer callback. No HTTP/kernel proof. */
public class PostgresBulkReadProbe {
  public static void main(String[] args) throws Exception {
    var root=Path.of("docs/evidence/slice6-phase2-review").toAbsolutePath().normalize();
    var data=root.resolve("pgdata-review").normalize();
    if(!data.startsWith(root))throw new IllegalStateException("Invalid review database directory");
    try(var pg=EmbeddedPostgres.builder().setDataDirectory(data).setCleanDataDirectory(false)
        .setServerConfig("listen_addresses","127.0.0.1").setPort(0).start()) {
      String database="review_"+UUID.randomUUID().toString().replace("-","");
      try(var admin=pg.getPostgresDatabase().getConnection();var st=admin.createStatement()) {
        st.execute("create database "+database);
      }
      var ds=pg.getDatabase("postgres",database);
      var infra=mock(BulkExecutionInfrastructure.class);when(infra.dataSource()).thenReturn(ds);
      var tenant=mock(TenantContext.class);when(tenant.orgao()).thenReturn(1);
      var ops=new ErgonBulkOperations();ReflectionTestUtils.setField(ops,"infrastructure",infra);
      ReflectionTestUtils.setField(ops,"tenant",tenant);
      var json=new ObjectMapper();
      SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("operator","unused",
          List.of(new SimpleGrantedAuthority("ERGON_READ"),new SimpleGrantedAuthority("ERGON_WRITE"))));
      try(var con=ds.getConnection()) {
        try(var st=con.createStatement()) {
          st.execute("create table empresas(empresa integer primary key);create table funcionarios(numero bigint primary key);"
              +"create table vinculos(numfunc bigint,numero integer,primary key(numfunc,numero));"
              +"create table documentos_legais(id bigint primary key);create table parametros_contagem_ferias(id bigint primary key)");
          String v5=Files.readString(Path.of("src/main/resources/db/migration/V5__averbacoes_e_licenca_especial.sql"));
          st.execute(v5.substring(0,v5.indexOf("-- Seed demonstrativo")));
          st.execute("insert into empresas values(1);insert into funcionarios values(1001);insert into vinculos values(1001,1)");
          st.execute("insert into averbacoes_tempo_servico(id,emp_codigo,numfunc,numvinc,descricao,dtini,chave,tipo_tempo,finalidade,total_dias,dias_finalidade) values(91,1,1001,1,'review','2000-01-01',1,'PUBLICO','APOSENTADORIA',5000,5000)");
          st.execute("insert into periodos_aquisitivos_licenca_especial(id,emp_codigo,numfunc,numvinc,descricao,dtini,dtfim,chave,dias_gozados,dias_vendidos,saldo_dias) values(91,1,1001,1,'review','2015-01-01','2019-12-31',1,10,10,70)");
          st.execute("insert into licencas_especiais_gozo(id,emp_codigo,numfunc,numvinc,descricao,dtini,dtfim,periodo_aquisitivo_licenca_id,chave_peraqlicesp,ordem_parcela,dtprevfim,dias_gozo) values(91,1,1001,1,'review','2026-01-01','2026-01-10',91,1,1,'2026-01-10',10)");
          st.execute("insert into vendas_licenca_especial(id,emp_codigo,numfunc,numvinc,descricao,dtini,periodo_aquisitivo_licenca_id,chave_peraqlicesp,dias_vendidos,dt_pagamento,valor_indenizacao,mes_ano_folha,num_folha) values(91,1,1001,1,'review','2026-01-01',91,1,10,'2026-01-05',1234.56,'2026-01-01',1)");
        }
        TransactionSynchronizationManager.bindResource(ds,new ConnectionHolder(con));
        try {
          for(var r:List.of(ErgonBulkDomain.Resource.AVERBACOES_TEMPO_SERVICO,
              ErgonBulkDomain.Resource.PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL,
              ErgonBulkDomain.Resource.LICENCAS_ESPECIAIS_GOZO,
              ErgonBulkDomain.Resource.VENDAS_LICENCA_ESPECIAL)) {
            String table=switch(r) {
              case AVERBACOES_TEMPO_SERVICO -> "averbacoes_tempo_servico";
              case PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL -> "periodos_aquisitivos_licenca_especial";
              case LICENCAS_ESPECIAIS_GOZO -> "licencas_especiais_gozo";
              default -> "vendas_licenca_especial";
            };
            try(var st=con.createStatement()) {
              try(var rows=st.executeQuery("select count(*) from public."+table+" where id=91 and emp_codigo=1")) {
                assertThat(rows.next()).isTrue();assertThat(rows.getInt(1)).isEqualTo(1);
              }
              String invalidTable=table.replace('_','-');
              try { st.executeQuery("select count(*) from public."+invalidTable+" where id::text in ('91') and emp_codigo=1");
                throw new AssertionError("Hyphenated table unexpectedly accepted");
              } catch(SQLException error) { assertThat(error.getSQLState()).isEqualTo("42601"); }
            }
            BulkReadAuthorizationProvider auth=ReflectionTestUtils.invokeMethod(ops,"authorization",r,"review.confirm");
            var decision=auth.authorize(null,"operator|orgao=1",
                List.of(new BulkReadAuthorizationProvider.Target(0,"91",json.createObjectNode().put("orgao",1))),Duration.ofSeconds(2));
            assertThat(decision.state()).isEqualTo(BulkReadAuthorizationProvider.State.AUTHORITY_UNAVAILABLE);
            System.out.println("CONFIRMED A: "+r+" existing authorized target -> "+decision.state()+"; bad SQLSTATE=42601");
          }
          try(var st=con.createStatement()) {
            st.execute(Files.readString(Path.of("src/main/resources/db/migration/V6__tempo_licenca_ciclo_de_vida.sql")));
            try(var rows=st.executeQuery("select situacao,dt_pagamento,valor_indenizacao from vendas_licenca_especial where id=91")) {
              assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("SOLICITADA");
              assertThat(rows.getDate(2).toLocalDate()).isEqualTo(java.time.LocalDate.of(2026,1,5));
              assertThat(rows.getBigDecimal(3)).isEqualByComparingTo("1234.56");
              System.out.println("CONFIRMED G: V5 payment dated 2026-01-05, value 1234.56 becomes SOLICITADA after unmodified V6");
            }
            try(var rows=st.executeQuery("select situacao,dtfim from licencas_especiais_gozo where id=91")) {
              assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("PROGRAMADA");
              assertThat(rows.getDate(2).toLocalDate()).isEqualTo(java.time.LocalDate.of(2026,1,10));
              System.out.println("CONFIRMED G: V5 gozo ended 2026-01-10 becomes PROGRAMADA after unmodified V6");
            }
          }
        } finally { TransactionSynchronizationManager.unbindResource(ds);SecurityContextHolder.clearContext(); }
      }
    }
    System.out.println("POSTGRES_BULK_READ_COUNTEREXAMPLES=4/4; DATABASE_CLOSED");
  }
}
