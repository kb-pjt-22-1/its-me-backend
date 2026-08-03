package site.benepay.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 컨트롤러 실행 소요시간 계산용 공통 Aspect
 */
@Slf4j
@Aspect
@Component
public class ExecutionTimeAspect {

    /**
     * {@code site.benepay.domain} 하위 모든 컨트롤러의 모든 메서드를 대상으로 한다.
     */
    @Pointcut("execution(* site.benepay.domain..controller..*.*(..))")
    public void controllerMethods() {
    }

    /**
     * 메서드 실행 소요시간을 측정 <p>
     * try/finally로 감싸서, 예외가 나도 소요시간은 남기고
     * 예외 자체는 삼키지 않은 채 그대로 던져 {@code GlobalExceptionHandler}로 흘러가게 한다.
     *
     * @param joinPoint 감쌀 호출 지점
     * @return 원래 메서드의 반환값 (가로채기 전과 동일하게 그대로 전달)
     * @throws Throwable 원래 메서드가 던진 예외를 그대로 재전파
     */
    @Around("controllerMethods()")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedMs = System.currentTimeMillis() - start;
            log.info("[TIME] {}.{}() {}ms",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    elapsedMs);
        }
    }
}
