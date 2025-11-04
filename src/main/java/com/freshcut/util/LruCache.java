package com.freshcut.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class LruCache<K, V> {
    private static class Entry<V> {
        final V value;
        final long timeMs;
        Entry(V value, long timeMs) { this.value = value; this.timeMs = timeMs; }
    }

    private final LinkedHashMap<K, Entry<V>> map;
    private final int maxSize;
    private final long ttlMs;

    public LruCache(int maxSize, long ttlMs) {
        this.maxSize = Math.max(1, maxSize);
        this.ttlMs = Math.max(0, ttlMs);
        this.map = new LinkedHashMap<>(16, 0.75f, true);
    }

    public synchronized V get(K key) {
        Entry<V> e = map.get(key);
        if (e == null) return null;
        if (ttlMs > 0 && (System.currentTimeMillis() - e.timeMs) > ttlMs) {
            map.remove(key);
            return null;
        }
        return e.value;
    }

    public synchronized void put(K key, V value) {
        map.put(key, new Entry<>(value, System.currentTimeMillis()));
        if (map.size() > maxSize) {
            Iterator<Map.Entry<K, Entry<V>>> it = map.entrySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    public synchronized void clear() { map.clear(); }
}