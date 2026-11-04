package br.com.praxis.ergonshowcase.operations;

import java.sql.*;
import java.util.*;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain.Resource;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkOperations;
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
    for (String resource : List.of("vinculos", "funcionarios", "cargos", "dependentes", "afastamentos", "frequencias", "campos-customizados", "parametros-contagem-ferias", "periodos-aquisitivos-ferias", "ferias-gozo")) {
      for (String mode : List.of("uniform", "items"))
        ids.add(new BulkOperationControlIdentity(NAMESPACE, "ergon." + resource + ".bulk." + mode));
    }
    for (String action : List.of("realocar-setor-lote", "homologar-estagio-lote"))
      ids.add(new BulkOperationControlIdentity(NAMESPACE, "ergon.vinculos." + action));
    ids.add(new BulkOperationControlIdentity(NAMESPACE, "ergon.dependentes.encerrar-dependencia-lote"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE, "ergon.afastamentos.homologar-afastamento-lote"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE, "ergon.frequencias.homologar-frequencia-lote"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE, "ergon.campos-customizados.ativar-campo-customizado-lote"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE, "ergon.parametros-contagem-ferias.ativar-parametro-ferias-lote"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE, "ergon.periodos-aquisitivos-ferias.homologar-periodo-aquisitivo-lote"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE, "ergon.ferias-gozo.homologar-ferias-gozo-lote"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE,"ergon.averbacoes-tempo-servico.bulk.uniform"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE,"ergon.averbacoes-tempo-servico.bulk.items"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE,"ergon.averbacoes-tempo-servico.homologar-averbacoes-lote"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE,"ergon.periodos-aquisitivos-licenca-especial.bulk.uniform"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE,"ergon.periodos-aquisitivos-licenca-especial.bulk.items"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE,"ergon.periodos-aquisitivos-licenca-especial.recalcular-saldos-licenca-especial-lote"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE,"ergon.licencas-especiais-gozo.bulk.uniform"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE,"ergon.licencas-especiais-gozo.bulk.items"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE,"ergon.licencas-especiais-gozo.homologar-licencas-especiais-lote"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE,"ergon.vendas-licenca-especial.bulk.uniform"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE,"ergon.vendas-licenca-especial.bulk.items"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE,"ergon.vendas-licenca-especial.autorizar-vendas-licenca-especial-lote"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE, "administracao-pessoal.designacoes.bulk.update"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE, "administracao-pessoal.designacoes.bulk.update-items"));
    ids.add(new BulkOperationControlIdentity(NAMESPACE, "ergon.designacoes.homologar-designacoes-lote"));
    for (var resource : List.of(Resource.FUNCOES, Resource.VAGAS_FUNCAO, Resource.SUBSTITUICOES)) {
      for (var mode : List.of(BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE)) {
        ids.add(new BulkOperationControlIdentity(NAMESPACE, ErgonBulkOperations.operation(resource, mode)));
      }
    }
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
