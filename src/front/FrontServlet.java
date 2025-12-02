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
import url.UrlPatternMatcher;

public class FrontServlet extends HttpServlet {

    RequestDispatcher defaultDispatcher;
    private Map<String, Map<String, Method>> urlMappings;
    private Map<String, Object> controllers;

    @Override
    public void init() {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");

        try {
            System.out.println("=== Initialisation du Framework ===");
            String classesPath = getServletContext().getRealPath("/WEB-INF/classes");
            
            ScannerFramework scanner = new ScannerFramework();
            scanner.scan(classesPath);
            
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
        String path = req.getRequestURI().substring(req.getContextPath().length());
        
        boolean resourceExists = getServletContext().getResource(path) != null;

        if (resourceExists) {
            defaultServe(req, res);
        } else {
            customServe(req, res);
        }
    }

    private void customServe(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        String method = req.getMethod();

        try {
            String matchingPattern = null;
            Map<String, Method> methodMap = null;

            if (urlMappings != null) {
                for (String pattern : urlMappings.keySet()) {
                    if (UrlPatternMatcher.matches(pattern, path)) {
                        matchingPattern = pattern;
                        methodMap = urlMappings.get(pattern);
                        break;
                    }
                }
            }

            if (methodMap != null && methodMap.containsKey(method)) {
                Method handlerMethod = methodMap.get(method);
                Object controllerInstance = findControllerInstance(handlerMethod.getDeclaringClass());

                if (controllerInstance != null) {
                    // UTILISATION DE LA NOUVELLE MÉTHODE
                    Object[] methodArgs = ScannerFramework.mapFormParametersToMethodArgs(handlerMethod, req, matchingPattern, path);
                    Object result = handlerMethod.invoke(controllerInstance, methodArgs);
                    handleResult(result, path, req, res);
                } else {
                    sendError(res, "Contrôleur non trouvé pour: " + path);
                }
            } else {
                sendNotFound(res, path);
            }
        } catch (Exception e) {
            sendError(res, "Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Object findControllerInstance(Class<?> controllerClass) {
        for (Object controller : controllers.values()) {
            if (controller.getClass().equals(controllerClass)) {
                return controller;
            }
        }
        return null;
    }

    private void handleResult(Object result, String path, HttpServletRequest req, HttpServletResponse res) 
            throws IOException, ServletException {
        
        if (result == null) {
            res.setContentType("text/plain; charset=UTF-8");
            res.getWriter().println("Méthode exécutée: " + path);
            return;
        }

        if (result instanceof String) {
            res.setContentType("text/plain; charset=UTF-8");
            res.getWriter().println("Résultat: " + result);
            return;
        }

        if (result instanceof view.ModelView) {
            view.ModelView mv = (view.ModelView) result;
            String viewPath = mv.getView();
            if (viewPath == null || viewPath.isEmpty()) {
                res.setContentType("text/plain; charset=UTF-8");
                res.getWriter().println("Erreur: Aucune vue spécifiée dans ModelView");
                return;
            }

            // Transférer les données du modèle vers la requête
            for (Map.Entry<String, Object> entry : mv.getModel().entrySet()) {
                req.setAttribute(entry.getKey(), entry.getValue());
            }

            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/" + viewPath);
            if (dispatcher != null) {
                dispatcher.forward(req, res);
            } else {
                res.setContentType("text/plain; charset=UTF-8");
                res.getWriter().println("Erreur: Vue non trouvée - " + viewPath);
            }
            return;
        }

        res.setContentType("text/plain; charset=UTF-8");
        res.getWriter().println("Résultat (" + result.getClass().getSimpleName() + "): " + result);
    }

    private void sendNotFound(HttpServletResponse res, String path) throws IOException {
        res.setContentType("text/html; charset=UTF-8");
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        res.getWriter().println("<h1>URL non supportée: " + path + "</h1>");
    }

    private void sendError(HttpServletResponse res, String message) throws IOException {
        res.setContentType("text/plain; charset=UTF-8");
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        res.getWriter().println(message);
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }
}