# Installation et premier démarrage

Ce guide explique pas à pas comment cloner, configurer et démarrer le projet depuis zéro.
Vous disposerez d'une API fonctionnelle, d'une base de données Postgres et d'un intercepteur
de mails en quelques minutes.

---

## Prérequis

Installez chaque outil de la liste ci-dessous avant de continuer. La version Java est
importante : Spring Boot 3.5.16 nécessite Java 17 et ne démarrera pas sur Java 8 ni Java 21
sans configuration explicite.

| Outil | Version | Comment vérifier |
|---|---|---|
| Java | 17 (LTS) | `java -version` |
| Maven | via le wrapper `./mvnw` (aucune installation nécessaire) | `./mvnw -v` |
| Docker | 24+ | `docker -v` |
| Docker Compose | 2.x (fourni avec Docker Desktop) | `docker compose version` |
| Git | toute version récente | `git --version` |

> **Pourquoi Docker ?** L'application a besoin de PostgreSQL 16 et de MailHog (intercepteur
> SMTP pour le développement). Docker Compose démarre les trois services (l'API, la base de
> données et l'intercepteur de mails) en une seule commande, sans installer Postgres
> localement.

---

## Option A - Stack complète via Docker Compose (recommandée)

Cette option construit et démarre l'application Spring Boot, PostgreSQL et MailHog ensemble.

```bash
git clone https://github.com/EdgarEldy/spring-boot-tutorial.git
cd spring-boot-tutorial
cp .env.example .env          # relisez et ajustez les valeurs si nécessaire
docker compose up --build
```

Docker Compose démarre trois conteneurs :

| Conteneur | Rôle | Port |
|---|---|---|
| `spring-boot-api` | Application Spring Boot | 8080 |
| `spring-boot-db` | PostgreSQL 16 | 5432 |
| `mailhog` | Intercepteur SMTP avec interface web | 8025 |

L'application est prête lorsque vous voyez une ligne similaire à
`Started SpringBootTutorialApplication in X seconds` dans les logs Docker.
Ouvrez `http://localhost:8080/swagger-ui.html` pour confirmer.

---

## Option B - Lancement Maven local (profil dev)

Utilisez cette option pour bénéficier du rechargement à chaud via DevTools ou pour itérer
sur le code.

**Étape 1 - Démarrer Postgres et MailHog :**

```bash
docker compose up spring-boot-db mailhog
```

**Étape 2 - Lancer l'application :**

```bash
# Linux / macOS
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=dev

# Windows (PowerShell)
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"; ./mvnw spring-boot:run `
  -Dspring-boot.run.profiles=dev
```

> **Pourquoi préciser JAVA_HOME ?** Si la version Java par défaut du système est 21 ou 8, le
> wrapper Maven l'utilisera à la place. Définir `JAVA_HOME` explicitement fixe le build à
> Java 17.

---

## Données de démarrage

Lorsque le profil `dev` est actif, `DataSeeder` s'exécute au démarrage et insère un
utilisateur admin initial ainsi que des catégories et des produits d'exemple (uniquement si
les tables sont vides).

| Champ | Valeur |
|---|---|
| Email | `admin@example.com` |
| Mot de passe | `Admin1@` |
| Rôle | `ADMIN` |

Utilisez ces identifiants pour votre première connexion via Swagger UI ou un client REST.

---

## Première requête via Swagger UI

Swagger UI à l'adresse `http://localhost:8080/swagger-ui.html` vous permet d'essayer
chaque endpoint sans client externe.

1. Dépliez **POST /auth/login**, cliquez sur **Try it out**.
2. Saisissez les identifiants de démarrage et exécutez.
3. Copiez le champ `access_token` depuis le corps de la réponse.
4. Cliquez sur **Authorize** (en haut à droite), collez `Bearer <token>` dans le champ, confirmez.
5. Toutes les requêtes suivantes de cette session porteront l'en-tête Authorization.

---

## Arrêt des services

```bash
# Arrêter les conteneurs (les données sont conservées)
docker compose down

# Arrêter les conteneurs et supprimer tous les volumes de données (repartir de zéro)
docker compose down -v
```

---

## Résolution des problèmes courants

**Le port 8080 est déjà utilisé.**
Trouvez et arrêtez le processus qui occupe le port 8080, ou modifiez `server.port` dans
`src/main/resources/application.yml`.

**Testcontainers échoue avec "Could not find a valid Docker environment".**
Testcontainers sonde `/var/run/docker.sock` par défaut. Si votre contexte Docker est
`desktop-linux`, relancez Maven avec le chemin du socket explicitement défini :

```bash
DOCKER_HOST=unix:///home/votreuser/.docker/desktop/docker.sock ./mvnw test
```

**L'application ne démarre pas : "JWT_SECRET environment variable not set".**
Copiez `.env.example` vers `.env` et définissez une valeur pour `JWT_SECRET` (au moins
32 caractères). Le profil `application-prod.yml` l'exige sans valeur par défaut pour éviter
un déploiement accidentel avec un secret faible.

**Erreur de migration Flyway au démarrage.**
Si vous avez modifié des fichiers de migration déjà appliqués, Flyway refusera de démarrer.
Supprimez et recréez la base de données, ou exécutez `docker compose down -v` pour effacer
le volume.
