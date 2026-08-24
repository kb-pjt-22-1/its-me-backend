package site.benepay.common.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * ExecutionTimeAspect/LoggingAspect가 공유하는 포인트컷. 둘 다 같은 대상(컨트롤러 전체)에
 * 걸리므로 각자 따로 선언해두면 한쪽만 바뀌고 다른 쪽은 안 바뀌는 드리프트가 생기기 쉽다.
 */
@Aspect
public class ControllerPointcuts {

	/**
	 * {@code site.benepay.domain} 하위 모든 컨트롤러의 모든 메서드를 대상으로 한다.
	 */
	@Pointcut("execution(* site.benepay.domain..controller..*.*(..))")
	public void controllerMethods() {
	}
}
