import { createResource, Show } from "solid-js";
import "./App.css";

export function App() {
    const [response] = createResource(async () => {
        const res = await fetch("/ai");
        return res.text();
    });

    return (
        <>
            Привіт!
            <div>
                <Show when={!response.loading} fallback={<p>Loading...</p>}>
                    <p>AI Response: {response()}</p>
                </Show>
            </div>
        </>
    );
}
