package ru.mail.polis.clhost.servlets;

import ru.mail.polis.clhost.dao.EntityDao;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class BaseEntityServlet extends HttpServlet {
    private EntityDao dao;

    public BaseEntityServlet(EntityDao dao) {
        this.dao = dao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getParameter("id").equals("")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            byte[] value = dao.get(req.getParameter("id"));

            if (value == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else {
                OutputStream os = resp.getOutputStream();
                os.write(value, 0, value.length);
                os.close();
                resp.setStatus(HttpServletResponse.SC_OK);
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String key = req.getParameter("id");

        if (key.equals("")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            if ("PUT".equalsIgnoreCase(req.getMethod())) {
                InputStream is = req.getInputStream();
                byte[] bytes = new byte[is.available()];

                is.read(bytes, 0, bytes.length);
                is.close();
                dao.upsert(key, bytes);
            }
            resp.setStatus(HttpServletResponse.SC_CREATED);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String key = req.getParameter("id");

        if (key.equals("")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            dao.delete(key);
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        }
    }
}
