package org.kiteq.example;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.kiteq.client.ClientConfigs;
import org.kiteq.client.KiteClient;
import org.kiteq.client.impl.DefaultKiteClient;
import org.kiteq.commons.exception.NoKiteqServerException;
import org.kiteq.commons.util.ParamUtils;
import org.kiteq.commons.util.ThreadUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.kiteq.protocol.KiteRemoting.Header;
import static org.kiteq.protocol.KiteRemoting.StringMessage;

/**
 * luofucong at 2015-04-08.
 */
public class KiteqProducer {

    private static final Logger LOGGER = Logger.getLogger(KiteqProducer.class);

    private static final AtomicLong UID = new AtomicLong(0);

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("kiteq.appName", "Producer");

        Map<String, String> params = ParamUtils.parse(args);
        String zkAddr = StringUtils.defaultString(params.get("-zkAddr"), "localhost:2181");
        LOGGER.info("zkAddr=" + zkAddr);
        final String groupId = StringUtils.defaultString(params.get("-groupId"), "s-mts-test");
        LOGGER.info("groupId=" + groupId);
        String secretKey = StringUtils.defaultString(params.get("-secretKey"), "123456");
        LOGGER.info("secretKey=" + secretKey);
        final String topic = StringUtils.defaultString(params.get("-topic"), "trade");
        LOGGER.info("topic=" + topic);
        final String messageType = StringUtils.defaultString(params.get("-messageType"), "pay-succ");
        LOGGER.info("messageType=" + messageType);
        final long sendInterval = NumberUtils.toLong(params.get("-sendInterval"), 1000);
        LOGGER.info("sendInterval=" + sendInterval);
        int clientNum = NumberUtils.toInt(params.get("-clients"), Runtime.getRuntime().availableProcessors() * 2);
        LOGGER.info("clientNum=" + clientNum);
        int workerNum = NumberUtils.toInt(params.get("-workers"), 10);
        LOGGER.info("workerNum=" + workerNum);

        ClientConfigs clientConfigs = new ClientConfigs(groupId, secretKey);
        KiteClient[] clients = new KiteClient[clientNum];
        for (int i = 0; i < clientNum; i++) {
            clients[i] = new DefaultKiteClient(zkAddr, clientConfigs);
            clients[i].setPublishTopics(new String[]{topic});
            clients[i].start();
        }

        ExecutorService executor = Executors.newCachedThreadPool();
        for (int i = 0; i < clientNum; i++) {
            final KiteClient client = clients[i];
            for (int j = 0; j < workerNum; ++j) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        while (!Thread.currentThread().isInterrupted()) {
                            try {
                                StringMessage message =
                                        buildMessage(topic, groupId, messageType, String.valueOf(UID.getAndIncrement()));
                                client.sendStringMessage(message);
                            } catch (NoKiteqServerException e) {
                                break;
                            }

                            if (sendInterval > 0) {
                                ThreadUtils.sleep(sendInterval);
                            }
                        }
                    }
                });
            }
        }

        TimeUnit.HOURS.sleep(1);
    }

    private static StringMessage buildMessage(String topic, String groupId, String messageType, String body) {
        Header header = Header.newBuilder()
                .setMessageId(UUID.randomUUID().toString().replace("-", ""))
                .setTopic(topic)
                .setMessageType(messageType)
                .setExpiredTime(System.currentTimeMillis() / 1000 + TimeUnit.MINUTES.toSeconds(10))
                .setDeliverLimit(100)
                .setGroupId(groupId)
                .setCommit(true)
                .setFly(false).build();
        return StringMessage.newBuilder().setHeader(header).setBody(body).build();
    }
}
