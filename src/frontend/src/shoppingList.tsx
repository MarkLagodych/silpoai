import { For, Show } from "solid-js";
import { StoreReturn } from "solid-js/store";

export interface ShoppingItem {
    id: number;
    name: string;
}

const shoppingItemPlaceholders = [
    "Хліб",
    "Молоко",
    "Все для борщу",
    "Щось до чаю",
];

const getRandomShoppingItemPlaceholder = () => {
    return shoppingItemPlaceholders[Math.floor(Math.random() * shoppingItemPlaceholders.length)];
};

export function ShoppingList(
    props: {
        search: (itemName: string) => void;
        itemStore: StoreReturn<ShoppingItem[]>;
    },
) {
    let _nextId = 1;
    const nextId = () => _nextId++;

    const [items, setItems] = props.itemStore;

    const setItemName = (index: number, oldItem: ShoppingItem, name: string) => {
        setItems(index, { ...oldItem, name: name });
    };

    const deleteItem = (index: number) => {
        setItems((items) => items.filter((_, i) => i !== index));
    };

    const insertItem = (index: number, value: string) => {
        setItems((
            items,
        ) => [...items.slice(0, index), { id: nextId(), name: value }, ...items.slice(index)]);

        focusItem(index);
    };

    const focusItem = (index: number) => {
        setTimeout(() => {
            const input =
                document.querySelectorAll<HTMLInputElement>(".shopping-item-input")[index];

            input?.focus();
        }, 0);
    };

    const handleKeyDown = (index: number, e: KeyboardEvent) => {
        switch (e.key) {
            case "Enter":
                e.preventDefault();
                if (items[index].name === "") {
                    return;
                }
                insertItem(index + 1, "");
                focusItem(index + 1);
                break;

            case "Backspace":
            case "Delete":
                if (items[index].name === "") {
                    e.preventDefault();
                    deleteItem(index);
                    focusItem(index - 1);
                }
                break;

            case "ArrowUp":
                e.preventDefault();
                if (index > 0) {
                    focusItem(index - 1);
                }
                break;

            case "ArrowDown":
                e.preventDefault();
                if (index < items.length - 1) {
                    focusItem(index + 1);
                }
                break;
        }
    };

    const deleteIfEmpty = (index: number) => () => {
        if (items[index].name === "") {
            deleteItem(index);
        }
    };

    insertItem(0, "");

    return (
        <ul>
            <For each={items}>
                {(item, index) => (
                    <li>
                        <input
                            class="shopping-item-input"
                            type="text"
                            value={item.name}
                            onInput={(e) => setItemName(index(), item, e.currentTarget.value)}
                            onKeyDown={(e) => handleKeyDown(index(), e)}
                            placeholder={getRandomShoppingItemPlaceholder()}
                            onBlur={deleteIfEmpty(index())}
                        />
                        <Show
                            when={item.name !== ""}
                        >
                            <button
                                type="button"
                                onClick={() => deleteItem(index())}
                            >
                                🗑️
                            </button>
                            <button type="button" onClick={() => props.search(item.name)}>
                                ✨ Знайти
                            </button>
                        </Show>
                    </li>
                )}
            </For>
            <li>
                <button type="button" onClick={() => insertItem(items.length, "")}>
                    ➕ Додати
                </button>
            </li>
        </ul>
    );
}
