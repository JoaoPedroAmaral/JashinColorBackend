# JashinColor Backend - Guia de Uso

Este backend gerencia a venda de livros de colorir digitais. O fluxo principal consiste em: **Criar Livro** -> **Gerar Pagamento** -> **Webhook do Mercado Pago** -> **Entrega do PDF**.

## 🚀 Fluxo de Uso Passo a Passo

### 1. Autenticação (JWT)
Primeiro, você precisa criar uma conta e logar para obter um token.
- **Registro:** `POST /api/auth/register` (JSON com email e password)
- **Login:** `POST /api/auth/login` (JSON com email e password)
> **Importante:** Guarde o `token` recebido. Todas as requisições abaixo devem enviar o header:  
> `Authorization: Bearer <SEU_TOKEN>`

---

### 2. Criar o seu Livro
Envie as imagens para criar o livro que será vendido.
- **Endpoint:** `POST /api/books`
- **Tipo:** `Multipart Form Data`
- **Parâmetros:**
  - `userId`: ID do seu usuário.
  - `title`: Nome do livro.
  - `price`: Preço que será cobrado (ex: `15.50`).
  - `files`: Selecione as imagens do livro.
- **Resposta:** Você receberá um `bookId`. Guarde ele.

---

### 3. Iniciar o Pagamento (Mercado Pago)
Agora você gera o link de pagamento para o cliente.
- **Endpoint:** `POST /api/checkout/preference?bookId=<ID_DO_LIVRO>`
- **Resposta:** Você receberá um `initPoint` (URL de pagamento).
- **Ação:** Redirecione o usuário para essa URL. Ele poderá pagar com Cartão, Boleto ou PIX dentro do sistema do Mercado Pago.

---

### 4. Processamento Automático (Webhook)
**Você não precisa fazer nada aqui.**  
Assim que o cliente pagar, o Mercado Pago avisará este backend através da rota `/api/payments/webhook`.
1. O backend confirma o pagamento.
2. O PDF é gerado automaticamente com as imagens enviadas.
3. O PDF é enviado para o Cloudinary.
4. O status do livro muda para `PAID` (Pago).

---

### 5. Download do PDF
Após o pagamento ser aprovado, o link de download fica disponível.
- **Endpoint:** `GET /api/books/<bookId>/download-url`
- **Ação:** Retorna o arquivo PDF pronto para o cliente.

---

## 🛠️ Configurações Necessárias (`application.properties`)

```properties
# Mercado Pago
mercadopago.access-token=SEU_TOKEN_AQUI

# Cloudinary (Para guardar os PDFs gerados)
cloudinary.cloud_name=NOME
cloudinary.api_key=CHAVE
cloudinary.api_secret=SEGREDO

# URL do seu servidor (Essencial para o Mercado Pago te avisar do pagamento)
app.baseUrl=https://sua-url-publica.com
```

*Dica: Para testar localmente, use o **ngrok** para criar uma URL pública que aponte para o seu `localhost:8080`.*
