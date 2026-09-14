package lagodych.silpoai;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    @GetMapping("/")
    String index(HttpSession session) {
        if (session.getAttribute("alreadyRedirectedToAuth") == null) {
            session.setAttribute("alreadyRedirectedToAuth", true);
            return "redirect:/ai/auth";
        }

        return "forward:/index.html";
    }
}
