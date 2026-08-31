# Tema do Projeto — Grossi Sports

## 1. Nome do sistema

**Grossi Sports**

Sistema de gerenciamento de produtos esportivos desenvolvido para fins acadêmicos.

## 2. Domínio

O sistema será utilizado para organizar e gerenciar produtos esportivos, permitindo o cadastro, consulta, alteração e exclusão de produtos e seus respectivos grupos de classificação.

## 3. Entidades principais

### GrupoProduto

Representa a classificação dos produtos esportivos.

Exemplos:

- Futebol
- Corrida
- Academia

### Produto

Representa um item esportivo comercializado pela Grossi Sports.

Cada produto possui:

- código
- descrição
- saldo em estoque
- valor unitário
- grupo de produto

## 4. Relacionamento

Um **GrupoProduto** pode possuir vários **Produtos**.

Cada **Produto** pertence a um único **GrupoProduto**.

```text
GrupoProduto 1 ─────────── N Produto
5. Exemplos de produtos
Código	Descrição	Grupo	Saldo	Valor unitário
001	Chuteira Society	Futebol	10	R$ 299,90
002	Bola de Futebol	Futebol	25	R$ 149,90
003	Camiseta Esportiva	Academia	15	R$ 89,90
6. Operações previstas

O sistema deverá permitir:

cadastrar grupo de produto;
listar grupos de produto;
alterar grupo de produto;
excluir grupo de produto;
cadastrar produto;
listar produtos;
consultar produto por código;
alterar produto;
excluir produto.
7. Identificação

O Produto será identificado por um código único.

O GrupoProduto também possuirá um identificador próprio.

8. Regras iniciais
O código do produto deve ser único.
Todo produto deve pertencer a um grupo de produto.
O saldo do produto não pode ser negativo.
O valor unitário deve ser maior que zero.
A descrição do produto é obrigatória.
O nome do grupo de produto é obrigatório.
