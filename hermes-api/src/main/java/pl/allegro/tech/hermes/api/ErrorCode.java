package pl.allegro.tech.hermes.api;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.*;

public enum ErrorCode {
    TIMEOUT(REQUEST_TIMEOUT),
    TOPIC_ALREADY_EXISTS(BAD_REQUEST),
    TOPIC_NOT_EXISTS(NOT_FOUND),
    GROUP_NOT_EXISTS(NOT_FOUND),
    SUBSCRIPTION_NOT_EXISTS(BAD_REQUEST),
    SUBSCRIPTION_ALREADY_EXISTS(BAD_REQUEST),
    VALIDATION_ERROR(BAD_REQUEST),
    INTERNAL_ERROR(INTERNAL_SERVER_ERROR),
    FORMAT_ERROR(BAD_REQUEST),
    GROUP_NOT_EMPTY(FORBIDDEN),
    TOPIC_NOT_EMPTY(FORBIDDEN),
    GROUP_ALREADY_EXISTS(BAD_REQUEST),
    OPERATION_DISABLED(NOT_ACCEPTABLE),
    OTHER(INTERNAL_SERVER_ERROR),
    UNAVAILABLE_RATE(BAD_REQUEST),
    SINGLE_MESSAGE_READER_EXCEPTION(INTERNAL_SERVER_ERROR),
    PARTITIONS_NOT_FOUND_FOR_TOPIC(NOT_FOUND),
    OFFSET_NOT_FOUND_EXCEPTION(NOT_FOUND),
    OFFSETS_NOT_AVAILABLE_EXCEPTION(INTERNAL_SERVER_ERROR),
    BROKERS_CLUSTER_NOT_FOUND_EXCEPTION(NOT_FOUND),
    SIMPLE_CONSUMER_POOL_EXCEPTION(INTERNAL_SERVER_ERROR),
    RETRANSMISSION_EXCEPTION(INTERNAL_SERVER_ERROR),
    TOKEN_NOT_PROVIDED(FORBIDDEN),
    GROUP_NOT_PROVIDED(FORBIDDEN),
    AUTH_ERROR(FORBIDDEN),
    SCHEMA_COULD_NOT_BE_LOADED(INTERNAL_SERVER_ERROR),
    SCHEMA_COULD_NOT_BE_SAVED(INTERNAL_SERVER_ERROR),
    SCHEMA_COULD_NOT_BE_DELETED(INTERNAL_SERVER_ERROR),
    AVRO_SCHEMA_REMOVAL_DISABLED(BAD_REQUEST),
    SUBSCRIPTION_ENDPOINT_ADDRESS_CHANGE_EXCEPTION(INTERNAL_SERVER_ERROR),
    OAUTH_PROVIDER_NOT_EXISTS(NOT_FOUND),
    OAUTH_PROVIDER_ALREADY_EXISTS(BAD_REQUEST),
    SUPPORT_TEAMS_COULD_NOT_BE_LOADED(INTERNAL_SERVER_ERROR),
    FILTER_VALIDATION_EXCEPTION(BAD_REQUEST);

    private final int httpCode;

    private ErrorCode(Response.Status httpCode) {
        this.httpCode = httpCode.getStatusCode();
    }

    public int getHttpCode() {
        return httpCode;
    }
}
