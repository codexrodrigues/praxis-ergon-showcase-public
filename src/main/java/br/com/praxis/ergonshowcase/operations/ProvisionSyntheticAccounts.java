package br.com.praxis.ergonshowcase.operations;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.SecureRandom;
import java.util.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

/** Explicit local provisioning. Prints paths only, never passwords/hashes or environment contents. */
public final class ProvisionSyntheticAccounts {
  private ProvisionSyntheticAccounts() {}
  public static void main(String[] args) throws Exception {
    if (args.length != 1 || !args[0].matches("[a-z][a-z0-9-]{0,31}"))
      throw new IllegalArgumentException("Informe o ambiente: local ou o ambiente real do serviço.");
    Path directory = Path.of(".local", "auth").toAbsolutePath().normalize();
    if (Files.exists(directory)) throw new IllegalStateException("Diretório já existe; preservar credenciais. Rotação exige remoção administrativa explícita.");
    Files.createDirectories(directory);
    var acl = Files.getFileAttributeView(directory, AclFileAttributeView.class);
    if (acl != null) {
      acl.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(Files.getOwner(directory))
          .setPermissions(EnumSet.allOf(AclEntryPermission.class))
          .setFlags(AclEntryFlag.DIRECTORY_INHERIT, AclEntryFlag.FILE_INHERIT).build()));
    } else {
      Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
    }
    var accounts = List.of(
        List.of("prova.ana", "Ana de Prova", "1", "PERSONAL", "atendimento"),
        List.of("prova.bia", "Bia de Prova", "1", "PERSONAL", "atendimento"),
        List.of("prova.gestor", "Gestor de Prova", "1", "MANAGER", "atendimento"),
        List.of("prova.consulta", "Consulta de Prova", "1", "READER", "atendimento"),
        List.of("prova.setor", "Setor de Prova", "1", "PERSONAL", "planejamento"),
        List.of("prova.empresa", "Empresa de Prova", "2", "PERSONAL", "atendimento"));
    var properties = new Properties();
    var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    var random = new SecureRandom();
    var credentials = new StringBuilder("# Credenciais sintéticas privadas; não copiar para chat ou versionamento.\n");
    for (int index = 0; index < accounts.size(); index++) {
      var account = accounts.get(index);
      byte[] entropy = new byte[24]; random.nextBytes(entropy);
      String password = Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
      String prefix = "showcase.security.browser-accounts[" + index + "].";
      properties.setProperty(prefix + "username", account.get(0));
      properties.setProperty(prefix + "password-hash", encoder.encode(password));
      properties.setProperty(prefix + "display-name", account.get(1));
      properties.setProperty(prefix + "organization", account.get(2));
      properties.setProperty(prefix + "organization-label", "Empresa sintética " + account.get(2));
      properties.setProperty(prefix + "environment", args[0]);
      properties.setProperty(prefix + "profile", account.get(3));
      properties.setProperty(prefix + "sector-label", account.get(4));
      credentials.append(account.get(0)).append('=').append(password).append('\n');
    }
    properties.setProperty("showcase.security.domain-environment", args[0]);
    // Java/Spring .properties loaders use ISO-8859-1; OutputStream store escapes Unicode portably.
    try (var output = Files.newOutputStream(directory.resolve("accounts.properties"), StandardOpenOption.CREATE_NEW)) {
      properties.store(output, "Synthetic Showcase accounts; private configuration");
    }
    Files.writeString(directory.resolve("credentials.txt"), credentials, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    System.out.println("Configuração: " + directory.resolve("accounts.properties"));
    System.out.println("Credenciais privadas: " + directory.resolve("credentials.txt"));
  }
}
