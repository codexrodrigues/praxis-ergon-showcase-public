package br.com.praxis.ergonshowcase.bulk;

/** Diagnóstico público deliberado, emitido somente após carregar um alvo autorizado. */
final class BulkInputViolation extends IllegalArgumentException {
  final String code;
  BulkInputViolation(String code) { super(code); this.code=code; }
}
