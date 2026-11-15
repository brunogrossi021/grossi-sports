package com.curso.grossisports.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Relógio controlável para testes que dependem de tempo (Aula 16).
 *
 * <p>Permite avançar o instante de forma determinística para exercitar
 * os limites de inatividade e de expiração absoluta sem esperas reais.</p>
 */
public class MutableClock extends Clock {

    private Instant instante;
    private final ZoneId zona;

    public MutableClock(Instant instante, ZoneId zona) {
        this.instante = instante;
        this.zona = zona;
    }

    public void definir(Instant novoInstante) {
        this.instante = novoInstante;
    }

    public void avancar(Duration duracao) {
        this.instante = this.instante.plus(duracao);
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(instante, zone);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
