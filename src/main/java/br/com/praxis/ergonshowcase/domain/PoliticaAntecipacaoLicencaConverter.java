package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Preserva a coluna booleana anulável: NULL herda, TRUE permite, FALSE veda. */
@Converter
public class PoliticaAntecipacaoLicencaConverter
    implements AttributeConverter<PoliticaAntecipacaoLicenca, Boolean> {
  @Override
  public Boolean convertToDatabaseColumn(PoliticaAntecipacaoLicenca value) {
    if (value == null || value == PoliticaAntecipacaoLicenca.HERDAR) return null;
    return value == PoliticaAntecipacaoLicenca.PERMITIR;
  }

  @Override
  public PoliticaAntecipacaoLicenca convertToEntityAttribute(Boolean value) {
    return value == null ? PoliticaAntecipacaoLicenca.HERDAR
        : value ? PoliticaAntecipacaoLicenca.PERMITIR : PoliticaAntecipacaoLicenca.VEDAR;
  }
}
