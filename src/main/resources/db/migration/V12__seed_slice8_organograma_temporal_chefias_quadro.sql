-- Massa demonstrativa isolada pelos códigos S8-*; preserva integralmente períodos preexistentes.
-- Nenhum estado de comando ou total de ocupação é persistido no radar.
INSERT INTO subempresas_localidades(emp_codigo,codigo,natureza_unidade,nome,nome_fantasia,
 razao_social,cnpj,cnpj_declarante_esocial,uf,municipio,dtini,ativo)
SELECT empresa,'S8-UG','SUBEMPRESA_FISCAL_ESOCIAL','Unidade gestora demonstrativa '||empresa,
 'UG-'||empresa,'Unidade gestora do órgão '||empresa,NULL,NULL,'SP','São Paulo',DATE '2020-01-01',true
FROM empresas WHERE empresa IN(1,2);
INSERT INTO subempresas_localidades(emp_codigo,codigo,natureza_unidade,nome,tipo_localidade,
 regiao_geografica,uf,municipio,dtini,ativo)
SELECT empresa,'S8-REG','LOCALIDADE_REGIONAL_COMARCA','Regional administrativa '||empresa,
 'REGIONAL','Sede','SP','São Paulo',DATE '2020-01-01',true FROM empresas WHERE empresa IN(1,2);
INSERT INTO grupos_setores(emp_codigo,sigla,nome,tipo_agrupamento,dtini,ativo)
SELECT empresa,'S8-REG','Coordenação regional demonstrativa '||empresa,'REGIONAL_OPERACIONAL',DATE '2020-01-01',true
FROM empresas WHERE empresa IN(1,2);

INSERT INTO historico_setores(emp_codigo,setor,dtini,dtfim,nome,sigla,tipo_setor,caminho_hierarquico,
 subemp_codigo,localidade_codigo,grupo_setor_id,motivo_reestruturacao)
SELECT e.empresa,'S8-'||e.empresa||'-RAIZ',DATE '2020-01-01',NULL,'Secretaria demonstrativa '||e.empresa,
 'S8-SECRETARIA','SECRETARIA','/'||encode(convert_to('S8-'||e.empresa||'-RAIZ','UTF8'),'hex')||'/',
 'S8-UG','S8-REG',g.id,'CRIACAO'
FROM empresas e JOIN grupos_setores g ON g.emp_codigo=e.empresa AND g.sigla='S8-REG' WHERE e.empresa IN(1,2);

-- Mesmo código administrativo passa por três períodos; o futuro não substitui o corrente.
INSERT INTO historico_setores(emp_codigo,setor,dtini,dtfim,nome,sigla,tipo_setor,setor_pai,
 setor_pai_hier1,setor_pai_hier2,setor_pai_hier3,setor_pai_hier4,caminho_hierarquico,nivel_hierarquico,
 subemp_codigo,localidade_codigo,motivo_reestruturacao)
SELECT e.empresa,'S8-'||e.empresa||'-DIR',v.inicio,v.fim,v.nome||' '||e.empresa,'S8-DIR','DIRETORIA',
 'S8-'||e.empresa||'-RAIZ','S8-'||e.empresa||'-RAIZ','S8-'||e.empresa||'-RAIZ',
 'S8-'||e.empresa||'-RAIZ','S8-'||e.empresa||'-RAIZ',
 '/'||encode(convert_to('S8-'||e.empresa||'-RAIZ','UTF8'),'hex')||'/'||encode(convert_to('S8-'||e.empresa||'-DIR','UTF8'),'hex')||'/',2,
 'S8-UG','S8-REG','REESTRUTURACAO'
FROM empresas e CROSS JOIN (VALUES
 (DATE '2020-01-01',DATE '2023-12-31','Diretoria histórica'),
 (DATE '2024-01-01',DATE '2029-12-31','Diretoria atual'),
 (DATE '2030-01-01',NULL::date,'Diretoria futura')) v(inicio,fim,nome)
WHERE e.empresa IN(1,2);

INSERT INTO setores(setor,emp_codigo,nome,sigla,setor_pai,tipo_setor,dtini,dtfim,
 nome_longo,data_criacao_original,caminho_hierarquico,nivel_hierarquico,subemp_codigo,localidade_codigo)
SELECT h.setor,h.emp_codigo,h.nome,h.sigla,h.setor_pai,h.tipo_setor,h.dtini,h.dtfim,
 h.nome_longo,(SELECT min(x.dtini) FROM historico_setores x WHERE x.emp_codigo=h.emp_codigo AND x.setor=h.setor),
 a.caminho,a.nivel,h.subemp_codigo,h.localidade_codigo
FROM empresas e CROSS JOIN LATERAL organograma_em(e.empresa,CURRENT_DATE,0) a
JOIN historico_setores h ON h.id=a.periodo_id
WHERE h.setor LIKE 'S8-%' AND NOT EXISTS(SELECT 1 FROM setores s WHERE s.setor=h.setor);

INSERT INTO setores_ergon(emp_codigo,setor,dtini,tipo_setor,tipo_acesso,centro_custo,grau_risco_ambiental)
SELECT emp_codigo,setor,dtini,tipo_setor,'URBANO','S8-CC-'||emp_codigo,'NAO_AVALIADO'
FROM historico_setores WHERE setor='S8-'||emp_codigo||'-RAIZ';

INSERT INTO cargos_setor(emp_codigo,setor,cargo_id,dtini,abrange_subordinados,tipo_controle_quadro,lotacao_maxima_autorizada)
SELECT e.empresa,'S8-'||e.empresa||'-RAIZ',c.cargo,DATE '2024-01-01',true,'RESERVA_SETOR',20
FROM empresas e JOIN LATERAL (SELECT cargo FROM cargos WHERE emp_codigo=e.empresa ORDER BY cargo LIMIT 1) c ON true
WHERE e.empresa IN(1,2);
INSERT INTO cargos_setor(emp_codigo,setor,cargo_id,dtini,abrange_subordinados,tipo_controle_quadro,quadro_pai_id,lotacao_maxima_autorizada)
SELECT emp_codigo,'S8-'||emp_codigo||'-DIR',cargo_id,dtini,false,'DISTRIBUICAO_SUBARVORE',id,8
FROM cargos_setor WHERE setor='S8-'||emp_codigo||'-RAIZ';

-- Cenários de comando são fatos de origem: o radar deriva os estados sem receber inserts.
-- Pessoas e vínculos exclusivos da demonstração (documentos sintéticos, sem uso operacional).
INSERT INTO funcionarios(numero,nome,dtnasc,cpf)
SELECT 800000+e.empresa*10+n.numero,'Pessoa demonstrativa S8-'||e.empresa||'-'||n.numero,
 DATE '1980-01-01',lpad((800000+e.empresa*10+n.numero)::text,11,'0')
FROM empresas e CROSS JOIN generate_series(1,6) n(numero) WHERE e.empresa IN(1,2);
INSERT INTO vinculos(numfunc,numero,emp_codigo,situacao,regimejur,tipovinc,categoria,dtexerc,setor)
SELECT 800000+e.empresa*10+n.numero,1,e.empresa,'ATIVO','ESTATUTARIO','EFETIVO','SERVIDOR',
 DATE '2024-01-01','S8-'||e.empresa||'-RAIZ'
FROM empresas e CROSS JOIN generate_series(1,6) n(numero) WHERE e.empresa IN(1,2);

INSERT INTO historico_setores(emp_codigo,setor,dtini,nome,sigla,tipo_setor,setor_pai,
 setor_pai_hier1,setor_pai_hier2,setor_pai_hier3,setor_pai_hier4,caminho_hierarquico,nivel_hierarquico)
SELECT e.empresa,'S8-'||e.empresa||'-'||v.codigo,DATE '2024-01-01',v.nome||' '||e.empresa,'S8-'||v.codigo,
 'COORDENADORIA','S8-'||e.empresa||'-RAIZ','S8-'||e.empresa||'-RAIZ','S8-'||e.empresa||'-RAIZ',
 'S8-'||e.empresa||'-RAIZ','S8-'||e.empresa||'-RAIZ',
 '/'||encode(convert_to('S8-'||e.empresa||'-RAIZ','UTF8'),'hex')||'/'||encode(convert_to('S8-'||e.empresa||'-'||v.codigo,'UTF8'),'hex')||'/',2
FROM empresas e CROSS JOIN (VALUES('TIT','Coordenação com titular'),('SUB','Coordenação com exercício interino'),
 ('AUS','Coordenação sem cobertura'),('VAC','Coordenação com chefia vaga'),('CON','Coordenação com atos conflitantes'))v(codigo,nome)
WHERE e.empresa IN(1,2);
INSERT INTO setores(setor,emp_codigo,nome,sigla,tipo_setor,setor_pai,dtini,caminho_hierarquico,nivel_hierarquico)
SELECT setor,emp_codigo,nome,sigla,tipo_setor,setor_pai,dtini,caminho_hierarquico,nivel_hierarquico
FROM historico_setores WHERE sigla IN('S8-TIT','S8-SUB','S8-AUS','S8-VAC','S8-CON');
INSERT INTO funcoes_confianca(emp_codigo,codigo,descricao,categoria,simbolo,controle_vaga,
 permite_substituicao,valor_gratificacao,exige_vinculo_efetivo,dtini,situacao)
SELECT emp_codigo,sigla,nome,'CHEFIA','FG-S8',false,true,2000,true,dtini,'ATIVA'
FROM historico_setores WHERE sigla IN('S8-TIT','S8-SUB','S8-AUS','S8-VAC','S8-CON');
INSERT INTO setores_ergon(emp_codigo,setor,dtini,funcao_chefe_id,tipo_setor,tipo_acesso,centro_custo,grau_risco_ambiental)
SELECT h.emp_codigo,h.setor,h.dtini,f.id,h.tipo_setor,'URBANO','S8-CC-'||h.emp_codigo,'NAO_AVALIADO'
FROM historico_setores h JOIN funcoes_confianca f ON f.emp_codigo=h.emp_codigo AND f.codigo=h.sigla
WHERE h.sigla IN('S8-TIT','S8-SUB','S8-AUS','S8-VAC','S8-CON');
INSERT INTO designacoes_funcao(emp_codigo,descricao,dtini,situacao,funcao_id,setor_id,dt_exercicio,
 titular_numfunc,titular_numvinc,opcao_remuneracao,valor_remuneracao)
SELECT h.emp_codigo,'Designação demonstrativa '||h.sigla,DATE '2026-01-01','EM_EXERCICIO',f.id,h.setor,DATE '2026-01-01',
 800000+h.emp_codigo*10+v.pessoa,1,'INTEGRAL',2000
FROM historico_setores h JOIN funcoes_confianca f ON f.emp_codigo=h.emp_codigo AND f.codigo=h.sigla
JOIN (VALUES('S8-TIT',1),('S8-SUB',2),('S8-AUS',3),('S8-CON',5),('S8-CON',6))v(sigla,pessoa) ON v.sigla=h.sigla;
INSERT INTO afastamentos(id,numfunc,numvinc,emp_codigo,dtini,dtfim,tipofreq,codfreq,situacao)
SELECT 800000+e.empresa*10+n.numero,800000+e.empresa*10+n.numero,1,e.empresa,
 DATE '2026-11-01',DATE '2026-11-30','LICENCA',201,'HOMOLOGADO'
FROM empresas e CROSS JOIN (VALUES(2),(3))n(numero) WHERE e.empresa IN(1,2);
INSERT INTO substituicoes_funcao(emp_codigo,descricao,dtini,dtfim,situacao,designacao_id,
 substituto_numfunc,substituto_numvinc,motivo,afastamento_id)
SELECT d.emp_codigo,'Exercício interino demonstrativo S8',DATE '2026-11-01',DATE '2026-11-30',
 'EM_EXERCICIO',d.id,800000+d.emp_codigo*10+4,1,'AFASTAMENTO',800000+d.emp_codigo*10+2
FROM designacoes_funcao d JOIN funcoes_confianca f ON f.id=d.funcao_id AND f.emp_codigo=d.emp_codigo
WHERE f.codigo='S8-SUB';
