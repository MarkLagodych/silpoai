import { createSignal, For, Show } from "solid-js";
import "./app.css";
import { ShoppingItem, ShoppingList } from "./shoppingList.tsx";
import { createStore } from "solid-js/store";
import { autofillShoppingList } from "./ai.tsx";

export function App() {
    const [preferences, setPreferences] = createSignal("");

    const [items, setItems] = createStore<ShoppingItem[]>([]);

    const autofill = async () => {
        try {
            const newItems = await autofillShoppingList(
                preferences(),
                items.map((item) => item.name),
            );
            setItems((items) => [...items, ...newItems.map((name) => ({ name }))]);
        } catch (error) {
            console.error("Error autofilling shopping list:", error);
        }
    };

    return (
        <>
            <h1>Smart список покупок</h1>
            <button type="button" onClick={autofill}>
                ✨ Наповнити автоматично
            </button>
            &nbsp;Побажання:
            <input
                type="text"
                placeholder="Побільше фруктів та риби"
                value={preferences()}
                onInput={(e) => setPreferences(e.currentTarget.value)}
            />

            <ShoppingList search={() => {}} itemStore={[items, setItems]} />

            <button type="button">
                ✨ Додати вибране до кошика
            </button>
            &nbsp;
            <a href="https://silpo.ua/" target="_blank" rel="noopener noreferrer">
                Перевірити кошик ↗
            </a>
        </>
    );
}
