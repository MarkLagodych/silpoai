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

        var llmAnswerBlock = "";
        if (StringUtils.hasText(query)) {
            var llmAnswer =
                    chatClient
                            .prompt(query + "\nWrite your answer in inline HTML.")
                            .tools(mcpToolCallbacks)
                            .call()
                            .content();

            llmAnswerBlock =
                    """
                    <h2>[%s]</h2>
                    <p>%s</p>
                    <form action="" method="GET">
                    <button type="submit">Clear</button>
                    </form>
                    """
                            .formatted(query, llmAnswer);
        }

        var currentMcpServersBlock =
                this.clients.stream()
                        .map(McpSyncClient::getClientInfo)
                        .map(McpSchema.Implementation::name)
                        .map("    <li>%s</li>"::formatted)
                        .collect(Collectors.joining("\n"));

        return """
                <html>
                <head>
                    <title>Silpo AI</title>
                </head>
                <body>
                    <h1>LLM chat with Silpo MCP</h1>
                    %s

                    <hr>

                    <h2>Ask LLM</h2>
                    <form action="" method="GET">
                        <input type="text" name="query" value="" placeholder="Hi there!" />
                        <button type="submit">Ask</button>
                    </form>

                    <h2>Registered MCP servers:</h2>
                    <ul>
                    %s
                    </ul>
                </body>
                </html>
                """
                .formatted(llmAnswerBlock, currentMcpServersBlock);
    }

    // @ExceptionHandler
    // String handleException(Exception e) {
    //     // switch (e) {
    //     //     case instanceof org.springframework.web.client.HttpClientErrorException.NotFound
    // notFound -> {
    //     //         throw e;
    //     //     }
    //     // }

    //     var trace = new StringWriter();
    //     e.printStackTrace(new java.io.PrintWriter(trace));

    //     return """
    //             <html>
    //             <head>
    //                 <title>Silpo AI: Exception</title>
    //             </head>
    //             <body>
    //                 <h1>%s</h1>
    //                 <h2>%s caused by:</h2>
    //                 %s
    //                 <hr/>
    //                 <h2>Stack trace:</h2>
    //                 <pre>%s</pre>
    //             </body>
    //             </html>
    //             """
    //             .formatted(e.getMessage(), e.getClass().getSimpleName(), e.getCause(), trace);
    // }
}
