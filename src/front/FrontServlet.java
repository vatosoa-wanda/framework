package front;

import java.io.IOException;
import jakarta.servlet.*; 
import jakarta.servlet.http.*;

// Ce servlet du framework ne fait que gérer les URL inconnues
public class FrontServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Le framework ne sait pas ce que l'app test a
        handleUnknownUrl(request.getRequestURI(), response);
    }

    private void handleUnknownUrl(String path, HttpServletResponse response) 
            throws IOException {
        response.getWriter().write("URL inconnue : " + path);
    }
}
