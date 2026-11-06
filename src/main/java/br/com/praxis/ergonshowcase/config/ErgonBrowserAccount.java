package br.com.praxis.ergonshowcase.config;

import java.util.ArrayList;
import java.util.List;

/** Temporary synthetic provisioning, never a legacy role or an authorization by sector. */
public record ErgonBrowserAccount(String username, String passwordHash, String displayName,
    int organization, String organizationLabel, String environment, Profile profile, String sectorLabel) {
  public enum Profile { READER, PERSONAL, MANAGER }
  public ErgonBrowserAccount {
    if (username == null || !username.matches("[a-z][a-z0-9._-]{2,63}")
        || passwordHash == null || !passwordHash.matches("\\{bcrypt}\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")
        || displayName == null || displayName.isBlank() || displayName.length() > 100
        || organization < 1 || organization > 999_999_999
        || organizationLabel == null || organizationLabel.isBlank()
        || environment == null || !environment.matches("[a-z][a-z0-9-]{0,31}") || profile == null)
      throw new IllegalArgumentException("Conta sintética exige identidade, hash bcrypt, apresentação, perfil e escopo explícitos.");
  }
  public List<String> capabilities() {
    var result = new ArrayList<>(List.of("ergon.functions.read", "ui.config.read"));
    if (profile != Profile.READER) result.add("ui.authoring.configure");
    if (profile == Profile.MANAGER) result.add("ui.config.tenant.write");
    return List.copyOf(result);
  }
  public List<String> authorities() {
    var result = new ArrayList<>(capabilities());
    result.add("ERGON_READ");
    result.add("ERGON_CONFIG_SCOPE:" + organization + ":" + environment);
    return List.copyOf(result);
  }
  @Override public String toString() { return "ErgonBrowserAccount[redacted]"; }
}
