package br.com.fiap.contacerta.service.front;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite simples de envios por origem (usado no formulário de contato, que não exige login).
 *
 * Guarda em memória os horários dos últimos envios de cada origem. Se a mesma origem tentar
 * mais de "maximo" envios dentro da "janela", o envio é recusado. Sem isso, um script poderia
 * encher a tabela tb_contato e ocupar as poucas conexões do Oracle.
 * A memória é zerada quando a API reinicia, o que é aceitável para este uso.
 */
public class LimiteDeEnvios {

    private final int maximo;
    private final Duration janela;
    private final Map<String, Deque<Instant>> enviosPorOrigem = new ConcurrentHashMap<>();

    public LimiteDeEnvios(int maximo, Duration janela) {
        this.maximo = maximo;
        this.janela = janela;
    }

    /** Registra uma tentativa; devolve false se a origem já passou do limite. */
    public boolean permitir(String origem, Instant agora) {
        Deque<Instant> envios = enviosPorOrigem.computeIfAbsent(origem, chave -> new ArrayDeque<>());
        synchronized (envios) {
            while (!envios.isEmpty() && envios.peekFirst().isBefore(agora.minus(janela))) {
                envios.pollFirst(); // envios antigos não contam mais
            }
            if (envios.size() >= maximo) {
                return false;
            }
            envios.addLast(agora);
            return true;
        }
    }
}
