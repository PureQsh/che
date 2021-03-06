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
package org.eclipse.che.api.core.util;

import org.eclipse.che.api.core.jsonrpc.commons.RequestTransmitter;
import org.slf4j.Logger;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

public class JsonRpcMessageConsumer<T> implements MessageConsumer<T> {
    private static final Logger LOG = getLogger(JsonRpcMessageConsumer.class);

    private final String                    method;
    private final RequestTransmitter        transmitter;
    private final JsonRpcEndpointIdProvider jsonRpcEndpointIdProvider;

    public JsonRpcMessageConsumer(String method, RequestTransmitter transmitter,
                                  JsonRpcEndpointIdProvider jsonRpcEndpointIdProvider) {
        this.method = method;
        this.transmitter = transmitter;
        this.jsonRpcEndpointIdProvider = jsonRpcEndpointIdProvider;
    }

    @Override
    public void consume(T message) throws IOException {
        try {
            jsonRpcEndpointIdProvider.get().forEach(it -> transmitter.newRequest()
                                                                     .endpointId(it)
                                                                     .methodName(method)
                                                                     .paramsAsDto(message)
                                                                     .sendAndSkipResult());
        } catch (IllegalStateException e) {
            LOG.error("Error trying send line {}", message);
        }
    }

    @Override
    public void close() throws IOException {
    }
}
