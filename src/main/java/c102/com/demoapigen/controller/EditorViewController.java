package c102.com.demoapigen.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EditorViewController {

    @GetMapping("/demoapigen/editor")
    public String editorPage() {
        return "forward:/demoapigen/index.html";
    }
}
