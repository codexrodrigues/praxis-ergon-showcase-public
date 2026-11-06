# Candidato de sessão corporativa — 2026-11-06

Escopo: login, contexto confirmado, isolamento Config pessoal/comum e cookie
cross-site entre os domínios existentes Firebase/Render. O candidato parte do
commit remoto `1d027b745c29d158cd6b1fca89d3c249e239564d`; não incorpora a migração
ontológica nem as mudanças concorrentes da árvore de trabalho original.

## Dependências e linha de versão

Os três JARs que o serviço existente usa diferem dos artefatos de mesma versão
no Maven Central. Os artefatos públicos não compilam esse host. Este candidato
preserva seus bytes e os identifica por coordenadas privadas imutáveis,
nas mesmas linhas de pré-lançamento, com sufixo `-ergon.1d027b745c29`.
Não publica SDK, não cria major e não afirma consumo dos JARs públicos.
O lock JSON, o manifesto SHA-256, o preflight e o Dockerfile verificam os inputs.

Essa distribuição privada é contingência operacional para reproduzir o serviço
existente. O débito é obter a fonte correspondente e substituí-la por versões
oficiais compatíveis após os gates canônicos SDK/host. Não misturar snapshots
da migração ontológica para contornar incompatibilidades de assinatura.

## Promoção e retorno

1. Validar os commits exatos de backend e frontend, CI, Docker e jornadas HTTPS
   com cookie Secure/HttpOnly/SameSite=None/Partitioned, incluindo negação real.
2. Resolver os gates integrais antes do aceite de implantação; prova focal não
   transforma FAIL ou fonte desconhecida em PASS.
3. Após aprovação da atualização de produção, desativar auto-deploy do serviço
   Render existente. Preparar o arquivo secreto `/etc/secrets/ergon-accounts.properties`
   fora do Git, usando provisionamento explícito; guardar as credenciais com ACL
   exclusiva. Configurar `SPRING_CONFIG_ADDITIONAL_LOCATION=file:/etc/secrets/ergon-accounts.properties`,
   `SHOWCASE_SESSION_SECURE=true`, `SHOWCASE_SESSION_SAME_SITE=none`, origens Firebase
   exatas e `SHOWCASE_PUBLIC_READ_ENABLED=false`. Preservar os bancos/usuários reais.
4. Promover o commit revisado ao `main` privado e ao espelho público. Criar a tag
   de aplicação `ergon-showcase-deploy-<identificador>` nesse commit. O workflow
   oficial exige o mesmo SHA no espelho e no Render, status live e saúde UP.
   Push de main e dispatch padrão executam validação; não implantam.
5. Publicar o frontend validado no site Firebase existente após a confirmação do
   backend. Não publicar um runtime-config de preview no canal live.
6. Retorno: reimplantar o SHA anterior do Render `1d027b745c29d158cd6b1fca89d3c249e239564d`
   e restaurar a release Firebase anterior registrada antes da promoção. Restaurar
   o conjunto anterior de variáveis/arquivo secreto; não executar rollback de DDL
   neste incremento de sessão. A sessão é local à instância: manter uma instância;
   reinício/deploy exige novo login.

Nenhuma tag de produção, alteração de ambiente produtivo ou promoção ao main
faz parte da preparação deste candidato.
