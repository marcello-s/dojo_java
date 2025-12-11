package org.example;

import java.util.*;

public class Wildcard {

    public void demo() {
        var intList = Arrays.asList(1, 2, 3);
        var doubleList = Arrays.asList(1.1, 2.2, 3.3);

        enumerateUpperBoundList(intList);
        enumerateUpperBoundList(doubleList);

        enumerateLowerBoundList(intList);
    }

    private void enumerateUpperBoundList(List<? extends Number> numbers) {
        for (var number : numbers) {
            System.out.println(number);
        }
    }

    private void enumerateLowerBoundList(List<? super Integer> numbers) {
        for (var number : numbers) {
            System.out.println(number);
        }
    }
}
