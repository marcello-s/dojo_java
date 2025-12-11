/*
 * The MIT License, Copyright (c) 2011-2025 Marcel Schneider
 * for details see License.txt
 */
package org.example.KataSpringBoot;

import org.springframework.stereotype.Component;

@Component
public class GreetingService {

    public String greet(String name) {
        return String.format("Hello %s!", name);
    }
}
