package ru.mail.polis.clhost.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import ru.mail.polis.KVService;
import ru.mail.polis.clhost.dao.EntityDao;
import ru.mail.polis.clhost.servlets.BaseEntityServlet;
import ru.mail.polis.clhost.servlets.StatusServlet;


public class JettyServer implements KVService {
    private ServletContextHandler context;
    private Server server;
    private EntityDao dao;

    public JettyServer(int port, EntityDao dao) {
        this.dao = dao;
        server = new Server(port);
        context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        configure();
    }

    private void configure() {
        context.addServlet(new ServletHolder(new BaseEntityServlet(dao)), "/v0/entity");
        context.addServlet(new ServletHolder(new StatusServlet()), "/v0/status");
        server.setHandler(context);
    }

    @Override
    public void start() {
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
