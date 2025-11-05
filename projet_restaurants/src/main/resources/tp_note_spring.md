# Examen Backend de la formation Web services

**Lisez bien l'énoncé avant de vous lancer.**

## Introduction

Le guide Michelin a décidé de mettre à jour son application web permettant de gérer les
restaurants visités et leur classement.

Grâce à vos compétences, vous avez été choisis pour effectuer cette tâche.
Dans un premier temps, vous devez créer leur API à l'aide de la technologie Springboot (Java 17 ou plus).

## Définitions

Un **restaurant** est caractérisé par :

- un identifiant unique (un nombre entier positif)
- Un nom (longueur max de 90 caractères)
- Une adresse (longueur max de 255 caractères)
- Une liste d'**évaluations**
- une image présentant le restaurant

Une **évaluation** est caractérisée par :

- un identifiant unique (un nombre entier positif)
- Le nom de l'évaluateur (longueur max de 50 caractères)
- Le commentaire (longueur max de 255 caractères)
- Le nombre d'étoiles recommandé (0,1,2 ou 3) appellée "note"
- Une ou plusieurs photo des plats

## Cahier des charges

L'API doit exposer les fonctionnalités suivantes :

#### Restaurants

- La possibilité de récupérer tous les restaurants\*
- La possibilité de récupérer un restaurant en particulier\*
- La possibilité de créer un restaurant\*
- La possibilité de mettre à jour le nom et l'adresse d'un restaurant

#### Evaluations

- La possibilité d'ajouter une évaluation sur un restaurant
- La possibilité de supprimer une évaluation
- La possibilité de récupérer les évaluations en fonction d'un (ou plusieurs) mots clefsd
- La possibilité pour un utilisateur de récupérer toutes les évaluations qu'il a lui même créé.

Les routes retournant un (ou plusieurs) restaurant (marquées par \*) doivent aussi retourner la moyenne des notes du-dit restaurant dans une propriété nommée "moyenne". Si le restaurant ne dispose d'aucune evaluation, la moyenne est de -1.

Les cas d'erreur doivent être gérés pour retourner une erreur (404, 500, etc) contenant :

- Un code
- Un message expliquant l'erreur

### Authentification

L'authentifcation se base sur une solution externe compatible OpenID Connect.

- Il existe deux types de users : 
  - les evaluateurs qui peuvent créer et éditer leurs prorpes évaluations. Ils ont le role : USER
  - les admins qui ont tous les droits dont créer les restaurant et éditer n'importe quelle évaluation. Ils ont le role : ADMIN

#### utilisateurs pré-définis : 
- Lucien Bramard
  - username : ```lucien.bramard@michelin.fr```
  - password: ```lasecuriteavantout```
  - role : ```USER```

- Noël Flantier
  - username : ```noel.flantier@michelin.fr```
  - password : ```cestbeaulhiver```
  - role : ```ADMIN```

Le rôle se trouve dans le JWT qui permet d'identifié un utilisateur.

#### Tests

- Les services doivent être testés a l'aide de tests unitaires

#### Swagger

L'api doit exposer un swagger décrivant ses différentes routes

## Conseils supplémentaires

- Planifiez bien les tâches que vous devrez faire afin d'organiser au mieux vos dossiers et votre code dès le début.
- Mieux vaut faire peu correctement que beaucoup salement.
- Vous ne faites pas du code seulement pour vous-même mais aussi pour qu'il puisse être compris par quelqu'un d'autre, notamment moi.
- Les commentaires expliquant ce que vous faites sont bien sûr les bienvenus.
- La partie des rôles n'a pas été vue en cours, je sais, vous devrez fouiller la doc pour voir comment mapper les rôles d'un JWT à un droit d'accès dans votre code (Lire la documentation est une partie importante du métier d'ingénieur)
- Pour les tests, pensez bien à couvrir un maximum de cas (restaurant avec et sans evaluations, etc...)
- Vous avez accès à votre précédent travail et à internet.
- En revanche, votre dignité vous interdit de faire appel à vos camarades.
- Vous pouvez bien sûr créer un nouveau projet à partir de https://start.spring.io/

## Rendu

- Sur github : envoyez moi le lien et assurez-vous que j'ai accès au repo en lecture
- Par zip : envoyez moi le zip par mail ou par discord