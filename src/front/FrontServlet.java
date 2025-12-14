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

import annotation.Json;
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

        // Normalize path by removing trailing slash (except for root "/")
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        boolean resourceExists = getServletContext().getResource(path) != null;

        if (resourceExists) {
            defaultServe(req, res);
        } else {
            customServe(req, res);
        }
    }

    private void customServe(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String path = req.getRequestURI().substring(req.getContextPath().length());

        // Normalize path by removing trailing slash (except for root "/")
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

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
                    req.setAttribute("calledMethod", handlerMethod);
                    handleResult(result, path, req, res, handlerMethod);
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

    private void handleResult(Object result, String path, HttpServletRequest req, HttpServletResponse res, Method handlerMethod)
            throws IOException, ServletException {

        Method calledMethod = (Method) req.getAttribute("calledMethod");

        boolean isJson = false;

        if (calledMethod != null && calledMethod.isAnnotationPresent(Json.class)) {
            isJson = true;
        }

        if (isJson) {
            res.setContentType("application/json; charset=UTF-8");
            String json = toJsonResponse(result);
            res.getWriter().write(json);
            return;
        }

        // Autres types (int, etc.)

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

    // Ajoute une méthode utilitaire pour formatter la réponse JSON

    private String toJsonResponse(Object result) {
        StringBuilder sb = new StringBuilder();

        sb.append("{\n");
        sb.append("  \"statut\": \"success\",\n");

        sb.append("  \"code\": 200,\n");

        if (result instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) result;
            sb.append("  \"count\": ").append(list.size()).append(",\n");
            sb.append("  \"data\": ").append(listToJson(list)).append("\n");
        } else {
            sb.append("  \"data\": ").append(objectToJson(result)).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    private String listToJson(java.util.List<?> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(objectToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    private String objectToJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) {
            return "\"" + obj.toString() + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        // Simple POJO to JSON (fields only, no nested objects)
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            try {
                sb.append("\"").append(fields[i].getName()).append("\": ");
               Object val = fields[i].get(obj);

                if (val == null) sb.append("null");
                else if (val instanceof String) sb.append("\"").append(val).append("\"");
                else sb.append(val.toString());
            } catch (Exception e) {
                sb.append("\"error\"");
            }
            if (i < fields.length - 1) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
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