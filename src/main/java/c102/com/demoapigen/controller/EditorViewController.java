package c102.com.demoapigen.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EditorViewController {

    /**
     * Forwards GET requests for the editor endpoint to the application's index resource.
     *
     * @return the view name {@code "forward:/demoapigen/index.html"} which causes an internal forward to {@code /demoapigen/index.html}
     */
    @GetMapping("/demoapigen/editor")
    public String editorPage() {
        return "forward:/demoapigen/index.html";
    }
}