package lagodych.silpoai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExceptionController {
    final String appName;

    public ExceptionController(@Value("${spring.application.name}") String appName) {
        this.appName = appName;
    }

    @ExceptionHandler
    String handleException(Exception e) throws Exception {
        switch (e) {
            case OAuth2AuthorizationException _ -> {
                throw e;
            }

            default -> {}
        }

        var trace = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(trace));

        return """
                <!DOCTYPE html>
                <html>
                <head> <title>%s: Error</title> </head>
                <body>
                    <h1>%s</h1> %s <hr/>
                    <h2>Cause</h2> %s <hr/>
                    <h2>Stack trace</h2> <pre>%s</pre>
                </body>
                </html>
                """
                .formatted(
                        appName, e.getClass().getSimpleName(), e.getMessage(), e.getCause(), trace);
    }
}
