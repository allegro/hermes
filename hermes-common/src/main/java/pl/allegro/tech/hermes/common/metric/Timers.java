package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.SCHEMA_REPO_TYPE;

public class Timers {

    public static final String SCHEMA = "schema." + SCHEMA_REPO_TYPE;
    public static final String GET_SCHEMA_LATENCY = SCHEMA + ".get-schema";
    public static final String GET_SCHEMA_VERSIONS_LATENCY = SCHEMA + ".get-schema-versions";

}
