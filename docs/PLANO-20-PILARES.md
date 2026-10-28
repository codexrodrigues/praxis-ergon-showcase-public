# Fechamento dos quatro gates — Issue 2

Classificação: contrato-publico. Fonte canônica: annotations, options, analytics e validadores do Metadata Starter. Consumidor: runtime Angular oficial. Sem DDL, sem migração de identidade ou aliases de surfaces.

1. Dados: lookups com seleção histórica, diretório, ordenação e dependência de órgão; microvisualizações de ocupação, jornada e presença de canais de contato. Preservar todas as colunas e a governança pessoal.
2. Operações: projeções analíticas em métodos HTTP reais, pareadas com o registry e com navegação apenas de buckets que realmente identificam registros. Apresentação tipada de exportação e acesso autenticado ao cockpit.
3. Gates: JUnit nativo não vacuamente verde, testes de cálculo e publicação, consultas Neon, auditor estático e manifesto operacional separado. Atualizar relatório, scorecard e PR.

Risco: enriquecimento aditivo de schemas; ordenação e seleção dos lookups passam a ter políticas explícitas. Recursos sem estado de ciclo de vida não recebem estados inventados. Fórmulas JPA somente de leitura não criam colunas.

Fontes de domínio: `docs-legado/v7x/aps/cargos_.tab` (controle de vagas), `funcionarios.tab` (contatos e recadastramento), `VINCULOS.TAB` e mapeamentos existentes (jornada e situação). Presença de canais não certifica validade do contato; jornada não é medida de produtividade nem adota uma jornada legal universal.

Artefatos derivados: relatório, scorecard, manifesto de evidências e documentação do host. Não há exemplo público, landing ou corpus HTTP desta nova versão a sincronizar. O aceite de Bulking/Angular anteriormente pendente não é substituído pelo PASS estático.

## Fechamento após revisão de 670dff7

1. Reinstalar o candidato consolidado do starter, preservando as alterações de pureza e `Filterable.relations` recebidas; registrar hash do JAR e diff da fonte, pois a consolidação ainda está na árvore local.
2. Aquecer explicitamente os catálogos PostgreSQL no harness antes da publicação; manter a janela de atestação do kernel. Executar discovery fora de transações de fixture e limpar os testes com commits em finally.
3. Adicionar busca ampla em cargos, pessoas e vínculos com OR canônico. Usar `pessoa`, `nomeSocial` e `eMail`, conforme as entidades reais, sem aliases para os nomes ilustrativos. Cobrir nome/CPF/filiação, navegação relacional e a revisão de todos os DTOs, inclusive bases abstratas.
4. Executar a suíte integral no Neon descartável, conferir o consumo Angular oficial, atualizar recibos/hashes e executar o auditor operacional. Nenhum resultado histórico é promovido automaticamente para esta fonte.

Impacto aditivo: filtros públicos e seleção QUERY de lote usam a mesma especificação canônica. Consumidores: formulários de filtro, serviço de consulta e captura do lote; documentação derivada restrita ao pacote de provas do host. Nenhuma alteração no schema físico ou release público. Fontes legadas consultadas: `v7x/aps/funcionarios.tab` (identificação e filiação), `cargos_.tab` (categoria e escolaridade mínima), `VINCULOS.TAB` (matrícula, lotação e observações).
