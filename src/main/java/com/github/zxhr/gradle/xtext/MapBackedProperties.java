package com.github.zxhr.gradle.xtext;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

class MapBackedProperties extends Properties {

    private final Map<String, String> map;

    MapBackedProperties(Map<String, String> map) {
        this.map = map;
    }

    @Override
    public synchronized Object setProperty(String key, String value) {
        return map.put(key, value);
    }

    @Override
    public String getProperty(String key) {
        return map.get(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }

    @Override
    public Enumeration<?> propertyNames() {
        return Collections.enumeration(map.keySet());
    }

    @Override
    public Set<String> stringPropertyNames() {
        return map.keySet();
    }

    @Override
    public synchronized int size() {
        return map.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return map.isEmpty();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public synchronized Enumeration<Object> keys() {
        return (Enumeration) Collections.enumeration(map.keySet());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public synchronized Enumeration<Object> elements() {
        return (Enumeration) Collections.enumeration(map.values());
    }

    @Override
    public synchronized boolean contains(Object value) {
        return map.containsValue(value);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public synchronized Object get(Object key) {
        return map.get(key);
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        return map.put((String) key, (String) value);
    }

    @Override
    public synchronized Object remove(Object key) {
        return map.remove(key);
    }

    @Override
    public synchronized void putAll(Map<? extends Object, ? extends Object> t) {
        for (Entry<? extends Object, ? extends Object> entry : t.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public synchronized void clear() {
        map.clear();
    }

    @Override
    public synchronized Object clone() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Set<Object> keySet() {
        return (Set) map.keySet();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Set<java.util.Map.Entry<Object, Object>> entrySet() {
        return (Set) map.entrySet();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Collection<Object> values() {
        return (Collection) map.values();
    }

    @Override
    public synchronized Object getOrDefault(Object key, Object defaultValue) {
        return map.getOrDefault(key, (String) defaultValue);
    }

    @Override
    public synchronized void forEach(BiConsumer<? super Object, ? super Object> action) {
        map.forEach(action);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public synchronized void replaceAll(BiFunction<? super Object, ? super Object, ? extends Object> function) {
        map.replaceAll((BiFunction) function);
    }

    @Override
    public synchronized Object putIfAbsent(Object key, Object value) {
        return map.putIfAbsent((String) key, (String) value);
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        return map.remove(key, value);
    }

    @Override
    public synchronized boolean replace(Object key, Object oldValue, Object newValue) {
        return map.replace((String) key, (String) oldValue, (String) newValue);
    }

    @Override
    public synchronized Object replace(Object key, Object value) {
        return map.replace((String) key, (String) value);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public synchronized Object computeIfAbsent(Object key, Function<? super Object, ? extends Object> mappingFunction) {
        return map.computeIfAbsent((String) key, (Function) mappingFunction);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public synchronized Object computeIfPresent(Object key,
            BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        return map.computeIfPresent((String) key, (BiFunction) remappingFunction);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public synchronized Object compute(Object key,
            BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        return map.compute((String) key, (BiFunction) remappingFunction);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public synchronized Object merge(Object key, Object value,
            BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        return map.merge((String) key, (String) value, (BiFunction) remappingFunction);
    }

    @Override
    public void store(Writer writer, String comments) throws IOException {
        super.store(new DateIgnoringWriter(writer), comments);
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        super.store(new DateIgnoringWriter(new OutputStreamWriter(out, "8859_1")), comments);
    }

    private static class DateIgnoringWriter extends BufferedWriter {

        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("#EEE MMM dd HH:mm:ss zzz yyyy");

        public DateIgnoringWriter(Writer out) {
            super(out);
        }

        @Override
        public void write(String s, int off, int len) throws IOException {
            try {
                DATE_FORMAT.parse(s.substring(off, off + len));
            } catch (ParseException e) {
                super.write(s, off, len);
            }
        }

    }

}
