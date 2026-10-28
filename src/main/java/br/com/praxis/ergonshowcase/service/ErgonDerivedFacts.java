package br.com.praxis.ergonshowcase.service;

/** Fatos determinísticos compartilhados pelo preview e pela projeção definitiva. */
public final class ErgonDerivedFacts {
  private ErgonDerivedFacts() {}

  public static boolean possuiDeficiencia(
      Boolean fisica, Boolean visual, Boolean auditiva, Boolean mental, Boolean intelectual) {
    return Boolean.TRUE.equals(fisica)
        || Boolean.TRUE.equals(visual)
        || Boolean.TRUE.equals(auditiva)
        || Boolean.TRUE.equals(mental)
        || Boolean.TRUE.equals(intelectual);
  }
}
