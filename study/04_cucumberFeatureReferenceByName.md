# Feature 파일에서 Id 제거 & 이름 기반 참조 적용

## 개요

- 기술적 배경 없이도 요구사항을 이해할 수 있도록 Feature 파일에 존재하는 Id 값 제거
- 이름 기반으로 엔티티를 참조할 수 있도록 구성

## 변경 전후 비교

### Feature 

- Before

```Gherkin
Given 회원 "보내는사람"(ID: 1)과 "받는사람"(ID: 2)이 존재한다
When 회원 1이 옵션 1을 3개 회원 2에게 "선물" 메시지와 함께 선물하면
```

- After
```Gherkin
Given 회원 "보내는사람"과 "받는사람"이 존재한다
When "보내는사람"이 옵션 "기본"을 3개 "받는사람"에게 "선물" 메시지와 함께 선물하면 
```

### ScenarioContext.java

```java
// 추가
private final Map<String, Long> ids = new HashMap<>();
```

### Step Definition (Given)

```java
// "보내는사람" -> 1
scenarioContext.storeId(name1, id1);
```

### Step Definition (When)

```java
long senderId = scenarioContext.getId(senderName);
```
