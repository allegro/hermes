package pl.allegro.tech.hermes.frontend.publishing;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;

public class ErrorSender {
    private ObjectMapper objectMapper;

    public ErrorSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void sendErrorResponse(ErrorDescription error, HttpServletResponse response, String messageId)
            throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON);
        response.setStatus(error.getCode().getHttpCode());
        response.setHeader(MESSAGE_ID.getName(), messageId);
        objectMapper.writeValue(response.getWriter(), error);
    }

    public void sendErrorResponseQuietly(ErrorDescription error, HttpServletResponse response, String messageId) {
        try {
            sendErrorResponse(error, response, messageId);
        } catch (IOException e) {
            throw new InternalProcessingException(e);
        }
    }
}
