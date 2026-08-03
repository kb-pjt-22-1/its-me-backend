package site.benepay.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 컨트롤러 로깅용 공통 Aspect
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * {@code site.benepay.domain} 하위 모든 컨트롤러의 메서드를 대상
     */
    @Pointcut("execution(* site.benepay.domain..controller..*.*(..))")
    public void controllerMethods() {
    }

    /**
     * 전처리 <p>
     * 컨트롤러 메서드 실행 전에 어떤 메서드가 어떤 인자로 호출됐는지 남긴다.
     * @param joinPoint 가로챈 호출 지점 (메서드 시그니처, 인자 접근용)
     */
    @Before("controllerMethods()")
    public void logBefore(JoinPoint joinPoint) {
        log.info(">>> {}.{}() args={}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                Arrays.toString(joinPoint.getArgs()));
    }

    /**
     * 후처리 <p>
     * 정상적으로 반환됐을 때 결과를 남긴다.
     * @param joinPoint 가로챈 호출 지점
     * @param result    메서드가 반환한 값
     */
    @AfterReturning(pointcut = "controllerMethods()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("<<< {}.{}() result={}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                result);
    }

    /**
     * 후처리 <p>
     * 예외 발생 시 (정상 반환이 아니라서 {@link #logAfterReturning}은 안 걸림).
     * @param joinPoint 가로챈 호출 지점
     * @param ex        메서드 실행 중 던져진 예외
     */
    @AfterThrowing(pointcut = "controllerMethods()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable ex) {
        log.warn("!!! {}.{}() threw {}: {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                ex.getClass().getSimpleName(),
                ex.getMessage());
    }
}
