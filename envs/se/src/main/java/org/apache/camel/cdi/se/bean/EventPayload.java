package org.apache.camel.cdi.se.bean;

public class EventPayload<T> {

    private final T payload;

    public EventPayload(T payload) {
        this.payload = payload;
    }

    public T getPayload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        EventPayload that = (EventPayload) o;
        return payload.equals(that.payload);
    }

    @Override
    public int hashCode() {
        return payload.hashCode();
    }
}
