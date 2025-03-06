## Documentation Utilisateur : Nodify

Nodify est un système de gestion de contenu (CMS) headless puissant et flexible conçu pour vous permettre de créer et de gérer votre contenu efficacement.

Ce document vous guidera à travers les fonctionnalités principales de Nodify.

## Connexion Initiale et Mise à Jour de la Licence
* **Connexion :**
    * **Utilisateur :** admin
    * **Mot de passe :** Admin13579++

## Interface Principale et Configuration
* **Tableau de bord :** L'interface principale offre une vue d'ensemble de votre contenu et diverses options de configuration.
* **Gestion des utilisateurs :** Créez et gérez les utilisateurs, définissez leurs rôles et attribuez-leur un accès spécifique à certains nœuds.
* **Gestion des langues :** Configurez les langues prises en charge par votre site et gérez les traductions.

## Structure du Contenu : Nœuds
* **Nœuds :** Les nœuds sont les éléments de base de votre structure de contenu. Ils peuvent contenir différents types de contenu (texte, HTML, JSON, images, fichiers, etc.).
* **Hiérarchie :** Vous pouvez créer une hiérarchie de nœuds pour organiser votre contenu de manière logique.
* **Visibilité :** Un nœud en cours de modification ou l'un de ses contenus ne sera visible que lorsqu'il sera déployé.
* **Métadonnées :** Associez des métadonnées (paires clé/valeur, balises) à vos nœuds pour faciliter la recherche et le filtrage.
* **Création de nœuds :** Pour créer un nouveau nœud (projet/environnement...), allez dans "Mes projets" dans le menu et cliquez sur le bouton "+". Les sous-nœuds sont ajoutés de la même manière. Pour la création d'un nœud, seul un nom et une langue par défaut sont requis ; les autres champs sont facultatifs.
* **Recommandations :** Il est conseillé de créer un nœud pour un type de contenu ou une catégorie spécifique. Par exemple, pour un nœud "blog", il est recommandé de créer un sous-nœud pour les contenus HTML ou JSON et un sous-nœud séparé pour les images de vos articles. Un modèle de blog est fourni sur ce dépôt : [AZIRARM/nodify-templates](https://github.com/AZIRARM/nodify-templates). Importez-le et examinez comment les nœuds sont organisés.
* **Données personnalisées :** Sur chaque nœud et chaque contenu, vous avez la possibilité d'ajouter des données personnalisées (paires clé/valeur) accessibles dans le contenu via le mot-clé : `$value(CODE_VALUE)`.
* **Traductions :** Sur chaque nœud et chaque contenu, vous avez la possibilité d'ajouter des traductions de mots (paires clé/valeur et code langue) accessibles dans le contenu via le mot-clé : `$trans(CODE_MESSAGE)`.
* **Règles de gestion :** Sur chaque nœud et chaque contenu, vous avez la possibilité d'ajouter des règles de gestion en cliquant sur "Règles". Deux options sont disponibles : booléen ou date, et l'action peut être de désactiver ou d'activer un nœud ou un contenu. Si un nœud est désactivé et que la règle est activée, le nœud et ses sous-nœuds ainsi que tous leurs contenus seront inaccessibles. Si un contenu est désactivé, seul ce contenu sera désactivé.
* **Héritage des données :** Pour les valeurs et les traductions, un contenu peut accéder aux traductions et valeurs de ses nœuds parents...
* **Déploiement :** Vous avez la possibilité de déployer un nœud ou un contenu, ce qui le fait passer à l'état "PUBLISHED". Cliquez sur le bouton "Déployer". Dès qu'un changement est effectué sur un contenu ou un nœud, il passe à l'état "SNAPSHOT".
    * **SNAPSHOT vs. PUBLISHED :** Ce qui est en "SNAPSHOT" reste visible uniquement en "SNAPSHOT" et ne sera visible en "PUBLISHED" que lorsque vous l'aurez déployé.
    * **Versions :** La version "SNAPSHOT" déployée sera sauvegardée, et un petit bouton vert à gauche du nœud ou du contenu apparaîtra pour indiquer qu'il est déployé. Orange indique qu'il est en cours de modification, et rouge qu'il vient d'être créé.
    * **Historique des versions :** Vous avez toujours la possibilité de revenir à une version précédente en cliquant sur ce petit bouton. L'ensemble des versions apparaîtra, et vous pourrez revenir à une version antérieure (attention à ne pas abuser de cette manipulation).
* **Suppression :** Vous avez également la possibilité de supprimer un nœud ou un contenu via le bouton "Supprimer" sur la ligne du contenu ou du nœud concerné dans "Actions". Ce n'est pas une suppression définitive. La suppression définitive intervient lorsque vous cliquez sur la corbeille en haut. Là, deux choix s'offrent à vous : supprimer définitivement ou annuler la suppression du contenu ou du nœud. La suppression d'un nœud entraîne la suppression de ses sous-nœuds et de leurs contenus.
* **Création de contenu :** Pour créer du contenu sur un nœud ou un sous-nœud, vous devez d'abord créer au moins un nœud. Ensuite, allez sur la ligne du nœud concerné et cliquez sur "Contenus". Une fenêtre affichant les contenus du nœud s'ouvrira, et vous pourrez y créer des contenus de différents types.

## Création et Gestion du Contenu
* **Types de contenu :** Créez différents types de contenu (articles, pages, produits, etc.) en définissant les champs et les propriétés spécifiques à chaque type.
* **Édition de contenu :** Modifiez le contenu de vos nœuds directement dans l'interface, en utilisant un éditeur WYSIWYG ou en saisissant du code.
* **Traduction :** Les traductions sont gérées au niveau du nœud et du contenu. Vous pouvez traduire les champs de métadonnées et le contenu lui-même.

## Fonctionnalités Avancées
* **Multilinguisme :** Nodify est entièrement multilingue. Vous pouvez créer des versions traduites de votre contenu pour différents marchés.
* **Flux de travail :** Mettez en œuvre des flux de travail pour approuver les modifications de contenu avant la publication.
* **Intégrations :** Intégrez Nodify avec d'autres outils et services (CRM, commerce électronique, etc.).

## Sécurité
* **Contrôle d'accès :** Configurez les autorisations avec précision pour chaque utilisateur afin de protéger votre contenu.
* **Sauvegardes :** Sauvegardez régulièrement votre contenu pour éviter la perte de données.

## Assistance
Pour toute question ou problème, veuillez contacter notre assistance à l'adresse suivante : [Remplacer par l'adresse e-mail de support]

## Conclusion
Ce document vous a donné un aperçu des fonctionnalités de Nodify. Pour une utilisation plus approfondie, nous vous invitons à explorer les différentes options offertes par la plateforme.
