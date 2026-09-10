package lagodych.silpoai;

import io.modelcontextprotocol.client.McpSyncClient;
import java.net.URI;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    private final SyncMcpToolCallbackProvider mcpToolCallbacks;

    private final List<McpSyncClient> clients;

    private final ChatClient chatClient;

    MainController(ChatClient.Builder chatClientBuilder, List<McpSyncClient> clients) {
        this.chatClient = chatClientBuilder.build();
        this.mcpToolCallbacks = SyncMcpToolCallbackProvider.builder().mcpClients(clients).build();
        this.clients = clients;
    }

    @GetMapping("/")
    ResponseEntity<String> index() {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/ai/auth")).build();
    }

    @GetMapping("/ai/ask")
    ResponseEntity<String> prompt(String prompt) {
        if (!StringUtils.hasText(prompt)) return ResponseEntity.ok().body("");

        prompt += "\n\nAnswer in Ukrainian with inline HTML.";

        String response;
        try {
            response = chatClient.prompt(prompt).tools(mcpToolCallbacks).call().content();
        } catch (OAuth2AuthorizationException e) {
            // Frontend code should handle this and redirect the user to /ai/auth
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/ai/auth")
    ResponseEntity<String> aiAuthorize() {
        for (var client : clients) {
            if (!client.isInitialized()) {
                // May throw an OAuth authorization exception,
                // which gets handled by Spring security and redirects to the MCP server login page
                client.initialize();
            }

            // May throw an OAuth authorization exception too
            client.listTools();
        }

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/index.html")).build();
    }
}
