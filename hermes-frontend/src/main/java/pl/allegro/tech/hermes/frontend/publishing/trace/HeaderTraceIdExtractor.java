package pl.allegro.tech.hermes.frontend.publishing.trace;

import pl.allegro.tech.hermes.api.TraceInfo;
import pl.allegro.tech.hermes.common.http.MessageMetadataHeaders;

import javax.servlet.http.HttpServletRequest;

public class HeaderTraceIdExtractor implements TraceExtractor {

    @Override
    public TraceInfo extractTraceInformation(HttpServletRequest request) {
        return new TraceInfo(request.getHeader(MessageMetadataHeaders.TRACE_ID.getName()));
    }
}
