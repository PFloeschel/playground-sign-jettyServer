package de.floeschel.jetty.server.servlet;

import de.floeschel.sign.SignRequest;
import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class SignServlet extends WebSocketServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        AsyncContext async = request.startAsync();
        request.getInputStream().setReadListener(new StreamReadListener(async, request, response, SignRequest.class));
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.register(SignWebSocket.class);
    }
}
