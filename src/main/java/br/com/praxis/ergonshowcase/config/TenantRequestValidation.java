package br.com.praxis.ergonshowcase.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Valida a sintaxe do contexto de órgão antes das projeções de disponibilidade.
 * O contexto não concede acesso individual; as autoridades e regras do recurso
 * continuam sendo verificadas por segurança, serviço e invariantes.
 */
@Configuration(proxyBeanMethods = false)
public class TenantRequestValidation implements WebMvcConfigurer, HandlerInterceptor {
  private final TenantContext tenant;

  public TenantRequestValidation(TenantContext tenant) {
    this.tenant = tenant;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(this);
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (handler instanceof HandlerMethod method
        && AnnotatedElementUtils.hasAnnotation(method.getBeanType(), ApiResource.class)) {
      tenant.resolveTenant();
    }
    return true;
  }
}
