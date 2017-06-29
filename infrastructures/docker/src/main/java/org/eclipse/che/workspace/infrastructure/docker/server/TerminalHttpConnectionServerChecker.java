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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

/**
 * This class is used as {@link ServerChecker} for terminal server as it doesn't have
 * an endpoint that responds with 200 and can be used with default {@link HttpConnectionServerChecker}
 *
 * @author Alexander Garagatyi
 */
// TODO add readiness endpoint to terminal and remove this class
class TerminalHttpConnectionServerChecker extends HttpConnectionServerChecker {
    public TerminalHttpConnectionServerChecker(URL url,
                                               String machineName,
                                               String serverRef,
                                               long period,
                                               long timeout,
                                               TimeUnit timeUnit,
                                               Timer timer) {
        super(url, machineName, serverRef, period, timeout, timeUnit, timer);
    }

    @Override
    public boolean isConnectionSuccessful(HttpURLConnection conn) {
        try {
            int responseCode = conn.getResponseCode();
            return responseCode == 404;
        } catch (IOException e) {
            return false;
        }
    }
}
