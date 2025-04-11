package com.example.cataniaunited;

/**
 * Interface of an observer (subscriber) in the Observer pattern.
 * Can subscribe to Publisher and receive notifications.
 *
 * @param <T> The type of Object this is
 * @param <N> The type of notification data this subscriber expects to receive
 *            in its 'update' method.
 */
public interface Subscriber<T, N> {

    /**
     * This method is called by the Publisher when a notification should to be sent.
     *
     * @param notification The notification data passed from the Publisher.
     *                     Its type is defined by the generic parameter N.
     */
    void update(N notification);

}