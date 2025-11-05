package kr.co.ouroboros.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OuroborosController {

    @GetMapping({"/ouroboros", "/ouroboros/"})
        public String index() {
            return "forward:/ouroboros/index.html";
        }

}
