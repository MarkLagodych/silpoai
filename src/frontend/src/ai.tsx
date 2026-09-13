import { Accessor, createResource, createSignal, For, Show } from "solid-js";

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
            throw new BackendAuthorizationError("Unauthorized. Redirecting to " + aiAuthUrl);
        } else {
            if (res.body === null) {
                throw new Error("Unknown error");
            }

            throw new Error("Error: " + await res.text());
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
