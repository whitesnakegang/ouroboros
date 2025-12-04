# AspectJ Method Tracing Setup Guide

Ouroboros의 AspectJ 모드를 사용하면 Spring AOP의 제약 없이 모든 메서드를 트레이싱할 수 있습니다.

## 왜 AspectJ 모드를 사용하나요?

**Spring AOP (기본 모드)**는 다음을 트레이싱할 수 없습니다:
- Self-invocation (같은 클래스 내부 메서드 호출)
- Private 메서드
- Final Class의 메서드
- Static 메서드

**AspectJ 모드**를 사용하면:
- 모든 메서드 트레이싱 가능
- Compile-time weaving으로 런타임 오버헤드 감소

## 빠른 시작 (Gradle)

### 1. build.gradle 설정

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.6'
    id 'io.spring.dependency-management' version '1.1.7'

    // AspectJ plugin 추가
    id 'io.freefair.aspectj.post-compile-weaving' version '8.11'
}

dependencies {
    implementation 'io.github.whitesnakegang:ouroboros:1.1.0'

    // AspectJ 설정 (2줄만 추가하면 됩니다!)
    aspect 'io.github.whitesnakegang:ouroboros:1.1.0'
    aspect 'org.aspectj:aspectjweaver:1.9.22'

    // ... 나머지 dependencies
}
```

### 2. application.properties 설정

```properties
ouroboros.method-tracing.enabled=true
ouroboros.method-tracing.allowed-packages=com.example.service, com.example.repository

# AspectJ 모드 활성화
ouroboros.method-tracing.mode=ASPECTJ  # mode 설정 추가
```

### 3. 빌드 및 실행

```bash
./gradlew clean build
./gradlew bootRun
```

## 동작 확인

애플리케이션 시작 시 다음 로그가 보이면 성공입니다:

```
AspectJ Compile-Time Weaving Mode Enabled for Method Tracing

Mode: ASPECTJ (Compile-Time Weaving)
Successfully retrieved AspectJ singleton instance via aspectOf()
```

## Maven 사용자

```xml
<dependencies>
    <dependency>
        <groupId>io.github.whitesnakegang</groupId>
        <artifactId>ouroboros</artifactId>
        <version>1.1.0</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>dev.aspectj</groupId>
            <artifactId>aspectj-maven-plugin</artifactId>
            <version>1.14</version>
            <configuration>
                <complianceLevel>17</complianceLevel>
                <aspectLibraries>
                    <aspectLibrary>
                        <groupId>io.github.whitesnakegang</groupId>
                        <artifactId>ouroboros</artifactId>
                    </aspectLibrary>
                </aspectLibraries>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## 문제 해결

### "aspectOf() method not found" 경고

**원인**: AspectJ weaving 설정 누락

**해결**:
1. `build.gradle`에 plugin 추가 확인:
   ```gradle
   id 'io.freefair.aspectj.post-compile-weaving' version '8.11'
   ```

2. `aspect` 설정 확인:
   ```gradle
   aspect 'io.github.whitesnakegang:ouroboros:1.1.0'
   aspect 'org.aspectj:aspectjweaver:1.9.22'
   ```

3. Clean build 실행:
   ```bash
   ./gradlew clean build
   ```

### 메서드가 트레이싱되지 않음

1. **Try 요청 헤더 확인**:
   ```bash
   curl -H "X-Ouroboros-Try: true" http://localhost:8080/api/test
   ```

2. **패키지 설정 확인**:
   ```properties
   # 패키지명이 정확한지 확인
   ouroboros.method-tracing.allowed-packages[0]=com.example
   ```

3. **Debug 로깅 활성화**:
   ```properties
   logging.level.kr.co.ouroboros=DEBUG
   ```

## 테스트 방법

```bash
# 1. Try 요청 보내기
curl -H "X-Ouroboros-Try: true" http://localhost:8080/api/test

# 응답 헤더에서 tryId 확인:
# X-Ouroboros-Try-Id: 550e8400-e29b-41d4-a716-446655440000

# 2. 트레이스 조회
curl http://localhost:8080/ouro/tries/550e8400-e29b-41d4-a716-446655440000
```

### AspectJ 모드 트레이싱 결과 예시

#### 테스트 코드 예시: Self-Invocation과 Private 메서드

다음은 AspectJ 모드에서 Self-invocation과 Private 메서드를 테스트하는 실제 코드입니다:

```java
@RestController
@RequestMapping("/api/aop-test")
public class AopTestController {

    private final AopTestService aopTestService;

    @GetMapping("/self-invocation")
    public ResponseEntity<String> testSelfInvocation() {
        aopTestService.externalCall();
        // Self-invocation: 같은 클래스 내부 메서드 직접 호출
        aopTestService.callInternalPublicMethod();
        aopTestService.callPrivateMethod();
        return ResponseEntity.ok("Self-invocation test completed");
    }
}

@Service
public class AopTestService {

    // 외부에서 호출되는 메서드
    public void externalCall() {
        System.out.println("External call");
    }

    // Self-invocation으로 호출되는 public 메서드
    public void callInternalPublicMethod() {
        internalPublicMethod();
    }

    public void internalPublicMethod() {
        System.out.println("Internal public method");
    }

    // Private 메서드 호출
    public void callPrivateMethod() {
        privateMethod();
    }

    // Private 메서드
    private void privateMethod() {
        System.out.println("Private method");
    }
}
```

![Spring AOP CGLIB 제약사항](../images/scrennshots/proxy-self-invocation.png)
*Spring AOP 모드에서는 Self-invocation, private 메서드가 트레이스에서 누락됨*

위 이미지에서 확인할 수 있듯이:
- ✅ `AopTestController.testSelfInvocation` (컨트롤러 메서드)
- ✅ `AopTestService.externalCall` (외부 호출)
- ✅ `AopTestService.callInternalPublicMethod` (Self-invocation)
- ❌ `AopTestService.internalPublicMethod` (내부 public 메서드 - **트레이스 누락**)
- ✅ `AopTestService.callPrivateMethod` (Private 메서드 호출)
- ❌ `AopTestService.privateMethod` (Private 메서드 - **트레이스 누락**)

Spring AOP 모드에서는 Self-invocation, private 메서드를 트레이싱할 수 없습니다.

![AspectJ Self-Invocation 트레이싱](../images/scrennshots/aspectj-self-invocation.png)
*AspectJ 모드에서 Self-invocation과 Private 메서드가 모두 트레이싱되는 예시*

위 이미지에서 확인할 수 있듯이:
- ✅ `AopTestController.testSelfInvocation` (컨트롤러 메서드)
- ✅ `AopTestService.externalCall` (외부 호출)
- ✅ `AopTestService.callInternalPublicMethod` (Self-invocation)
- ✅ `AopTestService.internalPublicMethod` (내부 public 메서드)
- ✅ `AopTestService.callPrivateMethod` (Private 메서드 호출)
- ✅ `AopTestService.privateMethod` (Private 메서드)

**AspectJ 모드를 사용하면** Self-invocation, private 메서드를 트레이싱할 수 있습니다!

## 비교: Spring AOP vs AspectJ

| 기능 | Spring AOP | AspectJ |
|------|-----------|---------|
| Self-invocation | 불가 | 가능 |
| Private 메서드 | 불가 | 가능 |
| Static 메서드 | 불가 | 가능 |
| 설정 복잡도 | 간단 | 약간 복잡 |
| 빌드 시간 | 빠름 | 약간 느림 |
| 런타임 성능 | Proxy 오버헤드 | 오버헤드 거의 없음 |

### Spring AOP(CGLIB)의 제약사항 예시

#### 테스트 코드 예시: Final class, Static 메서드

다음은 Spring AOP 모드의 Final class, Static 메서드 제약사항을 테스트하는 실제 코드입니다:

```java
@RestController
@RequestMapping("/api/aop-test")
public class AopTestController {

    private FinalClassService finalClassService;
    private final StaticMethodService staticMethodService;


    @GetMapping("/cglib-limitations")
    public ResponseEntity<String> testCglibLimitations() {
        finalClassService.finalClassMethod();
        staticMethodService.instanceMethod();
        return ResponseEntity.ok("CGLIB limitations test completed");
    }
}

@Service
public final class FinalClassService {

    public void finalClassMethod() {
        System.out.println("Final class method called");
    }
}

@Service
public class StaticMethodService {

    // 인스턴스 메서드 (트레이싱 가능)
    public void instanceMethod() {
        System.out.println("Instance method called");

        // Static 메서드 호출 (Spring AOP에서는 트레이싱 불가)
        staticMethod();
    }

    // Static 메서드 (Spring AOP에서는 트레이싱 불가!)
    public static void staticMethod() {
        System.out.println("Static method called");
    }
}
```

![Spring AOP CGLIB 제약사항](../images/scrennshots/proxy-cglib-limitations.png)
*Spring AOP 모드에서는 Final class, Static 메서드가 트레이스에서 누락됨*

위 이미지에서 확인할 수 있듯이:
- ✅ `AopTestController.testCglibLimitations` (컨트롤러 메서드)
- ✅ `StaticMethodService.instanceMethod` (인스턴스 메서드)
- ❌ `FinalClassService.finalClassMethod` (Final 메서드 - **proxy 생성 실패 (상속 실패 Error로 서버 종료)**)
- ❌ `StaticMethodService.staticMethod` (Static 메서드 - **트레이스 누락**)

Spring AOP 모드에서는 Final class, Static 메서드를 트레이싱할 수 없습니다.

![AspectJ GCLIB 제약사항 트레이싱](../images/scrennshots/aspectj-cglib-limitations.png)
*AspectJ 모드에서 Final과 Static 메서드가 모두 트레이싱 되는 예시*

위 이미지에서 확인할 수 있듯이:
- ✅ `AopTestController.testCglibLimitations` (컨트롤러 메서드)
- ✅ `StaticMethodService.instanceMethod` (인스턴스 메서드)
- ✅ `FinalClassService.finalClassMethod` (Final 메서드)
- ✅ `StaticMethodService.staticMethod` (Static 메서드)

**AspectJ 모드를 사용하면** Self-invocation, Private, Final class, Static 메서드를 모두 트레이싱할 수 있습니다!

## 더 알아보기

- [Ouroboros GitHub](https://github.com/whitesnakegang/ouroboros)
- [AspectJ 공식 문서](https://www.eclipse.org/aspectj/)

---

**문제가 해결되지 않으면**: [GitHub Issues](https://github.com/whitesnakegang/ouroboros/issues)에 문의해주세요.
