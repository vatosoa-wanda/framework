// package com.giga.spring.servlet;
package front;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import url.ScannerFramework;

/**
 * This is the servlet that takes all incoming requests targeting the app - If
 * the requested resource exists, it delegates to the default dispatcher - else
 * it shows the requested URL
 */
public class FrontServlet extends HttpServlet {

    RequestDispatcher defaultDispatcher;

    // Ajout : stockage des mappings détectés
    private Map<String, Method> urlMappings;
    private Map<String, Object> controllers;

    @Override
    public void init() {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");

        try {
            // Initialisation du framework
            System.out.println("=== Initialisation du Framework ===");

            // Récupération du chemin vers /WEB-INF/classes
            String classesPath = getServletContext().getRealPath("/WEB-INF/classes");

            // Appel du scanner
            ScannerFramework scanner = new ScannerFramework();
            scanner.scan(classesPath);

            // Récupération des mappings trouvés
            urlMappings = scanner.getUrlMappings();
            controllers = scanner.getControllers();

            System.out.println("=== Initialisation terminée ===");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'initialisation du Framework", e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        /**
         * Example: 
         * If URI is /app/folder/file.html 
         * and context path is /app,
         * then path = /folder/file.html
         */
        String path = req.getRequestURI().substring(req.getContextPath().length());
        
        boolean resourceExists = getServletContext().getResource(path) != null;

        if (resourceExists) {
            defaultServe(req, res);
        } else {
            customServe(req, res);
        }
    }

    private void customServe(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try (PrintWriter out = res.getWriter()) {
            String uri = req.getRequestURI();
            String responseBody = """
                <html>
                    <head><title>Resource Not Found</title></head>
                    <body>
                        <h1>Unknown resource</h1>
                        <p>The requested URL was not found: <strong>%s</strong></p>
                    </body>
                </html>
                """.formatted(uri);

            res.setContentType("text/html;charset=UTF-8");
            out.println(responseBody);
        }
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }
}
