# Silpo AI hackaton submission

## Build & run

1. Create a `config/application.properties` file and enter API keys that are
    missing from [src/main/resources/application.properties](./src/main/resources/application.properties)
2. Build the frontend (requires Deno):
    ```sh
    cd src/frontend
    deno install
    deno run build
    ```
3. Build the backend:
    ```sh
    ./gradlew bootJar
    ```
4. Run:
    ```sh
    java -jar build/libs/silpoai-0.0.1.jar
    ```

### Develop

1. Start the frontend build daemon:
    ```sh
    cd src/frontend
    deno run dev
    ```
2. Run the backend:
    ```sh
    ./gradlew bootRun
    ```
