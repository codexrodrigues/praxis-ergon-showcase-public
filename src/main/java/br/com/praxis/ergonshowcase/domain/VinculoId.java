package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Comment;
import org.praxisplatform.uischema.id.CompositeResourceId;

/** Identidade natural imutável de Vinculo. */
@Embeddable
public record VinculoId(
    /**
     * Número funcional do servidor titular do vínculo (componente 1 da chave composta
     * natural @EmbeddedId).
     */
    @Comment(
            "Número funcional do servidor titular do vínculo (componente 1 da chave composta natural @EmbeddedId).")
        @Column(name = "numfunc", nullable = false)
        Long numfunc,
    /**
     * Número sequencial do vínculo funcional do servidor (componente 2 da chave composta
     * natural @EmbeddedId; ex.: 1, 2 para acumulação constitucional).
     */
    @Comment(
            "Número sequencial do vínculo funcional do servidor (componente 2 da chave composta natural @EmbeddedId; ex.: 1, 2 para acumulação constitucional).")
        @Column(name = "numero", nullable = false)
        Integer numero)
    implements CompositeResourceId {}
