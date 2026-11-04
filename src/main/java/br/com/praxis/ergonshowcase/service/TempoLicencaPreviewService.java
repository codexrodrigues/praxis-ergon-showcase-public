package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.PeriodoAquisitivoLicencaEspecial;
import br.com.praxis.ergonshowcase.domain.VendaLicencaEspecial;
import br.com.praxis.ergonshowcase.dto.TempoLicencaPrevia;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.OperationalRuleSet;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.persistence.*;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Prévia de criação/edição; a escrita revalida o direito, o estado e a política sob os locks. */
@Service
@Transactional(readOnly=true)
public class TempoLicencaPreviewService {
  @PersistenceContext private EntityManager em;
  private final TenantContext tenant;
  private final ErgonRuleEngineFacade rules;

  public TempoLicencaPreviewService(TenantContext tenant) {
    this(tenant, new ErgonRuleEngineFacade());
  }

  @org.springframework.beans.factory.annotation.Autowired
  public TempoLicencaPreviewService(TenantContext tenant, ErgonRuleEngineFacade rules) {
    this.tenant = tenant;
    this.rules = rules;
  }
  public TempoLicencaPrevia.Resultado averbacao(TempoLicencaPrevia.Averbacao d) {
    rules.enforceOperational(OperationalRuleSet.PREVIA_AVERBACAO, tenant.orgao(), d.totalDias(), d.diasDesaverbados());
    return new TempoLicencaPrevia.Resultado(d.totalDias()-d.diasDesaverbados());
  }
  public TempoLicencaPrevia.Resultado periodo(TempoLicencaPrevia.Periodo d) {
    rules.enforceOperational(OperationalRuleSet.PREVIA_CONVERSAO, tenant.orgao(), d.diasDireito(), d.diasConvertidosDobro());
    return new TempoLicencaPrevia.Resultado(d.diasDireito()-d.diasConvertidosDobro());
  }
  public TempoLicencaPrevia.Resultado gozo(TempoLicencaPrevia.Gozo d) {
    rules.enforceOperational(OperationalRuleSet.PREVIA_GOZO_INTERVALO, tenant.orgao(), d.dtini(), d.dtprevfim());
    long dias=ChronoUnit.DAYS.between(d.dtini(),d.dtprevfim())+1;
    rules.enforceOperational(OperationalRuleSet.PREVIA_GOZO_LIMITE, tenant.orgao(), dias);
    return new TempoLicencaPrevia.Resultado(Math.toIntExact(dias));
  }
  public TempoLicencaPrevia.Resultado venda(TempoLicencaPrevia.Venda d) {
    var pa=em.find(PeriodoAquisitivoLicencaEspecial.class,d.periodoAquisitivoLicencaId());
    if(pa==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Período aquisitivo inexistente.");
    TempoLicencaValidation.orgao(tenant,pa.getEmpCodigo());
    rules.enforceOperational(OperationalRuleSet.PREVIA_VENDA_SALDO_APURADO, tenant.orgao(), pa.getSaldoDias());
    int available = pa.getSaldoDias();
    if (d.vendaId() != null) {
      var current = em.find(VendaLicencaEspecial.class, d.vendaId());
      if (current == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Venda não encontrada.");
      TempoLicencaValidation.orgao(tenant, current.getEmpCodigo());
      rules.enforceOperational(OperationalRuleSet.LICENCA_PERIODO_EDICAO, tenant.orgao(), current.getPeriodoAquisitivoLicencaId(), d.periodoAquisitivoLicencaId());
      rules.enforceOperational(OperationalRuleSet.VENDA_ABONO_ORIGEM, tenant.orgao(), current.getLicencaEspecialGozoId(), "previa", null, null, null, null, null, null, null, null);
      // Mesma população de consumo da escrita: canceladas/estornadas já liberaram seu débito.
      if (!java.util.Set.of("CANCELADO", "ESTORNADA").contains(current.getSituacao())) {
        rules.enforceOperational(OperationalRuleSet.PREVIA_VENDA_CONSUMO, tenant.orgao(), current.getDiasVendidos());
        available = Math.addExact(available, current.getDiasVendidos());
      }
    }
    rules.enforceOperational(OperationalRuleSet.PREVIA_VENDA_LIMITE, tenant.orgao(), d.diasVendidos(), available);
    return new TempoLicencaPrevia.Resultado(available-d.diasVendidos());
  }
}
