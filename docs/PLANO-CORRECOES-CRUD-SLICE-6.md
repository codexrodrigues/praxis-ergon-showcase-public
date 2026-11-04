# Correções R9–R15 do CRUD Slice 6

01/11/2026. Classe: `contrato-publico`. Dono das regras: domínio do
`praxis-ergon-showcase`; vocabulário/schema e ETag permanecem no starter rc.186.

1. R9–R11: resolver parâmetros efetivos de licença especial e aplicar permissão,
   proporção de abono e teto acumulado comum antes de persistir.
2. R12–R14: separar estorno de concessão, impedir cancelamento novo do pai com
   filhos ativos, validar novas origens e contar o intervalo efetivo quando fechado.
3. R15: remover diasGozados/diasVendidos/saldoDias de create/update do período,
   calculá-los no servidor e mantê-los no response com metadados read-only.
4. Converter C1–C8 em regressões positivas/negativas, executar testes focais,
   suíte sem Neon e auditor estrito; documentar resultados e limites.

Impacto: serviços, resolução de política, DTOs/mappers e testes do host; README,
guia Slice 6 e evidências. Consumidores metadata-driven/IA devem deixar de enviar
os três totais derivados. Isso é correção limpa de contrato beta, sem aliases.
Não há mudança de caminho, resourceKey, banco, starter ou biblioteca Angular.
Não foi identificado exemplo público/corpus HTTP específico destes recursos a
sincronizar; a documentação operacional permanece no showcase.

Fontes: PERAQLICESP e LIC_ESP; `fontes_ergon/pck_erg_venda_licesp.pkb:174–274`
(data de referência, overrides, venda, teto que inclui abono) e
`fontes_ergon/pck_lic_esp.pkb:213–325` (matriz, fallback, antecipação, bases D/G).
Não se replica o efeito de liquidação dos packages. Reserva usa previsão enquanto
dtfim está vazio; fechamento usa dtfim e mantém a previsão original.

Preservação: hashes e status anteriores em `.local/slice6-seven-fixes/`.
Nenhum reset/clean/stash, alteração de banco, release ou modificação do auditor.
