````markdown
# Installation Steps

1. **Clone the project from GitHub**  
   Open a terminal and run the following command:
   ```sh
   git clone https://github.com/AZIRARM/nodify.git
````

2. **Navigate to the project directory**

   ```sh
   cd nodify
   ```

3. **Configure environment variables**
   The `docker-compose.yml` file uses several environment variables.
   Default values are provided, but you can override them by creating a `.env` file at the root of the project or by exporting them before running Docker.

   Example `.env` file:

   ```env
   # Database
   MONGO_URL=mongodb://mongo:27017/nodify

   # Nodify Core
   ADMIN_PWD=Admin123              # Default admin password (change in production!)
   API_URL=http://localhost:9080   # URL of the Nodify API
   TZ=Europe/Paris                 # Timezone (configurable)

   # Nodify API
   # (inherits MONGO_URL and TZ)

   # Nodify UI
   CORE_URL=http://nodify-core:8080
   API_URL=http://localhost:9080
   ```

   > ⚠️ Make sure to update sensitive values such as `ADMIN_PWD` for production environments.

4. **Start the services**

   ```sh
   docker compose up -d
   ```

5. **Access the application**

   * **Nodify UI** → [http://localhost](http://localhost)
   * **Nodify API** → [http://localhost:9080](http://localhost:9080)

