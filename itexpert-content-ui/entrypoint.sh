#!/bin/sh

mv_folders() {
    mkdir -p "/usr/share/nginx/html/assets"
    mv "/usr/share/nginx/html/i18n" "/usr/share/nginx/html/assets/"
    mv "/usr/share/nginx/html/pictures" "/usr/share/nginx/html/assets/"
    mv "/usr/share/nginx/html/themes" "/usr/share/nginx/html/assets/"
    mv "/usr/share/nginx/html/icons" "/usr/share/nginx/html/assets/"
}

mv_folders

# Get environment variables
get_config() {
    key=$1
    default_value=$2

    value=$(printenv "$key")
    if [ -z "$value" ]; then
        value="$default_value"
    fi

    echo "$value"
}

api_url_key=$(get_config "API_URL" "")
core_url_key=$(get_config "CORE_URL" "")

echo "CORE_URL : $core_url_key"
echo "API_URL : $api_url_key"

if [ -z "$api_url_key" ]; then
    echo "Missing environment variable: API_URL!"
    api_url_key="$core_url_key"
fi

if [ -z "$core_url_key" ]; then
    echo "Missing environment variable: CORE_URL!"
    exit 1
fi

# Files
nginx_file="/etc/nginx/nginx.conf"

echo "nginx_file : $nginx_file"

if [ ! -f "$nginx_file" ]; then
    echo "Nginx configuration file '$nginx_file' does not exist."
    exit 1
fi

cp -p "$nginx_file" "${nginx_file}.bak"

# Replace placeholders in nginx config
sed -i 's@CORE_URL@'"${core_url_key}"'@g' "$nginx_file"

# Replace _API_URL_ in JS files
echo "Searching for _API_URL_ in JS files..."
find /usr/share/nginx/html -type f -name "*.js" | while read -r js_file; do
    if grep -q "_API_URL_" "$js_file"; then
        echo "Replacing _API_URL_ in $js_file"
        cp -p "$js_file" "${js_file}.bak"
        sed -i 's@_API_URL_@'"${api_url_key}"'@g' "$js_file"
    fi
done

# Start Nginx (for Docker)
echo "Starting Nginx"
nginx -g "daemon off;"
