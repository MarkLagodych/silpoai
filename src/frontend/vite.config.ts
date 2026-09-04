import { defineConfig } from "vite";
import solid from "vite-plugin-solid";
import { resolve } from "node:path";

export default defineConfig({
    plugins: [solid()],
    build: {
        outDir: "../../build/resources/main/public",
        emptyOutDir: true,
    },
    input: {
        main: resolve(import.meta.dirname, "index.html"),
        login: resolve(import.meta.dirname, "login.html"),
    },
});
