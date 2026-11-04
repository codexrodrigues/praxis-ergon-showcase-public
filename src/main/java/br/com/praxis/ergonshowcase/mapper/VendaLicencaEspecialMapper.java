package br.com.praxis.ergonshowcase.mapper;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;

@Mapper(imports = VinculoId.class, componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class VendaLicencaEspecialMapper implements ResourceMapper<VendaLicencaEspecial, VendaLicencaEspecialResponseDTO, CreateVendaLicencaEspecialDTO, UpdateVendaLicencaEspecialDTO, Long> {
  @Mapping(target = "orgaoNome", source = "orgao.nome")
  @Mapping(target = "pessoaNome", source = "pessoa.nome")
  @Mapping(target = "vinculoMatricula", source = "vinculo.matric")
  @Mapping(target = "documentoLegalNumero", source = "documentoLegal.numeroDocumento")
  @Mapping(target = "atoPublicacaoNumero", source = "atoPublicacao.numeroDocumento")
  @Mapping(target = "vinculoId", expression = "java(entity.getNumfunc() == null || entity.getNumvinc() == null ? null : new VinculoId(entity.getNumfunc(), entity.getNumvinc()))")
  public abstract VendaLicencaEspecialResponseDTO toResponse(VendaLicencaEspecial entity);
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "auditoria", ignore = true)
  @Mapping(target = "empCodigo", source = "empCodigo")
  @Mapping(target = "numfunc", source = "numfunc")
  @Mapping(target = "numvinc", source = "numvinc")
  @Mapping(target = "descricao", source = "descricao")
  @Mapping(target = "dtini", source = "dtini")
  @Mapping(target = "dtfim", source = "dtfim")
  @Mapping(target = "situacao", source = "situacao")
  @Mapping(target = "documentoLegalId", source = "documentoLegalId")
  @Mapping(target = "atoPublicacaoId", source = "atoPublicacaoId")
  @Mapping(target = "observacoes", source = "observacoes")
  @Mapping(target = "diasVendidos", source = "diasVendidos")
  @Mapping(target = "dtPagamento", source = "dtPagamento")
  @Mapping(target = "valorIndenizacao", source = "valorIndenizacao")
  @Mapping(target = "modalidade", source = "modalidade")
  @Mapping(target = "mesAnoFolha", source = "mesAnoFolha")
  @Mapping(target = "numFolha", source = "numFolha")
  @Mapping(target = "customFields", source = "customFields")
  @Mapping(target = "periodoAquisitivoLicencaId", source = "periodoAquisitivoLicencaId")
  @Mapping(target = "chavePeraqlicesp", source = "chavePeraqlicesp")
  public abstract VendaLicencaEspecial newEntity(CreateVendaLicencaEspecialDTO dto);
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "auditoria", ignore = true)
  public abstract VendaLicencaEspecial candidate(UpdateVendaLicencaEspecialDTO dto);
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "auditoria", ignore = true)
  @Mapping(target = "empCodigo", source = "empCodigo")
  @Mapping(target = "numfunc", source = "numfunc")
  @Mapping(target = "numvinc", source = "numvinc")
  @Mapping(target = "descricao", source = "descricao")
  @Mapping(target = "dtini", source = "dtini")
  @Mapping(target = "dtfim", source = "dtfim")
  @Mapping(target = "situacao", source = "situacao")
  @Mapping(target = "documentoLegalId", source = "documentoLegalId")
  @Mapping(target = "atoPublicacaoId", source = "atoPublicacaoId")
  @Mapping(target = "observacoes", source = "observacoes")
  @Mapping(target = "diasVendidos", source = "diasVendidos")
  @Mapping(target = "dtPagamento", source = "dtPagamento")
  @Mapping(target = "valorIndenizacao", source = "valorIndenizacao")
  @Mapping(target = "modalidade", source = "modalidade")
  @Mapping(target = "mesAnoFolha", source = "mesAnoFolha")
  @Mapping(target = "numFolha", source = "numFolha")
  @Mapping(target = "customFields", source = "customFields")
  @Mapping(target = "periodoAquisitivoLicencaId", source = "periodoAquisitivoLicencaId")
  @Mapping(target = "chavePeraqlicesp", source = "chavePeraqlicesp")
  public abstract void applyUpdate(@MappingTarget VendaLicencaEspecial entity, UpdateVendaLicencaEspecialDTO dto);
  public Long extractId(VendaLicencaEspecial entity) { return entity.getId(); }
}
