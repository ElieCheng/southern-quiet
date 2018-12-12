package com.ai.southernquiet.notification;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

/**
 * 发布通知。
 */
public interface NotificationPublisher<N> {
    /**
     * 发布通知。被{@link NotificationSource}标注的通知将通过其指定的源发送。
     */
    @SuppressWarnings("unchecked")
    default void publish(N notification) {
        publish(notification, getNotificationSource((Class<N>) notification.getClass()));
    }

    /**
     * 按指定的源发布通知。
     */
    void publish(N notification, String source);

    @SuppressWarnings("ConstantConditions")
    default String getNotificationSource(Class<N> cls) {
        NotificationSource annotation = AnnotationUtils.getAnnotation(cls, NotificationSource.class);
        return null == annotation || StringUtils.isEmpty(annotation.source()) ? cls.getName() : annotation.source();
    }
}