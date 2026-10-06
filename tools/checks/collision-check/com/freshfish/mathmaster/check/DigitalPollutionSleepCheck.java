package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.pollution.DigitalPollutionData;

/** Focused arithmetic checks for real-time and sleep-skipped purification. */
final class DigitalPollutionSleepCheck {
    private DigitalPollutionSleepCheck() {
    }

    static int run() {
        DigitalPollutionData data = new DigitalPollutionData();
        data.add(20);
        for (int tick = 0; tick < 1_199; tick++) data.tickInOverworld(0);
        require(data.getValue() == 20, "partial minute reduced pollution early");
        data.tickInOverworld(0);
        require(data.getValue() == 19, "awake minute did not reduce exactly one pollution");

        DigitalPollutionData sleepData = new DigitalPollutionData();
        sleepData.add(100);
        sleepData.advanceSleepTime(10_800L, 0);
        require(sleepData.getValue() == 10, "skipped night did not reduce ninety pollution");

        DigitalPollutionData identityData = new DigitalPollutionData();
        identityData.add(50);
        identityData.advanceSleepTime(24_000L, 1);
        require(identityData.getValue() == 1, "additive identity minimum was ignored");
        DigitalPollutionData wakeData = new DigitalPollutionData();
        wakeData.add(50);
        wakeData.advanceSleepTime(1_200L, 0);
        for (int tick = 0; tick < 1_200; tick++) wakeData.tickInOverworld(0);
        require(wakeData.getValue() == 39, "sleep rate leaked into awake purification");
        return 5;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
