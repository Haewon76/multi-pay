package com.mallowlink.common.logger;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.ConsoleAppender;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import static com.mallowlink.common.utils.MaskUtil.maskSensitiveData;

@Slf4j
public class MaskingLoggingAppender extends ConsoleAppender<ILoggingEvent> {


    @Override
    protected void append(ILoggingEvent event) {
        // 로깅 컨텍스트 및 로거 가져오기
        LoggerContext loggerContext = (LoggerContext) this.getContext();
        Logger logger = loggerContext.getLogger(event.getLoggerName());

        // 트레이스 정보 추가
        addTraceInformation();

        // 메시지 마스킹 처리
        String originalMessage = event.getFormattedMessage();
        String maskedMessage = maskSensitiveData(originalMessage);

        // 마스킹된 이벤트 생성 및 출력
        ILoggingEvent maskedEvent = createMaskedEvent(event, logger, maskedMessage);
        super.append(maskedEvent);
    }

    /**
     * 현재 스팬에서 트레이스 정보를 추출하여 MDC에 추가
     */
    private void addTraceInformation() {
        SpanContext context = Span.current().getSpanContext();
        if (context.isValid()) {
            MDC.put("trace_id", context.getTraceId());
            MDC.put("span_id", context.getSpanId());
            MDC.put("trace_flags", context.getTraceFlags().asHex());
        }
    }

    /**
     * 원본 로깅 이벤트에서 마스킹된 새 이벤트 생성
     */
    private ILoggingEvent createMaskedEvent(ILoggingEvent originalEvent, Logger logger, String maskedMessage) {
        // Throwable 추출
        Throwable throwable = extractThrowable(originalEvent);

        // 새 로깅 이벤트 생성
        LoggingEvent maskedEvent = new LoggingEvent(
                originalEvent.getLoggerName(),
                logger,
                originalEvent.getLevel(),
                maskedMessage,
                throwable,
                originalEvent.getArgumentArray()
        );

        // 호출자 데이터 복사
        maskedEvent.setCallerData(originalEvent.getCallerData());

        return maskedEvent;
    }

    /**
     * 로깅 이벤트에서 Throwable 객체 추출
     */
    private Throwable extractThrowable(ILoggingEvent event) {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy instanceof ThrowableProxy) {
            return ((ThrowableProxy) throwableProxy).getThrowable();
        }
        return null;
    }
}
