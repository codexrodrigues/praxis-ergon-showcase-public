package br.com.praxis.ergonshowcase.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.praxisplatform.uischema.command.JdbcResourceCommandReceiptStore;
import org.praxisplatform.uischema.command.ResourceCommandIdempotencyStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/** Item commands and their receipts commit together in the host's operational transaction. */
@Configuration
public class ResourceCommandReceiptsConfig {
  @Bean ResourceCommandIdempotencyStore resourceCommandIdempotencyStore(DataSource dataSource,
      PlatformTransactionManager transactionManager, ObjectMapper mapper) {
    return new JdbcResourceCommandReceiptStore(new JdbcTemplate(dataSource), transactionManager, mapper);
  }
}
