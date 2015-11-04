package pl.allegro.tech.hermes.frontend.publishing.trace;

import pl.allegro.tech.hermes.api.TraceInfo;

import javax.servlet.http.HttpServletRequest;

public interface TraceExtractor {

    TraceInfo extractTraceInformation(HttpServletRequest request);
}
