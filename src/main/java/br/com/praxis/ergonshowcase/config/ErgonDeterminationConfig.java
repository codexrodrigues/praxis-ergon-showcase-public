package br.com.praxis.ergonshowcase.config;

import java.util.*;
import org.praxisplatform.uischema.determination.*;
import org.springframework.context.annotation.*;

/** Bindings estáticos sem tenant, URLs ou decisões aplicadas no schema cacheável. */
@Configuration
public class ErgonDeterminationConfig {
  @Bean
  public ReactiveDeterminationDefinitionProvider ergonDeterminations() {
    return () ->
        List.of(
            definition(
                "ergon.pessoa.pcd",
                "ServidorPessoa",
                "determineErgonPcd",
                List.of("deffisica", "defvisual", "defauditiva", "defmental", "defintelectual"),
                List.of("deffisica", "defvisual", "defauditiva", "defmental", "defintelectual"),
                List.of(
                    new ReactiveDeterminationOutputBinding(
                        "/possuiDeficienciaPcD", "/possuiDeficienciaPcDPrevisto"))),
            definition(
                "ergon.vinculo.provimento",
                "Vinculo",
                "determineErgonProvimento",
                List.of("cargo"),
                List.of("cargo"),
                List.of(
                    new ReactiveDeterminationOutputBinding("/cargoNome", "/cargoNomePrevisto"),
                    new ReactiveDeterminationOutputBinding(
                        "/saldoVagasDisponiveis", "/saldoVagasPrevisto"))),
            definition(
                "ergon.cargo.aglutinador",
                "Cargo",
                "determineErgonAglutinador",
                List.of("cargoAglutinador"),
                List.of("cargo"),
                List.of(
                    new ReactiveDeterminationOutputBinding(
                        "/nome", "/cargoAglutinadorNomePrevisto"))),
            definition(
                "ergon.dependente.elegibilidade",
                "Dependente",
                "determineErgonDependenteElegibilidade",
                List.of("tipodepen", "dtnasc", "universitario", "invalido"),
                List.of("tipodepen", "dtnasc", "universitario", "invalido"),
                List.of(
                    new ReactiveDeterminationOutputBinding(
                        "/tipoDependenteDescricaoPrevista", "/tipoDependenteDescricaoPrevista"),
                    new ReactiveDeterminationOutputBinding(
                        "/idadeCalculadaAnos", "/idadeCalculadaAnos"),
                    new ReactiveDeterminationOutputBinding(
                        "/elegivelIrrfPrevisto", "/elegivelIrrfPrevisto"))),
            definition(
                "ergon.afastamento.previa",
                "Afastamento",
                "determineErgonAfastamentoPrevia",
                List.of("tipofreq", "codfreq", "dtini", "dtprevfim"),
                List.of("tipofreq", "codfreq", "dtini", "dtprevfim"),
                List.of(
                    new ReactiveDeterminationOutputBinding(
                        "/codigoFrequenciaNomePrevisto", "/codigoFrequenciaNomePrevisto"),
                    new ReactiveDeterminationOutputBinding(
                        "/quantidadeDiasPrevista", "/quantidadeDiasPrevista"),
                    new ReactiveDeterminationOutputBinding(
                        "/comOnusPrevisto", "/comOnusPrevisto"))),
            definition(
                "ergon.frequencia.previa",
                "Frequencia",
                "determineErgonFrequenciaPrevia",
                List.of("tipofreq", "codfreq", "dtini", "dtfim"),
                List.of("tipofreq", "codfreq", "dtini", "dtfim"),
                List.of(
                    new ReactiveDeterminationOutputBinding(
                        "/codigoFrequenciaNomePrevisto", "/codigoFrequenciaNomePrevisto"),
                    new ReactiveDeterminationOutputBinding(
                        "/formatoQuantidadePrevisto", "/formatoQuantidadePrevisto"),
                    new ReactiveDeterminationOutputBinding(
                        "/exigeHorarioPrevisto", "/exigeHorarioPrevisto"))),
            definition(
                "ergon.campoCustomizado.previa",
                "CampoCustomizado",
                "determineErgonCampoCustomizadoPrevia",
                List.of("empCodigo", "fieldType", "controlType", "optionTabela"),
                List.of("empCodigo", "fieldType", "controlType", "optionTabela"),
                List.of(
                    new ReactiveDeterminationOutputBinding(
                        "/optionTabelaDescricaoPrevista", "/optionTabelaDescricaoPrevista"),
                    new ReactiveDeterminationOutputBinding(
                        "/requerTabelaOpcoesPrevisto", "/requerTabelaOpcoesPrevisto"),
                    new ReactiveDeterminationOutputBinding(
                        "/escopoGlobalPrevisto", "/escopoGlobalPrevisto"))),
            definition(
                "ergon.parametroContagemFerias.previa",
                "ParametroContagemFerias",
                "determineErgonParametroContagemFeriasPrevia",
                List.of(
                    "temFerias",
                    "totDiasFer",
                    "abonoDirGozo",
                    "numDiasAbonoFer",
                    "denDiasAbonoFer"),
                List.of(
                    "temFerias",
                    "totDiasFer",
                    "abonoDirGozo",
                    "numDiasAbonoFer",
                    "denDiasAbonoFer"),
                List.of(
                    new ReactiveDeterminationOutputBinding("/maxDiasVendFer", "/maxDiasVendFer"),
                    new ReactiveDeterminationOutputBinding(
                        "/minDiasPerPrincFer", "/minDiasPerPrincFer"))),
            definition(
                "ergon.periodoAquisitivoFerias.previa",
                "PeriodoAquisitivoFerias",
                "determineErgonPeriodoAquisitivoPrevia",
                List.of(
                    "empCodigo",
                    "numfunc",
                    "numvinc",
                    "dtini",
                    "dtfim",
                    "totDiasDireito",
                    "faltasApuradas",
                    "diasAfastamentoSuspensivo",
                    "diasGozados",
                    "diasVendidosAbono",
                    "diasConvertidosDobro"),
                List.of(
                    "empCodigo",
                    "numfunc",
                    "numvinc",
                    "dtini",
                    "dtfim",
                    "totDiasDireito",
                    "faltasApuradas",
                    "diasAfastamentoSuspensivo",
                    "diasGozados",
                    "diasVendidosAbono",
                    "diasConvertidosDobro"),
                List.of(
                    new ReactiveDeterminationOutputBinding("/dtfim", "/dtfim"),
                    new ReactiveDeterminationOutputBinding("/totDiasDireito", "/totDiasDireito"),
                    new ReactiveDeterminationOutputBinding(
                        "/saldoDiasDisponivel", "/saldoDiasDisponivel"),
                    new ReactiveDeterminationOutputBinding("/dtPrescricao", "/dtPrescricao"),
                    new ReactiveDeterminationOutputBinding("/faltasApuradas", "/faltasApuradas"),
                    new ReactiveDeterminationOutputBinding(
                        "/diasAfastamentoSuspensivo", "/diasAfastamentoSuspensivo"))),
            definition(
                "ergon.feriasGozo.previa",
                "FeriasGozo",
                "determineErgonFeriasGozoPrevia",
                List.of(
                    "periodoAquisitivoId",
                    "dtini",
                    "dtprevfim",
                    "diasGozo",
                    "abonoPecuniario",
                    "diasAbonoVendidos"),
                List.of(
                    "periodoAquisitivoId",
                    "dtini",
                    "dtprevfim",
                    "diasGozo",
                    "abonoPecuniario",
                    "diasAbonoVendidos"),
                List.of(
                    new ReactiveDeterminationOutputBinding("/chavePeraqfer", "/chavePeraqfer"),
                    new ReactiveDeterminationOutputBinding(
                        "/totDiasDireitoPa", "/totDiasDireitoPa"),
                    new ReactiveDeterminationOutputBinding("/diasGozo", "/diasGozo"),
                    new ReactiveDeterminationOutputBinding("/dtprevfim", "/dtprevfim"),
                    new ReactiveDeterminationOutputBinding("/ordemParcela", "/ordemParcela"),
                    new ReactiveDeterminationOutputBinding(
                        "/diasAbonoVendidos", "/diasAbonoVendidos"),
                    new ReactiveDeterminationOutputBinding("/dtPagtoAbono", "/dtPagtoAbono"),
                    new ReactiveDeterminationOutputBinding(
                        "/saldoPaAposParcela", "/saldoPaAposParcela"))));
  }

  private ReactiveDeterminationDefinition definition(
      String id,
      String resource,
      String operation,
      List<String> source,
      List<String> request,
      List<ReactiveDeterminationOutputBinding> outputs) {
    var inputs = new ArrayList<ReactiveDeterminationInputBinding>();
    for (int i = 0; i < source.size(); i++)
      inputs.add(new ReactiveDeterminationInputBinding("/" + source.get(i), "/" + request.get(i)));
    return new ReactiveDeterminationDefinition(
        id,
        List.of(
            new ReactiveDeterminationScope(
                "createErgon" + resource, ReactiveDeterminationFormMode.CREATE),
            new ReactiveDeterminationScope(
                "updateErgon" + resource, ReactiveDeterminationFormMode.EDIT)),
        operation,
        ReactiveDeterminationTriggerMode.ON_CHANGE,
        source.stream().map(v -> "/" + v).toList(),
        inputs,
        outputs,
        new ReactiveDeterminationProvenance(
            ReactiveDeterminationProvenanceKind.HOST, "ergon-derived-facts", "1"));
  }
}
