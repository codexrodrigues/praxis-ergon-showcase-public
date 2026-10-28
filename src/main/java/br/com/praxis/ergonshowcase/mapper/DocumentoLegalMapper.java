package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.DocumentoLegal;
import br.com.praxis.ergonshowcase.dto.DocumentoLegalResponseDTO;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;

/** Projeção de leitura sem acesso ao banco dentro do mapper. */
@Mapper(componentModel = "spring")
public interface DocumentoLegalMapper
    extends ResourceMapper<DocumentoLegal, DocumentoLegalResponseDTO, Void, Void, Long> {
  @Mapping(target = "id", source = "id")
  DocumentoLegalResponseDTO toResponse(DocumentoLegal entity);

  default Long extractId(DocumentoLegal entity) {
    return entity.getId();
  }

  default DocumentoLegal newEntity(Void unused) {
    throw new UnsupportedOperationException("Cadastro somente de leitura.");
  }

  default void applyUpdate(DocumentoLegal entity, Void unused) {
    throw new UnsupportedOperationException("Cadastro somente de leitura.");
  }
}
