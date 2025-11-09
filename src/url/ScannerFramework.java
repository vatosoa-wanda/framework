package url;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import sprint2bis.Controller;
// import url.UrlMapping;

public class ScannerFramework {

    private Map<String, Method> urlMappings = new HashMap<>();
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
                        urlMappings.put(mapping.value(), method);

                        // Affichage lisible
                        String pkg = clazz.getPackage().getName();
                        System.out.printf("[Mapping] %-20s → %s.%s()  (HTTP %s)%n",
                                mapping.value(),
                                pkg + "." + clazz.getSimpleName(),
                                method.getName(),
                                mapping.method());
                    }
                }
            }

        } catch (ClassNotFoundException e) {
            // Classe inutile, on ignore
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, Method> getUrlMappings() {
        return urlMappings;
    }

    public Map<String, Object> getControllers() {
        return controllers;
    }
}
