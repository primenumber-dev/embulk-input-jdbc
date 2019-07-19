package org.embulk.input;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import javax.validation.constraints.Size;

import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
import org.embulk.input.jdbc.AbstractJdbcInputPlugin;
import org.embulk.input.jdbc.JdbcInputConnection;
import org.embulk.input.jdbc.getter.ColumnGetterFactory;
import org.embulk.input.sqlserver.SQLServerInputConnection;

import com.google.common.base.Optional;
import org.embulk.input.sqlserver.getter.SQLServerColumnGetterFactory;
import org.embulk.spi.PageBuilder;
import org.joda.time.DateTimeZone;

import com.microsoft.sqlserver.jdbc.SQLServerDriver;

import static java.util.Locale.ENGLISH;

public class SQLServerInputPlugin
    extends AbstractJdbcInputPlugin
{
    private static int DEFAULT_PORT = 1433;

    public interface SQLServerPluginTask
        extends PluginTask
    {
        @Config("driver_path")
        @ConfigDefault("null")
        public Optional<String> getDriverPath();

        @Config("host")
        @ConfigDefault("null")
        public Optional<String> getHost();

        @Config("port")
        @ConfigDefault("1433")
        public int getPort();

        @Config("instance")
        @ConfigDefault("null")
        public Optional<String> getInstance();

        @Config("database")
        @ConfigDefault("null")
        public Optional<String> getDatabase();

        @Config("integratedSecurity")
        @ConfigDefault("false")
        public boolean getIntegratedSecurity();

        @Config("url")
        @ConfigDefault("null")
        public Optional<String> getUrl();

        @Config("user")
        @ConfigDefault("null")
        public Optional<String> getUser();

        @Config("password")
        @ConfigDefault("\"\"")
        public String getPassword();

        @Config("schema")
        @ConfigDefault("null")
        public Optional<String> getSchema();

        @Config("transaction_isolation_level")
        @ConfigDefault("null")
        public Optional<String> getTransactionIsolationLevel();

        @Config("application_name")
        @ConfigDefault("\"embulk-input-sqlserver\"")
        @Size(max=128)
        public String getApplicationName();
    }

    @Override
    protected Class<? extends PluginTask> getTaskClass()
    {
        return SQLServerPluginTask.class;
    }

    private static class UrlAndProperties
    {
        private final String url;
        private final Properties properties;

        public UrlAndProperties(String url, Properties properties)
        {
            this.url = url;
            this.properties = properties;
        }

        public String getUrl()
        {
            return url;
        }

        public Properties getProperties()
        {
            return properties;
        }
    }

    @Override
    protected JdbcInputConnection newConnection(PluginTask task) throws SQLException
    {
        SQLServerPluginTask sqlServerTask = (SQLServerPluginTask) task;

        Driver driver;
        driver = new SQLServerDriver();

        UrlAndProperties urlAndProps = buildUrlAndProperties(sqlServerTask);

        Properties props = urlAndProps.getProperties();
        props.putAll(sqlServerTask.getOptions());
        logConnectionProperties(urlAndProps.getUrl(), props);

        Connection con = driver.connect(urlAndProps.getUrl(), props);
        try {
            SQLServerInputConnection c = new SQLServerInputConnection(con, sqlServerTask.getSchema().orNull(),
                                                                      sqlServerTask.getTransactionIsolationLevel().orNull());
            con = null;
            return c;
        }
        finally {
            if (con != null) {
                con.close();
            }
        }
    }

    @Override
    protected ColumnGetterFactory newColumnGetterFactory(PageBuilder pageBuilder, DateTimeZone dateTimeZone)
    {
        return new SQLServerColumnGetterFactory(pageBuilder, dateTimeZone);
    }

    private UrlAndProperties buildUrlAndProperties(SQLServerPluginTask sqlServerTask)
    {
        Properties props = new Properties();

        // common properties

        if (sqlServerTask.getUser().isPresent()) {
            props.setProperty("user", sqlServerTask.getUser().get());
        }
        props.setProperty("password", sqlServerTask.getPassword());

        // SQLServerDriver properties
        //TODO support more options as necessary
        // List of properties: https://msdn.microsoft.com/en-us/library/ms378988(v=sql.110).aspx
        props.setProperty("loginTimeout", String.valueOf(sqlServerTask.getConnectTimeout())); // seconds
        props.setProperty("applicationName", sqlServerTask.getApplicationName());

        // skip URL build if it's set
        if (sqlServerTask.getUrl().isPresent()) {
            if (sqlServerTask.getHost().isPresent()
                    || sqlServerTask.getInstance().isPresent()
                    || sqlServerTask.getDatabase().isPresent()
                    || sqlServerTask.getIntegratedSecurity()) {
                throw new ConfigException("'host', 'port', 'instance', 'database' and 'integratedSecurity' options are invalid if 'url' option is set.");
            }

            return new UrlAndProperties(sqlServerTask.getUrl().get(), props);
        }

        // build URL
        String url;

        if (!sqlServerTask.getHost().isPresent()) {
            throw new ConfigException("'host' option is required but not set.");
        }

        // SQLServerDriver URL: host:port[;databaseName=] or host\instance[;databaseName=]
        // host\instance:port[;databaseName] is allowed but \instance will be ignored in this case.
        if (sqlServerTask.getInstance().isPresent()) {
            if (sqlServerTask.getPort() != DEFAULT_PORT) {
                logger.warn("'port: {}' option is ignored because instance option is set", sqlServerTask.getPort());
            }
            url = String.format(ENGLISH, "jdbc:sqlserver://%s\\%s", sqlServerTask.getHost().get(), sqlServerTask.getInstance().get());
        }
        else {
            url = String.format(ENGLISH, "jdbc:sqlserver://%s:%d", sqlServerTask.getHost().get(), sqlServerTask.getPort());
        }

        // ;databaseName=
        if (sqlServerTask.getDatabase().isPresent()) {
            props.setProperty("databaseName", sqlServerTask.getDatabase().get());
        }

        // integratedSecutiry or user + password is required
        if (sqlServerTask.getIntegratedSecurity()) {
            if (sqlServerTask.getUser().isPresent()) {
                throw new ConfigException("'user' options are invalid if 'integratedSecutiry' option is set.");
            }
            props.setProperty("integratedSecurity", "true");
        }
        else {
            if (!sqlServerTask.getUser().isPresent()) {
                throw new ConfigException("'user' option is required but not set.");
            }
        }

        return new UrlAndProperties(url, props);
    }
}
