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

    private final String shopPrompt =
            """
            You are an AI agent for a smart shopping list.
            Add all items from the user's shopping list to a shopping cart in the Silpo online store.
            Do not remove any items from the shopping cart.
            Use MCP tools as efficiently as possible.

            Output a short list of things that you have done.

            Do not provide any explanations or additional information.
            Do not ask questions.
            Answer in Ukrainian.
            Use plain text, do not use Markdown formatting.
            """;

    private static final String autofillSystemPrompt =
            """
            Role: AI Agent for smart shopping list autocompletion.
            Task: Predict items the user will likely buy based on their shopping history, prompt, and purchase frequency (regular or rare).

            Instructions:
            - Use silpo_get_my_offline_orders for offline shopping history.
            - Use silpo_get_my_online_orders for online shopping history.
            - Output Ukrainian language only (neutral tone), regardless of the input language.
            - Do not check the shopping cart.
            - Do not include any explanations, additional text, or questions.

            Output Format:
            - Plain text only (NO Markdown formatting).
            - One item per line using shortest, simplest phrase.
            - NO prices or quantities.
            - Do not include too many details.

            Examples:
            Good:
            хліб Український
            картопля
            Coca-Cola без цукру

            Bad:
            хліб білий цільнозерновий Український 500г
            картопля Гала молода 2кг
            напій газований Coca-Cola без цукру 1.5л

            Fallback:
            If either online or offline shopping history is empty, use the other one.
            If no relevant items can be generated, output why on a single line.
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
            Set.of("silpo_get_my_online_orders", "silpo_get_my_offline_orders");

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

    @GetMapping("/shop")
    ResponseEntity<Object> shop(@RequestParam(defaultValue = "") String items) {
        return wrapAiEndpoint(
                () -> {
                    String response =
                            chatClient
                                    .prompt(items)
                                    .system(shopPrompt)
                                    .tools((Object[]) filterTools(SHOP_TOOLS))
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
