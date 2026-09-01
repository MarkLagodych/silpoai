package lagodych.silpoai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AIController {
    private final ChatClient chatClient;
    private final ToolCallbackProvider tools;

    public AIController(
            ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider) {
        chatClient = chatClientBuilder.build();
        this.tools = toolCallbackProvider;
    }

    @GetMapping("/ai")
    public String getAIResponse() {
        return chatClient.prompt("Які акції зараз у Сільпо?").tools(tools).call().content();
    }
}
