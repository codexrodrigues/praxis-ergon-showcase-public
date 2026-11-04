-- Dados demonstrativos dos órgãos 1 e 2. As referências vêm do cadastro e dos eventos
-- da própria base. Cenários de substituição usam impedimentos vigentes do titular.
INSERT INTO funcoes_confianca (
    emp_codigo, codigo, descricao, categoria, simbolo, controle_vaga, dt_inicio_contr_vaga,
    permite_substituicao, tipo_substituicao, dias_minimos_substituicao, valor_gratificacao,
    percentual_opcao_efetivo, exige_vinculo_efetivo, dtini, situacao, requisitos)
SELECT e.empresa, v.codigo, v.nome, v.categoria, v.simbolo, TRUE, DATE '2024-01-01',
       v.substituivel, CASE WHEN v.substituivel THEN 'IMPEDIMENTO DO TITULAR' END,
       CASE WHEN v.substituivel THEN 1 END, v.valor, 60.00, TRUE, DATE '2024-01-01', 'ATIVA', v.requisitos
FROM empresas e
CROSS JOIN (VALUES
    ('S7-DIR', 'Direção de Gestão de Pessoas', 'DIRECAO', 'DAS-3', TRUE, 4250.00, 'Experiência em gestão de equipes'),
    ('S7-CHF', 'Chefia de Administração Funcional', 'CHEFIA', 'FG-1', TRUE, 2350.00, 'Experiência com assentamentos funcionais'),
    ('S7-ASS', 'Assessoria de Gestão Institucional', 'ASSESSORAMENTO', 'CC-2', FALSE, 1780.00, 'Atuação em planejamento institucional')
) v(codigo, nome, categoria, simbolo, substituivel, valor, requisitos)
WHERE e.empresa IN (1, 2);

INSERT INTO vagas_funcao (
    emp_codigo, funcao_id, setor_id, numero_vaga, descricao, origem_vaga, dtini, situacao, motivo_bloqueio)
SELECT f.emp_codigo, f.id, unidade.setor, v.numero, v.nome, v.origem, f.dtini, v.situacao, v.motivo
FROM funcoes_confianca f
JOIN (SELECT emp_codigo, MIN(setor) AS setor FROM setores
      WHERE dtini <= CURRENT_DATE AND (dtfim IS NULL OR dtfim >= CURRENT_DATE)
      GROUP BY emp_codigo) unidade ON unidade.emp_codigo = f.emp_codigo
JOIN (VALUES
    ('S7-DIR', 701, 'Direção de Gestão de Pessoas', 'CRIACAO POR LEI', 'LIVRE', NULL),
    ('S7-CHF', 702, 'Chefia de Administração Funcional', 'CRIACAO POR LEI', 'LIVRE', NULL),
    ('S7-ASS', 703, 'Assessoria de Gestão Institucional', 'REDISTRIBUICAO', 'BLOQUEADA', 'Revisão da distribuição de atribuições'),
    ('S7-CHF', 704, 'Chefia de Atendimento Funcional', 'REDISTRIBUICAO', 'LIVRE', NULL),
    ('S7-CHF', 705, 'Chefia de Cadastro e Arquivo', 'CRIACAO POR LEI', 'LIVRE', NULL)
) v(codigo, numero, nome, origem, situacao, motivo) ON v.codigo = f.codigo
WHERE f.emp_codigo IN (1, 2);

-- Seleciona um titular em férias, um em afastamento e, havendo outro titular impedido,
-- uma chefia sem exercício interino. Indicações usam pessoas distintas dos titulares.
INSERT INTO designacoes_funcao (
    emp_codigo, descricao, dtini, situacao, funcao_id, vaga_funcao_id, setor_id, dt_exercicio,
    titular_numfunc, titular_numvinc, substituto1_numfunc, substituto1_numvinc,
    substituto2_numfunc, substituto2_numvinc, opcao_remuneracao, valor_remuneracao)
WITH impedimentos AS (
    SELECT emp_codigo, numfunc, numvinc, dtini, 'FERIAS' AS motivo
    FROM ferias_gozo
    WHERE situacao IN ('HOMOLOGADA', 'EM_GOZO') AND dtini <= CURRENT_DATE
      AND COALESCE(dtfim, dtprevfim) >= CURRENT_DATE
      AND (dt_interrupcao IS NULL OR dt_interrupcao > CURRENT_DATE)
    UNION ALL
    SELECT emp_codigo, numfunc, numvinc, dtini, 'AFASTAMENTO' AS motivo
    FROM afastamentos
    WHERE situacao = 'HOMOLOGADO' AND dtini <= CURRENT_DATE
      AND (dtfim IS NULL OR dtfim >= CURRENT_DATE)
), pessoas_impedidas AS (
    SELECT i.*, ROW_NUMBER() OVER (
        PARTITION BY i.emp_codigo, i.numfunc ORDER BY i.motivo DESC, i.dtini DESC, i.numvinc
    ) AS prioridade
    FROM impedimentos i
    JOIN vinculos v ON v.emp_codigo = i.emp_codigo AND v.numfunc = i.numfunc AND v.numero = i.numvinc
    WHERE i.emp_codigo IN (1, 2) AND v.situacao IN ('ATIVO', 'AFASTADO')
), titulares_impedidos AS (
    SELECT p.*, ROW_NUMBER() OVER (PARTITION BY p.emp_codigo, p.motivo ORDER BY p.numfunc) AS ordem
    FROM pessoas_impedidas p
    WHERE p.prioridade = 1
), indicados AS (
    SELECT v.emp_codigo, v.numfunc, MIN(v.numero) AS numvinc,
           ROW_NUMBER() OVER (PARTITION BY v.emp_codigo ORDER BY v.numfunc) AS ordem
    FROM vinculos v
    WHERE v.emp_codigo IN (1, 2) AND v.situacao = 'ATIVO'
      AND NOT EXISTS (SELECT 1 FROM pessoas_impedidas i WHERE i.emp_codigo = v.emp_codigo AND i.numfunc = v.numfunc)
    GROUP BY v.emp_codigo, v.numfunc
), titulares AS (
    SELECT emp_codigo, numfunc, numvinc, dtini, motivo, ordem FROM titulares_impedidos
    UNION ALL
    SELECT i.emp_codigo, i.numfunc, i.numvinc, CURRENT_DATE, 'EXERCICIO', i.ordem
    FROM indicados i
    WHERE i.ordem = 1
      AND NOT EXISTS (SELECT 1 FROM titulares_impedidos t WHERE t.emp_codigo = i.emp_codigo)
)
SELECT v.emp_codigo, v.descricao, GREATEST(t.dtini, v.dtini), 'EM_EXERCICIO', v.funcao_id, v.id, v.setor_id, GREATEST(t.dtini, v.dtini),
       t.numfunc, t.numvinc, i1.numfunc, i1.numvinc, i2.numfunc, i2.numvinc, 'INTEGRAL', f.valor_gratificacao
FROM vagas_funcao v
JOIN funcoes_confianca f ON f.id = v.funcao_id AND f.emp_codigo = v.emp_codigo
JOIN titulares t ON t.emp_codigo = v.emp_codigo AND v.numero_vaga = CASE
    WHEN t.motivo = 'FERIAS' AND t.ordem = 1 THEN 701
    WHEN t.motivo = 'AFASTAMENTO' AND t.ordem = 1 THEN 702
    WHEN t.motivo = 'AFASTAMENTO' AND t.ordem = 2 THEN 704
    WHEN t.motivo = 'EXERCICIO' THEN 705 END
LEFT JOIN indicados i1 ON i1.emp_codigo = v.emp_codigo AND i1.ordem = CASE WHEN v.numero_vaga IN (702, 705) THEN 2 ELSE 1 END
LEFT JOIN indicados i2 ON i2.emp_codigo = v.emp_codigo AND i2.ordem = CASE v.numero_vaga WHEN 702 THEN 1 WHEN 705 THEN 3 ELSE 2 END
WHERE v.emp_codigo IN (1, 2) AND f.codigo IN ('S7-DIR', 'S7-CHF');

UPDATE vagas_funcao v
SET situacao = 'OCUPADA',
    titular_atual_numfunc = (SELECT d.titular_numfunc FROM designacoes_funcao d WHERE d.vaga_funcao_id = v.id),
    titular_atual_numvinc = (SELECT d.titular_numvinc FROM designacoes_funcao d WHERE d.vaga_funcao_id = v.id)
WHERE EXISTS (SELECT 1 FROM designacoes_funcao d WHERE d.vaga_funcao_id = v.id AND d.emp_codigo = v.emp_codigo);

-- O seed referencia os eventos que a projeção encontrou para o titular na data corrente.
INSERT INTO substituicoes_funcao (
    emp_codigo, descricao, dtini, dtfim, situacao, designacao_id, substituto_numfunc, substituto_numvinc,
    motivo, afastamento_id, ferias_gozo_id)
SELECT d.emp_codigo, 'Exercício interino · ' || v.descricao,
       GREATEST(d.dt_exercicio, CASE WHEN v.numero_vaga = 701 THEN g.dtini ELSE a.dtini END),
       CASE WHEN v.numero_vaga = 701 THEN COALESCE(g.dtfim, g.dtprevfim) ELSE COALESCE(a.dtfim, CURRENT_DATE) END,
       'EM_EXERCICIO', d.id, d.substituto1_numfunc, d.substituto1_numvinc,
       CASE WHEN v.numero_vaga = 701 THEN 'FERIAS' ELSE 'AFASTAMENTO' END,
       CASE WHEN v.numero_vaga = 702 THEN a.id END,
       CASE WHEN v.numero_vaga = 701 THEN g.id END
FROM designacoes_funcao d
JOIN vagas_funcao v ON v.id = d.vaga_funcao_id AND v.emp_codigo = d.emp_codigo
JOIN radar_chefias_substituicoes r ON r.designacao_id = d.id AND r.emp_codigo = d.emp_codigo
LEFT JOIN afastamentos a ON a.id = r.afastamento_id
LEFT JOIN ferias_gozo g ON g.id = r.ferias_gozo_id
WHERE v.emp_codigo IN (1, 2) AND d.substituto1_numfunc IS NOT NULL
  AND ((v.numero_vaga = 701 AND g.id IS NOT NULL) OR (v.numero_vaga = 702 AND a.id IS NOT NULL));
