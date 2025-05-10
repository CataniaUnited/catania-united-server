package com.example.cataniaunited;

/**
 * Represents a subject (publisher) in the Observer pattern.
 * Publishers maintain a list of subscribers and provide methods
 * to manage these subscribers and notify them of changes or events.
 *
 * @param <T> The type of Subscribers that can subscribe to this publisher
 * @param <N> The type of notification data this publisher will send via
 *            the 'notifySubscribers' method.
 */
public interface Publisher<T extends Subscriber<N>, N> {

    /**
     * Registers (adds) a new subscriber to the publisher's list.
     *
     * @param subscriber The subscriber instance to add. must be compatible with both the Publisher's
     * Subscriber type T and notification type N (i.e., Subscriber<T, N>).
     * The added subscriber will receive future notifications of type N.
     *                   Must implement Subscriber<T, N>.
     */
    void addSubscriber(T subscriber);

    /**
     * Unregisters (removes) an existing subscriber from the publisher's list.
     * The removed subscriber will no longer receive notifications.
     *
     * @param subscriber The subscriber instance to remove.
     *                   Must implement Subscriber<T, N>.
     */
    void removeSubscriber(T subscriber);

    /**
     * Notifies all currently registered subscribers about an event or state change.
     *
     * @param notification The data to send to each subscriber.
     */
    void notifySubscribers(N notification);

}