/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016-2018 Grakn Labs Limited
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/agpl.txt>.
 */

package ai.grakn.remote;

import ai.grakn.GraknSession;
import ai.grakn.GraknTxType;
import ai.grakn.Keyspace;
import ai.grakn.exception.GraknTxOperationException;
import ai.grakn.rpc.util.RequestBuilder;
import ai.grakn.rpc.generated.GraknGrpc;
import ai.grakn.rpc.generated.GraknGrpc.GraknBlockingStub;
import ai.grakn.rpc.generated.GraknGrpc.GraknStub;
import ai.grakn.util.SimpleURI;
import com.google.common.annotations.VisibleForTesting;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;

import static ai.grakn.rpc.util.GrpcConfig.GRPC_MAX_MESSAGE_SIZE_IN_BYTES;

/**
 * Remote implementation of {@link GraknSession} that communicates with a Grakn server using gRPC.
 *
 * @see RemoteGraknTx
 * @see RemoteGrakn
 *
 * @author Felix Chapman
 */
public class RemoteGraknSession implements GraknSession {

    private final Keyspace keyspace;
    private final SimpleURI uri;
    private final ManagedChannel channel;

    protected RemoteGraknSession(Keyspace keyspace, SimpleURI uri, ManagedChannel channel) {
        this.keyspace = keyspace;
        this.uri = uri;
        this.channel = channel;
    }

    @VisibleForTesting
    public static RemoteGraknSession create(Keyspace keyspace, SimpleURI uri, ManagedChannel channel) {
        return new RemoteGraknSession(keyspace, uri, channel);
    }

    public static RemoteGraknSession create(Keyspace keyspace, SimpleURI uri){
        ManagedChannel channel =
                NettyChannelBuilder.forAddress(uri.getHost(), uri.getPort()).maxInboundMessageSize(GRPC_MAX_MESSAGE_SIZE_IN_BYTES).usePlaintext(true).build();

        return create(keyspace, uri, channel);
    }

    GraknStub stub() {
        return GraknGrpc.newStub(channel);
    }

    GraknBlockingStub blockingStub() {
        return GraknGrpc.newBlockingStub(channel);
    }

    @Override
    public RemoteGraknTx open(GraknTxType transactionType) {
        return RemoteGraknTx.create(this, RequestBuilder.openRequest(keyspace, transactionType));
    }

    @Override
    public void close() throws GraknTxOperationException {
        channel.shutdown();
    }

    @Override
    public String uri() {
        return uri.toString();
    }

    @Override
    public Keyspace keyspace() {
        return keyspace;
    }
}
