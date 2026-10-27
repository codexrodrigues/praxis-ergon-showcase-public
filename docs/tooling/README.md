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

O resultado é `STATIC PASS`, jamais aprovação arquitetural automática. O analisador de fonte não substitui o compilador Java, testes HTTP, análise transacional, autorização, carga concorrente ou prova de paridade com o legado. A detecção de consulta dentro do mapper é um sinal estático; o teste Neon mede separadamente as consultas de enriquecimento em lote.
