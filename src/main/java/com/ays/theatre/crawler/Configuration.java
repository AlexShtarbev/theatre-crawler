package com.ays.theatre.crawler;

import static io.agroal.api.configuration.AgroalConnectionPoolConfiguration.ConnectionValidator.defaultValidator;
import static java.time.Duration.ofSeconds;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import com.ays.theatre.crawler.theatreartbg.model.ImmutableTheatreArtQueuePayload;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Dependent
public class Configuration {

    public static final String CUSTOM_DSL = "CUSTOM_DSL";
    public static final String JDBC_URL = "jdbc:postgresql://localhost:5432/postgres";

    @Produces
    @Singleton
    ConcurrentLinkedQueue<ImmutableTheatreArtQueuePayload> getTheatreArtBgQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Produces
    @Singleton
    @Named(CUSTOM_DSL)
    public DSLContext getHikariDslContext() throws SQLException {
        return DSL.using(getDatasource(), SQLDialect.POSTGRES);
    }

    // https://stackoverflow.com/questions/68817155/how-to-define-a-data-source-programatically
    private AgroalDataSource getDatasource() throws SQLException {
        AgroalDataSourceConfigurationSupplier configuration = new AgroalDataSourceConfigurationSupplier()
                .dataSourceImplementation(AgroalDataSourceConfiguration.DataSourceImplementation.AGROAL)
                .metricsEnabled(false)
                .connectionPoolConfiguration(cp -> cp
                        .minSize(5)
                        .maxSize(20)
                        .initialSize(10)
                        .connectionValidator(defaultValidator())
                        .acquisitionTimeout(ofSeconds(5))
                        .leakTimeout(ofSeconds(5))
                        .validationTimeout(ofSeconds(50))
                        .reapTimeout(ofSeconds(500))
                        .connectionFactoryConfiguration(cf -> cf
                                .jdbcUrl(JDBC_URL)
                                .autoCommit( true )
                                .principal(new NamePrincipal("postgres"))
                                .credential(new SimplePassword("password"))
                        )
                );

        return AgroalDataSource.from(configuration);
    }
}
