# Phase 8 — feature/integration (intégration finale)

Crée la branche `feature/integration` depuis `feature/order-authorization`,
puis merge `feature/product-images`. Valide l'ensemble du parcours e-commerce.

## Prérequis

- Phase 7 terminée (`feature/order-authorization` complète)
- Phase 3 terminée (`feature/product-images` complète)

## Instructions pour l'agent

### Étape 0 — Vérification Git

```bash
git log --oneline feature/order-authorization -1
git log --oneline feature/product-images -1
git branch | grep integration
```

### Étape 1 — Créer la branche

```bash
git checkout feature/order-authorization
git checkout -b feature/integration
```

### Étape 2 — Merger feature/product-images

```bash
git merge feature/product-images --no-edit
```

Résoudre les conflits éventuels :
- `ProductResponse.java` : garder les champs de product-images (`List<ProductImageResponse> images`) ET ceux de la chaîne principale
- `ProductController.java` : garder les endpoints images ET les endpoints catalogue
- `application.yml` : fusionner les configurations `app.storage` et `app.notifications`
- Tests : vérifier que les tests product et image sont tous présents

Commit de merge : `merge(feature/product-images): integrate product image management into integration branch`

### Étape 3 — Vérification de cohérence

Inspecter et corriger les éventuelles incohérences :

**Migrations Flyway :**
```bash
# Lister toutes les migrations présentes
ls src/main/resources/db/migration/
# Vérifier l'ordre : V1, V3, V4, V13, V14, V15, V16
# V2 est dans feature/auth (absent sur cette branche), c'est normal
```

**Entités :** vérifier que `Product` a bien `description`, `active`, `BigDecimal unitPrice` ET
`List<ProductImage> images` (si cette relation est ajoutée dans product-images).

**SecurityConfig :** vérifier que toutes les règles sont présentes :
- `GET /api/v1/products/**` : public
- `POST/PUT/DELETE /api/v1/products/**` : ROLE_ADMIN
- `POST /api/v1/products/*/images` : ROLE_ADMIN
- `DELETE /api/v1/products/*/images/*` : ROLE_ADMIN
- `GET /api/v1/cart/**` : ROLE_USER
- `POST /api/v1/orders` : ROLE_USER
- `GET /api/v1/orders/**` : ROLE_USER ou ROLE_ADMIN
- `PATCH /api/v1/orders/*/status` : ROLE_ADMIN

**Variables d'environnement dans `.env.example` :**
```
APP_STORAGE_UPLOAD_DIR=./uploads/products
APP_STORAGE_MAX_FILE_SIZE_MB=5
APP_NOTIFICATION_ADMIN_EMAIL=admin@example.com
```

### Étape 4 — Test d'intégration end-to-end

**EcommerceIntegrationTest** : `@SpringBootTest` + Testcontainers (PostgreSQL + MailHog mock)

Scénario complet :
1. Register userA (ROLE_USER)
2. Login userA → obtenir JWT
3. GET /api/v1/products → vérifier catalogue visible sans auth
4. POST /api/v1/cart/items → ajouter 2 produits différents
5. GET /api/v1/cart → vérifier les 2 items et le total
6. PATCH /api/v1/cart/items/{id} → modifier quantité
7. POST /api/v1/orders → checkout → commande créée, panier vidé
8. GET /api/v1/orders → userA voit sa commande
9. GET /api/v1/orders/{id} → détail avec items, status=PENDING
10. Register admin + login admin → obtenir JWT admin
11. PATCH /api/v1/orders/{id}/status → {"status": "CONFIRMED"} → 200
12. GET /api/v1/orders/{id} comme userA → status=CONFIRMED
13. Vérifier que userA ne peut pas voir la commande de userB (GET → 404)

### Étape 5 — Vérification aucune route /admin

```bash
grep -r "/admin" src/main/java/
# Doit retourner AUCUN résultat dans les controllers
```

### Étape 6 — docker-compose.yml

Vérifier/ajouter :
- Volume pour le stockage des images : `./uploads:/app/uploads`
- MailHog service déjà présent (vérifier)
- Variables `APP_NOTIFICATION_ADMIN_EMAIL`, `APP_STORAGE_UPLOAD_DIR` dans le service app

### Étape 7 — Mise à jour README complète

Le README doit couvrir :
- Vue d'ensemble du parcours e-commerce
- Tableau de tous les endpoints (catalogue, images, panier, commandes)
- Configuration requise (MailHog, variables d'env, stockage)
- Instructions de démarrage (docker compose up)
- Flux e-commerce illustré (étapes numérotées)
- Section "Migration des données historiques" (Customer/AppUser distinction)
- Exemples curl pour les endpoints principaux (sans secrets)
- Checklist features couvertes

### Étape 8 — Rapport final

Créer `.claude/ECOMMERCE_REPORT.md` avec :
- Fonctionnalités implémentées
- Branches créées et leurs bases
- Migrations Flyway (V13 à V16)
- Endpoints ajoutés ou modifiés
- Variables d'environnement nécessaires
- Tests ajoutés
- Limitations connues
- Instructions pour la PR vers develop


## Validation finale

### Agents a invoquer dans cet ordre

#### 1. Agent test-runner -- verification des tests

Invoquer l'agent test-runner apres avoir termine tous les commits d'implementation.
L'agent lance la suite complete et renvoie un rapport des echecs avec leur cause probable.
Pour chaque echec : corriger le code ou le test dans un commit atomique dedie, puis relancer
l'agent jusqu'a ce que la suite soit entierement verte.

Note : si Testcontainers echoue, verifier docker-desktop et relancer avec
DOCKER_HOST=unix:///home/edgareldy/.docker/desktop/docker.sock ./mvnw test

#### 2. Agent code-reviewer -- relecture du code produit

Une fois les tests verts, invoquer l'agent code-reviewer. L'agent verifie :
pattern contrat/implementation, ApiResponse<T>, @Transactional, absence de fuite d'entite
JPA, presence de tests pour chaque methode publique.
Corriger chaque ecart bloquant dans un commit atomique dedie avant de clore la branche.
Les ecarts mineurs peuvent etre traites ou documentes.

#### 3. Agent spring-boot-expert -- conseil technique (a la demande)

Invoquer l'agent spring-boot-expert a tout moment en cas de doute sur une approche
technique : choix de pattern, comportement du framework, risque de N+1, gestion des
transactions, etc. A consulter avant d'implementer une decision incertaine.
## Commits attendus (atomiques)

```
merge(feature/product-images): integrate product image management into integration branch
fix(security): ensure all e-commerce endpoints have correct authorization rules
fix(integration): resolve any entity/migration inconsistencies after merge
test(integration): add EcommerceIntegrationTest end-to-end scenario
feat(docker): add image storage volume and notification env vars to docker-compose.yml
docs(readme): complete e-commerce documentation with all endpoints and configuration
docs: add ECOMMERCE_REPORT.md with implementation summary
```

## Vérification finale avant PR

```bash
# Vérifier les migrations dans l'ordre
ls src/main/resources/db/migration/

# Vérifier aucune route /admin
grep -r "/admin" src/main/java/ --include="*.java"

# Vérifier tous les nouveaux types ont leur Javadoc
grep -rL "@author" src/main/java/ --include="*.java" | head -20
```

## Rappel — PR finale

La seule PR prévue est `feature/integration → develop`.
**Ne PAS créer la PR sans autorisation explicite de l'utilisateur.**

```bash
# Créer la PR seulement après autorisation :
gh pr create --base develop --head feature/integration \
  --title "feat(ecommerce): add e-commerce extension (cart, checkout, orders, images, notifications)" \
  --body "..."
```

## Règles impératives

- Javadoc anglais sur chaque nouveau type créé sur cette branche
- Un commit par fichier
- Lancer les tests (DOCKER_HOST=unix:///home/edgareldy/.docker/desktop/docker.sock ./mvnw test) et corriger tous les echecs avant de continuer
- Relire le code une fois les tests verts et corriger les findings bloquants avant de clore la branche
- Mettre a jour le README (section de la branche courante) en dernier commit
- **Ne pas pousser, ne pas créer la PR sans autorisation**
