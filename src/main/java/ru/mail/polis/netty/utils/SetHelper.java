package ru.mail.polis.netty.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SetHelper {
    public static <T> Set<T> subSet(Set<T> set, int start, int length) {
        Set<T> subSet = new HashSet<>();
        Iterator<T> iterator = set.iterator();
        for (int i = 0; i < start + length; i++) {
            if (iterator.hasNext()) {
                if (i >= start) {
                    subSet.add(iterator.next());
                } else {
                    // continue iterate
                    iterator.next();
                }
            } else {
                // like residue class
                iterator = set.iterator();
                i--;
            }
        }
        return subSet;
    }
}
