package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import org.praxisplatform.rules.jsonlogic.PraxisJsonLogicEngine;
import org.praxisplatform.rules.jsonlogic.model.JsonLogicEvaluationOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

/** Candidate contract/source conformance only. Never starts Spring, JDBC, Flyway, or Hibernate. */
class OntologyCuratedLeadershipContractTest {
    private static final Path DIRECTORY = Path.of("docs/rules/ontology/curated");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void curationIsBoundToTheExaminedSourceRevisions() throws Exception {
        var hashes = read("manifest.json").path("sourceHashes").fields();
        while (hashes.hasNext()) {
            var entry = hashes.next();
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(Path.of(entry.getKey()))));
            assertThat(actual).as(entry.getKey() + ": re-curate after source changes").isEqualTo(entry.getValue().asText());
        }
    }

    @TestFactory
    Stream<DynamicTest> entityAccountingAndOperationBindingsAreExplicit() throws Exception {
        List<DynamicTest> tests = new ArrayList<>();
        var host = new ErgonRuleEngineFacade();
        for (JsonNode family : read("manifest.json").path("families")) {
            String entity = family.path("entity").asText();
            JsonNode bindings = read(family.path("bindings").asText());
            JsonNode catalog = read(family.path("catalog").asText());
            JsonNode fixtures = read(family.path("fixtures").asText());
            tests.add(DynamicTest.dynamicTest(entity + " entity accounting", () -> {
                Set<String> business = new HashSet<>();
                for (var field : Class.forName("br.com.praxis.ergonshowcase.domain." + entity).getDeclaredFields()) {
                    if (field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(Embedded.class)
                            || field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(Version.class)) business.add(field.getName());
                }
                Set<String> accounted = new HashSet<>();
                bindings.path("entityProjection").forEach(n -> assertThat(accounted.add(n.asText())).isTrue());
                bindings.path("exclusions").forEach(n -> {
                    assertThat(n.path("reason").asText()).isNotBlank();
                    assertThat(accounted.add(n.path("field").asText())).isTrue();
                });
                assertThat(accounted).containsExactlyInAnyOrderElementsOf(business);
            }));
            for (JsonNode binding : bindings.path("operations")) {
                String key = binding.path("operationKey").asText();
                JsonNode shape = findOperation(catalog, key).path("snapshotSchema");
                JsonNode positive = findByKey(fixtures, key).path("positive");
                tests.add(DynamicTest.dynamicTest(key + " canonical HTTP and concept binding", () -> {
                    Class<?> controller = Class.forName("br.com.praxis.ergonshowcase.controller." + binding.path("controller").asText());
                    String base = (String) ApiPaths.class.getField(binding.path("pathConstant").asText().replace("ApiPaths.", "")).get(null);
                    assertThat(base).startsWith("/api/ergon/");
                    Method endpoint;
                    if ("DELETE".equals(binding.path("mode").asText())) {
                        assertThat(binding.path("operationId").isNull()).isTrue();
                        String deleteMethod = key.endsWith(".delete-batch") ? "deleteBatch" : "delete";
                        endpoint = Arrays.stream(controller.getMethods()).filter(m -> m.isAnnotationPresent(DeleteMapping.class)
                                && m.getName().equals(deleteMethod)).findFirst().orElseThrow();
                        assertThat(binding.path("ruleSelection").asText()).isEqualTo("DELETE_GUARDS_TO_MIGRATE");
                    } else {
                        endpoint = Arrays.stream(controller.getMethods()).filter(m -> m.isAnnotationPresent(Operation.class)
                                && m.getAnnotation(Operation.class).operationId().equals(binding.path("operationId").asText()))
                                .findFirst().orElseThrow();
                    }
                    String[] paths;
                    String method;
                    if (endpoint.isAnnotationPresent(PostMapping.class)) { paths = endpoint.getAnnotation(PostMapping.class).value(); method = "POST"; }
                    else if (endpoint.isAnnotationPresent(PutMapping.class)) { paths = endpoint.getAnnotation(PutMapping.class).value(); method = "PUT"; }
                    else { paths = endpoint.getAnnotation(DeleteMapping.class).value(); method = "DELETE"; }
                    assertThat(binding.path("method").asText()).isEqualTo(method);
                    String normalizedPath = paths.length == 0 ? "" : paths[0].replace("{id:^(?!batch$).+}", "{id}");
                    assertThat(binding.path("relativePath").asText()).isEqualTo(normalizedPath);
                    Set<String> concepts = new HashSet<>();
                    shape.path("properties").fieldNames().forEachRemaining(concepts::add);
                    concepts.removeAll(Set.of("draft", "current", "context", "neighborhood"));
                    Set<String> bound = new HashSet<>();
                    for (JsonNode relation : binding.path("conceptRootBindings")) {
                        String root = relation.path("root").asText();
                        assertThat(bound.add(root)).as("duplicate concept " + root).isTrue();
                        assertThat(relation.path("target").path("serviceKey").asText()).isEqualTo("praxis-ergon-showcase");
                        assertThat(relation.path("target").path("contextKey").asText()).isEqualTo("administracao-pessoal");
                        assertThat(relation.path("target").path("resourceKey").asText()).startsWith("administracao-pessoal.");
                        assertThat(relation.path("foreignKey").size()).isEqualTo(relation.path("identity").size());
                        for (int i = 0; i < relation.path("foreignKey").size(); i++) {
                            JsonNode from = property(shape, relation.path("fromRoot").asText() + "." + relation.path("foreignKey").get(i).asText());
                            JsonNode to = property(shape, root + "." + relation.path("identity").get(i).asText());
                            assertThat(from.isMissingNode()).as("source " + relation).isFalse();
                            assertThat(to.isMissingNode()).as("target " + relation).isFalse();
                            JsonNode sourceValue = valueAt(positive.path(relation.path("fromRoot").asText()), relation.path("foreignKey").get(i).asText());
                            JsonNode targetValue = valueAt(positive.path(root), relation.path("identity").get(i).asText());
                            if (!positive.path(root).isNull()) assertThat(targetValue).isEqualTo(sourceValue);
                            else assertThat(sourceValue.isNull()).isTrue();
                        }
                        assertThat(property(shape, root + "." + relation.path("tenantPath").asText()).isMissingNode()).isFalse();
                        if (relation.has("ownerLink")) {
                            JsonNode owner = relation.path("ownerLink");
                            for (int i = 0; i < owner.path("fields").size(); i++) {
                                assertThat(property(shape, owner.path("fromRoot").asText() + "." + owner.path("fields").get(i).asText()).isMissingNode()).isFalse();
                                assertThat(property(shape, root + "." + owner.path("targetFields").get(i).asText()).isMissingNode()).isFalse();
                            }
                        }
                    }
                    assertThat(bound).containsExactlyInAnyOrderElementsOf(concepts);
                    Set<String> collections = new HashSet<>();
                    shape.path("properties").path("neighborhood").path("properties").fieldNames().forEachRemaining(collections::add);
                    Set<String> scopedCollections = new HashSet<>();
                    for (JsonNode collection : binding.path("collectionBindings")) {
                        String name = collection.path("collection").asText();
                        assertThat(scopedCollections.add(name)).isTrue();
                        JsonNode item = property(shape, "neighborhood." + name).path("items");
                        assertThat(collection.path("sourceFields").size()).isEqualTo(collection.path("targetFields").size());
                        for (int i = 0; i < collection.path("sourceFields").size(); i++) {
                            assertThat(property(shape, collection.path("fromRoot").asText() + "." + collection.path("sourceFields").get(i).asText()).isMissingNode()).isFalse();
                            assertThat(property(item, collection.path("targetFields").get(i).asText()).isMissingNode()).isFalse();
                        }
                        assertThat(property(item, collection.path("tenantTarget").asText()).isMissingNode()).isFalse();
                        assertThat(collection.path("queryPredicates").asText()).isEqualTo("IDENTITY_RELATION_AND_TENANT_ONLY");
                    }
                    assertThat(scopedCollections).containsExactlyInAnyOrderElementsOf(collections);
                    if ("PREVIEW".equals(binding.path("mode").asText())) {
                        var product = host.productRuleSet(bindings.path("ruleSetKey").asText());
                        var selected = product.bindings().stream().filter(b -> b.falseReasonCode().equals(binding.path("reasonCode").asText())).findFirst().orElseThrow();
                        assertThat(key).isEqualTo("preview-" + selected.bindingKey());
                        assertThat(binding.path("slotKey").asText()).isEqualTo(selected.slotKey());
                        assertThat(Arrays.stream(ErgonRuleEngineFacade.class.getMethods()).anyMatch(m -> m.getName().equals(binding.path("facadeMethod").asText()))).isTrue();
                        assertThat(shape.path("properties").has("neighborhood")).isFalse();
                    }
                }));
            }
        }
        return tests.stream();
    }

    @Test
    void lookupOnlyDeterminationDoesNotAcquireAGovernedEvaluation() throws Exception {
        JsonNode bindings = read("designacoes.operation-bindings.candidate.json");
        assertThat(bindings.path("nonEvaluatingOperations").get(0).path("operationId").asText()).isEqualTo("determineErgonDesignacaoFuncaoPrevia");
        for (JsonNode operation : bindings.path("operations")) assertThat(operation.path("mode").asText()).isNotEqualTo("PREVIEW");
    }

    @Test
    void everyProductAndFactoryCustomizationPathHasAnExplicitMigration() throws Exception {
        var host = new ErgonRuleEngineFacade();
        JsonNode migrations = read("path-migration.candidate.json").path("families");
        for (JsonNode family : read("manifest.json").path("families")) {
            String resource = family.path("resourceKey").asText();
            String shortName = resource.substring(resource.indexOf('.') + 1);
            String ruleSet = "ergon." + resource + ".chefia";
            Set<String> expected = new HashSet<>(host.productRuleSet(ruleSet).referencedFactPaths());
            expected.addAll(host.factoryPlan(ruleSet, 2).definition().referencedFactPaths());
            Set<String> accounted = new HashSet<>();
            JsonNode shape = findOperation(read(family.path("catalog").asText()),
                    "updateErgon" + family.path("entity").asText()).path("snapshotSchema");
            for (JsonNode migration : migrations.path(shortName)) {
                assertThat(accounted.add(migration.path("legacyPath").asText())).isTrue();
                assertThat(migration.path("decision").asText()).isNotBlank();
                for (JsonNode target : migration.path("targetFields")) {
                    assertThat(property(shape, target.asText()).isMissingNode()).as(target.asText()).isFalse();
                }
            }
            assertThat(accounted).as(resource).containsExactlyInAnyOrderElementsOf(expected);
        }
    }

    @TestFactory
    Stream<DynamicTest> curatedBusinessDistinctionsExecuteInTheCanonicalDialect() throws Exception {
        List<DynamicTest> tests = new ArrayList<>();
        var engine = new PraxisJsonLogicEngine();
        for (JsonNode fixture : read("semantic-fixtures.json").path("cases")) {
            tests.add(DynamicTest.dynamicTest(fixture.path("name").asText(), () -> {
                JsonNode facts = fixture.path("facts");
                List<String> roots = new ArrayList<>();
                facts.fieldNames().forEachRemaining(roots::add);
                Object actual = engine.evaluate(fixture.path("expression"), facts,
                        new JsonLogicEvaluationOptions(roots, null, false,
                                "2026-01-01T00:00:00Z", "America/Sao_Paulo"));
                if (fixture.path("expected").isNumber()) {
                    assertThat(new java.math.BigDecimal(actual.toString()))
                            .isEqualByComparingTo(fixture.path("expected").decimalValue());
                } else assertThat(actual).isEqualTo(fixture.path("expected").booleanValue());
            }));
        }
        return tests.stream();
    }

    private static JsonNode property(JsonNode shape, String path) {
        JsonNode result = shape;
        for (String part : path.split("\\.")) result = result.path("properties").path(part);
        return result;
    }
    private static JsonNode findOperation(JsonNode catalog, String key) {
        return findByKey(catalog.path("operations"), key);
    }
    private static JsonNode findByKey(JsonNode operations, String key) {
        for (JsonNode operation : operations) if (key.equals(operation.path("operationKey").asText())) return operation;
        throw new AssertionError("Missing schema " + key);
    }
    private static JsonNode valueAt(JsonNode value, String path) {
        for (String part : path.split("\\.")) value = value.path(part);
        return value;
    }
    private static JsonNode read(String name) throws Exception { return JSON.readTree(DIRECTORY.resolve(name).toFile()); }
}

