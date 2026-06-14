# Manual do Usuário — fkmovies

Bem-vindo ao **fkmovies**, o sistema de venda de ingressos de cinema. Este manual
explica, passo a passo, **todas as funcionalidades** disponíveis para os três perfis
de usuário:

- 🎬 **Cliente** — escolhe sessões, reserva assentos, paga e recebe ingressos.
- 🛠️ **Administrador** — cadastra filmes e agenda sessões.
- 🎟️ **Operador (bilheteria)** — localiza reservas e reimprime ingressos.

> Idioma: a interface vem em **Português (Brasil)** por padrão. Você pode alternar para
> **Inglês** a qualquer momento pelo botão **PT / EN** no canto superior direito.

---

## Sumário

1. [Acesso ao sistema](#1-acesso-ao-sistema)
2. [Conhecendo a tela inicial](#2-conhecendo-a-tela-inicial)
3. [Criar uma conta](#3-criar-uma-conta-cliente)
4. [Confirmar o e-mail](#4-confirmar-o-e-mail)
5. [Entrar e sair](#5-entrar-e-sair)
6. [Ver as sessões em cartaz](#6-ver-as-sessões-em-cartaz)
7. [Escolher assentos e reservar](#7-escolher-assentos-e-reservar)
8. [Concluir a compra (pagamento)](#8-concluir-a-compra-pagamento)
9. [Meu ingresso](#9-meu-ingresso)
10. [Minhas reservas (histórico e cancelamento)](#10-minhas-reservas-histórico-e-cancelamento)
11. [Área do administrador](#11-área-do-administrador)
12. [Área do operador (bilheteria)](#12-área-do-operador-bilheteria)
13. [Perguntas frequentes e solução de problemas](#13-perguntas-frequentes-e-solução-de-problemas)
14. [Glossário de status](#14-glossário-de-status)

---

## 1. Acesso ao sistema

Abra o navegador e acesse:

| O que você quer ver | Endereço |
|---|---|
| **Aplicativo (cliente, admin, operador)** | `http://127.0.0.1:4200` |

> **Importante:** use **`127.0.0.1`** e não `localhost`. No ambiente local, `localhost`
> pode travar; `127.0.0.1` funciona normalmente.

Não é preciso instalar nada: tudo funciona pelo navegador (computador ou celular — a
tela se adapta).

---

## 2. Conhecendo a tela inicial

No topo de todas as páginas há uma **barra de navegação**:

- **fkmovies** (à esquerda) — volta para a tela inicial.
- **Sessões** — lista de filmes/horários em cartaz.
- **Minhas reservas** — aparece depois que você entra.
- **Operador / Admin** — aparecem apenas para quem tem permissão.
- **PT / EN** — troca o idioma.
- **Entrar / Criar conta** — quando você não está logado.
- **Seu nome / Sair** — quando você está logado.

A tela inicial traz uma saudação e o botão **“Explorar sessões”**, que leva direto à
lista de sessões.

---

## 3. Criar uma conta (cliente)

1. Clique em **Criar conta** (topo) ou acesse `http://127.0.0.1:4200/cadastro`.
2. Preencha:
   - **Nome** (mínimo 2 caracteres);
   - **E-mail** válido;
   - **Senha** — mínimo **8 caracteres**, com **pelo menos uma letra e um número**;
   - **Confirmar senha** — deve ser igual à senha.
3. Clique em **Cadastrar**.

Pronto! Você já entra **automaticamente** logo após o cadastro. Aparecerá uma faixa
amarela no topo pedindo para **confirmar o e-mail** — veja a seção seguinte.

> Se o e-mail já estiver cadastrado, o sistema avisa “Este e-mail já está cadastrado”.

---

## 4. Confirmar o e-mail

Para **reservar assentos** você precisa confirmar seu e-mail (é uma exigência de
segurança). Logo após o cadastro, enviamos um **link de confirmação** para o seu
e-mail.

1. Abra o e-mail recebido e clique no link **“Confirmar e-mail”**.
2. Você cairá na página de confirmação, que mostrará **“E-mail confirmado!”**.
3. Clique em **Ver sessões** para continuar.

**Não recebeu o e-mail?**
- Use o botão **Reenviar link** na faixa amarela do topo, **ou**
- Na página de confirmação (quando o link expira), informe seu e-mail e clique em
  **Reenviar confirmação**.

> O link de confirmação **expira em 24 horas**. Se expirar, basta solicitar um novo.

---

## 5. Entrar e sair

**Entrar:** clique em **Entrar** (topo) ou acesse `/login`, informe **e-mail** e
**senha** e clique em **Entrar**.

- Clientes vão para a lista de **Sessões**.
- Administradores vão para o **painel Admin**.
- Operadores vão para o **console do Operador**.

**Sair:** clique em **Sair** no topo (ao lado do seu nome).

> **Sessão ativa:** enquanto você navega, permanece logado. Ao recarregar a página em
> `127.0.0.1`, sua sessão é restaurada automaticamente.

---

## 6. Ver as sessões em cartaz

Clique em **Sessões** (ou no botão **Explorar sessões** da tela inicial). Você verá
**cartões**, um por sessão, com:

- pôster (ou um ícone de filme, quando não há imagem);
- **classificação indicativa** (L, A10, A12, A14, A16, A18);
- **título** do filme;
- **sala** e **duração**;
- **data e horário**;
- **preço “a partir de”**;
- botão **Selecionar assentos**.

Se não houver sessões, aparece a mensagem “Nenhuma sessão disponível no momento”.
Se algo falhar ao carregar, há um botão **Tentar novamente**.

---

## 7. Escolher assentos e reservar

1. No cartão da sessão desejada, clique em **Selecionar assentos**.
2. Você verá o **mapa da sala**, com a **Tela** no topo e os assentos por fileira.
3. **Cores dos assentos** (legenda na própria tela):
   - **Livre** — disponível, pode clicar para selecionar;
   - **Selecionado** — você escolheu (destacado);
   - **Reservado** — em processo de compra por outra pessoa (indisponível);
   - **Vendido** — já comprado (indisponível).
4. Clique nos assentos livres para **selecionar/desmarcar**. O rodapé mostra a
   **quantidade** e o **total**.
5. Clique em **Reservar**.

**O que pode acontecer ao reservar:**
- **Você não está logado** → o botão leva você ao **login** e, após entrar, volta para
  concluir.
- **E-mail não confirmado** → o botão fica indisponível com o aviso para confirmar o
  e-mail primeiro.
- **Alguém reservou o assento antes de você** → o sistema avisa, **destaca em vermelho**
  os assentos que ficaram indisponíveis e atualiza o mapa. Basta escolher outros.
- **Tudo certo** → você vai para a página da **reserva**, com seus assentos em espera.

---

## 8. Concluir a compra (pagamento)

Ao reservar, você cai na página da reserva. Os estados são:

### Reserva pendente (aguardando sua decisão)
- Mostra seus **assentos**, o **total** e uma **contagem regressiva** — você tem um
  tempo limitado para concluir antes que os assentos sejam liberados.
- Botões:
  - **Confirmar compra** — inicia o pagamento;
  - **Cancelar** — desiste e libera os assentos.

### Processando pagamento
- Após **Confirmar compra**, aparece um indicador de **“Processando pagamento…”**.
- O pagamento é processado automaticamente e leva apenas **alguns segundos**. A página
  se atualiza sozinha — não precisa recarregar.

### Compra confirmada ✅
- Aparece **“Compra confirmada!”** com a lista de **ingressos** (assento + **código**).
- Há o botão **Ver minhas reservas**.

### Reserva expirada / cancelada
- Se o tempo acabar (**expirada**) ou você cancelar (**cancelada**), a página informa o
  ocorrido e oferece um caminho para **escolher assentos novamente**.

> O ingresso e a confirmação também são **enviados para o seu e-mail**.

---

## 9. Meu ingresso

O ingresso confirmado mostra, para cada assento, o **código do ingresso** (ex.:
`FKM-2026-000003`). Apresente esse código (ou seu e-mail/nome) na **bilheteria** — o
operador consegue localizar sua reserva e, se necessário, **reimprimir** o ingresso.

---

## 10. Minhas reservas (histórico e cancelamento)

Acesse **Minhas reservas** no topo (precisa estar logado).

**Filtros:**
- **Próximas** / **Todas** — alterna entre sessões futuras e todo o histórico.
- **Chips de status** — Todos, Pendente, Aguardando pagamento, Confirmada, Cancelada,
  Expirada.

**Cada reserva exibe:** filme, sala, data/hora (horário de Brasília), assentos, total
e um **selo de status** colorido. Use **Detalhes** para abrir a reserva.

**Cancelar uma reserva:**
- O botão **Cancelar** aparece quando o cancelamento é permitido:
  - reservas **pendentes** ou **aguardando pagamento** — sempre;
  - reservas **confirmadas** — somente **até 2 horas antes** do início da sessão.
- Ao clicar, uma janela pede **confirmação**.
- Se houver pagamento, o **reembolso é solicitado** automaticamente e você vê o aviso
  com o valor.
- Se a janela de cancelamento fechar no meio do caminho, o sistema avisa e atualiza a
  lista.

---

## 11. Área do administrador

> Acesso restrito a usuários **administradores**. O link **Admin** aparece no topo
> apenas para esse perfil. URL: `http://127.0.0.1:4200/admin`.

O painel tem duas abas: **Filmes** e **Sessões**.

### 11.1 Filmes (`/admin/filmes`)

Lista todos os filmes (título, duração, classificação, status).

- **Novo filme** — abre um formulário com:
  - **Título**;
  - **Duração (min)** — de 1 a 600;
  - **Classificação** — L, A10, A12, A14, A16, A18;
  - **Sinopse** (opcional);
  - **URL do pôster** (opcional).
  Clique em **Salvar**.
- **Editar** (✏️) — altera os dados de um filme existente.
- **Arquivar / Reativar** — arquivar **esconde** o filme de novas sessões (sem afetar
  sessões já criadas); reativar o traz de volta. Pede confirmação.

> Para agendar uma sessão, o filme precisa estar **Ativo**.

### 11.2 Sessões (`/admin/sessoes`)

Lista as sessões agendadas (filme, sala, data/hora, preço base, status).

- **Nova sessão** — abre um formulário com:
  - **Filme** — escolha entre os filmes **ativos**;
  - **Sala** — escolha entre as salas disponíveis;
  - **Data e hora** — informada no **horário de Brasília (America/Sao_Paulo)**; o
    sistema converte para o padrão interno automaticamente;
  - **Preço base (R$)**.
  Clique em **Salvar**.
- **Cancelar sessão** — cancela uma sessão agendada (pede confirmação).

**Mensagens de erro comuns ao agendar:**
- **Conflito de sala** — já existe uma sessão ocupando aquela sala no horário; escolha
  outro horário ou sala.
- **Muito em cima da hora** — a sessão precisa começar com uma antecedência mínima.

---

## 12. Área do operador (bilheteria)

> Acesso para **operadores** e **administradores**. O link **Operador** aparece no topo
> para esses perfis. URL: `http://127.0.0.1:4200/operator`.

Tela única de **busca de reservas**:

1. Escolha o **critério** (apenas um por busca):
   - **Código do ingresso** (ex.: `FKM-2026-000003`);
   - **ID da reserva**;
   - **E-mail** do cliente.
2. Digite o valor e clique em **Buscar**.
3. A lista de resultados mostra **cliente, filme, data/hora e assentos**.
4. Clique em **Ver detalhes** para abrir a reserva completa, com os **ingressos**.
5. Em cada ingresso, **Reimprimir** abre a **página de impressão** do ingresso.

**Página de impressão do ingresso:**
- Mostra o ingresso em formato limpo (fkmovies, filme, sala, assento, data/hora,
  cliente e **código**), além do número de **reimpressões**.
- A impressão é disparada automaticamente; você também pode clicar em **Imprimir**.
- Ao imprimir, o sistema esconde a navegação e imprime **somente o ingresso**.

---

## 13. Perguntas frequentes e solução de problemas

**“Cliquei em Reservar e nada acontece / pede login.”**
Você precisa estar **logado** e com **e-mail confirmado**. Entre, confirme o e-mail
(faixa amarela do topo) e tente novamente.

**“O assento que eu queria virou indisponível.”**
Outra pessoa reservou primeiro. Os assentos afetados ficam **destacados em vermelho** e
o mapa é atualizado — escolha outros assentos livres.

**“Minha reserva expirou.”**
A reserva fica em espera por tempo limitado. Se o cronômetro zerar antes de você
**Confirmar a compra**, os assentos são liberados. Basta reservar de novo.

**“Não recebi o e-mail de confirmação / ingresso.”**
Verifique a caixa de spam. Use **Reenviar link** (faixa amarela) ou a opção de reenviar
na página de confirmação.

**“Não consigo cancelar uma reserva confirmada.”**
O cancelamento de reservas **confirmadas** só é permitido até **2 horas antes** do
início da sessão.

**“A página não abre em `localhost`.”**
Use **`http://127.0.0.1:4200`** (e não `localhost`).

**“Não vejo os menus Admin/Operador.”**
Eles só aparecem para usuários com a permissão correta. Entre com uma conta de
administrador ou operador.

---

## 14. Glossário de status

**Status da reserva:**

| Status | Significado |
|---|---|
| **Pendente** | Assentos em espera; aguardando você confirmar a compra (com cronômetro). |
| **Aguardando pagamento** | Compra confirmada; pagamento sendo processado. |
| **Confirmada** | Pagamento aprovado; ingressos emitidos. |
| **Cancelada** | Reserva cancelada (por você ou pelo sistema). Pode haver reembolso. |
| **Expirada** | O tempo para confirmar acabou e os assentos foram liberados. |

**Classificação indicativa (filmes):**

| Sigla | Significado |
|---|---|
| **L** | Livre para todos os públicos |
| **A10 / A12 / A14 / A16 / A18** | Idade mínima recomendada (10, 12, 14, 16 ou 18 anos) |

---

*fkmovies — bom filme! 🍿*
