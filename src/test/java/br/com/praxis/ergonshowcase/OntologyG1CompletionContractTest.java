package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.OperationalRuleSet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
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
import org.praxisplatform.rules.facts.PraxisFactTreeBuilder;
import org.praxisplatform.rules.jsonlogic.PraxisJsonLogicEngine;
import org.praxisplatform.rules.jsonlogic.model.JsonLogicEvaluationOptions;

/** G1 source/contract evidence only. Does not construct Spring or a persistence context. */
class OntologyG1CompletionContractTest {
    private static final Path DIR=Path.of("docs/rules/ontology/curated/completion");
    private static final ObjectMapper JSON=new ObjectMapper();

    @Test
    void completeFamilyAndOperationalUniverseHasNoSilentExclusions() throws Exception {
        var host=new ErgonRuleEngineFacade();
        var expected=new HashSet<>(host.ruleSetKeys());
        var completed=new HashSet<String>();
        for(JsonNode f:read("manifest.json").path("families"))assertThat(completed.add(f.path("ruleSetKey").asText())).isTrue();
        assertThat(completed).hasSize(14);
        for(JsonNode f:JSON.readTree(DIR.resolve("../manifest.json").toFile()).path("families"))completed.add("ergon."+f.path("resourceKey").asText()+".chefia");
        assertThat(completed).containsExactlyInAnyOrderElementsOf(expected);
        Set<String> operational=new HashSet<>();
        int calls=0;
        for(JsonNode op:read("operational-bindings.json")) {
            assertThat(operational.add(op.path("enum").asText())).isTrue();
            OperationalRuleSet rule=OperationalRuleSet.valueOf(op.path("enum").asText());
            var definition=rule.productRuleSet(JSON);
            assertThat(op.path("slotKey").asText()).isEqualTo(definition.slots().getFirst().slotKey());
            assertThat(op.path("overridePolicy").asText()).isEqualTo(definition.slots().getFirst().overridePolicy().name());
            assertThat(op.path("reasonCode").asText()).isEqualTo(definition.bindings().getFirst().falseReasonCode());
            var keys=OperationalRuleSet.class.getDeclaredField("keys");keys.setAccessible(true);
            @SuppressWarnings("unchecked") List<String> actualKeys=(List<String>)keys.get(rule);
            assertThat(op.path("argumentMap")).extracting(n->n.path("legacyPath").asText()).containsExactlyElementsOf(actualKeys);
            for(JsonNode call:op.path("calls")) {
                assertThat(call.path("argumentBindings")).hasSize(actualKeys.size());
                assertThat(Files.exists(Path.of(call.path("source").asText()))).isTrue();calls++;
            }
        }
        assertThat(operational).containsExactlyInAnyOrderElementsOf(Arrays.stream(OperationalRuleSet.values()).map(Enum::name).toList());
        assertThat(calls).isEqualTo(50);
    }

    @TestFactory
    Stream<DynamicTest> pathsRolesCollectionsAndHttpIdentitiesAreBound() throws Exception {
        var tests=new ArrayList<DynamicTest>();var host=new ErgonRuleEngineFacade();
        JsonNode migrations=read("path-migration.candidate.json").path("entries");
        for(JsonNode f:read("manifest.json").path("families")) {
            String key=f.path("ruleSetKey").asText();JsonNode binding=read(f.path("bindings").asText());
            JsonNode catalog=read(f.path("catalog").asText());
            tests.add(DynamicTest.dynamicTest(key+" path destinations",()->{
                Set<String> expected=new HashSet<>(host.productRuleSet(key).referencedFactPaths());
                expected.addAll(host.factoryPlan(key,2).definition().referencedFactPaths());
                Set<String> covered=new HashSet<>();
                JsonNode shape=catalog.path("operations").get(key.endsWith("rn013")?0:1).path("snapshotSchema");
                for(JsonNode migration:migrations)if(key.equals(migration.path("ruleSetKey").asText())) {
                    assertThat(covered.add(migration.path("legacyPath").asText())).isTrue();
                    assertThat(read("decisions.json").has(migration.path("semanticDecision").asText())).isTrue();
                    for(JsonNode target:migration.path("targetFields"))assertThat(property(shape,target.asText()).isMissingNode()).as(target.asText()).isFalse();
                    if(migration.path("targetFields").isEmpty())assertThat(migration.path("decision").asText()).isEqualTo("REMOVE_UNBACKED_CONSTANT");
                }
                assertThat(covered).containsExactlyInAnyOrderElementsOf(expected);
            }));
            tests.add(DynamicTest.dynamicTest(key+" HTTP invocation inventory",()->{
                Class<?> controller=Class.forName("br.com.praxis.ergonshowcase.controller."+f.path("entity").asText()+"Controller");
                Set<String> recorded=new HashSet<>();
                for(String section:List.of("operations","nonEvaluatingOperations"))for(JsonNode invocation:binding.path(section)) {
                    String name=invocation.path("methodName").asText();recorded.add(name);
                    var method=Arrays.stream(controller.getMethods()).filter(m->!m.isBridge()&&m.getName().equals(name)).findFirst().orElseThrow();
                    var operation=method.getAnnotation(Operation.class);
                    assertThat(invocation.path("operationId").asText()).isEqualTo(operation==null?"":operation.operationId());
                    String annotation=invocation.path("method").asText().toLowerCase();annotation=Character.toUpperCase(annotation.charAt(0))+annotation.substring(1)+"Mapping";
                    var mapping=Arrays.stream(method.getAnnotations()).filter(a->a.annotationType().getSimpleName().equals(invocation.path("method").asText().substring(0,1)+invocation.path("method").asText().substring(1).toLowerCase()+"Mapping")).findFirst().orElseThrow();
                    assertThat(JSON.valueToTree(mapping.annotationType().getMethod("value").invoke(mapping))).isEqualTo(invocation.path("paths"));
                }
                for(var method:controller.getMethods())if(!method.isBridge())for(var a:method.getAnnotations())
                    if(Set.of("PostMapping","PutMapping","PatchMapping","DeleteMapping").contains(a.annotationType().getSimpleName()))assertThat(recorded).contains(method.getName());
            }));
            for(JsonNode profile:binding.path("profiles")) {
                String operationKey=profile.path("operationKey").asText();JsonNode operation=find(catalog.path("operations"),operationKey);
                tests.add(DynamicTest.dynamicTest(operationKey+" topology",()->{
                    JsonNode schema=operation.path("snapshotSchema");Set<String> roots=new HashSet<>();schema.path("properties").fieldNames().forEachRemaining(roots::add);roots.removeAll(Set.of("draft","current","context","neighborhood"));
                    Set<String> declared=new HashSet<>();
                    for(JsonNode relation:profile.path("conceptRootBindings")) {
                        assertThat(declared.add(relation.path("root").asText())).isTrue();
                        assertThat(relation.path("foreignKey").size()).isEqualTo(relation.path("identity").size());
                        assertThat(relation.path("resolution").asText()).isEqualTo("IDENTITY_FK_AND_SCOPE_ONLY");
                        for(int i=0;i<relation.path("foreignKey").size();i++) {
                            assertThat(property(schema,relation.path("fromRoot").asText()+"."+relation.path("foreignKey").get(i).asText()).isMissingNode()).isFalse();
                            assertThat(property(schema,relation.path("root").asText()+"."+relation.path("identity").get(i).asText()).isMissingNode()).isFalse();
                        }
                        if(relation.path("scope").asText().equals("TENANT"))assertThat(property(schema,relation.path("root").asText()+".empCodigo").isMissingNode()).isFalse();
                    }
                    assertThat(declared).containsExactlyInAnyOrderElementsOf(roots);
                    for(JsonNode collection:profile.path("collectionBindings")) {
                        JsonNode item=property(schema,"neighborhood."+collection.path("collection").asText()).path("items");
                        assertThat(item.isObject()).isTrue();
                        for(JsonNode path:collection.path("sourceFields"))assertThat(property(schema,collection.path("fromRoot").asText()+"."+path.asText()).isMissingNode()).isFalse();
                        for(JsonNode path:collection.path("targetFields"))assertThat(property(item,path.asText()).isMissingNode()).isFalse();
                        assertThat(collection.path("predicateOwner").asText()).isEqualTo("RULESET");
                    }
                }));
            }
        }
        return tests.stream();
    }

    @TestFactory
    Stream<DynamicTest> migratedOperationalCasesExecuteInTheCanonicalEngine() throws Exception {
        var tests=new ArrayList<DynamicTest>();var engine=new PraxisJsonLogicEngine();
        for(JsonNode c:read("operational-semantics.json").path("cases"))tests.add(DynamicTest.dynamicTest(c.path("name").asText(),()->{
            JsonNode facts=c.path("facts");List<String> roots=new ArrayList<>();facts.fieldNames().forEachRemaining(roots::add);
            Object result=engine.evaluate(c.path("expression"),facts,new JsonLogicEvaluationOptions(roots,null,false,"2026-11-04T12:00:00Z","America/Sao_Paulo"));
            assertThat(result).isEqualTo(c.path("expected").booleanValue());
            for(String referenced:PraxisFactTreeBuilder.extractReferencedFactPaths(c.path("expression")))
                assertThat(roots).contains(referenced.split("\\.")[0]);
        }));
        return tests.stream();
    }

    @Test
    void curationSourceRevisionsArePinned() throws Exception {
        var hashes=read("manifest.json").path("sourceHashes");
        assertThat(hashes.size()).isGreaterThan(50);
        var it=hashes.fields();while(it.hasNext()) {
            var e=it.next();assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(e.getKey()))))).as(e.getKey()).isEqualTo(e.getValue().asText());
        }
    }
    private static JsonNode read(String file)throws Exception{return JSON.readTree(DIR.resolve(file).toFile());}
    private static JsonNode find(JsonNode array,String key){for(JsonNode e:array)if(e.path("operationKey").asText().equals(key))return e;throw new AssertionError(key);}
    private static JsonNode property(JsonNode schema,String path){for(String part:path.split("\\."))schema=schema.path("properties").path(part);return schema;}
}
