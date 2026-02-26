package gift.application;

public record GiveGiftRequest(Long optionId, int quantity, Long receiverId, String message) {
}
