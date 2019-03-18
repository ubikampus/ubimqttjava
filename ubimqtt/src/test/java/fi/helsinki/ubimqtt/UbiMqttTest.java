package fi.helsinki.ubimqtt;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

// Make sure that a MQTT server answers at localhost:1883 before running these tests
// If you have access to Ubikampus VMs, you can forward the Ubikampus MQTT server
// port to localhost with the command "ssh -L 1883:10.120.0.4:1883 ubi@iot.ubikampus.net"

public class UbiMqttTest {

    private static final String TOPIC = "test/javatesttopic";
    private static final String SIGNED_TOPIC = "test/javasignedtesttopic";

    private static final String KEY_TOPIC = "publishers/javatestpublisher/publicKey";
    private static final String JAVA_TEST_PUBLISHER = "javatestpublisher";

    ScheduledThreadPoolExecutor delayer = new ScheduledThreadPoolExecutor(5);

    public void log(String s) {
        StackTraceElement l = new Exception().getStackTrace()[0];
        System.out.println(
                l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + ": " + s);
    }

    public <T> CompletableFuture<T> timeoutAfter(long timeout, TimeUnit unit) {
        CompletableFuture<T> result = new CompletableFuture<T>();
        delayer.schedule(() -> result.completeExceptionally(new TimeoutException()), timeout, unit);
        return result;
    }

    @Test
    public void testUbiMqtt_CanConnect() {
        log("testUbiMqtt_CanConnect()");

        CompletableFuture<String> future = new CompletableFuture<>();

        UbiMqtt ubiMqtt = new UbiMqtt("localhost:1883");

        ubiMqtt.connect(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                log("testUbiMqtt_CanConnect() Connecting to mqtt server succeeded");
                future.complete("success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                log("testUbiMqtt_CanConnect() Connecting to mqtt server failed");
                future.complete("failure");
            }
        });
        try {
            assertEquals("success", future.get(5, TimeUnit.SECONDS));

            CompletableFuture<String> disconnectFuture = new CompletableFuture<>();
            ubiMqtt.disconnect(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    log("testUbiMqtt_CanConnect() Disconnecting from mqtt server succeeded");
                    disconnectFuture.complete("success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    log("testUbiMqtt_CanConnect() Disconnecting from mqtt server failed");
                    disconnectFuture.complete("failure");
                }
            });
            try {
                assertEquals("success", disconnectFuture.get(5, TimeUnit.SECONDS));
            } catch (Exception e) {
                e.printStackTrace();
                assertEquals(null, e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }
    }

    @Test
    public void testUbiMqtt_CanHandleConnectFailure() {
        log("testUbiMqtt_CanHandleConnectFailure()");

        CompletableFuture<String> future = new CompletableFuture<>();

        UbiMqtt ubiMqtt = new UbiMqtt("awoeifjoij:1833");

        ubiMqtt.connect(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                log("testUbiMqtt_CanHandleConnectFailure() Connecting to non-existing address succeeded, this is an error");
                future.complete("success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                log("testUbiMqtt_CanHandleConnectFailure() Connecting to non-existing address failed as expedted");
                future.complete("failure");
            }
        });

        try {
            assertEquals("failure", future.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }
    }

    @Test
    public void testUbiMqtt_CanPublishAndSubscribe() {
        log("testUbiMqtt_CanPublishAndSubscribe()");

        CompletableFuture<String> future = new CompletableFuture<>();

        UbiMqtt ubiMqtt = new UbiMqtt("localhost:1883");

        ubiMqtt.connect(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                log("testUbiMqtt_CanPublishAndSubscribe() Connecting to mqtt server succeeded");
                future.complete("success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                log("testUbiMqtt_CanPublishAndSubscribe() Connecting to mqtt server failed");
                future.complete("failure");
            }
        });
        try {
            assertEquals("success", future.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }

        CompletableFuture<String> subscribeFuture = new CompletableFuture<>();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();

        IUbiMessageListener messageListener = new IUbiMessageListener() {
            @Override
            public void messageArrived(String s, MqttMessage mqttMessage, String subscriptionId) throws Exception {
                log("messageListener::messageArrived() topic: " + s + " message: " + mqttMessage.toString());
                messageFuture.complete(mqttMessage.toString());
            }
        };
        ubiMqtt.subscribe(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                log("testUbiMqtt_CanPublishAndSubscribe() subscribing from MQTT server succeeded");
                subscribeFuture.complete("success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                log("testUbiMqtt_CanPublishAndSubscribe() subscribning from MQTT server failed");
                subscribeFuture.complete("failure");
            }
        }, "test/javatesttopic", messageListener);
        try {
            assertEquals("success", subscribeFuture.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }

        try {
            messageFuture.acceptEither(timeoutAfter(10, TimeUnit.SECONDS), message -> assertEquals("Hello from Java!", message))
                    .thenAccept(message -> {
                        CompletableFuture<String> disconnectFuture = new CompletableFuture<>();
                        ubiMqtt.disconnect(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                log("testUbiMqtt_CanPublishAndSubscribe() Disconnecting from mqtt server succeeded");
                                disconnectFuture.complete("success");
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                log("testUbiMqtt_CanPublishAndSubscribe() Disconnecting from MQTT server failed");
                                disconnectFuture.complete("failure");
                            }
                        });
                        try {
                            assertEquals("success", disconnectFuture.get(5, TimeUnit.SECONDS));
                        } catch (Exception e) {
                            e.printStackTrace();
                            assertEquals(null, e);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }

        CompletableFuture<String> publishFuture = new CompletableFuture<>();

        ubiMqtt.publish(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                log("testUbiMqtt_CanPublishAndSubscribe() publishing to MQTT server succeeded");
                publishFuture.complete("success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                log("testUbiMqtt_CanPublishAndSubscribe() publishing to MQTT server failed");
                publishFuture.complete("failure");
            }
        }, TOPIC, "Hello from Java!");
        try {
            assertEquals("success", publishFuture.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }

    }

    @Test
    public void testUbiMqtt_CanPublishAndSubscribeSigned() {
        log("testUbiMqtt_CanPublishAndSubscribeSigned()");

        String privateKey = "";
        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            privateKey = new String(encoded, StandardCharsets.UTF_8);

        } catch (Exception e) {
            assertEquals(null, e);
        }

        String publicKey = "";
        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key-public.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            publicKey = new String(encoded, StandardCharsets.UTF_8);

        } catch (Exception e) {
            assertEquals(null, e);
        }

        CompletableFuture<String> future = new CompletableFuture<>();

        UbiMqtt ubiMqtt = new UbiMqtt("localhost:1883");

        ubiMqtt.connect(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                log("testUbiMqtt_CanPublishAndSubscribeSigned() Connecting to mqtt server succeeded");
                future.complete("success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                log("testUbiMqtt_CanPublishAndSubscribeSigned() Connecting to mqtt server failed");
                future.complete("failure");
            }
        });
        try {
            assertEquals("success", future.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }

        CompletableFuture<String> subscribeFuture = new CompletableFuture<>();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();

        IUbiMessageListener messageListener = new IUbiMessageListener() {
            @Override
            public void messageArrived(String s, MqttMessage mqttMessage, String subscriptionId) throws Exception {
                log("messageListener::messageArrived() topic: " + s + " message: " + mqttMessage.toString());
                messageFuture.complete(mqttMessage.toString());
            }
        };
        String[] publicKeys = {publicKey};

        ubiMqtt.subscribeSigned(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                log("testUbiMqtt_CanPublishAndSubscribeSigned() subscribing from MQTT server succeeded");
                subscribeFuture.complete("success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                log("testUbiMqtt_CanPublishAndSubscribeSigned() subscribning from MQTT server failed");
                subscribeFuture.complete("failure");
            }
        }, SIGNED_TOPIC, publicKeys, messageListener);
        try {
            assertEquals("success", subscribeFuture.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }

        try {
            messageFuture.acceptEither(timeoutAfter(10, TimeUnit.SECONDS), message -> assertEquals("Hello from Java!", message))
                    .thenAccept(message -> {
                        CompletableFuture<String> disconnectFuture = new CompletableFuture<>();
                        ubiMqtt.disconnect(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                log("testUbiMqtt_CanPublishAndSubscribeSigned() Disconnecting from mqtt server succeeded");
                                disconnectFuture.complete("success");
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                log("testUbiMqtt_CanPublishAndSubscribeSigned() Disconnecting from MQTT server failed");
                                disconnectFuture.complete("failure");
                            }
                        });
                        try {
                            assertEquals("success", disconnectFuture.get(5, TimeUnit.SECONDS));
                        } catch (Exception e) {
                            e.printStackTrace();
                            assertEquals(null, e);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }

        CompletableFuture<String> publishFuture = new CompletableFuture<>();

        ubiMqtt.publishSigned(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                log("testUbiMqtt_CanPublishAndSubscribeSigned() publishing to MQTT server succeeded");
                publishFuture.complete("success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                log("testUbiMqtt_CanPublishAndSubscribeSigned() publishing to MQTT server failed");
                publishFuture.complete("failure");
            }
        }, SIGNED_TOPIC, "Hello from Java!", privateKey);
        try {
            assertEquals("success", publishFuture.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }

    }


    @Test
    public void testUbiMqtt_CanSubscribeFromKnownPublisher() {
        log("testUbiMqtt_CanSubscribeFromKnownPublisher()");

        String privateKey = "";
        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            privateKey = new String(encoded, StandardCharsets.UTF_8);

        } catch (Exception e) {
            assertEquals(null, e);
        }

        String publicKey = "";
        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key-public.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            publicKey = new String(encoded, StandardCharsets.UTF_8);

        } catch (Exception e) {
            assertEquals(null, e);
        }

        CompletableFuture<String> future = new CompletableFuture<>();

        UbiMqtt ubiMqtt = new UbiMqtt("localhost:1883");

        ubiMqtt.connect(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                log("testUbiMqtt_CanSubscribeFromKnownPublisher() Connecting to mqtt server succeeded");
                future.complete("success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                log("testUbiMqtt_CanSubscribeFromKnownPublisher() Connecting to mqtt server failed");
                future.complete("failure");
            }
        });
        try {
            assertEquals("success", future.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }

        // Publish the the a public key for our "known" publisher

        CompletableFuture<String> publishKeyFuture = new CompletableFuture<>();

        ubiMqtt.publish(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                log("testUbiMqtt_CanSubscribeFromKnownPublisher() publishing key to MQTT server succeeded");
                publishKeyFuture.complete("success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                log("testUbiMqtt_CanSubscribeFromKnownPublisher() publishing key to MQTT server failed");
                publishKeyFuture.complete("failure");
            }
        }, KEY_TOPIC, publicKey, 1, true);
        try {
            assertEquals("success", publishKeyFuture.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }

        CompletableFuture<String> subscribeFuture = new CompletableFuture<>();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();

        IUbiMessageListener messageListener = new IUbiMessageListener() {
            @Override
            public void messageArrived(String s, MqttMessage mqttMessage, String subscriptionId) throws Exception {
                log("messageListener::messageArrived() topic: " + s + " message: " + mqttMessage.toString());
                messageFuture.complete(mqttMessage.toString());
            }
        };
        ubiMqtt.subscribeFromPublisher(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                log("testUbiMqtt_CanSubscribeFromKnownPublisher() subscribing from MQTT server succeeded");
                subscribeFuture.complete("success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                log("testUbiMqtt_CanSubscribeFromKnownPublisher() subscribning from MQTT server failed");
                subscribeFuture.complete("failure");
            }
        }, "test/javatesttopic", JAVA_TEST_PUBLISHER, messageListener);
        try {
            assertEquals("success", subscribeFuture.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }

        try {
            messageFuture.acceptEither(timeoutAfter(10, TimeUnit.SECONDS), message -> assertEquals("Hello from Java!", message))
                    .thenAccept(message -> {
                        CompletableFuture<String> disconnectFuture = new CompletableFuture<>();
                        ubiMqtt.disconnect(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                log("testUbiMqtt_CanSubscribeFromKnownPublisher() Disconnecting from mqtt server succeeded");
                                disconnectFuture.complete("success");
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                log("testUbiMqtt_CanSubscribeFromKnownPublisher() Disconnecting from MQTT server failed");
                                disconnectFuture.complete("failure");
                            }
                        });
                        try {
                            assertEquals("success", disconnectFuture.get(5, TimeUnit.SECONDS));
                        } catch (Exception e) {
                            e.printStackTrace();
                            assertEquals(null, e);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }

        CompletableFuture<String> publishFuture = new CompletableFuture<>();

        ubiMqtt.publishSigned(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                log("testUbiMqtt_CanSubscribeFromKnownPublisher() publishing to MQTT server succeeded");
                publishFuture.complete("success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                log("testUbiMqtt_CanSubscribeFromKnownPublisher() publishing to MQTT server failed");
                publishFuture.complete("failure");
            }
        }, TOPIC, "Hello from Java!", privateKey);
        try {
            assertEquals("success", publishFuture.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }

    }
}
