package chat.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public class ThreadSafeList<T> implements Iterable<T> {
    private final List<T> list;

    public ThreadSafeList() {
        this.list = new CopyOnWriteArrayList<>();
    }

    public ThreadSafeList(Collection<T> initialData) {
        this.list = new CopyOnWriteArrayList<>(initialData);
    }

    public synchronized void add(T element) {
        list.add(element);
    }

    public synchronized boolean remove(T element) {
        return list.remove(element);
    }

    public synchronized T get(int index) {
        if (index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    public synchronized int size() {
        return list.size();
    }

    public synchronized boolean isEmpty() {
        return list.isEmpty();
    }

    public synchronized boolean contains(T element) {
        return list.contains(element);
    }

    public synchronized List<T> toList() {
        return new ArrayList<>(list);
    }

    public synchronized void clear() {
        list.clear();
    }

    public synchronized boolean addAll(Collection<? extends T> collection) {
        return list.addAll(collection);
    }

    public synchronized Iterator<T> iterator() {
        return list.iterator();
    }

    public synchronized List<T> filter(Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T element : list) {
            if (predicate.test(element)) {
                result.add(element);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized T findFirst(Predicate<T> predicate) {
        for (T element : list) {
            if (predicate.test(element)) {
                return element;
            }
        }
        return null;
    }

    public synchronized void removeIf(Predicate<T> predicate) {
        list.removeIf(predicate);
    }
}