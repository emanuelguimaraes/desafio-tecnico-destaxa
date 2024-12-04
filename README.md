# Desafio Técnico Destaxa - Simulador de Autorização de Pagamentos

Este projeto simula um sistema de autorização de pagamentos com cartão de crédito e débito, similar ao TEF, utilizando o protocolo ISO8583 sobre sockets. A arquitetura é baseada em microsserviços, com um cliente API REST (`destaxa-api`) e um autorizador (`destaxa-autorizador`) comunicando-se assincronamente via RabbitMQ.

## Arquitetura

A arquitetura do sistema é composta por três componentes principais:

* **destaxa-api (Cliente API REST):**  Recebe requisições de autorização via API REST, converte-as para o formato ISO8583 e as envia para o RabbitMQ.  Recebe as respostas de autorização do RabbitMQ e as retorna ao cliente.
* **destaxa-autorizador (Autorizador):**  Consome mensagens ISO8583 do RabbitMQ, processa as requisições de autorização, gera respostas no formato ISO8583 e as envia de volta para o RabbitMQ.
* **RabbitMQ (Message Broker):**  Responsável pela comunicação assíncrona entre a API e o autorizador.


## Pré-requisitos

* Docker
* Docker Compose
* Java Development Kit (JDK) 17 (para build local, caso necessário)
* Maven (para build local, caso necessário)


## Executando a aplicação com Docker Compose

1. Clone o repositório:

```bash
git clone https://github.com/emanuelguimaraes/desafio-tecnico-destaxa.git
```

2. Navegue até o diretório do projeto:

```bash
cd desafio-tecnico-destaxa
```

3. Inicie os containers:

```bash
docker compose up -d
```

Isso irá construir e iniciar os containers do RabbitMQ, `destaxa-api` e `destaxa-autorizador`.

* **destaxa-api:**  Acessível em http://localhost:8080
* **destaxa-autorizador:** Acessível em http://localhost:8081
* **RabbitMQ Management Plugin:** Acessível em http://localhost:15672

Certifique-se de ter uma instância do RabbitMQ rodando e configure as propriedades de conexão no arquivo `application.properties` (ou `application.yml`) de cada aplicação.

## Endpoints da API (destaxa-api)

**Solicitar Autorização:**

* **URL:** `/api/authorization`
* **Método:** `POST`
* **Corpo da requisição (JSON):**

```json
{
  "externalId": "ext123",
  "value": 23.80,
  "cardNumber": "1234567890123456",
  "cvv": "123",
  "expMonth": 11,
  "expYear": 28,
  "holderName": "Destaxa",
  "installments": 1 
}
```

**Consultar Status da Autorização:**

* **URL:** `/api/authorization/{externalId}`
* **Método:** `GET`


## Tecnologias Utilizadas

* Java 17
* Spring Boot
* Spring Web
* Spring AMQP
* RabbitMQ
* jPOS (para processamento de mensagens ISO8583)
* Docker
* Docker Compose