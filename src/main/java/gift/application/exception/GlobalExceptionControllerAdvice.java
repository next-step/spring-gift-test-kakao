package gift.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ExceptionResponse> handleCustomException(CustomException ex) {

        HttpStatus httpStatus = ex.getHttpStatus();
        String message = ex.getClientMessage();

        ExceptionResponse response = new ExceptionResponse(message);

        return ResponseEntity
                .status(httpStatus)
                .body(response);
    }
}
