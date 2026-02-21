package gift.application.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends CustomException {

    public NotFoundException(String clientMessage) {
        super(HttpStatus.NOT_FOUND, clientMessage);
    }
}
