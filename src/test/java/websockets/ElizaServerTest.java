package websockets;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import websockets.web.ElizaServerEndpoint;

public class ElizaServerTest {

	private Server server;

	@Before
	public void setup() throws DeploymentException {
		server = new Server("localhost", 8025, "/websockets", new HashMap<String, Object>(), ElizaServerEndpoint.class);
		server.start();
	}

	@Test(timeout = 1000)
	public void onOpen() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(3);
		List<String> list = new ArrayList<>();
		ClientEndpointConfig configuration = ClientEndpointConfig.Builder.create().build();
		ClientManager client = ClientManager.createClient();
		client.connectToServer(new Endpoint() {

			@Override
			public void onOpen(Session session, EndpointConfig config) {
				session.addMessageHandler(new MessageHandler.Whole<String>() {

					@Override
					public void onMessage(String message) {
						list.add(message);
						latch.countDown();
					}
				});
			}

		}, configuration, new URI("ws://localhost:8025/websockets/eliza"));
		latch.await();
		assertEquals(3, list.size());
		assertEquals("The doctor is in.", list.get(0));
	}

	@Test(timeout = 1000)
	public void onChat() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
		// Expected to receive five messages from server.
		// Three correspond to the server start ("The doctor is in." and "What's on your mind?").
		// The other two are the response of the question.
		CountDownLatch latch = new CountDownLatch(5);
		List<String> list = new ArrayList<>();
		ClientEndpointConfig configuration = ClientEndpointConfig.Builder.create().build();
		ClientManager client = ClientManager.createClient();
		client.connectToServer(new Endpoint() {

			@Override
			public void onOpen(Session session, EndpointConfig config) {

				// Sends a message with only one possible response.
				// It's visible in Eliza.java (String[] temp4).
				session.getAsyncRemote().sendText("Maybe my mind is disturbed...");
				session.addMessageHandler(new MessageHandler.Whole<String>() {

					@Override
					public void onMessage(String message) {
						list.add(message);
						// Decreases the counter of the specified latch.
						latch.countDown();
					}
				});
			}

		}, configuration, new URI("ws://localhost:8025/websockets/eliza"));
		// Waits until latch counter reaches zero (due to invocations of the countDown() method).
		latch.await();
		// Check if the size of the list is the expected.
		assertEquals(5, list.size());
		// Check the message received.
		assertEquals("You don't seem very certain.", list.get(3));
	}

	@After
	public void close() {
		server.stop();
	}
}
