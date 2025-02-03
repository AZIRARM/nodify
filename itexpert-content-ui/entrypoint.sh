#!/bin/sh

mv_folders() {
    mkdir -p "/usr/share/nginx/html/assets"
    mv "/usr/share/nginx/html/configurations" "/usr/share/nginx/html/assets/"
    mv "/usr/share/nginx/html/i18n" "/usr/share/nginx/html/assets/"
    mv "/usr/share/nginx/html/pictures" "/usr/share/nginx/html/assets/"
    mv "/usr/share/nginx/html/themes" "/usr/share/nginx/html/assets/"
}

# Fonction pour obtenir une configuration depuis une variable d'environnement
get_config() {
    key=$1
    default_value=$2

    value=$(printenv "$key")
    if [ -z "$value" ]; then
        value="$default_value"
    fi

    # Validation optionnelle (non applicable pour `sh`)
    echo "$value"
}

# Exemple d'utilisation
api_url_key=$(get_config "EXPERT_CONTENT_API_URL" "")
core_url_key=$(get_config "EXPERT_CONTENT_CORE_URL" "")

echo "EXPERT_CONTENT_CORE_URL : $core_url_key"
echo "EXPERT_CONTENT_API_URL : $api_url_key"

if [ -z "$api_url_key" ]; then
    echo "Variable d'environnement EXPERT_CONTENT_API_URL manquante !"
    api_url_key="$core_url_key"
fi

if [ -z "$core_url_key" ]; then
    echo "Variable d'environnement EXPERT_CONTENT_CORE_URL manquante !"
    exit 1
fi

# Variables personnalisables
#fichier_nginx="/etc/nginx/conf.d/default.conf" # Adaptez au chemin de votre fichier
fichier_nginx="/etc/nginx/nginx.conf" # Adaptez au chemin de votre fichier

echo "fichier_nginx : $fichier_nginx"

# Vérification de l'existence du fichier
if [ ! -f "$fichier_nginx" ]; then
    echo "Le fichier de configuration Nginx '$fichier_nginx' n'existe pas."
    exit 1
fi

# Sauvegarde du fichier
cp -p "$fichier_nginx" "${fichier_nginx}.bak"

# Remplacement avec sed
sed -i 's@EXPERT_CONTENT_CORE_URL@'"${core_url_key}"'@g' "$fichier_nginx"
sed -i 's@EXPERT_CONTENT_API_URL@'"${api_url_key}"'@g' "$fichier_nginx"

mv_folders

# Relance de Nginx
echo "Relance de Nginx"
nginx -g "daemon off;"
echo "Nginx Relancé avec succès"
