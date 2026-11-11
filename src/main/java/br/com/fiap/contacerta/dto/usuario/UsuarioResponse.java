package br.com.fiap.contacerta.dto.usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Saída com os dados do usuário.
 * Repare no que NÃO existe aqui: o CPF completo e o hash da senha. Como a API devolve
 * este DTO (e não o model Usuario), esses dados nunca saem do servidor.
 */
public class UsuarioResponse {

    private Long id;
    private String nome;
    private String email;
    private String cpfMascarado;
    private BigDecimal rendaDeclarada;
    private Integer horasTrabalhoMes;
    private LocalDateTime dataCadastro;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCpfMascarado() { return cpfMascarado; }
    public void setCpfMascarado(String cpfMascarado) { this.cpfMascarado = cpfMascarado; }

    public BigDecimal getRendaDeclarada() { return rendaDeclarada; }
    public void setRendaDeclarada(BigDecimal rendaDeclarada) { this.rendaDeclarada = rendaDeclarada; }

    public Integer getHorasTrabalhoMes() { return horasTrabalhoMes; }
    public void setHorasTrabalhoMes(Integer horasTrabalhoMes) { this.horasTrabalhoMes = horasTrabalhoMes; }

    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }
}
