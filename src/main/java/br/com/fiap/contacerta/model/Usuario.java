package br.com.fiap.contacerta.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Espelha a tabela tb_usuario: a pessoa que usa o Conta Certa. */
public class Usuario {

    private Long id;                    // cd_usuario
    private String nome;                // nm_usuario
    private String email;               // ds_email (único, guardado em minúsculas)
    private String cpf;                 // nr_cpf (único, só os 11 números)
    private String senhaHash;           // ds_senha_hash (BCrypt; a senha pura nunca é guardada)
    private BigDecimal rendaDeclarada;  // vl_renda_declarada (opcional)
    private Integer horasTrabalhoMes;   // qt_horas_trabalho_mes (padrão 220)
    private LocalDateTime dataCadastro; // dt_cadastro

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    public BigDecimal getRendaDeclarada() { return rendaDeclarada; }
    public void setRendaDeclarada(BigDecimal rendaDeclarada) { this.rendaDeclarada = rendaDeclarada; }

    public Integer getHorasTrabalhoMes() { return horasTrabalhoMes; }
    public void setHorasTrabalhoMes(Integer horasTrabalhoMes) { this.horasTrabalhoMes = horasTrabalhoMes; }

    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }
}
