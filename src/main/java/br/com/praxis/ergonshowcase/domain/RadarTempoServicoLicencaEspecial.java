package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

/** Uma linha por ciclo; agregações prévias evitam multiplicar dias ao cruzar parcelas e vendas. */
@Entity @Immutable @Getter @Setter
@Subselect("""
    select pa.*,
      coalesce(a.total_averbado_vinculo, 0) as total_averbado_vinculo,
      coalesce(g.dias_programados, 0) as dias_programados,
      coalesce(v.dias_vendidos_registrados, 0) as dias_vendidos_registrados,
      cast(pa.dt_prescricao - current_date as integer) as dias_ate_prescricao
    from periodos_aquisitivos_licenca_especial pa
    left join (
      select emp_codigo, numfunc, numvinc, sum(total_dias - dias_desaverbados) as total_averbado_vinculo
      from averbacoes_tempo_servico
      where situacao in ('HOMOLOGADA','PARCIALMENTE_DESAVERBADA') and data_remocao is null
        and finalidade = 'LICENCA_ESPECIAL'
      group by emp_codigo, numfunc, numvinc
    ) a on a.emp_codigo = pa.emp_codigo and a.numfunc = pa.numfunc and a.numvinc = pa.numvinc
    left join (
      select periodo_aquisitivo_licenca_id, sum(dias_gozo) as dias_programados
      from licencas_especiais_gozo where situacao in ('PROGRAMADA','HOMOLOGADA') and dtini >= current_date
      group by periodo_aquisitivo_licenca_id
    ) g on g.periodo_aquisitivo_licenca_id = pa.id
    left join (
      select periodo_aquisitivo_licenca_id, sum(dias_vendidos) as dias_vendidos_registrados
      from vendas_licenca_especial where situacao in ('SOLICITADA','AUTORIZADA','PAGA') and licenca_especial_gozo_id is null
      group by periodo_aquisitivo_licenca_id
    ) v on v.periodo_aquisitivo_licenca_id = pa.id
    """)
@Synchronize({"periodos_aquisitivos_licenca_especial", "averbacoes_tempo_servico", "licencas_especiais_gozo", "vendas_licenca_especial"})
public class RadarTempoServicoLicencaEspecial extends AssentamentoTempoLicenca {
  @Column(name = "dias_direito") private Integer diasDireito;
  @Column(name = "saldo_dias") private Integer saldoDias;
  @Column(name = "dias_convertidos_dobro") private Integer diasConvertidosDobro;
  @Column(name = "dt_prescricao") private LocalDate dtPrescricao;
  @Column(name = "total_averbado_vinculo") private Long totalAverbadoVinculo;
  @Column(name = "dias_programados") private Long diasProgramados;
  @Column(name = "dias_vendidos_registrados") private Long diasVendidosRegistrados;
  @Column(name = "dias_ate_prescricao") private Integer diasAtePrescricao;
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial")
  @JdbcTypeCode(SqlTypes.JSON) @Column(name = "custom_fields") private Map<String, Object> customFields;
}
