package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.repository.*;
import java.util.OptionalLong;
import org.praxisplatform.uischema.service.base.AbstractReadOnlyResourceService;
import org.springframework.stereotype.Service;

/** Fronteira estritamente de leitura para referências e inspeção contextual. */
@Service
public class DocumentoLegalService
    extends AbstractReadOnlyResourceService<
        DocumentoLegal, DocumentoLegalResponseDTO, Long, DocumentoLegalFilterDTO> {
  private final DocumentoLegalMapper mapper;

  public DocumentoLegalService(DocumentoLegalRepository repository, DocumentoLegalMapper mapper) {
    super(repository, DocumentoLegal.class);
    this.mapper = mapper;
  }

  @Override
  protected DocumentoLegalMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected OptionalLong getEntityResourceVersion(DocumentoLegal entity) {
    return OptionalLong.of(entity.getVersion());
  }
}
