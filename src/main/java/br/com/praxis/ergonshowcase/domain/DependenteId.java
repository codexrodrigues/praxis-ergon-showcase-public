package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Comment;
import org.praxisplatform.uischema.id.CompositeResourceId;

/** Identidade natural imutável de Dependente. */
@Embeddable
public record DependenteId(
    /**
     * Número funcional do servidor responsável pelo dependente (componente 1 da chave composta
     * natural @EmbeddedId).
     */
    @Comment(
            "Número funcional do servidor responsável pelo dependente (componente 1 da chave composta natural @EmbeddedId).")
        @Column(name = "numfunc", nullable = false)
        Long numfunc,
    /**
     * Número sequencial identificador do dependente para o servidor (componente 2 da chave composta
     * natural @EmbeddedId; equivale a NUMERO/NUMDEP).
     */
    @Comment(
            "Número sequencial identificador do dependente para o servidor (componente 2 da chave composta natural @EmbeddedId; equivale a NUMERO/NUMDEP).")
        @Column(name = "numdep", nullable = false)
        Integer numdep)
    implements CompositeResourceId {}
