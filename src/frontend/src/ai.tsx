import { Accessor, createResource, createSignal, For, Show } from "solid-js";

const aiAutofillUrl = "/ai/autofill";
const aiAuthUrl = "/ai/auth";

export class BackendAuthorizationError extends Error {
    constructor(message: string) {
        super(message);
    }
}

export async function callAi(url: URL): Promise<string> {
    const res = await fetch(url.toString());

    console.log(res);
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

export async function autofillShoppingList(
    preferences: string,
    currentItems: string[],
): Promise<string[]> {
    const url = new URL(aiAutofillUrl, globalThis.location.href);
    url.searchParams.set("preferences", preferences);
    url.searchParams.set("currentItems", currentItems.join("\n"));

    const response = await callAi(url);

    return response.split("\n");
}
