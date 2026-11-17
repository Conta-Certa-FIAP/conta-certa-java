# Conta Certa · API Java

API REST do Conta Certa (Global Solution FIAP 2026/2). Spring Boot 3.5, Java 21, JDBC puro com Oracle.
O front em React consome esta API: [conta-certa-front](https://github.com/yasminmatsuok/conta-certa-front).

## Rodar no computador

```bash
# variáveis do banco (veja .env.example): DB_URL, DB_USER, DB_PASSWORD
mvn spring-boot:run          # sobe em http://localhost:8080
mvn test                     # testes unitários (JUnit 5)
```

`http://localhost:5173` (o front em desenvolvimento) já é liberado no CORS.
Sem `TOKEN_SECRET`, a API sorteia um segredo a cada vez que sobe (os logins caem ao reiniciar).

## Banco de dados

- **Banco novo:** rode `database/script.sql` no Oracle.
- **Banco que já tem as tabelas antigas:** rode `database/migracao_v2.sql` uma vez, **antes** de publicar
  esta versão. Ela acrescenta as colunas e tabelas que as rotas do front usam.

## Autenticação

`POST /auth/login` devolve um `token`. As demais rotas exigem `Authorization: Bearer <token>`,
exceto `/auth/cadastro`, `/contact`, `/bancos`, `/categorias`, `/indicadores` e `/health`.
Nas rotas com id na URL (`/usuarios/{id}`, `/contas/{id}`, `/dividas/{id}`...), a API confere se o
registro é de quem está logado (senão, 403).

## Rotas

- **Usadas pelo front (em inglês):** `/dashboard`, `/transactions`, `/debts`, `/debts/{id}/payments`,
  `/plan`, `/learning/tracks`, `/contact`. Contrato completo no `ENDPOINTS.md` do repositório do front.
- **Rotas originais (em português):** usuários, contas, cartões, transações, dívidas, pagamentos,
  cofres, análises (`/usuarios/{id}/situacao`, `/consultoria`, `/resumo-mensal`, `/custo-em-horas`,
  `/pagar-ou-investir`), projeção e renegociação de dívidas, indicadores e simulador do Tesouro Selic.

## Deploy no Render

1. Rode a migração no Oracle (seção acima).
2. No Render: **New > Blueprint** e escolha este repositório. O `render.yaml` cria o serviço com o
   `Dockerfile`, health check em `/health` e gera o `TOKEN_SECRET` sozinho.
3. Preencha `DB_URL`, `DB_USER` e `DB_PASSWORD` no painel. `FRONT_URL` já aponta para o front na Vercel.
4. Depois que a API subir, configure na Vercel (projeto do front) a variável
   `VITE_API_URL=https://<seu-servico>.onrender.com` e faça um novo deploy do front.

No plano gratuito o serviço dorme depois de 15 minutos sem uso: a primeira chamada depois disso demora
cerca de um minuto.
