Voici ta documentation mise à jour pour inclure la configuration de `REDIS_URL` pour `nodify-api` :

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

   # Redis (optional)
   # If REDIS_URL is not provided, nodify-api will default to redis://redis:6379
   REDIS_URL=redis://localhost:6379

   # Nodify Core
   ADMIN_PWD=Admin123              # Default admin password (change in production!)
   API_URL=http://localhost:1080   # URL of the Nodify API
   TZ=Europe/Paris                 # Timezone (configurable)

   # Nodify API
   # (inherits MONGO_URL, TZ, and can use REDIS_URL)

   # Nodify UI
   CORE_URL=http://nodify-core:8080
   API_URL=http://localhost:1080
   ```

   > ⚠️ Make sure to update sensitive values such as `ADMIN_PWD` for production environments.
   > ⚠️ The `REDIS_URL` variable is optional; if not set, the application will use the default Redis service defined in `docker-compose.yml`.

4. **Start the services**

   ```sh
   docker compose up -d
   ```

5. **Access the application**

   * **Nodify UI** → [http://localhost](http://localhost)
   * **Nodify API** → [http://localhost:1080](http://localhost:1080)

