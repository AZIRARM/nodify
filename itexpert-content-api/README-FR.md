# Documentation Développeur de l'API Nodify

## Vue d'ensemble
Bienvenue dans la documentation de l'API Nodify. Cette API fournit des endpoints pour gérer les retours utilisateurs (feedbacks), les nœuds de contenu, les affichages de contenu, les clics sur le contenu et les nœuds. L'API suit les principes RESTful et permet d'interagir avec divers composants du système Nodify.

**URL de base :** `http://localhost:9080`

---

## Endpoints

### Endpoints de Feedback
#### Récupérer tous les feedbacks
`GET /v0/feedbacks`
- **Réponse:** `200 OK`
- **Retourne:** Liste des objets feedback.

#### Créer un nouveau feedback
`POST /v0/feedbacks`
- **Corps de la requête:** Objet feedback.
- **Réponse:** `200 OK`
- **Retourne:** L'objet feedback créé.

#### Récupérer un feedback par ID
`GET /v0/feedbacks/id/{id}`
- **Paramètre de chemin:** `id` (UUID)
- **Réponse:** `200 OK`
- **Retourne:** Un objet feedback.

#### Supprimer un feedback par ID
`DELETE /v0/feedbacks/id/{id}`
- **Paramètre de chemin:** `id` (UUID)
- **Réponse:** `200 OK`
- **Retourne:** Booléen indiquant le succès.

#### Récupérer les feedbacks par ID utilisateur
`GET /v0/feedbacks/userId/{userId}`
- **Paramètre de chemin:** `userId` (String)
- **Réponse:** `200 OK`
- **Retourne:** Liste des feedbacks.

#### Récupérer les feedbacks par code de contenu
`GET /v0/feedbacks/contentCode/{code}`
- **Paramètre de chemin:** `code` (String)
- **Réponse:** `200 OK`
- **Retourne:** Liste des feedbacks.

---

### Endpoints des Nœuds de Contenu
#### Récupérer les données par code de contenu
`GET /v0/contents/code/{code}/data`
- **Paramètre de chemin:** `code` (String)
- **Paramètre de requête:** `status` (Optionnel, Par défaut: `PUBLISHED`, Enum: `SNAPSHOT`, `PUBLISHED`, `ARCHIVE`, `DELETED`)
- **Réponse:** `200 OK`
- **Retourne:** Liste des valeurs.

#### Sauvegarder les données par code de contenu
`PATCH /v0/contents/code/{code}/data`
- **Paramètre de chemin:** `code` (String)
- **Corps de la requête:** Objet valeur.
- **Réponse:** `200 OK`
- **Retourne:** Objet valeur mis à jour.

#### Récupérer tous les nœuds de contenu par code de nœud
`GET /v0/contents/node/code/{code}`
- **Paramètre de chemin:** `code` (String)
- **Réponse:** `200 OK`
- **Retourne:** Liste des nœuds de contenu.

#### Récupérer un nœud de contenu par code
`GET /v0/contents/code/{code}`
- **Paramètre de chemin:** `code` (String)
- **Réponse:** `200 OK`
- **Retourne:** Objet nœud de contenu.

---

### Vérification de l'état de l'API
#### Vérifier l'état de santé de l'API
`GET /health`
- **Réponse:** `200 OK`
- **Retourne:** Chaîne indiquant l'état de santé.

---

## Modèles de Données
### Feedback
```json
{
  "id": "uuid",
  "contentCode": "string",
  "evaluation": 0,
  "message": "string",
  "userId": "string",
  "verified": true
}
```

### Valeur
```json
{
  "id": "uuid",
  "key": "string",
  "value": "string"
}
```

### Nœud
```json
{
  "id": "uuid",
  "code": "string",
  "status": "PUBLISHED"
}
```

---

## Conclusion
Cette API offre un ensemble robuste d'endpoints pour gérer les feedbacks, les nœuds de contenu, les affichages, les clics et les nœuds dans le système Nodify. Pour toute assistance supplémentaire, contactez l'équipe de développement.