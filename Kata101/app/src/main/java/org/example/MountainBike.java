package org.example;

public class MountainBike extends SimpleBicycle {
    int seatHeight;

    public MountainBike(int startHeight, int startCadence,
                        int startSpeed, int startGear) {
        super();
        seatHeight = startHeight;
        changeCadence(startCadence);
        speed = startSpeed;
        gear = startGear;
    }

    void setHeight(int newValue) {
        seatHeight = newValue;
    }

    @Override
    void printStates() {
        super.printStates();
        System.out.println("seat height:" + seatHeight);
    }
    
}
