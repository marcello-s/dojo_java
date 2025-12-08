package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SimpleBicycleTest {
    @Test void testSimpleBicycle() {
        SimpleBicycle bike = new SimpleBicycle();
        bike.changeCadence(60);
        bike.speedUp(15);
        bike.changeGear(3);
        
        assertEquals(60, bike.cadence);
        assertEquals(15, bike.speed);
        assertEquals(3, bike.gear);
        
        bike.applyBrakes(5);
        
        assertEquals(10, bike.speed);
    }
}
