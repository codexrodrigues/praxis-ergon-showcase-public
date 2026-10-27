package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Comment;
import org.praxisplatform.uischema.id.CompositeResourceId;

/** Identidade natural imutável de ItemTabela. */
@Embeddable
public record ItemTabelaId(
    /**
     * Código da tabela geral de domínio à qual o item pertence (componente 1 da chave composta
     * natural).
     */
    @Comment(
            "Código da tabela geral de domínio à qual o item pertence (componente 1 da chave composta natural).")
        @Column(name = "tab", nullable = false, length = 40)
        String tab,
    /**
     * Código identificador do item dentro da tabela geral (componente 2 da chave composta natural).
     */
    @Comment(
            "Código identificador do item dentro da tabela geral (componente 2 da chave composta natural).")
        @Column(name = "item", nullable = false, length = 99)
        String item)
    implements CompositeResourceId {}
