package gift.cucumber;

import gift.model.Gift;
import gift.model.GiftDelivery;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class StubGiftDelivery implements GiftDelivery {

    private boolean shouldFail;
    private Gift lastDeliveredGift;
    private int deliveryCount;

    @Override
    public void deliver(Gift gift) {
        if (shouldFail) {
            throw new RuntimeException("배송 시스템 장애");
        }
        lastDeliveredGift = gift;
        deliveryCount++;
    }

    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    public Gift getLastDeliveredGift() {
        return lastDeliveredGift;
    }

    public int getDeliveryCount() {
        return deliveryCount;
    }

    public void reset() {
        shouldFail = false;
        lastDeliveredGift = null;
        deliveryCount = 0;
    }
}
