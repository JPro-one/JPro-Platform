package one.jpro.platform.auth.core.oauth2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Fake identity provider, reached by configuring it as the HTTP proxy of the OAuth2 client.
 */
class FakeIdp implements AutoCloseable {

    record Request(String line, String body) {
    }

    record Response(int status, String body) {
    }

    private final ServerSocket socket;
    private final Function<Request, Response> responder;
    private final List<Request> requests = new CopyOnWriteArrayList<>();

    FakeIdp(Function<Request, Response> responder) throws IOException {
        this.responder = responder;
        socket = new ServerSocket(0);
        Thread thread = new Thread(() -> {
            while (!socket.isClosed()) {
                try (Socket connection = socket.accept()) {
                    handle(connection);
                } catch (IOException ignored) {
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    Consumer<HttpClient.Builder> viaProxy() {
        return builder -> builder.proxy(ProxySelector.of(new InetSocketAddress("localhost", socket.getLocalPort())));
    }

    List<Request> requests() {
        return requests;
    }

    List<String> requestLines() {
        return requests.stream().map(Request::line).toList();
    }

    private void handle(Socket connection) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        String requestLine = in.readLine();
        if (requestLine == null) return;
        int contentLength = 0;
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring("content-length:".length()).trim());
            }
        }
        char[] body = new char[contentLength];
        int read = 0;
        while (read < contentLength) {
            int n = in.read(body, read, contentLength - read);
            if (n < 0) break;
            read += n;
        }
        Request request = new Request(requestLine, new String(body, 0, read));
        requests.add(request);

        Response response = responder.apply(request);
        byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
        OutputStream out = connection.getOutputStream();
        out.write(("HTTP/1.1 " + response.status() + " X\r\nContent-Type: application/json\r\nContent-Length: "
                + bytes.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.flush();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
