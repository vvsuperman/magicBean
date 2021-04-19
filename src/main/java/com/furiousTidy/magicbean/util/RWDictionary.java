package com.furiousTidy.magicbean.util;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RWDictionary {

    private final Map<Long, String> m = new TreeMap<Long, String>();

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    private final Lock r = rwl.readLock();

    private final Lock w = rwl.writeLock();


    public boolean isEmpty(){
        r.lock();
        try {
            return m.isEmpty();
        } finally {
            r.unlock();
        }
    }

    public String get(Long key) {

        r.lock();
        try {
            return m.get(key);
        } finally {
            r.unlock();
        }

    }

    public Long[] allKeys() {

        r.lock();
        try {
            return (Long[]) m.keySet().toArray();
        } finally {
            r.unlock();
        }

    }

    public String put(Long key, String value) {

        w.lock();
        try {
            return m.put(key, value);
        } finally {
            w.unlock();
        }

    }

    public boolean contain(Long key) {
        w.lock();
        try {
            return m.containsKey(key);
        } finally {
            w.unlock();
        }
    }

    public String remove(Long key) {
        w.lock();
        try {
            return m.remove(key);
        } finally {
            w.unlock();
        }
    }


    public void clear() {

        w.lock();
        try {
            m.clear();
        } finally {
            w.unlock();
        }

    }
}