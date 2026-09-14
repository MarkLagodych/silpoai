export class BackendAuthorizationError extends Error {
    constructor(message: string) {
        super(message);
    }
}

export async function callAi(action: string, args: Record<string, unknown>): Promise<string> {
    const url = new URL("/ai/" + action, globalThis.location.href);
    Object.entries(args).forEach(([key, value]) => {
        url.searchParams.set(key, String(value));
    });

    const res = await fetch(url.toString());

    console.log(res);
    if (!res.ok) {
        if (res.status === 401 /* Unauthorized */) {
            globalThis.location.href = "/ai/auth";
            throw new BackendAuthorizationError("Unauthorized. Redirecting to auth page...");
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
    const response = await callAi("autofill", {
        preferences,
        items: currentItems.join("\n"),
    });

    return response.split("\n");
}

export async function addToCart(items: string[]): Promise<string> {
    return await callAi("shop", {
        items: items.join("\n"),
    });
}
