package gift.application.exception;

import org.springframework.http.HttpStatus;

public abstract class CustomException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String clientMessage;

    public CustomException(HttpStatus httpStatus, String clientMessage) {
        this.httpStatus = httpStatus;
        this.clientMessage = clientMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getClientMessage() {
        return clientMessage;
    }
}
