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

import com.google.common.collect.ImmutableMap;

import org.eclipse.che.api.workspace.server.model.impl.ServerImpl;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.InternalInfrastructureException;

import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Checks readiness of servers of a machine.
 *
 * @author Alexander Garagatyi
 */
public class ServersReadinessChecker {
    // workaround to set correct paths for servers readiness checks
    // TODO replace with checks set in server config
    private static Map<String, String> livenessChecksPaths = ImmutableMap.of("wsagent", "/api/",
                                                                             "exec-agent", "/process",
                                                                             "terminal", "/");

    /**
     * Checks readiness of servers of a machine.
     *
     * @param machineName
     *         name of machine whose servers will be checked by this method
     * @param servers
     *         map of servers in a machine
     * @param serverReadinessHandler
     *         consumer which will be called with server reference as the argument when server become available
     * @throws InternalInfrastructureException
     *         if check of a server failed due to an unexpected error
     * @throws InfrastructureException
     *         if check of a server failed dut to interruption
     * @throws InfrastructureException
     *         if check of a server failed because it reached timeout
     */
    public void check(String machineName,
                      Map<String, ServerImpl> servers,
                      Consumer<String> serverReadinessHandler)
            throws InfrastructureException {

        List<ServerChecker> serverCheckers = getServerCheckers(machineName, servers);

        List<CompletableFuture<Void>> completableTasks = new ArrayList<>(serverCheckers.size());
        for (ServerChecker serverChecker : serverCheckers) {
            completableTasks.add(serverChecker.getReportCompFuture().thenAccept(serverReadinessHandler));
            serverChecker.start();
        }
        try {
            // TODO how much time should we check?
            CompletableFuture.allOf(completableTasks.toArray(new CompletableFuture[completableTasks.size()]))
                             .get(completableTasks.size() * 180, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InfrastructureException("Machine " + machineName + " start is interrupted");
        } catch (TimeoutException e) {
            throw new InfrastructureException("Servers readiness check of machine " + machineName + " timed out");
        } catch (ExecutionException e) {
            try {
                throw e.getCause();
            } catch (InfrastructureException rethrow) {
                throw rethrow;
            } catch (Throwable thr) {
                throw new InternalInfrastructureException(
                        "Machine " + machineName + " servers readiness check failed. Error: " + thr.getMessage(), thr);
            }
        } finally {
            // cleanup checkers tasks
            for (ServerChecker serverChecker : serverCheckers) {
                serverChecker.stop();
            }
        }
    }

    private List<ServerChecker> getServerCheckers(String machineName, Map<String, ServerImpl> servers)
            throws InfrastructureException {

        Timer timer = new Timer("ServerReadinessChecker", true);
        ArrayList<ServerChecker> checkers = new ArrayList<>(servers.size());
        for (Map.Entry<String, ServerImpl> serverEntry : servers.entrySet()) {
            // TODO replace with correct behaviour
            // workaround needed because we don't have server readiness check in the model
            if (!livenessChecksPaths.containsKey(serverEntry.getKey())) {
                continue;
            }
            checkers.add(getChecker(machineName,
                                    serverEntry.getKey(),
                                    serverEntry.getValue(),
                                    timer));
        }
        return checkers;
    }

    private ServerChecker getChecker(String machineName,
                                     String serverRef,
                                     ServerImpl server,
                                     Timer timer) throws InfrastructureException {
        // TODO replace with correct behaviour
        // workaround needed because we don't have server readiness check in the model
        String livenessCheckPath = livenessChecksPaths.get(serverRef);
        // Create server readiness endpoint URL
        URL url;
        try {
            url = UriBuilder.fromUri(server.getUrl())
                            .replacePath(livenessCheckPath)
                            .build()
                            .toURL();
        } catch (MalformedURLException e) {
            throw new InternalInfrastructureException(
                    "Server " + serverRef + " URL is invalid. Error: " + e.getMessage(), e);
        }

        HttpConnectionServerChecker serverChecker;
        if ("terminal".equals(serverRef)) {
            // TODO add readiness endpoint to terminal and remove this
            // workaround needed because terminal server doesn't have endpoint to check it readiness
            serverChecker = new TerminalHttpConnectionServerChecker(url,
                                                                    machineName,
                                                                    serverRef,
                                                                    3,
                                                                    180,
                                                                    TimeUnit.SECONDS,
                                                                    timer);
        } else {
            // TODO do not hardcode timeouts, use server conf instead
            serverChecker = new HttpConnectionServerChecker(url,
                                                            machineName,
                                                            serverRef,
                                                            3,
                                                            180,
                                                            TimeUnit.SECONDS,
                                                            timer);
        }
        return serverChecker;
    }
}
