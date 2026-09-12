package lagodych.silpoai;

import com.google.genai.errors.ClientException;
import io.modelcontextprotocol.client.McpSyncClient;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
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

    private static final Set<String> SHOP_TOOLS =
            Set.of(
                    "silpo_find_address",
                    "silpo_get_available_delivery_types",
                    "silpo_list_branches",
                    "silpo_get_time_slots",
                    "silpo_find_products_batch",
                    "silpo_get_products",
                    "silpo_get_product_details",
                    "silpo_get_similar_products",
                    "silpo_get_replacements",
                    "silpo_get_my_favorites",
                    "silpo_get_categories",
                    "silpo_get_category",
                    "silpo_get_categories_tree",
                    "silpo_get_product_sets",
                    "silpo_get_my_shopping_cart",
                    "silpo_create_shopping_cart",
                    "silpo_get_shopping_cart_by_id",
                    "silpo_add_or_update_cart_products",
                    "silpo_update_shopping_cart",
                    "silpo_get_my_delivery_addresses",
                    "silpo_get_my_food_restrictions");

    MainController(ChatClient.Builder chatClientBuilder, List<McpSyncClient> clients) {
        this.chatClient = chatClientBuilder.build();
        this.mcpClients = clients;
        this.mcpToolCallbacks = SyncMcpToolCallbackProvider.builder().mcpClients(clients).build();
    }

    private ToolCallback[] filterTools(Set<String> toolNames) {
        return Arrays.stream(mcpToolCallbacks.getToolCallbacks())
                .filter(tool -> toolNames.contains(tool.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
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
                            .tools((Object[]) filterTools(SHOP_TOOLS))
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
