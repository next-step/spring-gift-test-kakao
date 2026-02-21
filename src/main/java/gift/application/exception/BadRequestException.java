package gift.application.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends CustomException {

    public BadRequestException(String clientMessage) {
        super(HttpStatus.BAD_REQUEST, clientMessage);
    }
}
