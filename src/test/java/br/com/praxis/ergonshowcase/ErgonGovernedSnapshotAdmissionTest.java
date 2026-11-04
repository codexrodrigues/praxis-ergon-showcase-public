package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.praxisplatform.rules.contract.PublishedRuleSnapshot;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.praxisplatform.rules.contract.RuleSetRef;
import org.praxisplatform.rules.contract.RuleSnapshotApproval;
import org.praxisplatform.rules.contract.RuleSnapshotSource;

class ErgonGovernedSnapshotAdmissionTest {
  @Test
  void admitsANewImmutableVersionOnlyForItsAuthorizedScopeWithoutSelectingIt() {
    var facade = new ErgonRuleEngineFacade();
    String key = facade.ruleSetKeys().getFirst();
    var product = facade.productRuleSet(key);
    var ref = product.ref();
    var version = new RuleSetRef(ref.domainKey(), ref.boundedContextKey(),
        ref.ruleSetKey(), ref.operationKey(), ref.version() + 1);
    var definition = new RuleSetDefinition(version, product.availableRoots(), product.slots(),
        product.bindings(), product.compatibility(), product.failPolicy());
    var snapshot = new PublishedRuleSnapshot(PublishedRuleSnapshot.SNAPSHOT_CONTRACT_VERSION,
        "snapshot", "2", "prod", "praxis-ergon-showcase", 1, "2026-11-03T12:00:00Z", null,
        "ergon/1", "2026-11-03T12:00:00Z", null,
        List.of(new RuleSnapshotSource("source", "rule", 1, "A".repeat(64))),
        List.of(new RuleSnapshotApproval("approval", "RULE_COMPOSITION_APPROVER", "actor",
            "2026-11-03T11:00:00Z", "B".repeat(64))), definition);

    var before = facade.resolvePlan(key, 2);
    assertThat(facade.admitGovernedSnapshot(key, 2, "prod", "ergon/1", snapshot)
        .snapshot().ruleSet().ref()).isEqualTo(version);
    assertThat(facade.resolvePlan(key, 2).planDigest()).isEqualTo(before.planDigest());
    assertThatThrownBy(() -> facade.admitGovernedSnapshot(key, 7, "prod", "ergon/1", snapshot))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> facade.admitGovernedSnapshot(key, 2, "staging", "ergon/1", snapshot))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
