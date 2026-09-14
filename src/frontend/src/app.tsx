import { createSignal, Show } from "solid-js";
import { createStore } from "solid-js/store";

import { ShoppingItem, ShoppingList } from "./shoppingList.tsx";
import { addToCart, autofillShoppingList } from "./ai.tsx";

import "./app.css";

export function App() {
    const [preferences, setPreferences] = createSignal("");

    const [items, setItems] = createStore<ShoppingItem[]>([]);

    const [statusMessage, setStatusMessage] = createSignal<string | null>(null);

    const onAutofill = async () => {
        try {
            setStatusMessage("Доповнюю список покупок...");

            const newItems = await autofillShoppingList(
                preferences(),
                items.map((item) => item.name),
            );
            setItems((items) => [...items, ...newItems.map((name) => ({ name }))]);

            setStatusMessage("Готово!");
        } catch (error) {
            console.error("Error autofilling shopping list:", error);

            setStatusMessage("Помилка при доповненні списку покупок. Спробуйте ще раз.");
        }
    };

    const onAddToCart = async () => {
        try {
            setStatusMessage("Додаю до кошика...");

            await addToCart(items.map((item) => item.name));

            setStatusMessage("Готово!");
        } catch (error) {
            console.error("Error adding items to cart:", error);

            setStatusMessage("Помилка при додаванні до кошика. Спробуйте ще раз.");
        }
    };

    return (
        <>
            <h1>Smart список покупок</h1>
            <button type="button" onClick={onAutofill}>
                ✨ Наповнити автоматично
            </button>
            &nbsp;Побажання:
            <input
                type="text"
                placeholder="Побільше фруктів та риби"
                value={preferences()}
                onInput={(e) => setPreferences(e.currentTarget.value)}
            />

            <ShoppingList itemStore={[items, setItems]} />

            <button type="button" onClick={onAddToCart}>
                ✨ Додати до кошика
            </button>
            &nbsp;
            <a href="https://silpo.ua/" target="_blank" rel="noopener noreferrer">
                Відкрити кошик ↗
            </a>

            <div>
                <Show when={statusMessage() !== null}>
                    <p>{statusMessage()}</p>
                </Show>
            </div>
        </>
    );
}
