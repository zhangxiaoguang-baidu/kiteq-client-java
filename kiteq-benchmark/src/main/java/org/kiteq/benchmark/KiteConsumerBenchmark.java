package org.kiteq.benchmark;

import org.kiteq.client.ClientConfigs;
import org.kiteq.client.binding.Binding;
import org.kiteq.client.KiteClient;
import org.kiteq.client.impl.DefaultKiteClient;
import org.kiteq.client.message.ListenerAdapter;
import org.kiteq.client.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KiteConsumerBenchmark {
    
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(KiteConsumerBenchmark.class);
    
    private static final String ZK_ADDR = "localhost:2181";
    private static final String GROUP_ID = "s-mts-test";
    private static final String SECRET_KEY = "123456";
    
    private KiteClient consumer;
    
    public KiteConsumerBenchmark() {
        consumer = new DefaultKiteClient(ZK_ADDR, new ClientConfigs(GROUP_ID, SECRET_KEY), new ListenerAdapter() {
            @Override
            public boolean onMessage(Message message) {
                return true;
            }
        });
        consumer.setBindings(new Binding[] { Binding.bindDirect(GROUP_ID, "trade", "pay-succ", 1000, true) });
    }
    
    public void start() {
        consumer.start();
    }
    
    public static void main(String[] args) {
        System.setProperty("kiteq.appName", "Consumer");
        new KiteConsumerBenchmark().start();
    }
}
