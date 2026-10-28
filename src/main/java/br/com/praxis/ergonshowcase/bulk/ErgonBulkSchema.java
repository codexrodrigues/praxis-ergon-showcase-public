package br.com.praxis.ergonshowcase.bulk;

import br.com.praxis.ergonshowcase.dto.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.praxisplatform.uischema.bulk.*;

/** Modelos OpenAPI do protocolo: refinam a identidade wire dos DTOs genéricos do SDK. */
public final class ErgonBulkSchema {
  private ErgonBulkSchema() {}
  public record VinculoTarget(
      @Schema(minLength=1) String id,
      @Schema(description="ETag forte do registro, obtido na leitura; não é ETag do schema.") String expectedVersion) {}
  public record PessoaTarget(
      @Schema(type="string",format="int64-decimal",pattern="^(?:0|[1-9][0-9]*|-[1-9][0-9]*)$",maxLength=20,
          description="Canonical decimal string in the inclusive range -9223372036854775808 to 9223372036854775807; validated by this codec.") String id,
      @Schema(description="ETag forte da pessoa selecionada, preservado até a confirmação.") String expectedVersion) {}
  public record CargoTarget(
      @Schema(type="integer",format="int32",minimum="-2147483648",maximum="2147483647") Integer id,
      @Schema(description="ETag forte da revisão do cargo usada na avaliação.") String expectedVersion) {}
  public record VinculoSelection(
      @Schema(description="EXPLICIT usa identidades e versões; QUERY congela o resultado do filtro na avaliação.") BulkSelectionMode mode,
      @Schema(description="Vínculos explicitamente selecionados, com versão individual.") List<VinculoTarget> targets,
      @Schema(description="Predicados oficiais do recurso de vínculos para seleção QUERY.") VinculoFilterDTO filter,
      @Schema(description="Identidades removidas do resultado QUERY antes da captura.") List<String> excludedIds) {}
  public record PessoaSelection(
      @Schema(description="Forma explícita ou por filtro de selecionar o cadastro civil.") BulkSelectionMode mode,
      @Schema(description="Pessoas selecionadas com ETag individual.") List<PessoaTarget> targets,
      @Schema(description="Filtro oficial do cadastro de pessoas utilizado na captura QUERY.") ServidorPessoaFilterDTO filter,
      @Schema(description="Números funcionais excluídos da seleção QUERY, em representação decimal textual.") List<String> excludedIds) {}
  public record CargoSelection(
      @Schema(description="Modalidade de seleção explícita ou por filtro do quadro de cargos.") BulkSelectionMode mode,
      @Schema(description="Cargos e versões explicitamente selecionados.") List<CargoTarget> targets,
      @Schema(description="Filtro oficial de cargos para congelamento da seleção QUERY.") CargoFilterDTO filter,
      @Schema(description="Códigos excluídos do resultado QUERY antes de avaliar.") List<Integer> excludedIds) {}
  @Schema(description="Avaliação de uma alteração comum de lotação, jornada ou pagamento dos vínculos selecionados, sem efetivar a alteração.")
  public record VinculoUniform(
      @Schema(description="SYNC avalia os vínculos nesta requisição; cada confirmação futura terá resultado por vínculo.") BulkExecutionMode executionMode,
      @Schema(description="Vínculos e versões explícitos, ou filtro cuja população será congelada nesta avaliação.") VinculoSelection selection,
      @Schema(description="Intenções SET ou CLEAR para campos declarados editáveis em lote; campos omitidos conservam seu valor.") List<BulkFieldChange> changes) {}
  @Schema(description="Avaliação de alterações cadastrais comuns às pessoas selecionadas; não modifica CPF, nome ou nascimento.")
  public record PessoaUniform(
      @Schema(description="SYNC avalia o cadastro civil nesta requisição, sem iniciar execução assíncrona.") BulkExecutionMode executionMode,
      @Schema(description="Pessoas selecionadas por identidade e versão ou pelos predicados do cadastro civil.") PessoaSelection selection,
      @Schema(description="Alterações comuns de escolaridade, raça/cor, endereço ou pagamento, limitadas à lista publicada pelo recurso.") List<BulkFieldChange> changes) {}
  @Schema(description="Avaliação de reenquadramento comum dos cargos selecionados, conservando identidade, órgão e extinção.")
  public record CargoUniform(
      @Schema(description="SYNC avalia o quadro de cargos na mesma requisição, com validação individual dos cargos.") BulkExecutionMode executionMode,
      @Schema(description="Cargos e versões explícitos ou resultado congelado do filtro de cargos.") CargoSelection selection,
      @Schema(description="Intenções comuns sobre enquadramento, escolaridade e controle de vagas; as invariantes de vagas permanecem obrigatórias.") List<BulkFieldChange> changes) {}
  public record VinculoItem(
      @Schema(minLength=1) String id,
      @Schema(description="ETag forte do vínculo lido antes de propor suas alterações individualizadas.") String expectedVersion,
      @Schema(description="Alterações exclusivas deste vínculo; não são propagadas aos demais itens.") List<BulkFieldChange> changes) {}
  public record PessoaItem(
      @Schema(type="string",format="int64-decimal",pattern="^(?:0|[1-9][0-9]*|-[1-9][0-9]*)$",maxLength=20,
          description="Canonical decimal string in the inclusive range -9223372036854775808 to 9223372036854775807; validated by this codec.") String id,
      @Schema(description="ETag forte do cadastro civil lido antes de propor a alteração desta pessoa.") String expectedVersion,
      @Schema(description="Alterações cadastrais exclusivas desta pessoa, restritas aos campos liberados para lote.") List<BulkFieldChange> changes) {}
  public record CargoItem(
      @Schema(type="integer",format="int32",minimum="-2147483648",maximum="2147483647") Integer id,
      @Schema(description="ETag forte do cargo lido antes de propor seu reenquadramento.") String expectedVersion,
      @Schema(description="Alterações exclusivas deste cargo, sujeitas às regras de agregação e de vagas.") List<BulkFieldChange> changes) {}
  @Schema(description="Avaliação de jornadas, lotações ou condições de pagamento diferentes por vínculo.")
  public record VinculoItems(
      @Schema(description="SYNC conclui a avaliação nesta requisição; a gravação depende de confirmação posterior.") BulkExecutionMode executionMode,
      @Schema(description="Vínculos distintos, cada qual com seu ETag e sua própria lista de alterações.") List<VinculoItem> items) {}
  @Schema(description="Avaliação de alterações diferentes para cada pessoa do cadastro civil.")
  public record PessoaItems(
      @Schema(description="SYNC avalia cada pessoa sem gravar as alterações propostas.") BulkExecutionMode executionMode,
      @Schema(description="Pessoas distintas com versão e intenções cadastrais individuais.") List<PessoaItem> items) {}
  @Schema(description="Avaliação de reenquadramentos diferentes para cada cargo do quadro autorizado.")
  public record CargoItems(
      @Schema(description="SYNC avalia os cargos sem efetivar reenquadramento nem alterar o saldo de vagas.") BulkExecutionMode executionMode,
      @Schema(description="Cargos distintos com versão e alterações próprias.") List<CargoItem> items) {}
  @Schema(description="Avaliação de uma portaria coletiva de realocação de vínculos ativos ou cedidos dentro de seus órgãos.")
  public record RealocarCommand(
      @Schema(description="SYNC avalia a realocação nesta requisição, sem mudar lotação ou exercício.") BulkExecutionMode executionMode,
      @Schema(description="Vínculos explicitamente selecionados e suas versões anteriores à portaria.") VinculoSelection selection,
      @Schema(description="Setores de destino e documento legal que fundamentam o ato coletivo.") RealocarSetorLoteDTO parameters) {}
  @Schema(description="Avaliação de homologação coletiva de estágios ainda abertos de vínculos ativos.")
  public record HomologarCommand(
      @Schema(description="SYNC avalia a elegibilidade nesta requisição, sem registrar a homologação.") BulkExecutionMode executionMode,
      @Schema(description="Vínculos explicitamente selecionados com versão individual para o ato de homologação.") VinculoSelection selection,
      @Schema(description="Data e documento legal que formalizam a homologação administrativa coletiva.") HomologarEstagioLoteDTO parameters) {}
}
