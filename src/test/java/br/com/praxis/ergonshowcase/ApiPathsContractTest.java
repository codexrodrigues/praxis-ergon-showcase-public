package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.praxis.ergonshowcase.config.OptionSourcesConfig;
import br.com.praxis.ergonshowcase.controller.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.extension.CustomOpenApiResolver;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;

/** Coerência de autoria e metadata local; não substitui prova HTTP/Neon. */
class ApiPathsContractTest {
  @Test
  void lookupEndpointsResolveToPublishedSourcesAndControllerMappings() throws Exception {
    var descriptors = new OptionSourcesConfig().options().descriptors();
    var controllerPaths = new ArrayList<String>();
    for (var controller : List.of(OrgaoController.class, CargoController.class,
        ServidorPessoaController.class, VinculoController.class,
        SetorController.class, DocumentoLegalController.class,
        DependenteController.class, TipoDependenteController.class,
        TipoFrequenciaController.class, CodigoFrequenciaController.class,
        AfastamentoController.class, RegraFrequenciaEstatutariaController.class,
        FrequenciaController.class, TabelaController.class,
        ItemTabelaController.class, DefinicaoCampoCustomizadoController.class)) {
      var mapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
      assertThat(mapping).as(controller.getSimpleName()).isNotNull();
      controllerPaths.addAll(Arrays.asList(mapping.value()));
    }
    assertThat(descriptors).hasSize(16);
    for (var source : descriptors) {
      assertThat(controllerPaths).contains(source.resourcePath());
      assertThat(source.runtimeContract().byIdsEndpoint()).isNotBlank();
    }

    var converters = new ModelConverters();
    converters.addConverter(new CustomOpenApiResolver(new ObjectMapper().findAndRegisterModules()));
    int checked = 0;
    for (var dto : dtoClasses()) {
      var schema = converters.resolveAsResolvedSchema(new AnnotatedType(dto).resolveAsRef(false)).schema;
      for (var field : dto.getDeclaredFields()) {
        var ui = field.getAnnotation(UISchema.class);
        if (ui == null || ui.endpoint().isBlank()) continue;
        var matches = descriptors.stream()
            .filter(source -> source.runtimeContract().filterEndpoint().equals(ui.endpoint())).toList();
        assertThat(matches).as(dto.getSimpleName() + "." + field.getName()).hasSize(1);
        var property = (io.swagger.v3.oas.models.media.Schema<?>) schema.getProperties().get(field.getName());
        assertThat(property).as("Generated schema property " + field.getName()).isNotNull();
        var xui = new ObjectMapper().valueToTree(property.getExtensions().get("x-ui"));
        assertThat(xui.path("endpoint").asText()).isEqualTo(matches.getFirst().runtimeContract().filterEndpoint());
        checked++;
      }
    }
    assertThat(checked).isGreaterThanOrEqualTo(48);
  }

  @Test
  void productionCodeDoesNotScatterHostPathLiterals() throws Exception {
    var root = Path.of("src/main/java/br/com/praxis/ergonshowcase");
    var violations = new ArrayList<String>();
    try (var files = Files.walk(root)) {
      for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        if (file.equals(root.resolve("constants/ApiPaths.java"))) continue;
        if (Pattern.compile("\"/api/ergon(?:/|\")").matcher(Files.readString(file)).find())
          violations.add(root.relativize(file).toString());
      }
    }
    assertThat(violations).as("Host paths belong in ApiPaths").isEmpty();
  }

  private List<Class<?>> dtoClasses() throws Exception {
    var scanner = new ClassPathScanningCandidateComponentProvider(false) {
      @Override protected boolean isCandidateComponent(
          org.springframework.beans.factory.annotation.AnnotatedBeanDefinition bean) {
        return bean.getMetadata().isIndependent() && !bean.getMetadata().isInterface();
      }
    };
    scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
    var classes = new ArrayList<Class<?>>();
    for (var bean : scanner.findCandidateComponents("br.com.praxis.ergonshowcase.dto"))
      classes.add(Class.forName(bean.getBeanClassName()));
    return classes;
  }
}
