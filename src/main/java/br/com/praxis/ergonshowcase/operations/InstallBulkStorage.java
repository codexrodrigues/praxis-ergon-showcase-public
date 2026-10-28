package br.com.praxis.ergonshowcase.operations;

import java.sql.*;
import java.util.*;
import org.praxisplatform.uischema.bulk.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Instalação administrativa explícita; nunca executada pelo startup HTTP nem pelo runtime. */
public final class InstallBulkStorage {
  public static final String NAMESPACE = "ergon-showcase";
  public static final String DEPLOYMENT = "ergon-showcase-neon-proof";
  public static final String RUNTIME = "ergon_showcase_bulk_runtime";
  public static final String CONTROL = "ergon_showcase_bulk_control";

  public static List<BulkOperationControlIdentity> identities() {
    var ids = new ArrayList<BulkOperationControlIdentity>();
    for (String resource : List.of("vinculos", "funcionarios", "cargos")) {
      for (String mode : List.of("uniform", "items"))
        ids.add(new BulkOperationControlIdentity(NAMESPACE, "ergon." + resource + ".bulk." + mode));
    }
    for (String action : List.of("realocar-setor-lote", "homologar-estagio-lote"))
      ids.add(new BulkOperationControlIdentity(NAMESPACE, "ergon.vinculos." + action));
    return List.copyOf(ids);
  }

  public static void main(String[] args) throws Exception {
    if (!"br-super-sound-b7urizmd".equals(required("PROOF_NEON_BRANCH")))
      throw new IllegalArgumentException("A instalação desta prova exige a branch Neon isolada atribuída.");
    var ds = new DriverManagerDataSource(required("SPRING_DATASOURCE_URL"),
        required("BULK_OWNER_USERNAME"), required("BULK_OWNER_PASSWORD"));
    try (var connection = ds.getConnection()) {
      for (String role : List.of(RUNTIME, CONTROL)) {
        try (var query = connection.prepareStatement("select 1 from pg_roles where rolname=?")) {
          query.setString(1, role);
          try (var rows = query.executeQuery()) {
            if (!rows.next()) {
              String password = required(role.equals(RUNTIME) ? "BULK_RUNTIME_PASSWORD" : "BULK_CONTROL_PASSWORD");
              try (var statement = connection.createStatement()) {
                statement.execute("CREATE ROLE \"" + role + "\" LOGIN INHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD '" + password.replace("'", "''") + "'");
              }
            }
          }
        }
      }
      try (var statement = connection.createStatement()) {
        statement.execute("GRANT USAGE ON SCHEMA public TO " + RUNTIME);
        statement.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO " + RUNTIME);
        statement.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO " + RUNTIME);
      }
    } catch (SQLException failure) {
      throw new IllegalStateException("Provisionamento de papéis falhou; SQLSTATE=" + failure.getSQLState());
    }
    var roles = new BulkExecutionRoleConfiguration(required("BULK_OWNER_USERNAME"), Set.of(RUNTIME), Set.of(), Set.of(CONTROL));
    int migrations = BulkExecutionMigrator.migrate(ds, Map.of(NAMESPACE, DEPLOYMENT), roles, identities());
    BulkExecutionMigrator.validate(ds, roles);
    System.out.println("Infraestrutura técnica canônica instalada e validada; migrations=" + migrations);
  }

  private static String required(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) throw new IllegalArgumentException("Configuração obrigatória ausente: " + name);
    return value;
  }
}
