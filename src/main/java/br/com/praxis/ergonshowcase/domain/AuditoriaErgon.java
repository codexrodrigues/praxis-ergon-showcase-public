package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/** Datas de criação e manutenção do assentamento, em UTC. */
@Getter
@Setter
@Embeddable
public class AuditoriaErgon {
  /** Data e hora de criação do registro em UTC. */
  @Comment("Data e hora de criação do registro em UTC.")
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  /** Data e hora da última atualização do registro em UTC. */
  @Comment("Data e hora da última atualização do registro em UTC.")
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  public void criar() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  public void atualizar() {
    updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
