package url;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import sprint2bis.Controller;
import annotation.Json;
import annotation.UrlGet;
import annotation.UrlPost;
import framework.annotation.Param;
import url.UrlMapping;
import url.UrlPatternMatcher;

public class ScannerFramework {

    private Map<String, Map<String, Method>> urlMappings = new HashMap<>();
    private Map<String, Object> controllers = new HashMap<>();

    // Lance le scan à partir du chemin du répertoire /WEB-INF/classes
    public void scan(String basePath) {
        File baseDir = new File(basePath);
        if (baseDir.exists()) {
            try {
                scanPackage(baseDir, "");
                System.out.println("=== Scan terminé ===");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Dossier /WEB-INF/classes introuvable !");
        }
    }

    private void scanPackage(File directory, String packageName) throws Exception {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                scanPackage(file, packageName + file.getName() + ".");
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + file.getName().replace(".class", "");
                processClass(className);
            }
        }
    }

    private void processClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);

            // Vérifie si la classe est annotée @Controller
            if (clazz.isAnnotationPresent(Controller.class)) {
                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                controllers.put(clazz.getName(), controllerInstance);

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(UrlMapping.class)) {
                        UrlMapping mapping = method.getAnnotation(UrlMapping.class);
                        urlMappings.computeIfAbsent(mapping.value(), k -> new HashMap<>()).put(mapping.method(), method);

                        // Affichage lisible
                        String pkg = clazz.getPackage().getName();
                        System.out.printf("[Mapping] %-20s → %s.%s()  (HTTP %s)%n",
                                mapping.value(),
                                pkg + "." + clazz.getSimpleName(),
                                method.getName(),
                                mapping.method());
                    } else if (method.isAnnotationPresent(UrlGet.class)) {
                        UrlGet mapping = method.getAnnotation(UrlGet.class);
                        urlMappings.computeIfAbsent(mapping.value(), k -> new HashMap<>()).put("GET", method);

                        // Affichage lisible
                        String pkg = clazz.getPackage().getName();
                        System.out.printf("[Mapping] %-20s → %s.%s()  (HTTP GET)%n",
                                mapping.value(),
                                pkg + "." + clazz.getSimpleName(),
                                method.getName());
                    } else if (method.isAnnotationPresent(UrlPost.class)) {
                        UrlPost mapping = method.getAnnotation(UrlPost.class);
                        urlMappings.computeIfAbsent(mapping.value(), k -> new HashMap<>()).put("POST", method);

                        // Affichage lisible
                        String pkg = clazz.getPackage().getName();
                        System.out.printf("[Mapping] %-20s → %s.%s()  (HTTP POST)%n",
                                mapping.value(),
                                pkg + "." + clazz.getSimpleName(),
                                method.getName());
                    } else if (method.isAnnotationPresent(Json.class)) {
                        // @Json is a marker annotation (no path). Route is provided by @UrlGet/@UrlPost/@UrlMapping.
                        String pkg = clazz.getPackage().getName();
                        System.out.printf("[Mapping] %-20s → %s.%s()  (JSON API marker)%n",
                                "(uses @UrlGet/@UrlPost/@UrlMapping)",
                                pkg + "." + clazz.getSimpleName(),
                                method.getName());
                    } else if (method.isAnnotationPresent(UrlMapping.class)) {
                        UrlMapping mapping = method.getAnnotation(UrlMapping.class);
                        urlMappings.computeIfAbsent(mapping.value(), k -> new HashMap<>()).put("GET", method);

                        // Affichage lisible
                        String pkg = clazz.getPackage().getName();
                        System.out.printf("[Mapping] %-20s → %s.%s()  (HTTP GET)%n",
                                mapping.value(),
                                pkg + "." + clazz.getSimpleName(),
                                method.getName());
                    }
                }
            }

        } catch (ClassNotFoundException e) {
            // Classe inutile, on ignore
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 MAPPING DES PARAMÈTRES DE FORMULAIRE
    // ------------------------------
    public static Object[] mapFormParametersToMethodArgs(Method method, HttpServletRequest request) {
        return mapFormParametersToMethodArgs(method, request, null, null);
    }

    public static Object[] mapFormParametersToMethodArgs(Method method, HttpServletRequest request,
                                                     String urlPattern, String actualPath) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        Map<String, String> pathVars = (urlPattern != null && actualPath != null)
            ? UrlPatternMatcher.extractVariables(urlPattern, actualPath)
            : new HashMap<>();

        for (int i = 0; i < parameters.length; i++) {
            Parameter p = parameters[i];

            // Check if parameter is Map<String, Object>
            if (p.getType() == Map.class && p.getParameterizedType() instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) p.getParameterizedType();
                if (paramType.getActualTypeArguments().length == 2 &&
                    paramType.getActualTypeArguments()[0] == String.class &&
                    paramType.getActualTypeArguments()[1] == Object.class) {
                    // Create Map<String, Object> from request parameters
                    Map<String, Object> paramMap = new HashMap<>();
                    for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                        paramMap.put(entry.getKey(), entry.getValue().length > 0 ? entry.getValue()[0] : null);
                    }
                    args[i] = paramMap;
                    System.out.printf("[Param] Map<String, Object> populated with %d parameters%n", paramMap.size());
                    continue;
                }
            }

            // Check if parameter is a custom object (not primitive, not String, not Map)
            if (!p.getType().isPrimitive() && p.getType() != String.class && p.getType() != Map.class) {
                try {
                    Object obj = p.getType().getDeclaredConstructor().newInstance();
                    for (Field field : p.getType().getDeclaredFields()) {
                        field.setAccessible(true);
                        String fieldName = field.getName();
                        String paramValue = request.getParameter(fieldName);
                        if (paramValue != null) {
                            Object convertedValue = convertValue(paramValue, field.getType());
                            field.set(obj, convertedValue);
                        }
                    }
                    args[i] = obj;
                    System.out.printf("[Param] Custom object %s instantiated and populated%n", p.getType().getSimpleName());
                    continue;
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'instanciation de l'objet personnalisé: " + p.getType().getSimpleName());
                    e.printStackTrace();
                }
            }

            // Check if parameter is an array of custom objects
            if (p.getType().isArray() && !p.getType().getComponentType().isPrimitive() && p.getType().getComponentType() != String.class) {
                try {
                    // For simplicity, assume single object in array for now (can be extended)
                    Object[] array = (Object[]) Array.newInstance(p.getType().getComponentType(), 1);
                    Object obj = p.getType().getComponentType().getDeclaredConstructor().newInstance();
                    for (Field field : p.getType().getComponentType().getDeclaredFields()) {
                        field.setAccessible(true);
                        String fieldName = field.getName();
                        String paramValue = request.getParameter(fieldName);
                        if (paramValue != null) {
                            Object convertedValue = convertValue(paramValue, field.getType());
                            field.set(obj, convertedValue);
                        }
                    }
                    array[0] = obj;
                    args[i] = array;
                    System.out.printf("[Param] Array of custom objects %s[] instantiated and populated%n", p.getType().getComponentType().getSimpleName());
                    continue;
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'instanciation du tableau d'objets personnalisés: " + p.getType().getSimpleName());
                    e.printStackTrace();
                }
            }

            String value = null;

            // 1 Si annotation @Param
            if (p.isAnnotationPresent(Param.class)) {
                String name = p.getAnnotation(Param.class).value();
                value = request.getParameter(name);
                System.out.printf("[Param] Annotation @Param('%s') → valeur: %s%n", name, value);
            }

            // 2 Sinon on essaie par nom de paramètre
            if ((value == null || value.isEmpty()) && p.isNamePresent()) {
                value = request.getParameter(p.getName());
                System.out.printf("[Param] Nom paramètre '%s' → valeur: %s%n", p.getName(), value);
            }

            // 3 Sinon on regarde dans les path variables
            if ((value == null || value.isEmpty()) && pathVars.containsKey(p.getName())) {
                value = pathVars.get(p.getName());
                System.out.printf("[Param] Path variable '%s' → valeur: %s%n", p.getName(), value);
            }

            // 4 Conversion automatique
            args[i] = convertValue(value, p.getType());
            System.out.printf("[Param] Conversion %s → %s (%s)%n", value, args[i], p.getType().getSimpleName());
        }
        return args;
    }



    private static Object convertValue(String value, Class<?> type) {
        if (value == null) {
            // Retourne la valeur par défaut pour les types primitifs
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == double.class) return 0.0;
            if (type == float.class) return 0.0f;
            if (type == boolean.class) return false;
            return null;
        }
        
        try {
            if (type == String.class) return value;
            if (type == int.class || type == Integer.class) return Integer.parseInt(value);
            if (type == long.class || type == Long.class) return Long.parseLong(value);
            if (type == double.class || type == Double.class) return Double.parseDouble(value);
            if (type == float.class || type == Float.class) return Float.parseFloat(value);
            if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        } catch (Exception e) {
            System.err.println("Erreur conversion: " + value + " vers " + type.getSimpleName());
        }
        return null;
    }

    public Map<String, Map<String, Method>> getUrlMappings() {
        return urlMappings;
    }

    public Map<String, Object> getControllers() {
        return controllers;
    }
}