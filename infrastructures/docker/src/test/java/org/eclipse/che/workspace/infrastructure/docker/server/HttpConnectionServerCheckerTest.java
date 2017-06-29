/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.workspace.infrastructure.docker.server;

import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Alexander Garagatyi
 */
@Listeners(MockitoTestNGListener.class)
public class HttpConnectionServerCheckerTest {
    private String MACHINE_NAME = "mach1";
    private String SERVER_REF   = "ref1";

    @Mock
    private Timer             timer;
    @Mock
    private HttpURLConnection conn;

    private HttpConnectionServerChecker checker;

    @BeforeMethod
    public void setUp() throws Exception {
        checker = new HttpConnectionServerChecker(new URL("http://localhost"),
                                                  MACHINE_NAME,
                                                  SERVER_REF,
                                                  1,
                                                  10,
                                                  TimeUnit.SECONDS,
                                                  timer);
    }

    @Test(dataProvider = "successfulResponseCodeProvider")
    public void shouldConfirmConnectionSuccessIfResponseCodeIsBetween200And400(Integer responseCode) throws Exception {
        when(conn.getResponseCode()).thenReturn(responseCode);
        assertTrue(checker.isConnectionSuccessful(conn));
    }

    @DataProvider
    public static Object[][] successfulResponseCodeProvider() {
        return new Object[][]{
                {200},
                {201},
                {210},
                {301},
                {302},
                {303}
        };
    }

    @Test(dataProvider = "nonSuccessfulResponseCodeProvider")
    public void shouldNotConfirmConnectionSuccessIfResponseCodeIsLessThan200Or400OrMore(Integer responseCode) throws Exception {
        when(conn.getResponseCode()).thenReturn(responseCode);
        assertFalse(checker.isConnectionSuccessful(conn));
    }

    @DataProvider
    public static Object[][] nonSuccessfulResponseCodeProvider() {
        return new Object[][]{
                {199},
                {400},
                {401},
                {402},
                {403},
                {404},
                {405},
                {409},
                {500}
        };
    }

    @Test
    public void shouldOpenConnectionToProvided() throws Exception {
    }

    @Test
    public void shouldSetTimeoutsToConnection() throws Exception {
    }

    @Test
    public void shouldBeAbleToConfirmAvailability() throws Exception {
    }

    @Test
    public void shouldBeAbleToRejectAvailability() throws Exception {
    }

    @Test
    public void shouldRejectAvailabilityInCaseOfExceptionOnConnectionOpenning() throws Exception {
    }

    @Test
    public void shouldCloseConnectionIfAvailable() throws Exception {
    }

    @Test
    public void shouldCloseConnectionIfNotAvailable() throws Exception {
    }
}
