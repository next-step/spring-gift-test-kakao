# spring-gift-test

## 테스트 실행

### 1단계 인수 테스트 (H2)

```bash
./gradlew test
```

Docker 불필요. H2 인메모리 DB를 사용한다.

### Cucumber 인수 테스트 (Docker PostgreSQL)

Docker 이미지를 빌드하고 Docker Compose로 애플리케이션과 PostgreSQL을 띄운 뒤 테스트를 실행한다.

```bash
./gradlew dockerBuild   # Docker 이미지 빌드
./gradlew dockerUp      # Docker Compose 시작 (postgres + app)
./gradlew cucumberTest  # Cucumber 인수 테스트 실행
./gradlew dockerDown    # Docker Compose 종료
```

**Docker 런타임이 실행 중이어야 한다.**

## Docker 런타임 설정

Docker 관련 Gradle task(`dockerBuild`, `dockerUp`, `dockerDown`)는 아래 순서로 Docker 소켓을 자동 탐색한다:

| 우선순위 | 소켓 경로 | 런타임 |
|---------|----------|--------|
| 0 | `DOCKER_HOST` 환경변수 | 사용자 지정 (최우선) |
| 1 | `~/.docker/run/docker.sock` | Docker Desktop (macOS) |
| 2 | `~/.docker/desktop/docker.sock` | Docker Desktop (Linux) |
| 3 | `~/.colima/default/docker.sock` | Colima |
| 4 | `~/.orbstack/run/docker.sock` | OrbStack |
| 5 | `~/.rd/docker.sock` | Rancher Desktop |
| 6 | `/var/run/docker.sock` | Linux native / symlink |

대부분의 환경에서 Docker 런타임만 실행하면 추가 설정 없이 동작한다.

### 소켓 탐색에 실패하는 경우

자동 탐색 목록에 없는 런타임이거나, 소켓 경로가 다른 경우 `DOCKER_HOST`를 직접 지정한다:

```bash
DOCKER_HOST=unix:///path/to/docker.sock ./gradlew dockerBuild
```
