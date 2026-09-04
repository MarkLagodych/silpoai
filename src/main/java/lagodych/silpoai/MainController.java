package lagodych.silpoai;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
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
    String index(String query) {
        var currentWeatherBlock = "";
        if (StringUtils.hasText(query)) {
            var chatResponse = chatClient.prompt(query).tools(mcpToolCallbacks).call().content();

            currentWeatherBlock =
                    """
					<h2>Weather in %s</h2>
					<p>%s</p>
					<form action="" method="GET">
					<button type="submit">Clear</button>
					</form>
					"""
                            .formatted(query, chatResponse);
        }

        var currentMcpServersBlock =
                this.clients.stream()
                        .map(McpSyncClient::getClientInfo)
                        .map(McpSchema.Implementation::name)
                        .map("    <li>%s</li>"::formatted)
                        .collect(Collectors.joining("\n"));

        return """
				<h1>Demo controller</h1>
				%s

				<hr>

				<h2>Ask LLM</h2>
				<form action="" method="GET">
				    <input type="text" name="query" value="" placeholder="Hi there!" />
				    <button type="submit">Ask the LLM</button>
				</form>

				<hr>

				<h2>Registered MCP servers:</h2>
				<ul>
				%s
				</ul>
				"""
                .formatted(currentWeatherBlock, currentMcpServersBlock);
    }
}
