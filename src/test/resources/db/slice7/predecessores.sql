-- Dependências mínimas de V8/V9. Datas relativas mantêm o cenário reproduzível.
CREATE TABLE empresas (empresa INTEGER PRIMARY KEY);
CREATE TABLE setores (setor VARCHAR(30) PRIMARY KEY, emp_codigo INTEGER, nome VARCHAR(120), dtini DATE, dtfim DATE);
CREATE TABLE documentos_legais (id BIGINT PRIMARY KEY);
CREATE TABLE funcionarios (numero BIGINT PRIMARY KEY, nome VARCHAR(120));
CREATE TABLE vinculos (numfunc BIGINT, numero INTEGER, emp_codigo INTEGER, situacao VARCHAR(30),
    dtexerc DATE DEFAULT DATE '2024-01-01', dtvac DATE, dtini_cessao DATE, dtfim_cessao DATE,
    PRIMARY KEY (numfunc, numero));
CREATE TABLE afastamentos (id BIGINT PRIMARY KEY, emp_codigo INTEGER, numfunc BIGINT, numvinc INTEGER,
    dtini DATE, dtfim DATE, situacao VARCHAR(30), dtprevfim DATE);
CREATE TABLE ferias_gozo (id BIGINT PRIMARY KEY, emp_codigo INTEGER, numfunc BIGINT, numvinc INTEGER,
    dtini DATE, dtfim DATE, dtprevfim DATE, dt_interrupcao DATE, situacao VARCHAR(30));

INSERT INTO empresas VALUES (1), (2);
INSERT INTO setores VALUES
    ('GP-1', 1, 'Gestão de Pessoas do órgão 1', DATE '2024-01-01', NULL),
    ('GP-2', 2, 'Gestão de Pessoas do órgão 2', DATE '2024-01-01', NULL);
INSERT INTO funcionarios VALUES (103, 'Titular sem cobertura'), (203, 'Titular sem cobertura');
INSERT INTO vinculos (numfunc, numero, emp_codigo, situacao) VALUES
    (101, 1, 1, 'ATIVO'), (102, 1, 1, 'AFASTADO'), (103, 1, 1, 'AFASTADO'),
    (104, 1, 1, 'ATIVO'), (105, 1, 1, 'ATIVO'),
    (201, 1, 2, 'ATIVO'), (202, 1, 2, 'AFASTADO'), (203, 1, 2, 'AFASTADO'),
    (204, 1, 2, 'ATIVO'), (205, 1, 2, 'ATIVO');
INSERT INTO ferias_gozo VALUES
    (11, 1, 101, 1, CURRENT_DATE - 3, NULL, CURRENT_DATE + 6, NULL, 'EM_GOZO'),
    (12, 2, 201, 1, CURRENT_DATE - 3, NULL, CURRENT_DATE + 6, NULL, 'EM_GOZO');
INSERT INTO afastamentos (id, emp_codigo, numfunc, numvinc, dtini, dtfim, situacao) VALUES
    (21, 1, 102, 1, CURRENT_DATE - 2, CURRENT_DATE + 8, 'HOMOLOGADO'),
    (22, 1, 103, 1, CURRENT_DATE - 1, CURRENT_DATE + 2, 'HOMOLOGADO'),
    (23, 2, 202, 1, CURRENT_DATE - 2, CURRENT_DATE + 8, 'HOMOLOGADO'),
    (24, 2, 203, 1, CURRENT_DATE - 1, CURRENT_DATE + 2, 'HOMOLOGADO'),
    (25, 2, 101, 1, CURRENT_DATE - 1, CURRENT_DATE + 2, 'HOMOLOGADO');
