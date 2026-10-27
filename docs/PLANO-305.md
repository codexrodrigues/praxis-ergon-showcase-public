# Issue 305: plano e impacto
Classificação: arquitetural, com contrato público novo. Bootstrap independente em Java 21 / Spring Boot 3.5.15; starter local 8.0.0-rc.179. Nenhum código do quickstart consultado.
1. Mapear todas as 16 tabelas/392 colunas documentadas no Neon, mantendo IDs e restrições.
2. Publicar orgaos (empresas), cargos, servidor-pessoas (funcionarios), vinculos; preservar resourceKeys administracao-pessoal.* usados pelo catálogo existente.
3. Implementar DTOs deliberados, lookup, campos dinâmicos, fatos, superfícies, comandos e concorrência.
4. Executar clean test-compile, test e package com JDK 21 e Neon isolado, registrar fricções.
Dono canônico de contratos: praxis-metadata-starter. Dono de regras/persistência: este host.
Consumidores: futuro frontend do showcase; nenhum consumidor existente migrado.
Artefatos derivados: README, matriz de recursos, relatório e provas HTTP deste host. Landing, corpus global e Angular não publicam estes novos recursos; não há atualização prevista neles.
Risco: sem breaking change de plataforma; paths novos não substituem endpoints existentes.
Banco: holy-mouse-40140108 / neondb / PostgreSQL 17; branch de prova br-super-sound-b7urizmd, filha de br-bitter-bonus-b7i0vfko.
Fontes: handoff praxis-ergon-showcase-database-handoff.md na raiz (também lida a cópia anterior em docs/architecture); pg_catalog (392 comentários); docs-legado/v7x/aps/VINCULOS.TAB, CARGOS_.TAB, FUNCIONARIOS.TAB.
