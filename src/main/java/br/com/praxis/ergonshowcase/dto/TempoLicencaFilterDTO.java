package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.domain.VinculoId;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

/** Contrato de contexto; o rc.186 exige os predicados nas classes concretas. */
public abstract class TempoLicencaFilterDTO implements GenericFilterDTO {
  public abstract Integer getEmpCodigo();
  public abstract void setEmpCodigo(Integer empCodigo);
  public abstract Long getNumfunc();
  public abstract void setNumfunc(Long numfunc);
  public abstract Integer getNumvinc();
  public abstract void setNumvinc(Integer numvinc);

  public VinculoId getVinculoId() {
    return getNumfunc() != null && getNumvinc() != null
        ? new VinculoId(getNumfunc(), getNumvinc())
        : null;
  }

  public void setVinculoId(VinculoId vinculoId) {
    if (vinculoId == null) {
      setNumfunc(null);
      setNumvinc(null);
    } else {
      setNumfunc(vinculoId.numfunc());
      setNumvinc(vinculoId.numero());
    }
  }
}
