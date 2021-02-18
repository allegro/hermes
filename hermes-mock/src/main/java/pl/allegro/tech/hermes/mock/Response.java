package pl.allegro.tech.hermes.mock;

public class Response {
    private final int statusCode;

    private Response(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }


    public static final class Builder {
        private int statusCode = 200;

        private Builder() {
        }

        public static Builder aResponse() {
            return new Builder();
        }

        public Builder withStatusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Response build() {
            return new Response(statusCode);
        }
    }
}
