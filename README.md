# mTickets 🎫
> O sistema de suporte definitivo para seu servidor de Minecraft (1.8.8).

**mTickets** é uma solução completa que integra o **Servidor Minecraft**, um **Painel Web Moderno** e seu **Discord** em um único fluxo de atendimento. Seus jogadores abrem tickets no jogo, sua equipe responde pelo Painel Web ou pelo canal criado automaticamente no Discord, e tudo é sincronizado em tempo real.

![Java 8](https://img.shields.io/badge/Java-8-orange)
![Spigot](https://img.shields.io/badge/Spigot-1.8.8-yellow)
![Discord](https://img.shields.io/badge/Discord-JDA-blue)
![Status](https://img.shields.io/badge/Status-Stable-brightgreen)

---

## ✨ Funcionalidades

### 🎮 In-Game
- GUI intuitiva para criação e gerenciamento de tickets.
- Notificações em tempo real para Staff.
- Suporte a SQLite (padrão) e MySQL.

### 🌐 Painel Web
- Interface moderna e responsiva (Mobile-friendly).
- Login seguro via **Discord OAuth2**.
- Staff pode responder e fechar tickets sem abrir o jogo.
- Estatísticas de atendimento (Tempo médio, tickets resolvidos).
- **Zero configuração extra:** O painel roda dentro do próprio plugin!

### 🤖 Integração Discord
- **Canais por Ticket:** Cria um canal `text-channel` para cada ticket novo na categoria configurada.
- **Sincronização Bidirecional:**
  - Mensagem no Jogo -> Aparece no Discord.
  - Mensagem no Discord -> Aparece no Jogo e no Painel.
- Fechamento automático de canal/transcrição ao encerrar o ticket.

---

## 🚀 Instalação

1. Baixe o arquivo `mTickets-1.0.0.jar` da aba [Releases](#).
2. Coloque na pasta `/plugins` do seu servidor.
3. Reinicie o servidor para gerar o `config.yml`.
4. Configure o arquivo `config.yml` (veja abaixo).
5. Reinicie novamente. O Painel Web estará acessível em `http://SEU_IP:8080`.

---

## ⚙️ Configuração Obrigatória

### 1. Criando o Bot no Discord
Para que o painel e a sincronização funcionem, você precisa de um Bot.
1. Vá para o [Discord Developer Portal](https://discord.com/developers/applications).
2. Crie uma "New Application".
3. Vá em "Bot" > "Add Bot".
4. **IMPORTANTE:** Em "Privileged Gateway Intents", ATIVE:
   - ✅ **Presence Intent**
   - ✅ **Server Members Intent** (Para verificar cargos de Staff)
   - ✅ **Message Content Intent** (Para ler respostas da Staff)
5. Copie o **Token** do Bot.
6. Vá em "OAuth2" > Copie o **Client ID** e **Client Secret**.

### 2. Editando o config.yml
Abra `plugins/mTickets/config.yml` e preencha:

```yaml
discord:
  enabled: true
  token: "SEU_BOT_TOKEN_AQUI"
  guild-id: "ID_DO_SEU_SERVIDOR_DISCORD"
  ticket-category-id: "ID_DA_CATEGORIA_PARA_CANAIS_DE_TICKET"
  log-channel-id: "ID_CANAL_LOGS" # Opcional
  
  # Lista de IDs de Cargos (Roles) que podem acessar o Painel Web
  allowed-roles:
    - "123456789012345678" # ID Cargo Admin
    - "987654321098765432" # ID Cargo Mod

web:
  port: 8080 # Porta do Painel
  client-id: "SEU_CLIENT_ID"
  client-secret: "SEU_CLIENT_SECRET"
  # URL base ONDE o servidor está rodando (ex: http://jogar.meuserver.com:8080)
  base-url: "http://localhost:8080" 
```

> **Nota:** Para pegar IDs no Discord, ative o "Modo Desenvolvedor" nas configurações do Discord e clique com botão direito no Servidor/Canal/Cargo > "Copiar ID".

---

## 💻 Comandos e Permissões

| Comando | Descrição | Permissão |
|:--------|:----------|:----------|
| `/ticket` | Abre o menu de tickets | Nenhuma |
| `/ticket criar <msg>` | Cria um ticket rápido | Nenhuma |
| `/mtickets reload` | Recarrega a configuração | `mtickets.admin` |

---

## 🛠️ Compilando (Developer)

Se você deseja modificar o código:

1. Clone este repositório.
2. Certifique-se de ter **Maven** e **Java 8+** instalados.
3. Execute na raiz do projeto:

```bash
mvn clean package
```

4. O arquivo gerado estará em `target/mTickets-1.0.0.jar`.

---

## 🆘 Solução de Problemas Comuns

**Erro: "Address already in use: bind"**
> Outro programa (ou instância anterior do servidor) está usando a porta 8080. Mude a porta no `config.yml` ou mate o processo java.

**Erro 400 ao Logar no Painel**
> Verifique se o `client-id`, `client-secret` e `base-url` estão corretos. Certifique-se de adicionar a Redirect URI no Discord Dev Portal: `http://SEU_IP:PORTA/api/auth/callback`.

**Bot não responde ou não cria canais**
> Verifique se você ativou as "Intents" no Developer Portal e se o Bot tem permissão de "Gerenciar Canais" no Discord.

---

Feito com ❤️ por **MarcosFGDev**
