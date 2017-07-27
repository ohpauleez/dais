
package dais;

import java.util.Map;
import java.util.Map.Entry;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentMap;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class Maps {

    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public static <K, U> Collector<Map.Entry<K, U>, ?, Map<K, U>> entriesToMap() {
        return Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue());
    }

    public static <K, U> Collector<Map.Entry<K, U>, ?, ConcurrentMap<K, U>> entriesToConcurrentMap() {
        return Collectors.toConcurrentMap((e) -> e.getKey(), (e) -> e.getValue());
    }

    public static Map mapOf(final Object... kvs) {
        List kvsList = Arrays.asList(kvs);
        List<List> kvPairs = new ArrayList();

        // TODO: Pull this out into a List utils
        int partitionSize = 2;
        for (int i=0; i<kvsList.size(); i += partitionSize) {
            kvPairs.add(kvsList.subList(i, Math.min(i + partitionSize, kvsList.size())));
        }

        return kvPairs.stream().collect(Collectors.toMap(kv -> kv.get(0),
                                                         kv -> kv.get(1)));
    }

    public static <K, V> Map<K,V> put(Map<K,V> m, K key, V value) {
        m.put(key, value);
        return m;
    }

    public static <K, V> V get(Map<K,V> m, K key) {
        return m.get(key);
    }
    public static <K, V> V get(Map<K,V> m, K key, V notFound) {
        if (m.containsKey(key)) {
            return m.get(key);
        }
        return notFound;
    }

}
