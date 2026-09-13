import { createSignal, For, Show } from "solid-js";
import "./app.css";
import { ShoppingItem, ShoppingList } from "./shoppingList.tsx";
import { createStore } from "solid-js/store";

export function App() {
    const [prompt, setPrompt] = createSignal("");

    const [items, setItems] = createStore<ShoppingItem[]>([]);

    let [haveSearched, setHaveSearched] = createSignal(false);
    const search = (itemName: string) => {
        setHaveSearched(true);
        // TODO
    };

    return (
        <>
            <h1>Smart список покупок</h1>
            <button type="button">
                ✨ Наповнити автоматично
            </button>
            &nbsp;Побажання:
            <input
                type="text"
                placeholder="Побільше фруктів та риби"
                value={prompt()}
                onInput={(e) => setPrompt(e.currentTarget.value)}
            />

            <ShoppingList search={search} itemStore={[items, setItems]} />

            <Show when={haveSearched()}>
                <h2>Пошук</h2>
                <button type="button">
                    ✨ Додати вибране до кошика
                </button>
                &nbsp;
                <a href="https://silpo.ua/" target="_blank" rel="noopener noreferrer">
                    Перевірити кошик ↗
                </a>
                <div>
                    {/* TODO: Searched items */}
                </div>
            </Show>
        </>
    );
}
