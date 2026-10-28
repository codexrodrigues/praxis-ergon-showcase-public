# Auditor canônico: revisão e transporte

A fonte executada continua em `D:\Developer\praxis-plataform\scripts\workspace\audit-java-resource-completeness.py`. A raiz do workspace não é um repositório Git. Este PR guarda sua alteração como patch revisável, junto dos dois módulos auxiliares e dos testes, sem instalar um segundo auditor executável no host.

`auditor-canonico-base.txt` é a captura anterior às correções desta rodada. O patch inclui as verificações de Quick View, referências, superfícies e consultas em mapper presentes na versão vigente do workspace, além das correções de contabilidade funcional. Ele preserva essas verificações; não representa autoria exclusiva de todos os trechos novos. Os hashes identificam a base, a versão final e os arquivos auxiliares.

Para reproduzir em um diretório temporário vazio:

1. Crie `scripts/workspace/` e copie `auditor-canonico-base.txt` para `scripts/workspace/audit-java-resource-completeness.py`, preservando UTF-8 e LF.
2. Execute `git apply --check <caminho-absoluto-do-patch>` e `git apply <caminho-absoluto-do-patch>` nesse diretório.
3. Execute `python scripts/workspace/test_audit_java_resource_completeness.py -v`.
4. Execute `python scripts/workspace/audit-java-resource-completeness.py <caminho-absoluto-do-host> --strict`.

Esse transporte foi aplicado e testado em diretório temporário independente. Evidência: [auditor-tests.txt](../evidence/auditor-tests.txt). Em uma instalação já alterada, compare os hashes antes de aplicar; não sobrescreva mudanças concorrentes.

O gate exige 100% de leitura e destino de escrita para as colunas de negócio, mantém relações JPA graváveis no denominador, exclui apenas associações espelhadas somente de leitura e distingue comandos alcançáveis de setters presentes apenas na projeção de resposta. Rejeita projetos vazios, redução do limiar, referências inexistentes e falhas mesmo sem `--strict`. O título de cada recurso precisa de referência própria; relacionamentos para outros recursos publicados precisam de referência com superfícies resolvíveis.

O resultado é `STATIC PASS`, jamais aprovação arquitetural automática. O analisador de fonte não substitui o compilador Java, testes HTTP, análise transacional, autorização, carga concorrente ou prova de paridade com o legado. A detecção de consulta dentro do mapper é um sinal estático; o teste Neon mede o caminho inteiro de página, cursor e exportação em contexto frio.

Nesta rodada, 29 regressões passaram também após o transporte isolado. O auditor relaciona FK escalar a JoinColumn real; exige tipo, controle e ícone explícitos para datas, decimais e FKs; exige fonte nomeada para lookup; detecta projeções relacionais sem referência e duplicação de título/subtítulo no Quick View. Não deduz semântica de nomes ou gera descrições de domínio.

`--require-runtime-evidence` acrescenta os gates schema, http, availability, persistence, parity, angularReadiness, coldQueries, reactiveDeterminations e governedDecisions. O manifesto padrão é docs/evidence/runtime-gates.json; pode ser indicado por --evidence-manifest. Cada pass exige artefatos locais com SHA-256; not-applicable exige justificativa; estados ausentes/pendentes, artefatos alterados e fonte divergente falham. sourceSha256 cobre caminhos relativos e conteúdo UTF-8 normalizado dos arquivos java/yml/yaml/ts/mjs/json em src e scripts/workspace. Documentação não entra no fingerprint. Os hashes provam integridade/frescor do recibo, não a veracidade de uma aprovação declarada pelo próprio autor.

O manifesto atual conserva governedDecisions como awaiting-input. Esse resultado reprova o gate operacional mesmo com seis recursos STATIC PASS. A matriz de paridade explica o insumo necessário e os limites do ensaio Angular. Não há uma segunda cópia executável do auditor neste host; o patch continua sendo o transporte da fonte canônica da raiz.
