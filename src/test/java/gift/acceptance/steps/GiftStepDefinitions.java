package gift.acceptance.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static gift.acceptance.steps.CommonStepDefinitions.*;
import static org.assertj.core.api.Assertions.assertThat;

public class GiftStepDefinitions {

    @Autowired
    private GiftApiClient api;

    @Autowired
    private CategoryApiClient categoryApi;

    @Autowired
    private ProductApiClient productApi;

    @Given("{string}과 {string} 회원이 등록되어 있다")
    public void 회원이_등록되어_있다(String sender, String receiver) {
        memberIds.put(sender, api.회원을_등록한다(sender, sender + "@test.com"));
        memberIds.put(receiver, api.회원을_등록한다(receiver, receiver + "@test.com"));
    }

    @Given("{string} 옵션의 재고가 {int}개 있다")
    public void 옵션의_재고가_있다(String optionName, int quantity) {
        if (defaultProductId == null) {
            Long categoryId = categoryApi.카테고리를_DB에_등록한다("기본카테고리");
            defaultProductId = productApi.상품을_DB에_등록한다(
                "기본상품", 10000, "http://test.com/img.jpg", categoryId);
        }
        Long optionId = api.옵션을_등록한다(optionName, quantity, defaultProductId);
        optionIds.put(optionName, optionId);
    }

    @When("{string}이 {string} {int}개를 {string}에게 선물한다")
    public void 선물한다(String senderName, String optionName, int quantity, String receiverName) {
        latestResponse = api.선물을_보낸다(
            memberIds.get(senderName),
            optionIds.get(optionName),
            quantity,
            memberIds.get(receiverName));
        latestStatusCode = latestResponse.statusCode();
    }

    @When("{string}이 존재하지 않는 옵션 {int}개를 {string}에게 선물한다")
    public void 존재하지_않는_옵션으로_선물한다(String senderName, int quantity, String receiverName) {
        latestResponse = api.선물을_보낸다(
            memberIds.get(senderName), 999L, quantity, memberIds.get(receiverName));
        latestStatusCode = latestResponse.statusCode();
    }

    @When("{string}이 {string} {int}개를 존재하지 않는 회원에게 선물한다")
    public void 존재하지_않는_회원에게_선물한다(String senderName, String optionName, int quantity) {
        latestResponse = api.선물을_보낸다(
            memberIds.get(senderName), optionIds.get(optionName), quantity, 999L);
        latestStatusCode = latestResponse.statusCode();
    }

    @When("{int}명이 동시에 {string} {int}개씩 선물한다")
    public void 동시에_선물한다(int threadCount, String optionName, int quantity) throws InterruptedException {
        Long senderId = memberIds.values().iterator().next();
        Long receiverId = memberIds.values().stream().skip(1).findFirst().orElseThrow();
        Long optionId = optionIds.get(optionName);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    api.선물을_보낸다(senderId, optionId, quantity, receiverId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        executor.shutdown();
    }

    @Then("{string} 옵션의 재고가 {int}개이다")
    public void 옵션의_재고가_개이다(String optionName, int expectedQuantity) {
        int stock = api.옵션_재고를_조회한다(optionIds.get(optionName));
        assertThat(stock).isEqualTo(expectedQuantity);
    }

    @Then("{string} 옵션의 재고가 {int} 이상이다")
    public void 옵션의_재고가_이상이다(String optionName, int minQuantity) {
        int stock = api.옵션_재고를_조회한다(optionIds.get(optionName));
        assertThat(stock).isGreaterThanOrEqualTo(minQuantity);
    }

}
