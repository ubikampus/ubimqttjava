package fi.helsinki.ubimqtt;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public class ContinuousPublishingTest {

        private static final String TOPIC = "test/javatesttopic";
        private static final String SIGNED_TOPIC = "test/javasignedtesttopic";
        private static final String ENCRYPTED_TOPIC = "test/javaencryptedtesttopic";

        private static final String KEY_TOPIC = "publishers/javatestpublisher/publicKey";
        private static final String JAVA_TEST_PUBLISHER = "javatestpublisher";

        private ScheduledThreadPoolExecutor delayer = new ScheduledThreadPoolExecutor(5);

        private void log(String s) {
            StackTraceElement l = new Exception().getStackTrace()[0];
            System.out.println(
                    l.getClassName() + "/" + l.getMethodName() + ":" + l.getLineNumber() + ": " + s);
        }

        private <T> CompletableFuture<T> timeoutAfter(long timeout, TimeUnit unit) {
            CompletableFuture<T> result = new CompletableFuture<T>();
            delayer.schedule(() -> result.completeExceptionally(new TimeoutException()), timeout, unit);
            return result;
        }

        @Test
        public void testUbiMqtt_CanConnect() {
            log("testUbiMqtt_CanConnect()");

            CompletableFuture<String> future = new CompletableFuture<>();

            UbiMqtt ubiMqtt = new UbiMqtt("localhost:1883");

            ubiMqtt.connect(new IUbiActionListener() {
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
                ubiMqtt.disconnect(new IUbiActionListener() {
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

            ubiMqtt.connect(new IUbiActionListener() {
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

        private UbiMqtt ubiMqtt = null;

        @Test
        public void testUbiMqtt_CanPublishAndSubscribe() {
            log("testUbiMqtt_CanPublishAndSubscribe()");

            if (ubiMqtt ==null ) {
                CompletableFuture<String> future = new CompletableFuture<>();

                ubiMqtt = new UbiMqtt("localhost:1883");

                ubiMqtt.connect(new IUbiActionListener() {
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
                ubiMqtt.subscribe("test/javatesttopic", messageListener, new IUbiActionListener() {
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
                });
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
                                ubiMqtt.disconnect(new IUbiActionListener() {
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
            }
            CompletableFuture<String> publishFuture = new CompletableFuture<>();

            ubiMqtt.publish(TOPIC, "Hello from Java!", new IUbiActionListener() {
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
            });
            try {
                //assertEquals("success", publishFuture.get(5, TimeUnit.SECONDS));
                Thread.sleep(1000);
                testUbiMqtt_CanPublishAndSubscribe();
            } catch (Exception e) {
                e.printStackTrace();
                assertEquals(null, e);
            }

        }

}
