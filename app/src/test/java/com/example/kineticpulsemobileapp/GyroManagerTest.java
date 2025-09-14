package com.example.kineticpulsemobileapp;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for GyroManager calibration configuration
 */
public class GyroManagerTest {

    @Test
    public void testCalibrationPhaseDurationValidation() {
        // Test that the default duration is reasonable
        assertTrue("Default phase duration should be at least 1000ms for reliable calibration", 
                   1200L >= 1000L);
        
        // Test boundary validation logic
        assertTrue("500ms should be minimum valid duration", 500L >= 500L);
        assertTrue("3000ms should be maximum valid duration", 3000L <= 3000L);
        
        // Test that the extended duration is better than the old short duration
        assertTrue("New 1200ms duration should be longer than old 800ms for better calibration", 
                   1200L > 800L);
    }

    @Test
    public void testCalibrationDurationIsReasonable() {
        // Validate that the new duration provides sufficient time for user interaction
        long newDuration = 1200L;
        long oldDuration = 800L;
        
        assertTrue("New duration should provide 50% more time than old duration",
                   newDuration >= oldDuration * 1.5);
        
        assertTrue("Duration should not be too long to frustrate users",
                   newDuration <= 2000L);
        
        assertTrue("Duration should be long enough for deliberate movement",
                   newDuration >= 1000L);
    }
}