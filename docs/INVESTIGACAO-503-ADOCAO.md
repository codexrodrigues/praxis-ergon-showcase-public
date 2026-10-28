# Investigação do 503 e revisão de adoção

Rodada de 28/10/2026, posterior a `ac6dd29`. Plano e mapa de impacto em
[PLANO-503-ADOCAO.md](PLANO-503-ADOCAO.md). Sem merge, release ou mudança de schema.

## Correção comprovada e limite

O guardião de publicação conservava a causa da exceção, mas o HTTP sanitizado e
os logs da campanha anterior não permitiam identificar a causa específica do 503.
O commit SDK `039c9838c92d3b6bffe9383f4d4ab90b27473048` acrescenta observação
privada limitada aos tipos canônicos e SQLSTATE válido. Mensagens, SQL, payload,
URL e stack trace não são enviados ao logger; ciclos e falhas de logging não
substituem a recusa original. O HTTP e a causa privada permanecem compatíveis.

Passaram 26 testes focais do SDK, incluindo publicação/preparação e três novas
regressões de diagnóstico. A primeira execução detectou uma alteração indevida
da mensagem privada; ela foi revertida antes da execução verde. Não houve
relaxamento de assertions, deadlines, grants ou revalidação LIVE.

O candidato de diagnóstico é `8.0.0-bulk.308-consolidation.2-SNAPSHOT`, instalado apenas
no cache Maven privado. Sua origem está em
[503-sdk-diagnostics-provenance.json](evidence/503-sdk-diagnostics-provenance.json). A correção de
observabilidade não deve ser apresentada como correção comprovada da
indisponibilidade. A campanha anterior de 73/74 continua sendo evidência histórica
válida de falha; uma repetição verde não a apaga.

Foi identificado separadamente um defeito de composição: a identidade estrita de
uma operação bulk atravessava grupos OpenAPI por leituras protegidas isoladamente,
sem um fence da composição inteira. Duas regressões reproduziram o problema no
código anterior (2 falhas, 0 erros), inclusive aceitação sem verificar uma
invalidação final. O primeiro ensaio dessas fixtures teve erro de preparação do
mock, corrigido antes de registrar a reprodução válida.

O commit `21ca36df724eeef101101460601c052d2cd53931` reutiliza o escopo canônico
`withSchemaCacheReadLock` para a composição completa. Preserva a fotografia
imutável e autoridade LIVE na entrada e saída; reduz verificações repetidas dentro
da mesma leitura sem cachear autorização ou envolver escrita de domínio. Passaram
77 testes de binding, resolução e publicação, incluindo as duas regressões. Os 26
testes anteriores estão parcialmente contidos nessa bateria e não devem ser somados.
O candidato resultante é `.3-SNAPSHOT`; ver
[proveniência final do SDK](evidence/503-sdk-provenance.json). Não se atribui
retrospectivamente a esse defeito a causa específica do 503 anterior.

O `swagger-config-bulk.json` do host declara nove grupos (seis recursos,
`application`, `ergon` e `praxis-metadata-infra`). Fora de um escopo já aberto,
a composição estrita podia abrir nove leituras guardadas independentes, cada
uma com entrada/saída. A correção compartilha um único escopo com duas checagens
LIVE para essa composição. Essa contagem é derivada do fluxo de código e do
inventário de grupos; não é medição de latência nem redução dos fences próprios
das transações posteriores de domínio.

## Adoção avaliada

[Decisão por recurso e fontes de domínio](DUPLICATE-DRAFT-ADOCAO.md): cinco
não adoções justificadas (órgãos, pessoas, vínculos, setores e documentos legais)
e Cargo aplicável, pendente de integração do contrato neutro e provas de adoção.
O inventário canônico guarda autoria, justificativa e hash do documento. Isso não
equivale a aprovação humana ou revisão independente de todo o PR.

Foram acrescentados seis testes negativos sobre origens existentes: GET funciona,
POST duplicate-draft não tem mapping e o hash dos registros da tabela permanece
idêntico. Esses testes verificam ausência de adoção acidental, não certificam a
futura implementação de Cargo nem a experiência visual Angular.

## Auditor

A execução inicial encontrou 11 falhas em 120 testes do estado então visível.
A resolução textual de `getService` aceitava métodos de tipos internos e podia
associar a entidade errada. Durante a inspeção o arquivo foi atualizado para usar
`TypeGraph.method_return`/`direct_method_return` e o binding efetivo em todas as
consultas; o patch preparado nesta frente não foi aplicado sobre a atualização.
No estado seguinte, os 74 testes descobertos passaram; a revisão posterior
do auditor acrescentou um caso e os 75 testes atuais também passaram. Não somar contagens de
estados diferentes nem atribuir a esta frente a autoria daquela substituição.
Os hashes e o log da execução estão em
[503-auditor-regression.json](evidence/503-auditor-regression.json).

## Provas Neon

A cópia `br-spring-butterfly-b79ht1rc`, derivada da original
`br-super-sound-b7urizmd`, utiliza endpoint direto, 1 CU fixo e autosuspend
desativado. A reprodução focal conserva o candidato anterior; a campanha seguinte
consome o candidato de diagnóstico. JFR e logs integrais ficam privados em
`.local/`; apenas evidências sanitizadas são publicadas. Não se repetem escritas
automaticamente após 503. A limpeza da cópia foi autorizada pelo mantenedor.

Na reprodução focal, o candidato anterior `.1-SNAPSHOT` passou nos seis testes
de lote e não produziu exceção de publicação, timeout ou PostgreSQL na captura
JFR selecionada. A instrumentação emitiu aviso de canal nativo do Surefire, mas
Maven encerrou com sucesso. É uma execução focal instrumentada, sem garantia de
disponibilidade: [recibo](evidence/503-baseline-test-results.json).

A campanha de diagnóstico (.2) passou 80/80 antes da correção de composição.
A campanha final (.3) também passou **80/80, zero falhas/erros/ignorados**, em uma
única execução Maven: lote 6, domínio 64 (incluindo seis novos casos), pilares 6
e metadados 4. As campanhas permanecem separadas nos recibos
[diagnóstico](evidence/503-diagnostic-full-test-results.json) e
[final](evidence/503-final-test-results.json). Não se somam casos de candidatos
diferentes para produzir uma aprovação.

O [consumidor HTTP](evidence/503-angular-http-results.json) passou com seis schemas,
seis execuções reativas, seis contratos, fluxo de lote, cancelamento, CLEAR,
resultado parcial e submit parcial. A reconciliação de publicação foi explícita.
A biblioteca oficial permaneceu no commit 421012f90 da frente delegada, sem edição
por esta frente. A prova é headless/HTTP, não aceite visual ou instalação npm.

A [comparação final](evidence/503-neon-preservation.json) confirmou 16 tabelas,
392 colunas, constraints, índices, comentários e hashes dos dados iguais à
original. A [limpeza](evidence/503-neon-cleanup.json) confirma exclusão da cópia
e preservação da original. O servidor auxiliar foi encerrado. A autorização
permanente de limpeza está registrada no plano e na memória solicitada.

Os gates operacionais passaram com hashes atuais. O auditor global retorna
**FAILED: uma pendência**, adoção de duplicate-draft em Cargo. Cinco decisões
N/A têm justificativa atribuída. Não há aprovação de merge, publicação ou
certificação de SLO; a causa específica do 503 histórico continua indeterminada.
A auditoria é evidência estática + integridade de recibos, não revisão humana.

## Fricção de empacotamento

O primeiro `package -DskipTests` incremental preservou o boot JAR anterior (.1),
mesmo após os testes usarem .3. A verificação SHA-256 do JAR interno detectou o
desvio; o servidor foi encerrado antes da prova HTTP. Os quatro XMLs do Surefire
confirmam .3 no classpath dos 80 casos. Após arquivar os recibos, `clean package
-DskipTests` produziu o JAR com o hash esperado de .3. O README passa a exigir
essa reconstrução ao trocar o candidato. Não se confunde sucesso de Maven com
comprovação dos bytes empacotados; ver `503-packaged-provenance.json`.
