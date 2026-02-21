package gift.model;

public record Gift(
        Long from,
        Long to,
        Option option,
        int quantity,
        String message
) {

}
