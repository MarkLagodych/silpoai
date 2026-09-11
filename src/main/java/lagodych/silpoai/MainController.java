package lagodych.silpoai;

import com.google.genai.errors.ClientException;
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

    private final ChatClient chatClient;

    private final List<McpSyncClient> mcpClients;
    private final SyncMcpToolCallbackProvider mcpToolCallbacks;

    private final String systemPrompt =
            """
            You are an AI agent that simply executes MCP tools.
            Be as concise and efficient as possible. Do not provide any explanations or additional
            information. Do not ask questions. Always output a short list of things you've done.
            Answer in Ukrainian with inline HTML. Do not use Markdown formatting.
            """;

    MainController(ChatClient.Builder chatClientBuilder, List<McpSyncClient> clients) {
        this.chatClient = chatClientBuilder.build();
        this.mcpClients = clients;
        this.mcpToolCallbacks = SyncMcpToolCallbackProvider.builder().mcpClients(clients).build();
    }

    @GetMapping("/")
    ResponseEntity<String> index() {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/ai/auth")).build();
    }

    @GetMapping("/ai/ask")
    ResponseEntity<String> prompt(String prompt) {
        if (!StringUtils.hasText(prompt)) return ResponseEntity.ok().body("");

        String response;
        try {
            response =
                    chatClient
                            .prompt(prompt)
                            .system(systemPrompt)
                            .tools(mcpToolCallbacks)
                            .call()
                            .content();
        } catch (OAuth2AuthorizationException e) {
            // Frontend code should handle this manually and redirect the user to /ai/auth
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (ClientException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("AI request failed: " + e.getMessage());
        }

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/ai/auth")
    ResponseEntity<String> aiAuthorize() {
        for (var client : mcpClients) {
            if (!client.isInitialized()) {
                // May throw an OAuth authorization exception, which gets handled
                // by Spring Security and redirects to the MCP server login page
                client.initialize();
            }

            // May throw an OAuth authorization exception too
            client.listTools();
        }

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/index.html")).build();
    }
}
