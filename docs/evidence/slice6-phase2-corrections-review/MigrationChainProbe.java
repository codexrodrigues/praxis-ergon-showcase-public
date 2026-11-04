package br.com.praxis.ergonshowcase;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.*;
import java.sql.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** Unmodified V5 including seed, then unmodified V6, using synthetic parent records. */
public class MigrationChainProbe {
  public static void main(String[] args) throws Exception {
    var root=Path.of("docs/evidence/slice6-phase2-corrections-review").toAbsolutePath().normalize();
    var data=root.resolve("pgdata-gateway").normalize();
    if(!data.startsWith(root))throw new IllegalStateException("Invalid data directory");
    try(var pg=EmbeddedPostgres.builder().setDataDirectory(data).setCleanDataDirectory(false)
        .setServerConfig("listen_addresses","127.0.0.1").setPort(0).start()) {
      String name="review_"+UUID.randomUUID().toString().replace("-","");
      try(var c=pg.getPostgresDatabase().getConnection();var st=c.createStatement()) { st.execute("create database "+name); }
      try(var c=pg.getDatabase("postgres",name).getConnection();var st=c.createStatement()) {
        st.execute("create table empresas(empresa integer primary key);create table funcionarios(numero bigint primary key);"
            +"create table vinculos(numfunc bigint,numero integer,emp_codigo integer,primary key(numfunc,numero));"
            +"create table documentos_legais(id bigint primary key);create table parametros_contagem_ferias(id bigint primary key)");
        st.execute("insert into empresas values(1);insert into funcionarios values(1001);insert into vinculos values(1001,1,1)");
        st.execute(Files.readString(Path.of("src/main/resources/db/migration/V5__averbacoes_e_licenca_especial.sql")));
        try(var rows=st.executeQuery("select count(*) from vendas_licenca_especial where modalidade='ABONO'")) {
          assertThat(rows.next()).isTrue();assertThat(rows.getInt(1)).isEqualTo(1);
        }
        String v6=Files.readString(Path.of("src/main/resources/db/migration/V6__tempo_licenca_ciclo_de_vida.sql"));
        assertThatThrownBy(()->st.execute(v6)).isInstanceOf(SQLException.class).hasMessageContaining("reconcilie os registros ABONO");
        System.out.println("CONFIRMED: complete V5 demo seed creates ABONO; unchanged V6 aborts on fresh synthetic owner");
        // Isolate origin-state migration without unknown old ABONO: remove only synthetic demo row.
        st.execute("delete from vendas_licenca_especial where modalidade='ABONO'");
        st.execute(v6);
        try(var rows=st.executeQuery("select a.situacao,p.situacao,p.dias_gozados from averbacoes_tempo_servico a join periodos_aquisitivos_licenca_especial p on p.averbacao_origem_id=a.id")) {
          assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("EM_ANALISE");
          assertThat(rows.getString(2)).isEqualTo("PARCIALMENTE_GOZADO");assertThat(rows.getInt(3)).isEqualTo(30);
          System.out.println("CONFIRMED: after synthetic ABONO reconciliation, V6 accepts EM_ANALISE origin -> consumed PARCIALMENTE_GOZADO period");
        }
      }
    }
  }
}
