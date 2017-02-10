package org.embulk.input.oracle;

import static org.embulk.input.oracle.OracleTests.execute;
import static org.embulk.test.EmbulkTests.readSortedFile;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.input.OracleInputPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.test.EmbulkTests;
import org.embulk.test.TestingEmbulk;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class BasicTest
{
    private static final String BASIC_RESOURCE_PATH = "org/embulk/input/oracle/test/expect/basic/";

    private static ConfigSource loadYamlResource(TestingEmbulk embulk, String fileName)
    {
        return embulk.loadYamlResource(BASIC_RESOURCE_PATH + fileName);
    }

    private static String readResource(String fileName)
    {
        return EmbulkTests.readResource(BASIC_RESOURCE_PATH + fileName);
    }

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .registerPlugin(InputPlugin.class, "oracle", OracleInputPlugin.class)
            .build();

    private ConfigSource baseConfig;

    @Before
    public void setup() throws Exception
    {
        baseConfig = OracleTests.baseConfig();
        execute(embulk, readResource("setup.sql")); // setup rows
    }

    @Test
    public void test() throws Exception
    {
        Path out1 = embulk.createTempFile("csv");
        TestingEmbulk.RunResult result1 = embulk.runInput(baseConfig.merge(loadYamlResource(embulk, "test_config.yml")), out1);
        assertThat(readSortedFile(out1), is(readResource("test_expected.csv")));
        assertThat(result1.getConfigDiff(), is((ConfigDiff) loadYamlResource(embulk, "test_expected.diff")));
    }

    @Test
    public void testLower() throws Exception
    {
        Path out1 = embulk.createTempFile("csv");
        TestingEmbulk.RunResult result1 = embulk.runInput(baseConfig.merge(loadYamlResource(embulk, "test_lower_config.yml")), out1);
        assertThat(readSortedFile(out1), is(readResource("test_expected.csv")));
        assertThat(result1.getConfigDiff(), is((ConfigDiff) loadYamlResource(embulk, "test_expected.diff")));
    }

    @Test
    public void testQuery() throws Exception
    {
        Path out1 = embulk.createTempFile("csv");
        TestingEmbulk.RunResult result1 = embulk.runInput(baseConfig.merge(loadYamlResource(embulk, "test_query_config.yml")), out1);
        assertThat(readSortedFile(out1), is(readResource("test_expected.csv")));
        assertThat(result1.getConfigDiff(), is((ConfigDiff) loadYamlResource(embulk, "test_expected.diff")));
    }

    @Test
    public void testQueryLower() throws Exception
    {
        Path out1 = embulk.createTempFile("csv");
        TestingEmbulk.RunResult result1 = embulk.runInput(baseConfig.merge(loadYamlResource(embulk, "test_query_lower_config.yml")), out1);
        assertThat(readSortedFile(out1), is(readResource("test_expected.csv")));
        assertThat(result1.getConfigDiff(), is((ConfigDiff) loadYamlResource(embulk, "test_expected.diff")));
    }

    @Test
    public void testColumnOptions() throws Exception
    {
        Path out1 = embulk.createTempFile("csv");
        TestingEmbulk.RunResult result1 = embulk.runInput(baseConfig.merge(loadYamlResource(embulk, "test_column_options_config.yml")), out1);
        assertThat(readSortedFile(out1), is(readResource("test_column_options_expected.csv")));
        assertThat(result1.getConfigDiff(), is((ConfigDiff) loadYamlResource(embulk, "test_column_options_expected.diff")));
    }

    @Test
    public void testColumnOptionsLower() throws Exception
    {
        Path out1 = embulk.createTempFile("csv");
        TestingEmbulk.RunResult result1 = embulk.runInput(baseConfig.merge(loadYamlResource(embulk, "test_column_options_lower_config.yml")), out1);
        assertThat(readSortedFile(out1), is(readResource("test_column_options_expected.csv")));
        assertThat(result1.getConfigDiff(), is((ConfigDiff) loadYamlResource(embulk, "test_column_options_expected.diff")));
    }

}
