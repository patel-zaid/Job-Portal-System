package jobportal.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import jobportal.dao.JobDao;
import jobportal.model.Job;

/**
 * A very small REST API built with the HTTP server that comes with the JDK.
 * (No framework needed.)
 *
 *   GET http://localhost:8080/jobs        -> all OPEN jobs as JSON
 *   GET http://localhost:8080/jobs/3      -> job number 3 as JSON
 *
 * Status codes used: 200 OK, 400 Bad Request, 404 Not Found, 405 Method Not Allowed, 500 Server Error.
 */
public class RestApiServer {

    private final int port;
    private HttpServer server;

    public RestApiServer(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/jobs", new JobsHandler());    // all URLs that start with /jobs
        server.start();
        System.out.println("REST API started: http://localhost:" + port + "/jobs");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Handles every request that comes to /jobs and /jobs/{id}. */
    private class JobsHandler implements HttpHandler {

        private final JobDao jobDao = new JobDao();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("GET")) {
                sendJson(exchange, 405, "{\"error\": \"Only GET is supported\"}");
                return;
            }

            String path = exchange.getRequestURI().getPath();       // "/jobs" or "/jobs/3"
            try {
                if (path.equals("/jobs") || path.equals("/jobs/")) {
                    List<Job> jobs = jobDao.searchJobs("");
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < jobs.size(); i++) {
                        if (i > 0) {
                            json.append(",");
                        }
                        json.append(toJson(jobs.get(i)));
                    }
                    json.append("]");
                    sendJson(exchange, 200, json.toString());
                } else {
                    int jobId = Integer.parseInt(path.substring("/jobs/".length()));
                    Job job = jobDao.getJobById(jobId);
                    if (job == null) {
                        sendJson(exchange, 404, "{\"error\": \"Job not found\"}");
                    } else {
                        sendJson(exchange, 200, toJson(job));
                    }
                }
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, "{\"error\": \"Job id must be a number\"}");
            } catch (SQLException e) {
                sendJson(exchange, 500, "{\"error\": \"Database error\"}");
            }
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /** Converts one Job into a JSON object text. */
    private String toJson(Job job) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\": ").append(job.getJobId()).append(", ");
        json.append("\"title\": \"").append(escape(job.getTitle())).append("\", ");
        json.append("\"company\": \"").append(escape(job.getCompanyName())).append("\", ");
        json.append("\"location\": \"").append(escape(job.getLocation())).append("\", ");
        json.append("\"type\": \"").append(escape(job.getJobType())).append("\", ");
        json.append("\"salaryLpa\": ").append(job.getSalaryLpa()).append(", ");
        json.append("\"skills\": \"").append(escape(job.getRequiredSkills())).append("\", ");
        json.append("\"experienceYears\": ").append(job.getExperienceRequired()).append(", ");
        json.append("\"status\": \"").append(job.getStatus()).append("\", ");
        json.append("\"postedOn\": \"").append(job.getPostedDate()).append("\"");
        json.append("}");
        return json.toString();
    }

    /** Escapes characters that are not allowed directly inside a JSON string. */
    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
