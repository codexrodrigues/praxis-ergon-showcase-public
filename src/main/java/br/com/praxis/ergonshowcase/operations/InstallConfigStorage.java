package br.com.praxis.ergonshowcase.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.flywaydb.core.Flyway;

/** Migra a unidade Config antes do startup HTTP, com a identidade administrativa. */
public final class InstallConfigStorage {
  private InstallConfigStorage() {}

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("Informe o caminho do JAR Config selecionado.");
    }
    String url = required("CONFIG_DATASOURCE_URL");
    String schema = required("CONFIG_DATASOURCE_SCHEMA");
    String migrator = required("CONFIG_MIGRATION_USERNAME");
    if (!url.startsWith("jdbc:postgresql://") || !schema.matches("[a-z_][a-z0-9_]*")
        || migrator.equals(required("CONFIG_DATASOURCE_USERNAME"))) {
      throw new IllegalArgumentException("Config exige PostgreSQL, schema e migrador distinto do runtime.");
    }
    Path directory = Files.createTempDirectory("ergon-config-migrations-");
    try (var artifact = new ZipFile(args[0])) {
      if (artifact.getEntry("org/praxisplatform/config/service/DomainRuleSnapshotService.class") == null) {
        throw new IllegalArgumentException("O artefato informado não é o Config Starter.");
      }
      for (var entry : artifact.stream().filter(file ->
          file.getName().matches("db/migration/[A-Za-z0-9_.-]+\\.sql")).toList()) {
        try (var sql = artifact.getInputStream(entry)) {
          Files.copy(sql, directory.resolve(Path.of(entry.getName()).getFileName()));
        }
      }
      var migrations = Flyway.configure()
          .dataSource(url, migrator, required("CONFIG_MIGRATION_PASSWORD"))
          .schemas(schema).defaultSchema(schema)
          .locations("filesystem:" + directory.toString().replace('\\', '/'))
          .failOnMissingLocations(true)
          .load();
      System.out.println("Migrações Config aplicadas: " + migrations.migrate().migrationsExecuted);
      migrations.validate();
    } finally {
      try (var files = Files.list(directory)) {
        for (Path file : files.toList()) Files.delete(file);
      }
      Files.delete(directory);
    }
  }

  private static String required(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Configuração obrigatória ausente: " + name);
    }
    return value;
  }
}
