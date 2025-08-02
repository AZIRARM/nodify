# Documentation de l'API Nodify

Ce document fournit un aperçu complet de l'API du CMS Headless Nodify, conçue pour gérer le contenu, les données, les retours et les nœuds.

## Table des matières

1. Introduction
2. URL de base
3. Points de terminaison de l'API
    * Point de terminaison d'affichage du contenu
    * Point de terminaison de clic sur le contenu
    * Contrôleur de données
    * Point de terminaison du nœud de contenu
    * Point de terminaison des retours d’expérience
    * Point de terminaison de santé
    * Point de terminaison des nœuds
4. Modèles de données
5. Exemples d'utilisation
6. Gestion des erreurs
7. Authentification (si applicable)

## 1. Introduction

L'API Nodify permet aux développeurs d'interagir avec le CMS Headless Nodify de manière programmatique. Elle propose des points de terminaison pour gérer l'affichage des contenus, les clics, les objets de données, les nœuds de contenu, les retours d'expérience et les nœuds. Ce document présente les points de terminaison disponibles, les formats de requêtes/réponses et des exemples d'utilisation.

## 2. URL de base

L'URL de base de l'API Nodify est : `http://localhost:9080`

## 3. Points de terminaison de l'API

### Point de terminaison d'affichage du contenu

* **`GET /v0/content-displays/contentCode/{code}`**
    * Récupérer une entrée d'affichage de contenu par code de contenu.
    * Paramètres :
        * `code` (chemin, string, requis) : Le code du contenu.
    * Réponse : Objet `ContentDisplay`.

* **`PATCH /v0/content-displays/contentCode/{code}`**
    * Incrémenter le nombre d'affichages pour un code de contenu.
    * Paramètres :
        * `code` (chemin, string, requis) : Le code du contenu.
    * Réponse : booléen.

### Point de terminaison de clic sur le contenu

* **`GET /v0/content-clicks/contentCode/{code}`**
    * Récupérer un clic sur un contenu par code de contenu.
    * Paramètres :
        * `code` (chemin, string, requis) : Le code du contenu.
    * Réponse : Objet `ContentClick`.

* **`PATCH /v0/content-clicks/contentCode/{code}`**
    * Enregistrer un clic sur un contenu.
    * Paramètres :
        * `code` (chemin, string, requis) : Le code du contenu.
    * Réponse : booléen.

### Contrôleur de données

* **`POST /v0/datas/`**
    * Enregistrer un nouvel objet de données.
    * Corps de la requête : Objet `Data` (requis).
    * Réponse : Objet `Data`.

* **`GET /v0/datas/key/{key}`**
    * Trouver une donnée par clé.
    * Paramètres :
        * `key` (chemin, string, requis) : La clé de l'objet de données.
    * Réponse : Objet `Data`.

### Point de terminaison des retours d'expérience

* **`POST /v0/feedbacks/`**
    * Enregistrer un nouveau retour d'expérience.
    * Corps de la requête : Objet `Feedback` (requis).
    * Réponse : Objet `Feedback`.

* **`GET /v0/feedbacks/verified/{verified}`**
    * Récupérer les retours d'expérience selon leur statut de vérification.
    * Paramètres :
        * `verified` (chemin, booléen, requis).
    * Réponse : Tableau d'objets `Feedback`.

### Point de terminaison des nœuds

* **`GET /v0/nodes/`**
    * Récupérer tous les nœuds.
    * Paramètres :
        * `status` (requête, string, optionnel, défaut : "PUBLISHED").
    * Réponse : Tableau d'objets `Node`.

### Point de terminaison de santé

* Ce point de terminaison vérifie l'état de santé de l'application.

## 4. Modèles de données

Se référer à la documentation Swagger/OpenAPI pour des informations détaillées sur les schémas `Feedback`, `Data`, `ContentDisplay`, `ContentClick`, `ContentNode`, `Node` et `FeedbackCharts`.

## 5. Exemples d'utilisation

(Ajouter des extraits de code pour diverses requêtes API, en utilisant `curl` ou des clients HTTP spécifiques aux langages)

## 6. Gestion des erreurs

(Décrire les codes d'erreur courants et comment les gérer)

## 7. Authentification

(Si applicable, fournir des détails sur les méthodes d'authentification et leur utilisation)

### Exemple d'authentification par clé API

Si l'API requiert une authentification par clé API, vous devez inclure la clé dans l'en-tête de chaque requête.

```
Authorization: Bearer VOTRE_CLE_API
```

* **Où obtenir la clé API ?** Expliquer comment les développeurs peuvent obtenir leurs clés API.
* **Sécurité :** Insister sur l'importance de garder les clés API sécurisées et de ne pas les inclure dans le code côté client.

### Exemple d'authentification OAuth 2.0

Si l'API utilise OAuth 2.0, décrire les étapes pour obtenir un jeton d'accès.

1. **Obtenir un jeton d'autorisation :** Expliquer comment les développeurs peuvent obtenir un jeton d'autorisation.
2. **Utiliser le jeton d'accès :** Montrer comment inclure le jeton d'accès dans l'en-tête de chaque requête.