package br.com.praxis.ergonshowcase.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

/**
 * Projeção analítica somente-leitura para governança executiva de férias, risco de prescrição,
 * acúmulo de períodos aquisitivos vencidos, queima de saldo e programação futura de gozo
 * ({@code PERAQFER} + {@code VINCULOS} + {@code FUNCIONARIOS} + {@code SETORES_} + {@code CARGOS_}
 * + {@code FERIAS}).
 */
@Entity
@Immutable
@Subselect(
    """
    select
      pa.id as id,
      pa.emp_codigo as emp_codigo,
      o.nome as orgao_nome,
      pa.numfunc as numfunc,
      pa.numvinc as numvinc,
      v.matric as vinculo_matricula,
      p.nome as pessoa_nome,
      v.setor as setor_codigo,
      s.nome as setor_nome,
      v.cargo as cargo_codigo,
      c.nome as cargo_nome,
      v.regimejur as regime_juridico,
      v.tipovinc as tipo_vinculo,
      v.categoria as categoria,
      pa.chave as chave,
      pa.dtini as dtini,
      pa.dtfim as dtfim,
      pa.exercicio as exercicio,
      pa.situacao as situacao,
      pa.tot_dias_direito as tot_dias_direito,
      pa.faltas_apuradas as faltas_apuradas,
      pa.dias_afastamento_suspensivo as dias_afastamento_suspensivo,
      pa.dias_gozados as dias_gozados_total,
      pa.dias_vendidos_abono as dias_abono_total,
      coalesce(fg_prog.dias_programados_futuros, 0) as dias_programados_futuros,
      pa.saldo_dias_disponivel as saldo_dias_disponivel,
      greatest(0, pa.saldo_dias_disponivel - coalesce(fg_prog.dias_programados_futuros, 0)) as saldo_sem_programacao,
      pa.dt_prescricao as dt_prescricao,
      case
        when pa.dt_prescricao is null then 999
        else cast(pa.dt_prescricao - current_date as integer)
      end as dias_ate_prescricao,
      case
        when pa.situacao in ('PRESCRITO', 'CONVERTIDO_DOBRO') or (pa.dt_prescricao is not null and pa.dt_prescricao < current_date and pa.saldo_dias_disponivel > 0) then 'VENCIDO_PRESCRITO'
        when pa.dt_prescricao is not null and pa.saldo_dias_disponivel > 0 and (pa.dt_prescricao - current_date) <= 30 then 'CRITICO_30D'
        when pa.dt_prescricao is not null and pa.saldo_dias_disponivel > 0 and (pa.dt_prescricao - current_date) <= 60 then 'ALERTA_60D'
        when pa.dt_prescricao is not null and pa.saldo_dias_disponivel > 0 and (pa.dt_prescricao - current_date) <= 90 then 'ATENCAO_90D'
        else 'REGULAR'
      end as faixa_urgencia_prescricao,
      case
        when (
          select count(1)
          from periodos_aquisitivos_ferias pa2
          where pa2.emp_codigo = pa.emp_codigo
            and pa2.numfunc = pa.numfunc
            and pa2.numvinc = pa.numvinc
            and pa2.saldo_dias_disponivel > 0
            and pa2.situacao in ('DISPONIVEL', 'PARCIALMENTE_GOZADO')
        ) >= 2 then true
        else false
      end as em_risco_acumulo_periodos,
      case
        when coalesce(fg_prog.qtd_programadas, 0) > 0 then true
        else false
      end as possui_parcela_programada,
      fg_prog.proxima_parcela_inicio as proxima_parcela_inicio,
      (
        select fg2.situacao
        from ferias_gozo fg2
        where fg2.periodo_aquisitivo_id = pa.id
          and fg2.situacao in ('PROGRAMADA', 'HOMOLOGADA', 'EM_GOZO')
        order by fg2.dtini asc, fg2.ordem_parcela asc
        fetch first 1 row only
      ) as proxima_parcela_situacao,
      pa.version as version
    from periodos_aquisitivos_ferias pa
    left join empresas o on o.empresa = pa.emp_codigo
    left join funcionarios p on p.numero = pa.numfunc
    left join vinculos v on v.numfunc = pa.numfunc and v.numero = pa.numvinc
    left join setores s on s.setor = v.setor
    left join cargos c on c.cargo = v.cargo
    left join (
      select
        fg.periodo_aquisitivo_id as periodo_aquisitivo_id,
        sum(
          case
            when fg.situacao in ('PROGRAMADA', 'HOMOLOGADA') then
              coalesce(fg.dias_gozo, 0)
              + (case when fg.abono_pecuniario = true then coalesce(fg.dias_abono_vendidos, 0) else 0 end)
            else 0
          end
        ) as dias_programados_futuros,
        count(case when fg.situacao in ('PROGRAMADA', 'HOMOLOGADA', 'EM_GOZO') then 1 end) as qtd_programadas,
        min(case when fg.situacao in ('PROGRAMADA', 'HOMOLOGADA', 'EM_GOZO') then fg.dtini end) as proxima_parcela_inicio
      from ferias_gozo fg
      group by fg.periodo_aquisitivo_id
    ) fg_prog on fg_prog.periodo_aquisitivo_id = pa.id
    """)
@Synchronize({
  "periodos_aquisitivos_ferias",
  "ferias_gozo",
  "vinculos",
  "funcionarios",
  "setores",
  "cargos",
  "empresas"
})
@Comment(
    "Projeção analítica executiva de saldos de períodos aquisitivos de férias, faixas de urgência de prescrição, acúmulo de ciclos e cobertura de programação futura de gozo.")
@Getter
@Setter
@NoArgsConstructor
public class RadarFeriasPrescricao {
  @Id
  @Column(name = "id")
  private Long id;

  @Column(name = "emp_codigo", nullable = false)
  private Integer empCodigo;

  @Column(name = "orgao_nome", length = 100)
  private String orgaoNome;

  @Column(name = "numfunc", nullable = false)
  private Long numfunc;

  @Column(name = "numvinc", nullable = false)
  private Integer numvinc;

  @Column(name = "vinculo_matricula", length = 20)
  private String vinculoMatricula;

  @org.praxisplatform.uischema.annotation.OptionLabel
  @Column(name = "pessoa_nome", length = 300)
  private String pessoaNome;

  @Column(name = "setor_codigo", length = 30)
  private String setorCodigo;

  @Column(name = "setor_nome", length = 120)
  private String setorNome;

  @Column(name = "cargo_codigo")
  private Integer cargoCodigo;

  @Column(name = "cargo_nome", length = 120)
  private String cargoNome;

  @Column(name = "regime_juridico", length = 20)
  private String regimeJuridico;

  @Column(name = "tipo_vinculo", length = 20)
  private String tipoVinculo;

  @Column(name = "categoria", length = 20)
  private String categoria;

  @Column(name = "chave", nullable = false)
  private Integer chave;

  @Column(name = "dtini", nullable = false)
  private LocalDate dtini;

  @Column(name = "dtfim", nullable = false)
  private LocalDate dtfim;

  @Column(name = "exercicio")
  private Integer exercicio;

  @Column(name = "situacao", nullable = false, length = 30)
  private String situacao;

  @Column(name = "tot_dias_direito", nullable = false)
  private Integer totDiasDireito = 30;

  @Column(name = "faltas_apuradas", nullable = false)
  private Integer faltasApuradas = 0;

  @Column(name = "dias_afastamento_suspensivo", nullable = false)
  private Integer diasAfastamentoSuspensivo = 0;

  @Column(name = "dias_gozados_total", nullable = false)
  private Integer diasGozadosTotal = 0;

  @Column(name = "dias_abono_total", nullable = false)
  private Integer diasAbonoTotal = 0;

  @Column(name = "dias_programados_futuros", nullable = false)
  private Integer diasProgramadosFuturos = 0;

  @Column(name = "saldo_dias_disponivel", nullable = false)
  private Integer saldoDiasDisponivel = 30;

  @Column(name = "saldo_sem_programacao", nullable = false)
  private Integer saldoSemProgramacao = 30;

  @org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn
  @Column(name = "dt_prescricao")
  private LocalDate dtPrescricao;

  @Column(name = "dias_ate_prescricao", nullable = false)
  private Integer diasAtePrescricao = 999;

  @Column(name = "faixa_urgencia_prescricao", nullable = false, length = 30)
  private String faixaUrgenciaPrescricao = "REGULAR";

  @Column(name = "em_risco_acumulo_periodos", nullable = false)
  private Boolean emRiscoAcumuloPeriodos = false;

  @Column(name = "possui_parcela_programada", nullable = false)
  private Boolean possuiParcelaProgramada = false;

  @Column(name = "proxima_parcela_inicio")
  private LocalDate proximaParcelaInicio;

  @Column(name = "proxima_parcela_situacao", length = 30)
  private String proximaParcelaSituacao;

  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "emp_codigo",
      referencedColumnName = "empresa",
      insertable = false,
      updatable = false)
  private Orgao orgao;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "numfunc",
      referencedColumnName = "numero",
      insertable = false,
      updatable = false)
  private ServidorPessoa pessoa;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "numfunc",
        referencedColumnName = "numfunc",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "numvinc",
        referencedColumnName = "numero",
        insertable = false,
        updatable = false)
  })
  private Vinculo vinculo;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "setor_codigo",
      referencedColumnName = "setor",
      insertable = false,
      updatable = false)
  private Setor setor;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "cargo_codigo",
      referencedColumnName = "cargo",
      insertable = false,
      updatable = false)
  private Cargo cargo;
}
