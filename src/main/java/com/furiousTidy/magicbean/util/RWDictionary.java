package com.furiousTidy.magicbean.util;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RWDictionary {

    private final Map<String, String> m = new TreeMap();

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    private final Lock r = rwl.readLock();

    private final Lock w = rwl.writeLock();

    public Set<Map.Entry<String,String>> entrySet(){
        r.lock();
        try {
            return m.entrySet();
        } finally {
            r.unlock();
        }
    }


    public boolean isEmpty(){
        r.lock();
        try {
            return m.isEmpty();
        } finally {
            r.unlock();
        }
    }

    public String get(String key) {

        r.lock();
        try {
            return m.get(key);
        } finally {
            r.unlock();
        }

    }

    public String[] allKeys() {

        r.lock();
        try {
            return (String[]) m.keySet().toArray();
        } finally {
            r.unlock();
        }

    }

    public String put(String key, String value) {

        w.lock();
        try {
            return m.put(key, value);
        } finally {
            w.unlock();
        }

    }

    public boolean contain(String key) {
        w.lock();
        try {
            return m.containsKey(key);
        } finally {
            w.unlock();
        }
    }

    public String remove(String key) {
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