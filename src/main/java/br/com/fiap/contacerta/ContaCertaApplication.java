package br.com.fiap.contacerta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da API do Conta Certa.
 *
 * Caminho de uma requisição dentro do projeto:
 *   controller (recebe o HTTP) -> dto (dados que entram/saem) -> service (regras e cálculos)
 *   -> dao (SQL com JDBC) -> banco Oracle.
 *
 * Pacotes:
 *   model      - classes que espelham as tabelas do banco
 *   dao        - acesso ao banco com JDBC puro (um DAO por tabela)
 *   dto        - objetos de entrada e saída da API (o model nunca sai direto)
 *   controller - rotas HTTP
 *   service    - regras de negócio e cálculos financeiros
 *   client     - cliente HTTP da API do Banco Central
 *   exception  - exceções próprias
 *   handler    - transforma qualquer erro em JSON padronizado
 *   config     - CORS, ModelMapper, BCrypt
 */
@SpringBootApplication
public class ContaCertaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContaCertaApplication.class, args);
    }
}
