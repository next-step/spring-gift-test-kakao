# AI 활용 기록

## 사용 도구
- Claude Code (Anthropic Claude)

## 활용 내역

### 1. 프로젝트 구조 분석
- AI를 통해 프로젝트의 엔티티, 컨트롤러, 서비스, 리포지토리 구조를 탐색
- 컨트롤러의 `@RequestBody` 유무에 따른 파라미터 바인딩 방식 차이 발견

### 2. 테스트 전략 수립
- 검증할 External Behavior 6개를 우선순위에 따라 선정
- 테스트 격리, 데이터 준비, 검증 조합 전략 설계

### 3. 테스트 코드 작성
- `AcceptanceTestBase`: 공통 설정 (포트, DB 클린업)
- `CategoryAcceptanceTest`: 카테고리 생성/조회 행위 검증
- `ProductAcceptanceTest`: 상품 생성/조회 + 에러 계약 검증
- `GiftAcceptanceTest`: 선물 전송 + 재고 차감 + 에러 계약 검증

### 4. AI 기여도
- 코드 구조 분석 및 엔드포인트 파악: AI 주도
- 테스트 전략 설계: AI가 초안 작성, 사용자 검토/승인
- 테스트 코드 구현: AI가 작성, 사용자 검토
- 빌드 및 테스트 실행: AI가 실행 및 디버깅
