-- Slice 6 / Fase 2. Migração explícita do cadastro da V5 para os estados operacionais.
-- A V5 permitia ABONO sem uma FK de gozo. Não há associação determinística segura
-- para inventar durante a migração: reconciliação explícita é necessária se houver tais dados.
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM vendas_licenca_especial WHERE modalidade='ABONO') THEN
    RAISE EXCEPTION 'V6: reconcilie os registros ABONO sem origem de gozo antes da migração; associação automática não é segura';
  END IF;
  IF EXISTS (SELECT 1 FROM vendas_licenca_especial
      WHERE situacao<>'CANCELADO' AND dt_pagamento IS NOT NULL
        AND (valor_indenizacao IS NULL OR valor_indenizacao<=0 OR mes_ano_folha IS NULL
             OR num_folha IS NULL OR num_folha<1)) THEN
    RAISE EXCEPTION 'V6: reconcilie a liquidação histórica sem valor positivo ou identificação da folha antes da migração';
  END IF;
  IF EXISTS (SELECT 1 FROM licencas_especiais_gozo WHERE situacao<>'CANCELADO'
      AND dias_gozo<>(COALESCE(dtfim,dtprevfim)-dtini+1)) THEN
    RAISE EXCEPTION 'V6: reconcilie os dias e o intervalo efetivo dos gozos históricos antes da migração';
  END IF;
  IF EXISTS (SELECT 1 FROM periodos_aquisitivos_licenca_especial WHERE situacao<>'CANCELADO'
      AND ((dias_convertidos_dobro>0 AND dt_cnt_dobro IS NULL)
        OR (dias_convertidos_dobro=0 AND dt_cnt_dobro IS NOT NULL))) THEN
    RAISE EXCEPTION 'V6: reconcilie os dias e a data de efeito da conversão em dobro antes da migração';
  END IF;
  IF EXISTS (SELECT 1 FROM periodos_aquisitivos_licenca_especial p
      JOIN averbacoes_tempo_servico a ON a.id=p.averbacao_origem_id
      WHERE p.situacao<>'CANCELADO' AND (a.situacao='CANCELADO' OR a.data_remocao IS NOT NULL
        OR a.finalidade<>'LICENCA_ESPECIAL' OR a.dias_desaverbados=a.total_dias AND a.total_dias>0)) THEN
    RAISE EXCEPTION 'V6: reconcilie o período ativo vinculado a uma origem removida ou incompatível antes da migração';
  END IF;
END $$;
ALTER TABLE averbacoes_tempo_servico DROP CONSTRAINT averbacoes_tempo_servico_situacao_check;
UPDATE averbacoes_tempo_servico a SET situacao=CASE
  WHEN data_remocao IS NOT NULL THEN 'CANCELADO'
  WHEN dias_desaverbados>0 AND dias_desaverbados=total_dias THEN 'DESAVERBADA_TOTAL'
  WHEN dias_desaverbados>0 THEN 'PARCIALMENTE_DESAVERBADA'
  -- O vínculo explícito já materializou direito na V5: não voltar à análise.
  WHEN finalidade='LICENCA_ESPECIAL' AND EXISTS (
    SELECT 1 FROM periodos_aquisitivos_licenca_especial p
    WHERE p.averbacao_origem_id=a.id AND p.situacao<>'CANCELADO') THEN 'HOMOLOGADA'
  ELSE 'EM_ANALISE' END, version=version+1 WHERE situacao='REGISTRADO';
ALTER TABLE averbacoes_tempo_servico ALTER COLUMN situacao SET DEFAULT 'EM_ANALISE';
ALTER TABLE averbacoes_tempo_servico ADD CHECK(situacao IN ('EM_ANALISE','HOMOLOGADA','PARCIALMENTE_DESAVERBADA','DESAVERBADA_TOTAL','CANCELADO'));

ALTER TABLE periodos_aquisitivos_licenca_especial DROP CONSTRAINT periodos_aquisitivos_licenca_especial_situacao_check;
UPDATE periodos_aquisitivos_licenca_especial SET situacao=CASE
  WHEN dias_convertidos_dobro>0 AND dias_convertidos_dobro=dias_direito THEN 'CONVERTIDO_EM_DOBRO'
  WHEN saldo_dias=0 AND dias_gozados+dias_vendidos>0 THEN 'QUITADO'
  WHEN dias_gozados+dias_vendidos>0 THEN 'PARCIALMENTE_GOZADO'
  WHEN dias_convertidos_dobro>0 THEN 'CONVERTIDO_EM_DOBRO'
  ELSE 'EM_AQUISICAO' END, version=version+1 WHERE situacao='REGISTRADO';
ALTER TABLE periodos_aquisitivos_licenca_especial ALTER COLUMN situacao SET DEFAULT 'EM_AQUISICAO';
ALTER TABLE periodos_aquisitivos_licenca_especial ADD CHECK(situacao IN ('EM_AQUISICAO','DISPONIVEL','PARCIALMENTE_GOZADO','CONVERTIDO_EM_DOBRO','QUITADO','SUSPENSO','PRESCRITO','CANCELADO'));

ALTER TABLE licencas_especiais_gozo DROP CONSTRAINT licencas_especiais_gozo_situacao_check;
-- O término efetivo prova encerramento, mas não prova o motivo de eventual antecipação.
UPDATE licencas_especiais_gozo SET situacao=CASE WHEN dtfim IS NOT NULL THEN 'GOZADA' ELSE 'PROGRAMADA' END,
  version=version+1 WHERE situacao='REGISTRADO';
ALTER TABLE licencas_especiais_gozo ALTER COLUMN situacao SET DEFAULT 'PROGRAMADA';
ALTER TABLE licencas_especiais_gozo ADD CHECK(situacao IN ('PROGRAMADA','HOMOLOGADA','EM_GOZO','GOZADA','INTERROMPIDA','CANCELADO'));

ALTER TABLE vendas_licenca_especial DROP CONSTRAINT vendas_licenca_especial_situacao_check;
UPDATE vendas_licenca_especial SET situacao=CASE WHEN situacao='CANCELADO' THEN 'ESTORNADA'
  WHEN dt_pagamento IS NOT NULL THEN 'PAGA' ELSE 'SOLICITADA' END, version=version+1;
ALTER TABLE vendas_licenca_especial ALTER COLUMN situacao SET DEFAULT 'SOLICITADA';
ALTER TABLE vendas_licenca_especial ADD CHECK(situacao IN ('SOLICITADA','AUTORIZADA','PAGA','ESTORNADA'));

ALTER TABLE licencas_especiais_gozo ADD COLUMN numfunc_substituto BIGINT REFERENCES funcionarios(numero);
ALTER TABLE vendas_licenca_especial ADD COLUMN licenca_especial_gozo_id BIGINT REFERENCES licencas_especiais_gozo(id);
ALTER TABLE vendas_licenca_especial ADD COLUMN dt_estorno DATE;
ALTER TABLE vendas_licenca_especial ADD CHECK(dt_estorno IS NULL OR situacao='ESTORNADA');
ALTER TABLE vendas_licenca_especial ADD CHECK(dt_estorno IS NULL OR dt_pagamento IS NULL OR dt_estorno>=dt_pagamento);
-- Estornos conservam o pagamento original. Reemissão cria outra autorização;
-- somente uma pode estar ativa para a mesma parcela de gozo.
CREATE UNIQUE INDEX ux_abono_licenca_ativo ON vendas_licenca_especial(licenca_especial_gozo_id)
  WHERE licenca_especial_gozo_id IS NOT NULL AND situacao<>'ESTORNADA';
ALTER TABLE vendas_licenca_especial DROP CONSTRAINT vendas_licenca_especial_modalidade_check;
ALTER TABLE vendas_licenca_especial ADD CHECK(modalidade IN ('PECUNIA','ABONO_JUNTO_GOZO'));
ALTER TABLE vendas_licenca_especial ADD CHECK((modalidade='PECUNIA' AND licenca_especial_gozo_id IS NULL) OR (modalidade='ABONO_JUNTO_GOZO' AND licenca_especial_gozo_id IS NOT NULL));

-- Um recibo só existe quando o efeito e sua revisão foram confirmados na mesma transação.
-- Chave SHA-256 inclui solicitante, órgão, recurso, item, ação e Idempotency-Key.
CREATE TABLE tempo_licenca_command_receipts (
  receipt_key VARCHAR(64) PRIMARY KEY,
  fingerprint VARCHAR(64) NOT NULL,
  response_body TEXT NOT NULL,
  response_version BIGINT NOT NULL CHECK(response_version>=0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
