package lagodych.silpoai;

import io.modelcontextprotocol.client.McpSyncClient;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiController {

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

    private static final String autofillSystemPrompt =
            """
            You are an AI agent for a smart shopping list.
            Your task is to autocomplete a shopping list based on user's shopping history and preferences.
            Output a list of items that the user is likely to add to their shopping list this time.

            Output format:
            - one item per line, each described in the shortest and simplest phrase possible;
            - use Ukrainian language;
            - use plain text, no Markdown formatting.

            Do not specify brand names, prices, or quantities, use generic product names unless necessary to distinguish a particular type of a product.
            Examples: "молоко", "молоко 2.5%", "молоко безлактозне", "молоко кокосове".

            Consider:
            - the user's prompt;
            - the user's food restrictions;
            - what the user buys regularly;
            - what the user buys rarely but might buy this time.

            You can execute MCP tools to get the user shopping history.
            Be as efficient as possible.

            Do not provide any explanations or additional information.
            Do not ask questions.
            Use neutral Ukrainian language only, even if the user uses a different language.

            If you cannot output anything relevant, output "хліб" on a single line.
            """;

    private static final String autofillUserPromptTemplate =
            """
            User prompt (can be empty):
            %s

            Currently added shopping items (can be empty):
            %s
            """;

    private static String autofillUserPrompt(String preferences, String currentItems) {
        return String.format(autofillUserPromptTemplate, preferences, currentItems);
    }

    private static final Set<String> AUTOFILL_TOOLS =
            Set.of(
                    "silpo_get_my_online_orders",
                    "silpo_get_my_offline_orders",
                    "silpo_get_my_food_restrictions",
                    "silpo_get_my_family");

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

    private AiController(ChatClient.Builder chatClientBuilder, List<McpSyncClient> clients) {
        this.chatClient = chatClientBuilder.build();
        this.mcpClients = clients;
        this.mcpToolCallbacks = SyncMcpToolCallbackProvider.builder().mcpClients(clients).build();
    }

    private ToolCallback[] filterTools(Set<String> toolNames) {
        return Arrays.stream(mcpToolCallbacks.getToolCallbacks())
                .filter(tool -> toolNames.contains(tool.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
    }

    @FunctionalInterface
    private interface AiEndpoint {
        ResponseEntity<Object> run() throws RuntimeException;
    }

    private ResponseEntity<Object> wrapAiEndpoint(AiEndpoint endpoint) throws RuntimeException {
        try {
            return endpoint.run();
        } catch (Exception e) {
            if (NestedExceptionUtils.getRootCause(e)
                    instanceof ClientAuthorizationRequiredException) {
                // Frontend code should handle this manually and redirect the user to /ai/auth
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            throw e;
        }
    }

    @GetMapping("/autofill")
    ResponseEntity<Object> autofill(
            @RequestParam(defaultValue = "") String preferences,
            @RequestParam(defaultValue = "") String items) {

        String userPrompt = autofillUserPrompt(preferences, items);

        return wrapAiEndpoint(
                () -> {
                    String response =
                            chatClient
                                    .prompt(userPrompt)
                                    .system(autofillSystemPrompt)
                                    .tools((Object[]) filterTools(AUTOFILL_TOOLS))
                                    .call()
                                    .content();

                    return ResponseEntity.ok().body(response);
                });
    }

    @GetMapping("/auth")
    ResponseEntity<String> authorize() {
        for (var client : mcpClients) {
            if (!client.isInitialized()) {
                // May throw an OAuth authorization exception, which gets handled
                // by Spring Security and redirects to the MCP server login page
                client.initialize();
            }

            // May throw an OAuth authorization exception too
            client.listTools();
        }

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/")).build();
    }
}
