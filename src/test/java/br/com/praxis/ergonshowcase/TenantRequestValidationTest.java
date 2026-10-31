package br.com.praxis.ergonshowcase;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.config.TenantRequestValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TenantRequestValidationTest {
  @RestController
  @ApiResource(resourceKey = "proof.tenant", path = "/tenant-proof")
  static class Resource {
    private int invocations;
    @GetMapping String read() {
      invocations++;
      return new TenantContext().resolveTenant().orElseThrow();
    }
  }

  @RestController
  static class Unrelated {
    @GetMapping("/unrelated-proof") String read() { return "unrelated"; }
  }

  private final Resource resource = new Resource();

  private org.springframework.test.web.servlet.MockMvc mvc() {
    return MockMvcBuilders.standaloneSetup(resource, new Unrelated())
        .addInterceptors(new TenantRequestValidation(new TenantContext())).build();
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid", "0", "-1", "1000000000"})
  void malformedContextIsRejectedBeforeResourceInvocation(String value) throws Exception {
    mvc().perform(get("/tenant-proof").header("X-Tenant", value)).andExpect(status().isBadRequest());
    org.junit.jupiter.api.Assertions.assertEquals(0, resource.invocations,
        "Malformed context must be rejected before the resource can evaluate availability");
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "2", "999999999"})
  void validContextKeepsItsValue(String value) throws Exception {
    mvc().perform(get("/tenant-proof").header("X-Tenant", value))
        .andExpect(status().isOk()).andExpect(content().string(value));
  }

  @Test void absentContextKeepsTheDocumentedDefault() throws Exception {
    mvc().perform(get("/tenant-proof")).andExpect(status().isOk()).andExpect(content().string("1"));
  }

  @Test void unrelatedHandlersDoNotAcquireTenantSemantics() throws Exception {
    mvc().perform(get("/unrelated-proof").header("X-Tenant", "invalid"))
        .andExpect(status().isOk()).andExpect(content().string("unrelated"));
  }
}
