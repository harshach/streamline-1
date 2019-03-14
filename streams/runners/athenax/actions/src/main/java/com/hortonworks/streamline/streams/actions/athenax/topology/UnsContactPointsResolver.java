package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.uber.infra.uns.Task;
import com.uber.infra.uns.client.ClientResolver;
import com.uber.infra.uns.client.ResolverContext;
import com.uber.tchannel.api.SubChannel;
import com.uber.tchannel.api.TChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * UnsContactPointsResolver is an example of how to resolve Cassandra cluster's contact points
 * and native transport port through UNS.
 */
public class UnsContactPointsResolver {
    private static final Logger LOG = LoggerFactory.getLogger(UnsContactPointsResolver.class);

    /**
     * getContactPointsPort looks up UNS to resolve both contact points and native transport port
     * given a cluster's UNS path. Cluster's UNS path can be looked up at https://cmm-ui.uberinternal.com/
     * @param unsPath
     * @return
     */
    public List<String> getContactPoints(String unsPath, final String portKey) throws ExecutionException, InterruptedException {
        TChannel tchannel = new TChannel.Builder("athenax").build();
        SubChannel subChannel = tchannel.makeSubChannel("uns");
        ClientResolver resolver = ClientResolver.builder().subChannel(subChannel).build();
        ResolverContext context = ResolverContext.builder().build();
        ListenableFuture<List<Task>> result = resolver.resolve(context, unsPath);
        final List<String> hostAddrs = new ArrayList<>();

        Futures.addCallback(result, new FutureCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                int port = -1;
                for (Task task : tasks) {
                    if (task.getPorts().containsKey(portKey)) {
                        port = task.getPorts().get(portKey);
                    } else if (task.getPorts().containsKey(portKey.toUpperCase())) {
                        port = task.getPorts().get(portKey.toUpperCase());
                    } else if (task.getPorts().containsKey(portKey.toLowerCase())) {
                        port = task.getPorts().get(portKey.toLowerCase());
                    }
                    hostAddrs.add(String.format("%s:%s", task.getIpAddress(), port));
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("Error occurred while resolving contact points : " + throwable.getMessage());
            }
        });

        try {
            result.get();
        } finally {
            tchannel.shutdown(true);
        }

        return hostAddrs;
    }
}