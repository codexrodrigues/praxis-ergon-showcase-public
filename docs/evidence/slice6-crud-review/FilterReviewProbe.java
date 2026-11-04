import br.com.praxis.ergonshowcase.domain.*;
import org.hibernate.boot.*;
import org.hibernate.boot.registry.*;
import java.nio.file.*;

/** Build selected Hibernate mappings without connecting to a database. */
public class FilterReviewProbe {
  public static void main(String[] args) throws Exception {
    var registry = new StandardServiceRegistryBuilder()
      .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
      .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
      .applySetting("hibernate.hbm2ddl.auto", "none").build();
    try {
      var sources = new MetadataSources(registry);
      try (var paths = Files.list(Path.of("target/classes/br/com/praxis/ergonshowcase/domain"))) {
        for (var path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
          var type = Class.forName("br.com.praxis.ergonshowcase.domain."+path.getFileName().toString().replace(".class",""));
          if (type.isAnnotationPresent(jakarta.persistence.Entity.class)) sources.addAnnotatedClass(type);
        }
      }
      try (var sf = sources.buildMetadata().buildSessionFactory(); var session = sf.openSession()) {
        var cb = sf.getCriteriaBuilder(); var q = cb.createQuery(PeriodoAquisitivoLicencaEspecial.class);
        var root = q.from(PeriodoAquisitivoLicencaEspecial.class);
        System.out.println("Path Java type="+root.get("permGozoLicEspAntFimPe").getJavaType());
        q.select(root).where(cb.isFalse(root.get("permGozoLicEspAntFimPe")));
        session.createQuery(q);
        System.out.println("IS_FALSE accepted by Hibernate criteria");
      }
    } finally { StandardServiceRegistryBuilder.destroy(registry); }
  }
}
