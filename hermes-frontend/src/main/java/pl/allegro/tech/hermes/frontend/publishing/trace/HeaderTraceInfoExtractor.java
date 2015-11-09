package pl.allegro.tech.hermes.frontend.publishing.trace;

import pl.allegro.tech.hermes.api.TraceInfo;
import pl.allegro.tech.hermes.common.http.MessageMetadataHeaders;

import javax.servlet.http.HttpServletRequest;

import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.PARENT_SPAN_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SPAN_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TRACE_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TRACE_REPORTED;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TRACE_SAMPLED;

public class HeaderTraceInfoExtractor implements TraceExtractor {

    @Override
    public TraceInfo extractTraceInformation(HttpServletRequest request) {

        return TraceInfo.builder()
                .withTraceId(getHeaderValue(request, TRACE_ID))
                .withSpanId(getHeaderValue(request, SPAN_ID))
                .withParentSpanId(getHeaderValue(request, PARENT_SPAN_ID))
                .withTraceSampled(getHeaderValue(request, TRACE_SAMPLED))
                .withTraceReported(getHeaderValue(request, TRACE_REPORTED))
                .build();
    }

    private static String getHeaderValue(HttpServletRequest request, MessageMetadataHeaders header) {
        return request.getHeader(header.getName());
    }
}
