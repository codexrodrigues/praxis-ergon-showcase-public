package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Comment;
import org.praxisplatform.uischema.id.CompositeResourceId;

/** Identidade natural imutável de CodigoFrequencia. */
@Embeddable
public record CodigoFrequenciaId(
    /**
     * Tipo macro de frequência ao qual o código pertence (componente 1 da chave composta natural).
     */
    @Comment(
            "Tipo macro de frequência ao qual o código pertence (componente 1 da chave composta natural).")
        @Column(name = "tipofreq", nullable = false, length = 20)
        String tipofreq,
    /**
     * Código numérico identificador da ocorrência de frequência ou afastamento (componente 2 da
     * chave composta natural).
     */
    @Comment(
            "Código numérico identificador da ocorrência de frequência ou afastamento (componente 2 da chave composta natural).")
        @Column(name = "codigo", nullable = false)
        Integer codigo)
    implements CompositeResourceId {}
