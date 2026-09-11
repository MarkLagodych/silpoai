import { Accessor, createResource, createSignal, Show } from "solid-js";
import "./App.css";

const aiAskUrl = "/ai/ask";
const aiAuthUrl = "/ai/auth";

export class BackendAuthorizationError extends Error {
    constructor(message: string) {
        super(message);
    }
}

export async function askAi(prompt: string): Promise<string> {
    const url = new URL(aiAskUrl, globalThis.location.href);
    url.searchParams.set("prompt", prompt);

    const res = await fetch(url.toString());

    if (!res.ok) {
        if (res.status === 401 /* Unauthorized */) {
            globalThis.location.href = aiAuthUrl;
            throw new BackendAuthorizationError("Unauthorized. Must redirect to " + aiAuthUrl);
        } else {
            if (res.body === null) {
                return "Unknown error";
            }

            return "Error: " + await res.text();
        }
    }

    return res.text();
}

export function AiResponse(props: { prompt: Accessor<string> }) {
    const [response] = createResource(props.prompt, askAi);

    return (
        <div>
            AI Response:
            <Show when={!response.loading} fallback={<p>Loading...</p>}>
                <div innerHTML={response()}></div>
            </Show>
        </div>
    );
}

export function App() {
    const [prompt, setPrompt] = createSignal("");
    const [effectivePrompt, setEffectivePrompt] = createSignal("");

    return (
        <>
            <input
                type="text"
                placeholder="Enter your message..."
                on:input={(e) => setPrompt(e.currentTarget.value)}
            />
            <button type="button" onClick={() => setEffectivePrompt(prompt())}>Send</button>
            <AiResponse prompt={effectivePrompt} />
        </>
    );
}
