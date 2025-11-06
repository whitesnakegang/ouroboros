package kr.co.ouroboros.ui.frontend;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for serving the Ouroboros frontend SPA.
 *
 * <p>Handles routing for the React-based frontend application located at /ouroboros.
 * All client-side routes are forwarded to index.html to support React Router's
 * browser-based routing.</p>
 *
 * @author Ouroboros Team
 * @since 0.1.0
 */
@Controller
public class FrontendController {

    /**
     * Serves the frontend index.html for all /ouroboros routes.
     *
     * <p>This method handles both the root path and all sub-paths to enable
     * React Router's client-side routing. Static assets (CSS, JS) are served
     * directly by Spring Boot's static resource handling and are not affected
     * by this controller.</p>
     *
     * @return forward path to the frontend index.html
     */
    @GetMapping(value = {
        "/ouroboros",
        "/ouroboros/",
    })
    public String forwardToFrontend() {
        return "forward:/ouroboros/index.html";
    }
}