package br.com.praxis.ergonshowcase.bulk;

import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.lang.reflect.Type;
import org.praxisplatform.uischema.bulk.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;

/** Preserva tokens originais do protocolo antes de qualquer coerção Jackson do host. */
public final class ErgonBulkMessageConverter extends AbstractGenericHttpMessageConverter<Object> {
  private final ObjectMapper mapper;
  private final java.util.List<org.praxisplatform.uischema.filter.web.FilterPayloadNormalizer> normalizers;
  public ErgonBulkMessageConverter(ObjectMapper mapper, java.util.List<org.praxisplatform.uischema.filter.web.FilterPayloadNormalizer> normalizers) {
    super(MediaType.APPLICATION_JSON); this.mapper = mapper; this.normalizers=java.util.List.copyOf(normalizers);
  }
  private boolean protocol(Class<?> type) {
    return type == BulkUniformEvaluationRequest.class || type == BulkItemEvaluationRequest.class
        || type == BulkCommandEvaluationRequest.class || type == BulkConfirmationRequest.class;
  }
  @Override protected boolean supports(Class<?> type) { return protocol(type); }
  @Override public boolean canRead(Type type, Class<?> context, MediaType media) {
    return protocol(mapper.constructType(type).getRawClass()) && (media == null || MediaType.APPLICATION_JSON.isCompatibleWith(media));
  }
  @Override public boolean canWrite(Type type, Class<?> raw, MediaType media) { return false; }
  @Override public Object read(Type type, Class<?> context, HttpInputMessage message) throws IOException {
    JavaType body = mapper.constructType(type);
    byte[] raw = message.getBody().readNBytes(BulkProtocolLimits.defaults().maxRequestBytes() + 1);
    try {
      int identityIndex = body.hasRawClass(BulkCommandEvaluationRequest.class) ? 1 : 0;
      JavaType wire = body.containedType(identityIndex);
      boolean person = context == br.com.praxis.ergonshowcase.controller.ServidorPessoaController.class
          || body.hasRawClass(BulkUniformEvaluationRequest.class) && body.containedType(1).hasRawClass(br.com.praxis.ergonshowcase.dto.ServidorPessoaFilterDTO.class);
      BulkIdentityCodec codec = wire != null && wire.hasRawClass(Integer.class) ? BulkIdentityCodecs.integers() : person ? BulkIdentityCodecs.longs() : BulkIdentityCodecs.strings();
      BulkProtocolReader reader = new BulkProtocolReader(codec);
      if (body.hasRawClass(BulkConfirmationRequest.class)) return reader.readConfirmation(raw);
      if (body.hasRawClass(BulkItemEvaluationRequest.class)) return reader.readItems(raw);
      if (body.hasRawClass(BulkUniformEvaluationRequest.class))
        return reader.readUniform(raw, node -> bind((JsonNode) node, body.containedType(1)));
      return reader.readCommand(raw, node -> bind((JsonNode) node, body.containedType(0)), node -> bind((JsonNode) node, body.containedType(2)));
    } catch (RuntimeException failure) {
      throw new HttpMessageNotReadableException("Documento de operação em lote inválido.", message);
    }
  }
  private Object bind(JsonNode node, JavaType type) {
    if(type!=null && org.praxisplatform.uischema.filter.dto.GenericFilterDTO.class.isAssignableFrom(type.getRawClass())
        && node instanceof com.fasterxml.jackson.databind.node.ObjectNode object)
      for(var normalizer:normalizers) normalizer.normalizeInPlace(object,type.getRawClass());
    return mapper.convertValue(node, type == null ? mapper.constructType(JsonNode.class) : type);
  }
  @Override protected Object readInternal(Class<?> type, HttpInputMessage message) throws IOException { return read(type, null, message); }
  @Override protected void writeInternal(Object value, Type type, HttpOutputMessage message) { throw new UnsupportedOperationException(); }
}
