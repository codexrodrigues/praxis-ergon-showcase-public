package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.*;
import java.util.*;

/**
 * Restaura dados de testes HTTP com commits na cópia descartável, sem DDL ou bypass
 * de constraints. O baseline fica apenas em memória; nenhum dado pessoal é exportado.
 */
final class NeonDomainFixture {
  private record Table(String name, List<String> columns, List<String> keys) {
    String relation() { return "public." + quote(name); }
  }
  private final List<Table> tables = new ArrayList<>();
  private final Map<String, String> baseline;

  NeonDomainFixture() throws Exception {
    String branch = System.getenv("BULK_PROOF_DISPOSABLE_BRANCH");
    assertThat(branch).matches("br-[a-z0-9-]+").isNotEqualTo("br-super-sound-b7urizmd");
    assertThat(System.getenv("PROOF_NEON_BRANCH")).isEqualTo(branch);
    assertThat(System.getenv("SPRING_DATASOURCE_URL"))
        .contains("//" + System.getenv("BULK_PROOF_EXPECTED_HOST") + "/")
        .doesNotContain("ep-soft-hall-b7y4wgla");
    try (var connection = connect()) {
      connection.setReadOnly(true);
      var columns = new TreeMap<String, List<String>>();
      var keys = new HashMap<String, List<String>>();
      var parents = new HashMap<String, Set<String>>();
      try (var query = connection.createStatement(); var rows = query.executeQuery(
          "select table_name,column_name from information_schema.columns "
              + "where table_schema='public' order by table_name,ordinal_position")) {
        while (rows.next()) columns.computeIfAbsent(rows.getString(1), k -> new ArrayList<>()).add(rows.getString(2));
      }
      assertThat(columns).hasSize(16);
      try (var query = connection.createStatement(); var rows = query.executeQuery(
          "select c.relname,a.attname from pg_index i join pg_class c on c.oid=i.indrelid "
              + "join pg_namespace n on n.oid=c.relnamespace "
              + "cross join lateral unnest(i.indkey) with ordinality k(attnum,seq) "
              + "join pg_attribute a on a.attrelid=c.oid and a.attnum=k.attnum "
              + "where n.nspname='public' and i.indisprimary order by c.relname,k.seq")) {
        while (rows.next()) keys.computeIfAbsent(rows.getString(1), k -> new ArrayList<>()).add(rows.getString(2));
      }
      try (var query = connection.createStatement(); var rows = query.executeQuery(
          "select c.relname,p.relname from pg_constraint f join pg_class c on c.oid=f.conrelid "
              + "join pg_class p on p.oid=f.confrelid join pg_namespace n on n.oid=c.relnamespace "
              + "where n.nspname='public' and f.contype='f' and c.oid<>p.oid")) {
        while (rows.next()) parents.computeIfAbsent(rows.getString(1), k -> new HashSet<>()).add(rows.getString(2));
      }
      var pending = new TreeSet<>(columns.keySet());
      while (!pending.isEmpty()) {
        var ready = pending.stream().filter(t -> Collections.disjoint(parents.getOrDefault(t, Set.of()), pending)).toList();
        assertThat(ready).as("Fixture requires acyclic dependencies between tables").isNotEmpty();
        for (String name : ready) {
          assertThat(keys.get(name)).isNotEmpty();
          tables.add(new Table(name, columns.get(name), keys.get(name)));
        }
        pending.removeAll(ready);
      }
      baseline = snapshot(connection);
    }
  }

  void restore() throws Exception {
    try (var connection = connect()) {
      if (snapshot(connection).equals(baseline)) return;
      connection.setAutoCommit(false);
      try {
        // Parents precede children. Only changed existing rows are updated.
        for (Table t : tables) {
          var mutable = t.columns().stream().filter(c -> !t.keys().contains(c)).toList();
          String conflict = mutable.isEmpty() ? "do nothing" : "do update set "
              + String.join(",", mutable.stream().map(c -> quote(c) + "=excluded." + quote(c)).toList())
              + " where " + String.join(" or ", mutable.stream()
                  .map(c -> "current_row." + quote(c) + " is distinct from excluded." + quote(c)).toList());
          execute(connection, "insert into " + t.relation() + " as current_row (" + joined(t.columns())
              + ") select " + joined(t.columns()) + " from jsonb_populate_recordset(null::"
              + t.relation() + ",cast(? as jsonb)) on conflict (" + joined(t.keys()) + ") " + conflict,
              baseline.get(t.name()));
        }
        var reverse = new ArrayList<>(tables);
        Collections.reverse(reverse);
        for (Table t : reverse) execute(connection, "delete from " + t.relation()
            + " current_row where not exists (select 1 from jsonb_populate_recordset(null::"
            + t.relation() + ",cast(? as jsonb)) baseline_row where " + String.join(" and ",
                t.keys().stream().map(k -> "current_row." + quote(k) + "=baseline_row." + quote(k)).toList())
            + ")", baseline.get(t.name()));
        assertThat(snapshot(connection).equals(baseline)).as("Exact domain restoration before commit").isTrue();
        connection.commit();
      } catch (Exception | AssertionError failure) {
        connection.rollback();
        throw failure;
      }
    }
  }

  private Map<String, String> snapshot(Connection connection) throws SQLException {
    var result = new TreeMap<String, String>();
    String sql = String.join(" union all ", tables.stream().map(t -> "select '" + t.name()
        + "',coalesce(jsonb_agg(to_jsonb(r) order by to_jsonb(r)::text),'[]'::jsonb)::text from "
        + t.relation() + " r").toList());
    try (var query = connection.createStatement(); var rows = query.executeQuery(sql)) {
      while (rows.next()) result.put(rows.getString(1), rows.getString(2));
    }
    return result;
  }

  private static void execute(Connection connection, String sql, String data) throws SQLException {
    try (var query = connection.prepareStatement(sql)) { query.setString(1, data); query.executeUpdate(); }
  }
  private static Connection connect() throws SQLException {
    return DriverManager.getConnection(System.getenv("SPRING_DATASOURCE_URL"),
        System.getenv("BULK_OWNER_USERNAME"), System.getenv("BULK_OWNER_PASSWORD"));
  }
  private static String quote(String name) { return "\"" + name.replace("\"", "\"\"") + "\""; }
  private static String joined(List<String> names) { return String.join(",", names.stream().map(NeonDomainFixture::quote).toList()); }
}
