package br.com.praxis.ergonshowcase.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Opções IDADE_MINIMA e IDADE_MAXIMA_FUNC por órgão, com os defaults do cadastro legado. */
@Component
@ConfigurationProperties(prefix = "ergon.cadastro-servidor")
public class ServidorPessoaAgeOptions {
  public record Limites(Integer idadeMinima, Integer idadeMaximaFunc) {
    public Limites {
      idadeMinima = idadeMinima == null ? 10 : idadeMinima;
      idadeMaximaFunc = idadeMaximaFunc == null ? 100 : idadeMaximaFunc;
      if (idadeMinima < 0 || idadeMaximaFunc < idadeMinima)
        throw new IllegalArgumentException("Faixa etária do cadastro de servidores inválida.");
    }
  }

  private static final Limites DEFAULTS = new Limites(null, null);
  private Map<Integer, Limites> porOrgao = Map.of();

  public Limites resolve(int orgao) {
    return porOrgao.getOrDefault(orgao, DEFAULTS);
  }

  public Map<Integer, Limites> getPorOrgao() {
    return porOrgao;
  }

  public void setPorOrgao(Map<Integer, Limites> porOrgao) {
    this.porOrgao = Map.copyOf(porOrgao);
  }
}
