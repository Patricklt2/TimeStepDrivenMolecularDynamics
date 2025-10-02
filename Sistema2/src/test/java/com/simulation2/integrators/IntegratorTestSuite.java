package com.simulation2.integrators;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Suite de tests completa para todos los integradores
 */
@Suite
@SelectClasses({
    VelocityVerletTest.class,
    BeemanTest.class,
    IntegratorComparisonTest.class,
    EnergyConservationTest.class
})
public class IntegratorTestSuite {
    // Esta clase está vacía, sirve como holder para la suite de tests
}