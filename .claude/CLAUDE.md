# CLAUDE.md — Mémoire de contexte du projet

> **À lire en premier à chaque nouvelle session, avant de continuer l'implémentation.**
> La spécification complète et détaillée est dans `README.md` à la racine (en anglais) — ce
> fichier n'en est pas une copie, il sert de mémoire d'implémentation : décisions prises,
> écarts, état d'avancement. Mets-le à jour toi-même dès qu'une décision technique est prise
> ou qu'une branche avance/termine.

## Règles impératives (feedback utilisateur)

- **Jamais de `Co-Authored-By: Claude` / `Anthropic` dans les messages de commit.**
  L'utilisateur commit lui-même ou valide le message avant tout commit ; ne jamais ajouter
  cette ligne, y compris dans le template de commit habituel de Claude Code.
- **Javadoc obligatoire sur toute nouvelle classe, interface, enum, record, etc.**
  À chaque création d'un de ces types, ajouter un bloc Javadoc juste avant sa déclaration,
  rédigé en anglais, au format suivant (adapter la description au rôle réel du type,
  la date du jour au format `M/D/YY`, et le nom du projet) :

  ```java
  /**
   * <Description du rôle de la classe/interface/enum/record, 1 à 3 lignes>
   * <p>
   * Created edgar.muhamyangabo on <M/D/YY>
   * Author : edgar.muhamyangabo
   * Date : <M/D/YY>
   * Project : spring-boot-tutorial
   */
  ```

  - Author fixe : `edgar.muhamyangabo` (quel que soit le git user.name configuré sur la machine).
  - Format de date : `M/D/YY` sans zéro de tête (ex. 4 juillet 2026 → `7/4/26`).
  - S'applique à tous les packages (`entity`, `dto`, `mapper`, `service`, `service/impl`,
    `controller`, `repository`, `exception`, `security`, `event`, `aspect`, `config`, etc.),
    dès `feature/config` et pour toutes les branches suivantes.
- **Jamais de tiret cadratin (le caractere "em dash") dans le code.**
  Ni dans le Javadoc, ni dans les commentaires, ni dans les chaines de caracteres, ni dans les
  logs. Utiliser un tiret simple `-`, une virgule, ou reformuler en deux phrases. A eviter
  aussi, par coherence, dans les autres fichiers du projet (README, YAML, SQL, Dockerfile).
- **Commits atomiques : un commit par fichier cree ou modifie.**
  Ne pas grouper plusieurs fichiers non lies dans un seul commit. Chaque fichier source cree
  ou modifie (entite, DTO, mapper, service, controleur, config, migration Flyway, etc.) doit
  faire l'objet de son propre commit, avec un message qui decrit precisement ce changement.
  Les fichiers etroitement couples (ex. une interface de service et son implementation creees
  ensemble, ou un DTO et son mapper) peuvent partager un commit si les separer n'aurait pas de
  sens fonctionnellement, mais par defaut privilegier un commit par fichier.
- **Format de message de commit (Conventional Commits + corps pedagogique la ou c'est utile).**
  Sujet : `type(scope): resume imperatif court` (types usuels : feat, fix, refactor, docs,
  test, chore, build, ci, style, perf ; scope = package ou domaine concerne, ex. `dto`,
  `controller`, `security`, `flyway`, `docker`, `ci`). Une ligne vide, puis un corps en
  paragraphe(s) expliquant ce qui change, comment, et surtout **pourquoi**, redige comme pour
  un lecteur qui suit le tutoriel (projet educatif) : expliquer le concept/pattern demontre,
  pas juste reformuler le diff. Le corps n'est pas obligatoire pour un changement trivial et
  auto-explicite, mais reste la norme par defaut des qu'un concept merite d'etre explique.
  Reste concis et précis sur ce qui a changé dans ce commit précis et pourquoi : ne pas
  réexpliquer des notions Java/Spring déjà établies dans le codebase (ex. "qu'est-ce qu'un
  record", déjà expliqué sur `ApiResponse`/`PageResponse`) à chaque nouveau fichier qui les
  utilise. N'explique un mécanisme Spring que s'il est directement pertinent pour comprendre
  la décision prise ici (ex. pourquoi `@Query` plutôt qu'une méthode dérivée sur ce cas
  précis), pas comme un rappel de cours générique. Exemple :

  ```
  refactor(controller): rewrite CategoryController with DTOs and validation

  Controllers now depend on CategoryService (interface). POST handlers
  receive @Valid CategoryDto bound to the form and check BindingResult
  before calling the service. Successful writes redirect with a flash
  message so the user sees feedback after the redirect.
  ```

## Stack technique — versions exactes retenues

- Spring Boot **3.5.16** (Spring Framework 6) — attention : la version Maven réelle n'a **pas**
  de suffixe `.RELEASE` (contrairement à l'identifiant `3.5.16.RELEASE` renvoyé par l'API
  Spring Initializr `/metadata/client`, qui est un identifiant interne). Vérifié directement
  sur `repo.maven.apache.org`.
- Java **17** (LTS) — le système a aussi Java 8/21 installés ; utiliser explicitement
  `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` pour tous les builds Maven
- Maven (wrapper `./mvnw` fourni par Spring Initializr)
- PostgreSQL 16 (Docker Compose)
- Flyway (`flyway-core` + `flyway-database-postgresql`)
- Spring Data JPA / Hibernate
- MapStruct **1.6.3** + Lombok — annotation processors dans cet ordre exact dans
  `maven-compiler-plugin` : `mapstruct-processor`, `lombok`, `lombok-mapstruct-binding` (0.2.0)
- Jakarta Bean Validation
- springdoc-openapi-starter-webmvc-ui **2.8.16**
- Spring Boot Actuator
- Spring Security 6 + JWT — jjwt **0.12.6** (api/impl/jackson)
- Spring AOP (`spring-boot-starter-aop`)
- Tests : JUnit 5, Mockito, Testcontainers (junit-jupiter + postgresql), AssertJ, spring-security-test
- CI : GitHub Actions · Conteneurisation : Docker + docker-compose
- Repo GitHub : https://github.com/EdgarEldy/spring-boot-tutorial.git

### Gestion des versions dans le pom.xml

- Toutes les versions de dépendances hors-BOM Spring Boot sont dans `<properties>`
  (`springdoc-openapi.version`, `mapstruct.version`, `lombok-mapstruct-binding.version`,
  `jjwt.version`) et référencées via `${...}`.
- `dependencyManagement` importe explicitement le BOM `spring-boot-dependencies` en
  `${project.parent.version}`, en plus de l'héritage via `<parent>` (redondant mais demandé
  explicitement par l'utilisateur — ne pas retirer sans lui demander).
- Ne jamais mettre de `<version>` en dur directement sur une `<dependency>` gérée par
  `dependencyManagement` ou par le BOM Spring Boot.

## Package racine et conventions de code

Package racine : `com.edgareldy.springboottutorial` (renomme depuis `edgareldy.springboottutorial` le
9/3/26, voir la section dediee plus bas pour le detail du rename et sa cascade de branches).

- **DTOs en `record`** Java (immutables), jamais de classes pour les DTOs
- **Pattern contrat/implémentation pour les services** :
  - interface `XxxService` à la racine de `service/`
  - implémentation `XxxServiceImpl` dans `service/impl/`, annotée `@Service`
  - contrôleurs et tests injectent **toujours l'interface**, jamais l'impl directement
  - `@Transactional` sur les méthodes d'écriture, posée sur l'**implémentation**
- Aucune logique métier dans les contrôleurs : uniquement délégation au service
- **Toutes** les réponses de contrôleur (succès et erreurs, y compris `GlobalExceptionHandler`)
  sont enveloppées dans `ApiResponse<T>` (`dto/common/ApiResponse.java`)
- Les listes renvoient `PageResponse<T>` (jamais une `List<T>` nue) comme `data` de `ApiResponse`
- Endpoints au pluriel, kebab-case si composés ; chaque endpoint documenté avec `@Operation`
- **Tous les endpoints REST sont prefixes par `/api/v1`** (ex. `/api/v1/categories`), decide
  avant de commencer `feature/products` afin que la convention de versioning soit posee des le
  socle technique plutot que rajoutee apres coup. Chaque `@RestController` doit porter
  `@RequestMapping("/api/v1/...")` (ou equivalent) en consequence.
- Voir le README (§ "Project structure" et § "Code conventions") pour l'arborescence complète
  des packages — ne pas la dupliquer ici, seulement noter les écarts éventuels ci-dessous.

## Stratégie de branches et ordre de travail

`master` (prod, merge only) ← `develop` (intégration) ← `feature/*`

Ordre initial (README) :

1. `feature/core-architecture` (anciennement `feature/config`) — socle technique
2. `feature/products` — Category + Product
3. `feature/customers`
4. `feature/orders` — depend de products ET customers
5. `feature/auth` — securise l'ensemble
6. `develop` -> `master`

Branches ajoutees lors de l'adaptation SST -> SBT (session 10/30/26) :

6. `feature/roles-permissions` — Role/Permission JPA + CRUD + MethodSecurity
7. `feature/token-management` — ActivationToken, BlacklistedToken, PasswordResetToken, EmailService async, SecureTokenGenerator, TokenCleanupScheduler
8. `feature/user-management` — AppUser refacto (Role enum -> Role JPA), role_user join table (V7), UserController, endpoints admin

Branches e-commerce (11/19-20/26) :

9. `feature/shopping-cart` — Cart/CartItem, 5 endpoints /api/v1/cart
10. `feature/order-checkout` — checkout atomique panier->commande, V18 user_id sur orders
11. `feature/order-notifications` — OrderEmailService async, NotificationProperties, OrderCreatedEventListener enrichi
12. `feature/order-authorization` — role-aware findAll/findById, updateStatus lifecycle, OrderAuthorizationTest
13. `feature/integration` — merge feature/order-authorization + feature/product-images, EcommerceIntegrationTest e2e

Ne jamais commiter directement sur `master`. Ne jamais merger `develop` dans une branche
`feature/*` (sens unique : feature -> develop uniquement).

**Flux PR effectif depuis la session 9/3/26 :** `gh` est disponible et authentifie (HTTPS).
Chaque branche feature fait l'objet d'une vraie PR GitHub vers `develop` (`gh pr create`),
CI verifiee, puis mergee (`gh pr merge --merge`). `develop` est ensuite sync localement
(`git pull`). Les branches anterieure a la PR #1 (9/3/26) n'ont pas de PR retroactive.

## État d'avancement des branches

- [x] `master`, `develop` créées et poussées sur GitHub. `develop` mergée dans `master`
      (localement, merge commit via `ort`, pas de push) le 8/7/26 une fois toutes les
      branches `feature/*` terminees et 152 tests verts. Le push vers `origin/master` est
      laisse a l'utilisateur, comme pour toutes les autres branches.
- [x] `feature/core-architecture` (anciennement `feature/config`) — terminée, relue par
      code-reviewer, mergée dans `develop`.
- [x] `feature/products` — terminée, mergée dans `develop` (CRUD Category + Product complet :
      entites, repositories, DTOs, mappers, services, controllers, tests
      unitaires/repository/controller, relue par code-reviewer, tous les gaps corriges).
      53 tests passent (etat original pre-ecommerce).
      **Extension e-commerce (11/17/26) :** `Product` etendu avec `description` (VARCHAR 500,
      nullable), `active` (BOOLEAN NOT NULL DEFAULT TRUE), `unitPrice` migre de REAL vers
      NUMERIC(12,2)/BigDecimal (migration V13). `ProductRequest`/`ProductResponse` mis a jour.
      `ProductMapper` ignore `active` sur toutes les ecritures. `ProductSpecifications`,
      `ProductService`, `ProductController` : `Float` -> `BigDecimal` sur `minPrice`/`maxPrice`.
      `.gitignore` corrige : `.claude/` traque desormais les fichiers de projet (CLAUDE.md,
      GAP_ANALYSIS.md, agents/, commands/, skills/) ; settings.local.json et memory/ restent
      ignores. 87/87 tests verts. Branche non poussee (30 commits en avance sur origin).
- [x] `feature/product-images` — terminee (11/17/26). Branch depuis le HEAD de
      `feature/products`. Implementation : migration V16 (`product_images` avec index unique
      partiel `WHERE is_primary = TRUE`), entite `ProductImage`, `StorageProperties`
      (`@ConfigurationProperties(prefix="app.storage")`), `StorageConfig`,
      `FileStorageService` interface + `LocalFileStorageService` (UUID filename, validation
      extension/MIME/taille, path traversal guard), `ProductImageRepository`,
      `ProductImageResponse`, `ProductImageMapper`, `ProductImageService` interface +
      `ProductImageServiceImpl` (gestion primary swap, ownership check sur delete),
      `ProductServiceImpl.findById` charge les images via `ProductImageRepository` et
      delegue a `ProductMapper.toDetailResponse`, `ProductResponse` gagne un 8e champ
      `List<ProductImageResponse> images` (vide sur findAll, peuple sur findById),
      `ProductController` gagne POST `/{id}/images` et DELETE `/{id}/images/{imageId}`.
      Tests : `ProductImageServiceImplTest` (6 cas), `LocalFileStorageServiceTest` (7 cas),
      `ProductControllerTest` etendu (+2 endpoints, `@MockitoBean ProductImageService`).
      102/102 tests verts. Code-reviewer en cours.
      **Decisions techniques cles :**
      - `boolean primary` (pas `isPrimary`) sur `ProductImage` : Lombok genere `isPrimary()`
        comme getter, ce que MapStruct normalise en propriete `primary` (strip du `is`).
        Nommer le champ `isPrimary` creait un mismatch ; renomme en `primary`, colonne
        SQL reste `is_primary`.
      - Migration V16 (pas V15) : V14 et V15 reserves a `feature/orders` (branches
        independantes). V16 evite le conflit Flyway lors de la future integration.
      - `ProductMapper.toDetailResponse` est une methode `default` (pas multi-source
        MapStruct) qui reconstruit le record depuis `toResponse` + la liste images fournie
        par l'appelant.
      - `ProductServiceCacheTest` : ajout de `@MockitoBean ProductImageRepository` et
        `@MockitoBean ProductImageMapper` ; `toResponse` -> `toDetailResponse` pour les
        assertions findById.
- [x] `feature/shopping-cart` — terminee (11/19/26). Branche depuis `feature/user-management`,
      merge `feature/orders` (2 conflits mineurs resolus : imports DataSeeder, Javadoc
      OrderControllerTest). Implementation : migration V17 (`carts` + `cart_items`, renomme
      depuis V16 car V16 est utilise par `feature/product-images` sur une branche parallele -
      conflit Flyway resolu en avance en choisissant V17), entites `Cart`/`CartItem`, DTOs
      `CartItemRequest`/`CartItemUpdateRequest`/`CartItemResponse`/`CartResponse`,
      `CartMapper` (subtotal inline expression + toResponse default method),
      `CartRepository` (EntityGraph eager-load items+product), `CartItemRepository`,
      `CartService` interface + `CartServiceImpl` (lazy cart creation, active product check,
      quantity increment on duplicate, ownership check on update/remove), `CartController`
      (5 endpoints a `/api/v1/cart`, utilisateur resolu via `@AuthenticationPrincipal` -
      jamais depuis le payload), `SecurityConfig` gagne rule `hasRole("USER")` sur
      `/api/v1/cart/**`. Tests : `CartServiceImplTest` (9 cas), `CartControllerTest` (7 cas),
      `CartRepositoryTest` (5 cas). Relue par code-reviewer, 6 findings corriges (V16->V17,
      @JsonProperty("product_id") manquant sur CartItemRequest, @Transactional(readOnly=true)
      retire de getCart, type d'exception precise a DataIntegrityViolationException dans
      CartRepositoryTest, Javadoc getCart documente null id).
      **Decisions techniques cles :**
      - Panier cree lazily sur le premier `addItem`, jamais sur `getCart` (evite des lignes
        orphelines pour des utilisateurs qui n'ajoutent jamais rien).
      - `CartMapper.toResponse` est une methode `default` pour le meme motif que
        `ProductMapper.toDetailResponse` : impossible d'utiliser `@AfterMapping` sur un
        record immutable.
      - `cartRepository.findByUserId` avec `@EntityGraph` utilise apres chaque mutation
        pour renvoyer l'etat persiste (evite de renvoyer l'objet mutable avant flush).
      - `@WithMockUser` (classe) plutot que `.with(user(...))` dans CartControllerTest :
        avec `addFilters=false`, `SecurityContextHolderFilter` ne tourne jamais, donc
        `.with(user(...))` ne peuple pas `SecurityContextHolder` et `@AuthenticationPrincipal`
        retourne null. `@WithMockUser` passe par `WithSecurityContextTestExecutionListener`
        qui contourne le filtre entierement.
- [x] `feature/order-checkout` — terminee (11/19/26). Branche depuis `feature/shopping-cart`.
      Implementation : migration V18 (`user_id` nullable sur `orders`), champ `AppUser user`
      sur `Order`, `checkout(String username)` sur `OrderService`/`OrderServiceImpl`
      (atomique : save + cart.getItems().clear() dans le meme `@Transactional` - voir note
      feature/integration pour la raison du changement de deleteAllByCartId), `OrderController`
      remplace `create(@RequestBody OrderRequest)` par `checkout(@AuthenticationPrincipal)`,
      `SecurityConfig` : POST /api/v1/orders passe de ROLE_ADMIN a ROLE_USER,
      `CartItemRepository.deleteAllByCartId`. Tests : 6 cas checkout dans `OrderServiceImplTest`,
      3 cas dans `OrderControllerTest` (_04_/_05_/_06_ remplaces). 408/408 tests verts.
      Relue par code-reviewer, aucun bloquant, 2 mineurs corriges (`CheckoutRequest` supprime
      (code mort), test `_checkout_05_` ajoute pour le chemin "utilisateur introuvable").
      **Point de vigilance :** `OrderCreatedEvent` publie `customerId=null`/`productId=null`
      pour les commandes checkout (pas de lien Customer/Product unique). Si le listener evolue
      vers un email de confirmation, l'evenement devra embarquer l'`AppUser` directement.
      **Decisions techniques cles :**
      - Prix toujours re-lus depuis la DB dans `checkout()` pour chaque `CartItem`
        (`productRepository.findByIdWithCategory`) : protection contre la manipulation de prix
        via un panier stale. Couvert par `_checkout_06_ShouldUsePriceFromDB_NotFromCachedCartItem`.
      - `Order.user` est un SOURCE non mappe dans `OrderMapper.toResponse(Order)` :
        MapStruct ignore les sources non mappees sans warning (politique IGNORE par defaut).
        La convention d'annotation explicite du projet ne s'applique qu'aux proprietes TARGET
        non mappees (comme pour `ProductMapper` sur `feature/products`).
      - `CheckoutRequest` retire : endpoint sans corps, la Javadoc de `OrderController.checkout()`
        documente l'intention ; un record vide sans appelant est du code mort.
- [x] `feature/order-notifications` — terminee (11/19/26). Branche depuis `feature/order-checkout`.
      Implementation : `NotificationProperties` (`@ConfigurationProperties(prefix="app.notifications")`
      avec `adminEmail`), enregistree dans `AsyncConfig` ; `OrderEmailService` interface +
      `OrderEmailServiceImpl` (`@Async("emailTaskExecutor")`, `SimpleMailMessage` plain-text,
      `MailException` attrape et loggee uniquement, jamais propagee) ; `findByIdWithItemsAndUser`
      ajoute a `OrderRepository` (LEFT JOIN FETCH user + items, fonctionne pour checkout ET legacy
      orders) ; `OrderCreatedEventListener` enrichi avec `@Async("emailTaskExecutor")` + rechargement
      complet de l'ordre depuis DB + appels email. Tests : `OrderEmailServiceImplTest` (3 cas :
      confirmation, notification admin, exception SMTP avalee), `OrderCreatedEventListenerTest`
      (3 cas : avec user, sans user, ordre introuvable apres commit). 416/416 tests verts.
      Relue par code-reviewer, 1 bloquant corrige (Java `assert` natif dans 2 tests -> AssertJ),
      1 mineur corrige (test _03_ manquant dans `OrderCreatedEventListenerTest` pour le chemin
      not-found). Branche non poussee.
      **Point de vigilance clos (ouvert sur feature/order-checkout) :** le listener recharge desormais
      l'`AppUser` depuis la DB via `findByIdWithItemsAndUser`, donc `customerId`/`productId` nuls
      dans l'event ne posent plus de probleme pour les emails.
      **Decisions techniques cles :**
      - `findByIdWithItemsAndUser` ajoute plutot que modifier `findByIdWithDetails` : l'existant
        utilise INNER JOIN sur `customer`/`product`, ce qui retournait Optional.empty() pour les
        commandes checkout. Separer les deux requetes evite de casser les tests existants de
        `OrderServiceImpl.findById`.
      - Stub `notificationProperties.adminEmail()` place dans chaque test qui l'utilise (pas dans
        `@BeforeEach`) : stricte compliance avec le mode strict de Mockito qui leve
        `UnnecessaryStubbingException` sur les stubs non consommes dans un test donne.
- [x] `feature/order-authorization` — terminee (11/20/26). Branche depuis `feature/order-notifications`.
      Implementation : `OrderStatusUpdateRequest` DTO, `OrderService.findAll(String username,
      boolean isAdmin, Pageable)` role-aware (admin voit tout, user voit ses propres commandes
      via `user_id` FK), `OrderService.findById(Long, String, boolean)` (404 sur commande d'un
      autre user), `OrderService.updateStatus(Long, OrderStatus)` avec matrice de transitions
      (PENDING->CONFIRMED/CANCELLED, CONFIRMED->CANCELLED, sinon 422), `OrderService.findAllByCustomerId`
      pour `GET /customers/{id}/orders` (nouveau contrat pour `CustomerController.findOrders`),
      `OrderController` reecrit avec `@AuthenticationPrincipal` et `@PreAuthorize`.
      `findByIdWithDetails` corrige : INNER JOIN -> LEFT JOIN FETCH sur customer et product
      (les commandes checkout n'ont pas de FK customer/product, l'INNER JOIN les excluait).
      `OrderAuthorizationTest` : 8 cas e2e Testcontainers (3 acteurs : userA, userB, admin).
      Revue par code-reviewer : 3 bloquants corriges (tests manquants findAll admin/user path,
      findById user path, updateStatus). Branche non poussee.
- [x] `feature/integration` — terminee (11/20/26). Branche depuis `feature/order-authorization`,
      merge `feature/product-images` (6 conflits resolus : application.yml, .env.example,
      ProductServiceImpl, ProductControllerTest, ProductServiceCacheTest, ProductServiceImplTest).
      **Bug critique corrige :** `checkout()` ne vidait pas le panier apres la commande.
      Cause : `cartItemRepository.deleteAllByCartId()` dans une transaction Hibernate ou `Cart`
      est managed avec sa collection `items` (cascade=ALL, orphanRemoval=true) -> `cascade PERSIST`
      au flush re-inserait les items juste supprimes. Corrige : `cart.getItems().clear()` utilise
      orphan removal correctement, `CartItemRepository` retire de `OrderServiceImpl`.
      `ProductResponse` constructeur etendu a 8 args (champ `images`) : corrige dans
      `OrderMapper.toResponse(OrderProjection)`, `CustomerControllerTest`, `OrderControllerTest`,
      `OrderServiceImplTest`. `EcommerceIntegrationTest` : 1 test e2e, 11 etapes, Testcontainers.
      Revue par code-reviewer : aucun bloquant, 1 mineur corrige (assertion trop lache step 5).
      454/454 tests verts. Branche non poussee, en attente de PR.
- [x] `feature/customers` — terminée (CRUD Customer complet : entite, repository avec
      recherche nom + unicite email insensible a la casse, DTOs, mapper, service,
      controller, tests unitaires/repository/controller, relue par code-reviewer, bug
      bloquant corrige). 80 tests passent. Mergee dans `develop` (localement, fast-forward,
      pas de push) le 8/7/26 pour servir de base a `feature/orders`.
- [x] `feature/orders` — terminee (CRUD Order complet : entite avec `@ManyToOne` vers Customer
      et Product, `OrderCreatedEvent`/`OrderCreatedEventListener` en `AFTER_COMMIT`,
      repository avec projection JPQL pour les listes et `JOIN FETCH` pour le detail, DTOs,
      mapper avec double surcharge `toResponse`, service, controller, endpoint imbrique
      `GET /customers/{id}/orders`, tests unitaires/repository/controller). 108 tests passent.
      Relue par code-reviewer le 8/7/26, gap corrige (voir plus bas). Mergee dans `develop`
      (localement, pas de push) le 8/7/26 pour servir de base a `feature/auth`.
      **Extension e-commerce (11/17/26) :** `OrderStatus` enum, `OrderItem` entity (snapshot
      pattern), migration V14 (schema + backfill) + V15 (legacy total nullable), `Order` etendu
      avec `totalAmount`/`orderNumber`/`status`/`items`, `OrderItemResponse` DTO, `OrderItemMapper`,
      `OrderMapper`/`OrderServiceImpl`/`OrderRepository` réécrits pour BigDecimal et cascade
      `OrderItem`, `OrderCreatedEvent` enrichi (`totalAmount`, `orderNumber`). 158/158 tests
      verts. Revue par code-reviewer (11/17/26) : aucun bloquant, 1 mineur corrige (test 200
      manquant sur `GET /orders/{id}`). Commits réécrits avec corps pedagogiques (filter-branch,
      commits non encore pousses sur origin).
- [x] `feature/auth` — terminee (Spring Security 6 + JWT complet : entite AppUser/Role,
      migration `V2__init_users_and_roles.sql`, AppUserRepository, JwtService,
      UserDetailsServiceImpl, JwtAuthFilter, JwtAuthenticationEntryPoint/JwtAccessDeniedHandler,
      SecurityConfig avec regles d'autorisation par ressource, AuthService/AuthServiceImpl,
      AuthController avec register/login/me, tests unitaires/repository/controller, plus
      un test d'integration bout en bout SecurityAuthorizationTest avec Testcontainers).
      150 tests passent. Relue par code-reviewer le 8/7/26, un bug bloquant et un gap de
      securite corriges (voir plus bas). C'est la derniere branche fonctionnelle prevue par
      le README ; toutes les branches feature/* initiales sont maintenant terminees.
- [x] `feature/roles-permissions` — terminee (Role/Permission JPA entities, CRUD complet :
      migration V5, entites, repositories, DTOs, mappers, services, controllers,
      MethodSecurityConfig, CustomPermissionEvaluator). Les deux controllers sont ADMIN-only
      (`@PreAuthorize` classe). `findAll()` retourne `PageResponse<T>` conforme a la
      convention. `RoleRepository.findAll(Pageable)` porte `@EntityGraph` pour eviter N+1
      sur les permissions. `RoleServiceImpl.delete()` sans garde d'assignation utilisateur
      (intentionnel, deporte a V7). PR #8 -> `develop`, 256 tests verts (10/30/26).
- [x] `feature/token-management` — terminee (ActivationToken, BlacklistedToken, PasswordResetToken
      entities + migrations V5/V6 ; ActivationTokenService, PasswordResetTokenService,
      BlacklistedTokenService avec PESSIMISTIC_WRITE sur findByToken ; EmailService async
      (`@Async("emailTaskExecutor")`) ; SecureTokenGenerator ; TokenCleanupScheduler
      (`@Scheduled`). Bug corrige : `@ActiveProfiles("test")` manquant sur 3 classes
      `@SpringBootTest` -> JavaMailSender absent. PR #9 -> `develop`, 285 tests verts (10/30/26).
      Adaptation SST -> SBT (T1-T3, 10/30/26) : JwtService ajoute claim `jti` +
      `extractJti()`/`extractExpiration()`, supprime `isTokenValid()` ; AuthService/AuthServiceImpl/
      AuthController recoivent `logout()` (POST /api/v1/auth/logout) ; JwtAuthFilter injecte
      BlacklistedTokenService, extrait `jti`, consulte `isBlacklisted()` avant de peupler le
      SecurityContext, catchs DataAccessException. Reviewer : 5 findings corriges (mock manquant
      AuthServiceImplTest, test e2e SecurityAuthorizationTest._16_, DataAccessException catch,
      verify() sur _08_, BEARER_PREFIX constant) ; 2 findings perf deferees (double/triple parse
      JWT). 289 tests verts. Branche poussee sur origin.
      **Adaptation OTP activation + mobile password reset (11/21/26) :**
      Merge `feature/core-architecture` (CorsConfig dynamique : `CorsConfigurationSource` bean,
      origines LAN/emulateur). `SecurityConfig` : `.cors(withDefaults())` + `GET /activate-account`
      -> `POST`. `PasswordResetTokenType` enum (`LINK`/`OTP`) + migration V19 (backfill
      `PASSWORD_RESET`->`LINK`, CHECK constraint). `SecureTokenGenerator.generateOtp(int)`.
      `ActivationTokenServiceImpl` reecrit : OTP 6 chiffres / TTL 15 min, `validate(email, otp)`
      scoped par user via `findByUserAndToken` (evite collision OTP entre comptes).
      `PasswordResetTokenServiceImpl` reecrit : `generate(user, PasswordResetTokenType)` (LINK=UUID
      1h, OTP=6 chiffres 5 min), `verifyOtp(email, otp)` (non-destructif, pour le step mobile
      "afficher champ nouveau mdp"), `validateAndConsume` inchange. `EmailService` :
      `sendActivationEmail(user, otp)` (signature renommee) + `sendPasswordResetOtpEmail(user, otp)`.
      Templates : `activate_account.html` (lien retire, TTL 15 min) + `reset_password_otp.html`
      nouveau (palette rouge, TTL 5 min). `ActivateAccountRequest` record (`email` +
      `activation_code`). `AuthController.activate()` : GET?token= -> POST corps JSON.
      `UserService.activate(email, otp)`, `forgotPassword` force `PasswordResetTokenType.LINK`.
      Tests mis a jour : 6 classes de test (ActivationTokenServiceImplTest, PasswordResetTokenServiceImplTest,
      EmailServiceImplTest, UserServiceImplTest, AuthControllerTest, PasswordResetTokenRepositoryTest).
      Reviewer (11/21/26) : 5 findings mineurs corriges (SecurityConfig GET->POST + .cors(), doublon
      _08_ -> _19_ dans AuthControllerTest, verifyOtp readOnly retire, _06_ user-not-found dans
      ActivationTokenServiceImplTest, _10_ user-not-found dans PasswordResetTokenServiceImplTest).
      341/341 tests verts. Branche non poussee, en attente de PR.
      **Decisions techniques cles :**
      - `findByUserAndToken(user, otp)` scope la recherche OTP au user resolu par email : deux
        comptes ayant par hasard le meme OTP de 6 chiffres actif simultanément ne peuvent pas
        s'activer mutuellement.
      - `verifyOtp` (non-destructif) vs `validateAndConsume` (destructif) : l'UX mobile affiche
        d'abord un ecran "OTP correct" avant de montrer le champ "nouveau mot de passe". Le meme
        OTP est re-soumis au `confirm` (validateAndConsume) ; ok car chaque user a au plus un
        token pending a la fois.
      - `forgotPassword` sur cette branche ne supporte que le flow LINK (web). Le flow OTP mobile
        (`forgotPasswordMobile`) est ajoute sur `feature/user-management`.
      - `CorsConfigurationSource` bean actif sur cette branche car c'est le premier point de la
        chaine ou il coexiste avec `SecurityConfig`. `.cors()` appartient donc ici.
- [x] `feature/user-management` — terminee et mise a jour (11/8/26, voir "Quatrieme lot" plus bas).
      Ajouts : `@ValidPassword`, `@PasswordsMatch` + `confirmPassword`, `firstName`/`lastName`
      (V9 migration, AppUser, RegisterRequest, UserResponse, DataSeeder), `@Unique` avec
      `@Autowired(required=false)` EntityManager. 347/347 tests verts.
      Historique precedent :
      AppUser refacto Role enum -> `@ManyToMany Set<Role>`
      + `boolean enabled` ; migration V7 : role_user join table, seed ADMIN/USER, migration
      des donnees existantes ; UserDetailsServiceImpl mis a jour (ROLE_ prefix, disabled flag) ;
      `@EntityGraph(attributePaths="roles")` sur `findByUsername` pour eviter
      `LazyInitializationException` dans JwtAuthFilter ; AuthServiceImpl.register() sets
      enabled=false, charge USER depuis DB, envoie email d'activation ; UserService +
      UserServiceImpl : findAll/findById/assignRoles/delete + activate/forgotPassword/resetPassword ;
      UserController ADMIN-only ; AuthController : activate/forgot-password/reset-password.
      Reviewer : aucun bloquant, 5 mineurs corriges (lazy roles, @Transactional readOnly,
      tests couvrage authcontroller/secauth/userdetails/repository).
      Adaptation SST -> SBT (R1-R4, U1-U3, 10/30/26) :
      R1 : JwtService.generateToken(AppUser) embarque claims roles/permissions ;
      R2 : AuthServiceImpl.login() charge AppUser post-authenticate, @Transactional(readOnly=true) ;
      R3 : UserDetailsServiceImpl emet ROLE_*/PERMISSION_* authorities, .accountLocked() ;
      R4 : UserResponse.roles passe de Set<String> a List<RoleResponse>, AppUserMapper uses RoleMapper ;
      U1 : AppUser.accountLocked + V8 migration ;
      U2 : UpdateProfileRequest, UserService/Impl lock/unlock/updateProfile, UserController 3 endpoints ;
      U3 : DataSeeder seme admin initial quand appUserRepository.count()==0.
      Reviewer code (10/30/26) : 2 findings bloquants corriges (JwtAuthFilter ignorait
      isAccountNonLocked() - bug securite ; @EntityGraph etendu a roles.permissions - N+1) ;
      6 findings mineurs/perf/theoriques differes (triple parse JWT pre-existant, readOnly login,
      double findByUsername, password hardcode DataSeeder dev-only, edge cases auth header).
      Test _19_ (logout blacklist) + _20_ (compte verrouille) ajoutes en SecurityAuthorizationTest.
      333 tests verts.
      Correction SST->SBT manquante (10/30/26) : UserResponse.accountLocked expose le champ
      account_locked (boolean, serialise snake_case via @JsonProperty) ; MapStruct mappe
      AppUser.isAccountLocked() automatiquement. Constructeurs dans 4 classes de test mis a
      jour (UserControllerTest, AuthControllerTest, AuthServiceImplTest, UserServiceImplTest) ;
      assertions account_locked ajoutees sur lock/unlock dans UserControllerTest. 54 tests
      affectes, 0 echec.
      Cinquieme lot (11/9/26) : AuthResponse refactore (token -> access_token, username supprime,
      expires_in et refresh_token ajoutes) ; JwtService.getExpirationMs() expose ; cascade
      feature/auth -> feature/token-management -> feature/user-management. Tests corriges :
      _16_ (SecurityAuthorizationTest, feature/token-management), _20_ (feature/user-management),
      AuthDtoMaskingTest, AuthControllerTest, AuthServiceImplTest. 347/347 tests verts.
      PR #20 (feature/user-management -> develop) et PR #21 (develop -> master) mergees (CI verte),
      master et develop synchronises localement (11/9/26).
      **Adaptation mobile password reset + merge feature/token-management (11/21/26) :**
      Merge `feature/token-management` (9 conflits resolus). Nouveaux elements :
      `VerifyOtpRequest` record (email + otp, validations Bean Validation) ;
      `PasswordResetController` avec 4 endpoints sous `/api/v1/password-reset` : `/web`
      (flow LINK, delegue a `UserService.forgotPassword`), `/mobile` (flow OTP, nouveau
      `forgotPasswordMobile`), `/verify-otp` (non-destructif, `verifyPasswordResetOtp`),
      `/confirm` (token param + corps `ResetPasswordRequest`) ; `SecurityConfig` : rule
      `POST /api/v1/password-reset/**` -> `permitAll()`. Choix de design : `/web` maintenu
      dans `PasswordResetController` pour la symetrie web/mobile, doublon delibere avec
      `AuthController.forgotPassword`. Corrections de bugs : `DataSeeder.seedDemoData()`
      scinde en `@Bean seedDemoData()` + surcharge package-private `seedDemoData(boolean)`
      pour la testabilite (le test `_03_` appelait `seedDemoData(true)`) ;
      `ActivationTokenServiceImpl.getUserByToken` supprime (code mort, interface ne le declare
      plus). `@Transactional(readOnly=true)` retire de `verifyPasswordResetOtp` (delegation
      pure, la methode appelee gere sa propre transaction). Reviewer (11/21/26) : aucun
      bloquant, 4 mineurs corriges (readOnly retire, Swagger @ApiResponses ajoutes,
      test _06_ /confirm erreur, test _09e_ forgotPasswordMobile locked). 395/395 tests
      verts. Branche non poussee, en attente de PR.

Commit initial (`Initial commit`) fait et poussé par l'utilisateur lui-même sur `master`
(sans `Co-Authored-By`). Le push est géré manuellement par l'utilisateur, pas par Claude.

`feature/core-architecture` a été relue par l'agent code-reviewer le 7/4/26. Un bug bloquant
(permissions Docker empechant l'ecriture de `logs/app.log` en prod) a ete corrige, ainsi que
la Javadoc de `GlobalExceptionHandler` (annoncait `ApiResponse<Void>` au lieu du vrai type
`ApiResponse<ErrorResponse>`). Des tests unitaires ont ete ajoutes pour `ApiResponse` et
`GlobalExceptionHandler`. Points de vigilance releves, a traiter dans les branches suivantes :

- **`LoggingAspect` logue les arguments et le retour de chaque methode de service en clair.**
  Pas de probleme tant qu'aucun service n'existe (cas actuel), mais des `feature/customers`
  (email, telephone, adresse) et surtout `feature/auth` (mots de passe, JWT), il faudra
  masquer ou exclure les champs sensibles des logs.
- **`GlobalExceptionHandler` a un catch-all `Exception.class` -> 500 assez large.** Conforme
  au perimetre de `feature/core-architecture`, mais des `feature/products` il faudra
  probablement ajouter des handlers plus specifiques (ex. `HttpMessageNotReadableException`
  -> 400, `HttpRequestMethodNotSupportedException` -> 405) pour eviter de renvoyer 500 sur des
  erreurs de requete mal formee.
- **`OpenApiConfig`/Swagger UI est actif dans tous les profils, y compris prod.** A revisiter
  une fois `feature/auth` en place : peut-etre le desactiver ou le proteger en prod.

`feature/products` (Category + Product CRUD) a ete relue par l'agent code-reviewer le 7/4/26.
Aucun ecart bloquant : pattern service, `ApiResponse<T>`, `@Transactional`, absence de fuite
d'entite JPA, tout conforme. Gaps mineurs releves et corriges avant cloture de branche :
tests manquants pour `update()` (services et controllers des deux ressources), tests
manquants pour `delete()` (200/404) sur `ProductController`, couverture manquante pour
l'override `@EntityGraph` de `ProductRepository.findAll`, warning MapStruct sur
`CategoryMapper` (proprietes non mappees non explicitees). Les deux points de vigilance
"LoggingAspect" et "catch-all 500" notes ci-dessus depuis `feature/core-architecture` restent
ouverts, a traiter dans `feature/customers`/`feature/auth`.

Ecart de convention Java a noter : `io.swagger.v3.oas.annotations.responses.ApiResponse`
(annotation Swagger) partage son nom simple avec `edgareldy...dto.common.ApiResponse` (le DTO
du projet). Decision retenue : garder l'import normal de notre `ApiResponse` et qualifier
l'annotation Swagger en ligne (`@io.swagger.v3.oas.annotations.responses.ApiResponse(...)`)
plutot que d'utiliser un import statique des factories. A reappliquer pareil dans les
branches suivantes des qu'un controller a besoin de documenter des codes d'erreur Swagger.

`feature/customers` (Customer CRUD) a ete relue par l'agent code-reviewer le 8/7/26. Un bug
bloquant a ete corrige : `CustomerServiceImpl.create()` verifiait l'unicite de l'email de
facon sensible a la casse (`existsByEmail`) alors que `update()` la traitait comme insensible
a la casse (`equalsIgnoreCase`), permettant a deux clients d'avoir des emails identiques a la
casse pres (ex. `ada@example.com` et `ADA@EXAMPLE.COM`). Corrige en renommant la methode
repository en `existsByEmailIgnoreCase`, utilisee de facon coherente dans `create()` et
`update()`. Tests ajoutes pour couvrir ce cas (service, repository). Checklist README de la
branche mise a jour (etait restee decochee malgre le travail termine).

Point de vigilance ouvert, note par le reviewer mais volontairement non traite sur cette
branche : `CustomerServiceImpl.delete()` ne verifie pas l'existence de commandes liees (aucune
entite JPA `Order` n'existe encore, seule la table SQL existe via Flyway). Supprimer un client
ayant des commandes en base declenchera une violation de contrainte FK brute, remontee en 500
par le catch-all de `GlobalExceptionHandler` (meme point de vigilance que le "catch-all 500"
deja note depuis `feature/core-architecture`). A traiter explicitement dans `feature/orders`.
**Traite sur `feature/orders`, voir plus bas.**

`feature/orders` (Order CRUD) a ete relue par l'agent code-reviewer le 8/7/26. Aucun ecart
bloquant sur les conventions du projet (pattern service, `ApiResponse<T>`/`PageResponse<T>`,
`@Transactional` sur ecritures uniquement, absence de fuite d'entite JPA, injection par
interface, Javadoc, absence de tiret cadratin, couverture de tests). Le timing de publication
de `OrderCreatedEvent` (`@TransactionalEventListener(AFTER_COMMIT)`) a ete verifie coherent
avec le `@Transactional` de `OrderServiceImpl.create()`. Un gap a ete releve et corrige avant
cloture de branche : ni `CustomerServiceImpl.delete()` ni `ProductServiceImpl.delete()` ne
verifiaient l'existence de commandes liees avant suppression, ce qui aurait declenche une
`DataIntegrityViolationException` (violation de contrainte FK sur `orders.customer_id`/
`orders.product_id`, toutes deux `NOT NULL`) remontee en 500 brut par le catch-all de
`GlobalExceptionHandler`. Corrige en ajoutant `OrderRepository.existsByCustomerId`/
`existsByProductId` et en les appelant en tete de chaque `delete()`, levant une
`BusinessRuleException` (422) si des commandes existent, sur le meme modele que la
verification d'unicite d'email deja en place sur `CustomerServiceImpl`. Tests ajoutes dans
`CustomerServiceImplTest`/`ProductServiceImplTest` pour couvrir le cas de refus. Ceci cloture
definitivement le point de vigilance ouvert depuis `feature/customers`. Remarque mineure du
reviewer, non traitee (design acceptable, pas de logique metier au sens strict) :
`CustomerController.findOrders` orchestre `CustomerService.findById` (pour le 404) puis
`OrderService.findAll` directement au niveau controller plutot que via une methode de service
dediee ; a surveiller si ce pattern se repete sur d'autres ressources imbriquees.

Les deux points de vigilance "LoggingAspect" et "catch-all 500" notes depuis
`feature/core-architecture` restent ouverts pour le reste du perimetre (pas de champs
sensibles supplementaires introduits par `Order`, pas de nouveau cas d'erreur de requete mal
formee specifique aux commandes) ; a traiter dans `feature/auth` (mots de passe, JWT pour
`LoggingAspect` ; `HttpMessageNotReadableException`/`HttpRequestMethodNotSupportedException`
restent a examiner si l'occasion se presente).

`feature/auth` (Spring Security 6 + JWT) a ete relue par l'agent code-reviewer le 8/7/26.
**Le point de vigilance "LoggingAspect" ouvert depuis `feature/core-architecture` s'est
materialise en bug bloquant reel, exactement comme anticipe** : le pointcut de `LoggingAspect`
(`execution(* edgareldy.springboottutorial.service..*(..))`) intercepte `AuthServiceImpl
.register()`/`.login()`, et loggue les arguments/retour via leur `toString()`. Les `record`
`RegisterRequest`/`LoginRequest`/`AuthResponse` n'avaient pas de `toString()` personnalise,
donc le mot de passe en clair (register/login) et le JWT emis (login) se retrouvaient dans les
logs applicatifs, y compris `logs/app.log` en prod (`application-prod.yml`, `logging.file.name`,
niveau `root: INFO` sans filtrage par package). Corrige en surchargeant `toString()` sur ces
trois `record` pour masquer le champ sensible (`password=***` / `token=***`), sans toucher a
`LoggingAspect` lui-meme qui reste generique. Tests dedies ajoutes (`AuthDtoMaskingTest`) pour
verrouiller ce comportement. **Ce point de vigilance est desormais definitivement clos.**

Gaps mineurs releves et corriges avant cloture de branche : aucun test n'exercait
`JwtAuthFilter` de bout en bout avec un vrai JWT (tous les tests d'acces authentifie
utilisaient `SecurityMockMvcRequestPostProcessors.user(...)`, qui contourne le filtre) ; ajoute
`SecurityAuthorizationTest.loginIssuesTokenThatAuthenticatesSubsequentRequests` qui fait
register -> login -> appel d'un endpoint protege avec le vrai token recu. Couverture etendue a
`products`/`orders` (symetrique a `categories`/`customers`) et a la joignabilite publique de
`/auth/login`. Tests unitaires dedies ajoutes pour `UserDetailsServiceImpl` et pour le handler
`AuthenticationException` de `GlobalExceptionHandler` (coherence avec le reste de la suite,
qui a un test par handler).

Un bug reel independant a ete trouve et corrige en cours de session (avant meme la revue,
via un test qui echouait de facon inattendue) : `AuthController.me()` utilisait
`Authentication.getName()`, qui ne "sait" retourner le username que si le principal implemente
`Principal`/`AuthenticatedPrincipal`. Le principal pose par `JwtAuthFilter`/
`UserDetailsServiceImpl` est un `org.springframework.security.core.userdetails.User` brut, qui
n'implemente ni l'un ni l'autre : `getName()` retombait donc sur `principal.toString()` (le
gros descriptif `User [Username=..., Password=..., ...]`) au lieu du username, et
`AuthService.me()` cherchait alors un utilisateur inexistant. Corrige en utilisant
`@AuthenticationPrincipal UserDetails userDetails` puis `userDetails.getUsername()`. Recherche
exhaustive confirmee : aucun autre endroit du code ne retombe dans ce piege.

Ecart technique a noter, independant du contenu de la revue : ajouter `JwtAuthFilter` (un
`Filter`) a cette branche a casse tous les `@WebMvcTest` existants (Category/Product/Customer/
Order), meme ceux avec `@AutoConfigureMockMvc(addFilters = false)`. Cause : Spring Boot inclut
automatiquement dans le slice `@WebMvcTest` tout bean de type `Filter` present dans le contexte,
independamment de `addFilters` (qui controle seulement si le filtre s'execute pendant la
requete, pas s'il doit etre construit). `JwtAuthFilter` a donc besoin de `JwtService`/
`UserDetailsService` pour se construire meme dans un slice qui ne les utilise jamais. Corrige
en ajoutant `@MockitoBean` pour ces deux types dans les 4 classes de test existantes. A
reappliquer systematiquement sur tout futur `@WebMvcTest` cree apres cette branche.

Enfin, `JwtAuthFilter` referencait `UserDetailsServiceImpl` (la classe concrete) au lieu de
`UserDetailsService` (l'interface Spring Security), seul endroit du projet a deroger au pattern
contrat/implementation. Corrige au passage : en plus de la coherence, cela a permis de mocker
un seul type dans les tests plutot que de trainer `AppUserRepository` en cascade.

## Concepts Spring Boot completes apres coup (8/7/26)

Toutes les branches `feature/*` etaient terminees et mergees (voir plus haut), mais la liste
"Concepts covered" du README annoncait 4 concepts jamais reellement implementes :
`@ConfigurationProperties`, le cache applicatif (`@Cacheable`), le seed de donnees
(`CommandLineRunner`), et les types d'advice AOP autres que `@Around` (`@Before`, `@After`,
`@AfterReturning`, `@AfterThrowing`). Chacun a ete attribue a la branche la plus pertinente,
implemente et teste, puis re-merge dans `develop` et `master` :

- **`feature/core-architecture`** : `CacheConfig` (Caffeine, caches nommes `categories`/
  `products`, TTL 10 min, 500 entrees max, prepare a l'avance pour `feature/products` comme
  `OpenApiConfig` l'avait deja fait pour `feature/auth`) ; `RepositoryAuditAspect`, un troisieme
  aspect (apres `LoggingAspect` sur `service` et `ExecutionTimeAspect` sur `controller`) avec
  `@Before`/`@After`/`@AfterReturning`/`@AfterThrowing` sur la couche `repository`. Egalement,
  suite a une demande separee de l'utilisateur : `.env.example` + `docker-compose.yml` reecrit
  pour lire `POSTGRES_*`/`DB_*`/`JWT_SECRET` via `${VAR:-default}` au lieu de valeurs en dur
  (le `.env` reel restant gitignore).
- **`feature/products`** : `@Cacheable`/`@CacheEvict` sur `CategoryServiceImpl.findById`/
  `update`/`delete` et `ProductServiceImpl.findById`/`update`/`delete` (cle `#id`, `findAll`
  volontairement non cache car pagine/trie) ; `DataSeeder` (`CommandLineRunner`, profil "dev"
  uniquement comme `CorsConfig`, garde d'idempotence sur `categoryRepository.count() > 0`).
- **`feature/auth`** : `JwtProperties` (`record` avec `@ConfigurationProperties(prefix = "jwt")`,
  constructor binding implicite) remplace les deux `@Value` de `JwtService` ; enregistre via
  `@EnableConfigurationProperties(JwtProperties.class)` sur `SecurityConfig` plutot que
  `@ConfigurationPropertiesScan` sur la classe principale, pour garder le changement local a
  cette branche.

Relu par l'agent code-reviewer le 8/7/26 (limite a `Read`/`Grep`/`Glob`, pas de `Bash`). Un
point souleve comme "bloquant a tres haut risque" par le reviewer a ete **infirme
empiriquement** avant toute correction : le reviewer soutenait, par un raisonnement Java
correct sur `Method.getDeclaringClass()`, que `RepositoryAuditAspect` ne devrait jamais
s'declencher sur les methodes CRUD heritees (`save`, `findById`, etc., declarees sur
`CrudRepository`, hors du package `edgareldy.springboottutorial.repository` cible par le
pointcut `execution()`), et que `CategoryRepository` (interface vide) ne devrait meme pas
recevoir de proxy AOP. Verifie avec un test jetable (`ListAppender` Logback attache au logger
de l'aspect, appel reel de `categoryRepository.save(...)` via un `@SpringBootTest` avec
Testcontainers) : **l'advice se declenche bel et bien**, sur les trois evenements
(`@Before`/`@AfterReturning`/`@After`). Le matching `execution()` de Spring AOP sur un proxy
JDK considere l'ensemble de la hierarchie d'interfaces implementee par le proxy, pas seulement
la classe declarante de la methode invoquee. Un test permanent (`RepositoryAuditAspectTest`,
package racine pour reutiliser `TestcontainersConfiguration`) verrouille ce comportement,
y compris `@AfterThrowing` via une violation de contrainte NOT NULL. **Lecon retenue : verifier
empiriquement un comportement Spring AOP avant de corriger un pointcut sur la seule foi d'un
raisonnement theorique, meme correct sur le papier.**

Gaps mineurs reeels corriges suite a la meme revue :
- `docker-compose.yml` : le healthcheck Postgres (`pg_isready -U spring_boot_tutorial`) restait
  en dur alors que `POSTGRES_USER` etait devenu parametrable ; corrige en
  `pg_isready -U ${POSTGRES_USER:-spring_boot_tutorial}`.
- Aucun test n'existait pour le cache de `ProductServiceImpl` (`ProductServiceImplTest` est un
  test Mockito pur qui n'active jamais le proxy AOP portant `@Cacheable`) ; ajoute
  `ProductServiceCacheTest`, meme approche que `CategoryServiceCacheTest`.
- `CategoryServiceCacheTest` ne couvrait pas l'eviction par `delete()` ; ajoutee.

En ecrivant ces tests de cache, deux bugs de test (pas de code de prod) ont ete decouverts et
corriges :
1. **Contamination inter-tests** : `@SpringBootTest` reutilise le meme contexte, donc le meme
   `CacheManager`, entre toutes les methodes de test d'une classe. Sans nettoyage explicite,
   une entree laissee par un test polluait le suivant, masquant de vraies erreurs d'assertion
   selon l'ordre (non garanti) d'execution de JUnit 5. Corrige en vidant le cache dans un
   `@BeforeEach` sur `CategoryServiceCacheTest` et `ProductServiceCacheTest`.
2. **Mauvais compte d'appels attendu** : une fois l'isolation corrigee,
   `updateEvictsCacheForThatId`/`deleteEvictsCacheForThatId` sur `CategoryServiceCacheTest`
   attendaient 2 appels a `categoryRepository.findById()` mais en recevaient 3. Cause :
   `update()`/`delete()` chargent l'entite via `getCategoryOrThrow()`, qui appelle
   `categoryRepository.findById()` directement, jamais via la methode `findById()` du service
   (la seule annotee `@Cacheable`) : cet appel n'est donc jamais mis en cache. Corrige le compte
   attendu a 3, documente en ligne dans le test. `ProductServiceCacheTest` n'a pas ce probleme
   par asymetrie du code de production : `findById()` du service appelle
   `findByIdWithCategory()`, `update()`/`delete()` appellent `findById()` (methode differente),
   donc le `verify(..., times(N)).findByIdWithCategory(...)` ne compte que les appels passant
   reellement par le cache.

Ecart supplementaire trouve seulement une fois `develop` reconstruit en entier (invisible sur
`feature/products` seule) : `ProductServiceCacheTest` (ecrit et verifie sur `feature/products`,
qui n'a que 3 dependances de constructeur sur `ProductServiceImpl`) echouait sur `develop` une
fois merge, car `feature/orders` y avait deja ajoute une 4e dependance (`OrderRepository`, pour
la verification de commandes existantes dans `delete()`). Corrige directement sur `develop`
(pas de retour sur `feature/products`, le probleme n'existant qu'a l'intersection des deux
branches) en ajoutant le mock manquant. Meme categorie de probleme que la regression
`JwtAuthFilter`/`@WebMvcTest` decouverte sur `feature/auth` : **un slice de test etroit
(`@SpringBootTest(classes = {...})`, `@WebMvcTest`, etc.) qui liste explicitement les
dependances d'un bean doit etre revisite des qu'une branche posterieure change la signature
du constructeur de ce bean.**

Suite complete verifiee a 162/162 sur `develop`, puis `develop` mergee dans `master` (localement,
merge commit via `ort`, pas de push, comme a chaque fois).

## Deuxieme lot de concepts Spring Boot (8/25/26)

Suite a une analyse comparative du README/code de ce projet face au projet frere
`spring-security-tutorial`, l'utilisateur a demande d'ajouter 6 elements supplementaires :
deux corrections de documentation (tableau Tech stack sans Caffeine, pas de mention de
l'ecart PR reel vs decrit) et trois concepts Spring Boot non encore illustres
(`ProblemDetail`/RFC 7807, verrouillage optimiste `@Version`, filtrage dynamique
`Specification`) plus deux concepts d'infrastructure (endpoint Actuator custom, Spring Boot
DevTools) plus l'auditing JPA natif. Contrainte explicite de l'utilisateur pour la strategie
de branches : **respecter le gitflow existant, ne jamais faire `git merge develop` dans une
branche `feature/*`** (seul le sens feature -> develop est autorise, jamais l'inverse).

Sequence retenue, chaque etape suivant la boucle habituelle (implementer -> Javadoc anglais ->
commit atomique par fichier -> `mvn clean test` -> agent `code-reviewer` -> corriger les
findings -> merge) :

1. **`feature/core-architecture`** : les 2 corrections doc, remplacement de
   `exception/ErrorResponse.java` par `org.springframework.http.ProblemDetail` (la vraie
   classe Spring 6, pas un DTO reinvente) dans `GlobalExceptionHandler`, toujours enveloppe
   dans `ApiResponse<T>` (`ApiResponse<ProblemDetail>` au lieu de `ApiResponse<ErrorResponse>`)
   pour respecter la regle "toutes les reponses passent par `ApiResponse<T>`" sans la casser.
   Verification prealable (`git grep` sur `master`) que ce changement ne touchait qu'un seul
   fichier de test (`GlobalExceptionHandlerTest`), pas tout le projet comme redoute au depart.
   Ajout aussi de `actuator/AppInfoEndpoint.java` (`@Endpoint(id = "appinfo")`, expose en dev
   uniquement), `spring-boot-devtools` (pom, `optional`/`runtime`), et
   `config/JpaAuditingConfig.java` (`@EnableJpaAuditing`, prepare a l'avance pour
   `feature/products`, meme logique que `CacheConfig` documentee plus haut).
2. **`feature/products`** (apres `git merge feature/core-architecture --no-edit`, jamais
   `develop`) : `@Version` sur `Product` (colonne `version`, migration **`V3`**, pas `V2` :
   `V2` est reserve a `feature/auth`/`V2__init_users_and_roles.sql`, absent de cette branche a
   ce stade mais dont le numero doit rester libre pour la suite) + handler
   `OptimisticLockingFailureException` -> 409 dans `GlobalExceptionHandler`. Filtrage
   dynamique via `ProductSpecifications`/`JpaSpecificationExecutor` sur `productName`/
   `minPrice`/`maxPrice`, `ProductServiceImpl.findAll` ne bascule sur ce chemin que si un de
   ces parametres est fourni (sinon conserve les chemins `@EntityGraph` existants), N+1 assume
   et documente sur le chemin `Specification` uniquement. Auditing JPA (`@CreatedDate`/
   `@LastModifiedDate`) applique concretement a `Category` (migration `V4`), `CategoryResponse`
   gagne `createdAt`/`updatedAt`. Revue par `code-reviewer` : aucun bloquant, 4 findings
   mineurs corriges (`ProductMapper` sans `@Mapping(ignore)` explicite sur `version`,
   doc Swagger 409 manquante, test controller 409 manquant, Javadoc `JpaAuditingConfig`
   restee "not consumed yet" apres que `Category` l'ait justement consommee).
3. **`feature/customers`** (merge direct de `feature/products`) : aucun nouveau code, la
   branche absorbe simplement les changements partages.
4. **`feature/orders`** (merge de `feature/products` puis de `feature/customers`) : meme
   probleme d'intersection que celui deja documente plus haut pour `ProductServiceCacheTest`
   (constructeur `ProductServiceImpl` a 4 dependances sur cette branche `OrderRepository`
   compris, le test n'en mockait que 3) s'est reproduit a l'identique et a ete corrige de la
   meme facon, directement sur cette branche.
5. **`feature/auth`** (merge de `feature/orders`) : 3 vrais conflits, tous resolus a la main.
   `GlobalExceptionHandler.java` : `feature/auth` avait ajoute `handleAuthentication` avec
   l'ancien type `ErrorResponse` (avant le refactor `ProblemDetail`, cette branche ayant
   diverge avant) ; resolu en gardant les deux handlers (`handleAuthentication` +
   `handleOptimisticLocking`) et en migrant le premier vers `ProblemDetail`. Le test associe
   n'etait pas marque en conflit par git (auto-merge silencieux qui cassait quand meme la
   compilation) : `GlobalExceptionHandlerTest.authenticationExceptionMapsTo401` utilisait
   encore `ApiResponse<ErrorResponse>`/`status()`, corrige a la main vers
   `ApiResponse<ProblemDetail>`/`getStatus()`. `README.md` : conflit sur la ligne `CorsConfig`
   (`localhost:4200` vs `localhost:3000`/`5173`) ; verification du vrai `CorsConfig.java` (non
   conflictuel) qui confirme `4200`, ligne gardee en consequence. `docker-compose.yml` :
   `feature/auth` avait `JWT_SECRET` en dur sans les autres variables parametrees,
   `feature/orders` avait `DB_*` parametres (`${VAR:-default}`) sans `JWT_SECRET` du tout ;
   resolu en appliquant le meme pattern aux deux, `JWT_SECRET` ajoute a `.env.example`.
6. **`develop`** (merge de `feature/auth`) : 2 conflits, tous deux triviaux. `docker-compose.yml`
   marque en conflit par git sans le moindre marqueur present dans le fichier resultant (les
   deux cotes avaient converge vers un contenu identique), simplement re-ajoute tel quel.
   `ProductServiceCacheTest.java` : conflit sur un commentaire explicatif au-dessus du mock
   `OrderRepository` (present sur `develop`, absent sur `feature/auth`), commentaire conserve.
7. **`master`** (merge de `develop`) : aucun conflit.

Suite complete verifiee a chaque etape (`mvn clean test`, Testcontainers reellement execute),
170/170 sur `develop` et `master` a la fin. Deux agents `code-reviewer` distincts invoques
(un sur le lot `feature/core-architecture`, un sur le lot `feature/products`) plus un
troisieme dedie specifiquement aux 3 resolutions de conflit du merge `feature/orders` ->
`feature/auth` : aucun ecart bloquant trouve sur aucun des trois passages, uniquement des
findings mineurs deja tous corriges. Point de vigilance releve par ce dernier (non bloquant,
pre-existant, pas une regression de cette session) : `application-prod.yml` exige
`JWT_SECRET` sans valeur par defaut (`${JWT_SECRET}`) pour forcer sa presence en vrai
deploiement prod, mais `docker-compose.yml` lui fournit quand meme un defaut de demo
(`${JWT_SECRET:-...}`), contournant cette garde-fou dans le contexte Docker Compose local
uniquement ; deja le cas pour `DB_*` avant cette session, non retraite ici.

**Incident environnemental rencontre en cours de route, sans lien avec le code** : Docker
Desktop s'est arrete tout seul entre deux etapes de la cascade (`docker info` KO,
`systemctl --user status docker-desktop` -> `inactive (dead)`, alors qu'un `dockerd` systeme
tournait separement sur le socket par defaut sans que l'utilisateur y ait les permissions).
Redemarre avec `systemctl --user start docker-desktop`, operationnel apres quelques secondes.
A retenir comme reflexe si Testcontainers echoue avec "Could not find a valid Docker
environment" et que le contournement `DOCKER_HOST` deja documente plus bas ne suffit pas non
plus : verifier `systemctl --user status docker-desktop` avant de conclure a un probleme de
code.

## Troisieme lot : refacto ResponseEntity / snake_case / DTOs imbriques (9/3/26)

L'utilisateur a demande un refacto transversal de l'API deja terminee (toutes branches
`feature/*` closes depuis le lot precedent), applique branche par branche dans l'ordre du
gitflow (`feature/products` -> `feature/customers` -> `feature/orders` -> `feature/auth`),
avec trois volets :

1. **Controllers** : retourner `ResponseEntity<ApiResponse<T>>` au lieu de `ApiResponse<T>`
   nu, `@ResponseStatus(CREATED)` remplace par `ResponseEntity.status(HttpStatus.CREATED)
   .body(...)`. Comportement observable (code HTTP, corps JSON) strictement inchange.
2. **Snake_case sur les DTO**. Deux alternatives explicitement rejetees par l'utilisateur
   apres discussion : une strategie Jackson globale (`spring.jackson.property-naming-strategy:
   SNAKE_CASE`, testee puis retiree) et le renommage litteral des champs Java en snake_case
   (essaye puis interrompu). Solution retenue : `@JsonProperty("snake_case")` champ par champ
   sur chaque record DTO concerne, qui laisse les noms Java en camelCase (coherence avec le
   reste du code, correspondance automatique MapStruct entite <-> DTO preservee) tout en
   decouplant le contrat JSON expose au client. **Regle absolue verifiee a plusieurs
   reprises** : les messages de validation Bean Validation (`@NotBlank`, `@NotNull`, etc.)
   doivent rester de la prose lisible ("Category name must not be blank"), jamais le nom brut
   du champ en camelCase ni en snake_case ("category_name must not be blank" est un ecart deja
   corrige une fois, a ne plus jamais reproduire).
3. **DTOs avec relations exposees** : `CategoryResponse.products` peuple uniquement sur le
   detail (`GET /categories/{id}`, via `CategoryRepository.findByIdWithProducts` +
   `CategoryMapper.toDetailResponse`), jamais sur la liste paginee (N+1 assume uniquement a la
   demande explicite, jamais par defaut) ; `OrderResponse` embarque desormais les DTO complets
   `CustomerResponse`/`ProductResponse` au lieu d'anciens records resumes `CustomerSummary`/
   `ProductSummary`, sur le detail (`OrderRepository.findByIdWithDetails`, `JOIN FETCH`
   etendu) et sur la liste paginee (`OrderProjection` etendu a 14 colonnes, toujours une
   projection JPQL plate donc toujours sans N+1) ; `CustomerResponse.findById` retourne aussi
   les produits que ce client a commandes, via une nouvelle requete dediee
   `OrderRepository.findDistinctProductsByCustomerId` plutot qu'une nouvelle relation JPA
   bidirectionnelle sur `Customer`/`Product` (coherent avec le design unidirectionnel existant
   `Order` -> `Customer`/`Product`), uniquement sur le detail, jamais `findAll`.

`feature/products` et `feature/customers` termines et mergés (feature-a-feature, jamais via
`develop`) avant de commencer `feature/orders`. `feature/orders` termine a son tour : 15
commits atomiques, revue par l'agent `code-reviewer`, aucun ecart bloquant. Deux points
releves et corriges avant cloture :
- **Bug de test preexistant, sans lien avec ce refacto** : `OrderRepositoryTest` et
  `ProductRepositoryTest` creent des `Category` dans leur `setUp()` sans importer
  `JpaAuditingConfig` (contrairement a `CategoryRepositoryTest`, qui l'importe), donc
  `created_at`/`updated_at` ne sont jamais renseignes quand la classe tourne seule
  (`mvn test -Dtest=OrderRepositoryTest` echoue sur une violation de contrainte NOT NULL) ;
  ne passait dans la suite complete qu'a cause d'une fuite du contexte d'auditing d'une
  classe de test anterieure dans la meme JVM. Corrige en ajoutant l'import manquant aux deux
  classes. **A verifier systematiquement sur tout futur `@DataJpaTest` qui cree une
  `Category`.**
- **Gap N+1 expose par le refacto (pas une regression du code touche)** :
  `OrderServiceImpl.getProductOrThrow` chargeait le produit via `productRepository.findById`
  simple, laissant `Product.category` lazy ; inoffensif avant, mais `OrderResponse` embarque
  maintenant un `ProductResponse` complet (avec `categoryName`), donc `create()`/`update()`
  declenchaient un select supplementaire evitable. Corrige en utilisant
  `productRepository.findByIdWithCategory` (deja utilisee par `ProductServiceImpl`).
  Tests de mapper dedies ajoutes (`CustomerMapperTest`, `OrderMapperTest`, meme motif que
  `CategoryMapperTest` : `CustomerMapper`/`OrderMapper` etant entierement mockes dans les
  tests de service, le code reellement genere par MapStruct n'etait jamais execute par aucun
  test avant cet ajout).

138 tests verts sur `feature/orders` a la cloture de ce lot.

**Lot cloture le 9/3/26** : `feature/auth` a recu le meme refacto `ResponseEntity`/snake_case
sur `AuthController`/`AuthResponse.tokenType`, revue par `code-reviewer` (aucun bloquant). Vraie
PR GitHub ouverte (`gh pr create`, deviation exceptionnelle documentee plus haut, `gh` disponible
et authentifie sur cette machine) : [PR #1](https://github.com/EdgarEldy/spring-boot-tutorial/pull/1)
`feature/auth` -> `develop`, CI verte, mergee (`gh pr merge --merge`). `develop` mergee dans
`master` sans conflit, 184/184 tests verts. Toutes les branches (`feature/products`,
`feature/customers`, `feature/orders`, `feature/auth`, `develop`, `master`) poussees sur
`origin`, CI verte partout.

## Rename du package racine : `edgareldy` -> `com.edgareldy` (9/3/26)

Demande complementaire de l'utilisateur, traitee juste apres la cloture du lot ci-dessus une
fois `master` a jour et pousse : renommer le package Java racine en `com.edgareldy` (convention
reverse-DNS), en repartant de `feature/core-architecture` puis en recascadant le changement
(`feature/products` -> `feature/customers` -> `feature/orders` -> `feature/auth` -> `develop`
-> `master`), meme ordre et meme discipline (commit -> `code-reviewer` -> merge feature-a-feature,
jamais via `develop`) que les lots precedents.

**Decision** : seul le package Java change (`edgareldy.springboottutorial` ->
`com.edgareldy.springboottutorial`, y compris le repertoire `src/main/java/edgareldy` ->
`src/main/java/com/edgareldy` et son equivalent `src/test/java`). Le `groupId` Maven du
`pom.xml` (`edgareldy`, bare, sans meme `.springboottutorial`) reste **intentionnellement
inchange** : c'est une coordonnee de build, un sujet distinct du nom de package Java, non
demande explicitement.

**`feature/core-architecture`** (premiere etape, 20 fichiers Java a ce stade) : `git mv` des
deux repertoires racine (`src/main/java/edgareldy` et `src/test/java/edgareldy` vers
`.../com/edgareldy`), puis remplacement scripte de `edgareldy.springboottutorial` (et sa forme
a base de `/` pour les chemins) par `com.edgareldy.springboottutorial` dans tous les `.java`,
`README.md`, et `src/main/resources/application-dev.yml` (qui a une entree `logging.level`
nommant le package). Un seul commit atomique : un rename de package ne peut pas etre scinde en
plusieurs commits sans laisser l'arbre dans un etat qui ne compile pas entre les deux. Grep
exhaustif (case-insensitive, tout le depot) confirme qu'aucune reference `edgareldy` orpheline
ne subsiste ailleurs (`pom.xml` mis a part, decision ci-dessus) : ni dans
`application.yml`/`application-test.yml`/`application-prod.yml`, ni `.github/workflows/ci.yml`,
ni `Dockerfile`/`docker-compose.yml`. Revue par `code-reviewer` : aucun ecart bloquant sur le
code ; seul ce fichier CLAUDE.md n'avait pas ete mis a jour dans la foulee (corrige a l'instant
meme ou ce paragraphe est ecrit). `mvn clean test` : 8/8 verts sur cette branche.

**Cascade terminee le 9/3/26** : `feature/products` -> `feature/customers` -> `feature/orders`
-> `feature/auth` -> `develop` -> `master`, merge direct feature-a-feature a chaque etape
(jamais `develop` dans une feature), toutes poussees, CI verte partout. 184/184 tests verts sur
`develop`/`master` a la fin (75 sur `feature/products`, 102 sur `feature/customers`, 138 sur
`feature/orders`, 182 sur `feature/auth`, coherent avec les comptes d'avant-rename).

**Piege methodologique decouvert et corrige en cours de cascade, a ne pas reproduire** :
le premier reflexe pour verifier qu'aucune reference `edgareldy` (sans `com.`) ne subsiste
etait `grep -rn "edgareldy" | grep -v "com.edgareldy"`. Ce filtre est **silencieusement
inefficace** des que le nom de repertoire lui-meme contient `com/edgareldy` (ce qui est le cas
de tout fichier une fois le rename commence) : `grep -v` matche sur la ligne complete
`chemin:numero:contenu`, donc le chemin `com/edgareldy` suffit a faire passer le filtre meme
si le *contenu* de la ligne a une reference stale. Deux categories de reference ont echappe a
la verification pendant plusieurs commits avant d'etre repere : des tags Javadoc `{@link
edgareldy.springboottutorial...}` (inoffensifs a la compilation, juste une reference cassee
dans la doc generee) et, bien plus grave, une chaine JPQL en dur dans un `@Query` (`SELECT new
edgareldy.springboottutorial.repository.OrderProjection(...)` dans `OrderRepository`), qui a
fait planter le demarrage du contexte Spring (`Could not create query for ...
findAllProjected`) decouvert seulement a l'execution des tests, pas a la compilation. **Le
grep correct doit d'abord retirer le chemin de fichier avant de filtrer le contenu** (ex. via
`awk -F: '{path=$1; line=$2; content=substr($0, length(path)+length(line)+3); if (content !~
/com\.edgareldy/) print}'`), sans quoi le sweep de verification est un theatre de securite qui
donne une fausse confiance. A refaire systematiquement de cette facon pour toute verification
future de ce type (recherche d'un identifiant qui apparait aussi dans un chemin de fichier).

Deux classes de references sont invisibles a un sed scope sur les lignes `^(package|import)
edgareldy\.` (necessaire par ailleurs pour eviter de doubler le prefixe `com.` sur des lignes
deja renommees dans un merge partiel) et doivent etre cherchees separement a chaque etape
future similaire : les tags Javadoc `{@link ...}`/`{@code ...}` qualifies, et les chaines JPQL
`SELECT new <FQCN>(...)` dans les `@Query`. Verifie a la main sur les 5 branches concernees a
la fin de la cascade : plus aucune occurrence de `edgareldy` non prefixee `com.` nulle part
dans `src/`.

Merges avec conflits reels (pas seulement des conflits "file location" resolus par un simple
deplacement + renommage de package) : sur les branches qui avaient deja evolue leur propre
logique en plus du rename herite (`feature/products` -> `feature/customers` -> `feature/orders`
-> `feature/auth`, puis `feature/auth` -> `develop`), chaque fichier en conflit de contenu
avait une caracteristique constante : le cote aval (la branche qui recoit le merge) contenait
systematiquement un sur-ensemble strict du cote amont (memes lignes que la branche source
apres rename, plus les ajouts propres a la branche aval). Resolu a chaque fois en gardant le
contenu du cote aval (`git show :2:<path>` pour recuperer "ours") et en lui reappliquant le
rename, sans jamais fusionner ligne a ligne les deux cotes. Cette regularite n'est pas garantie
en general pour un merge Git, mais elle decoule directement du fait que le rename ne touchait
que des lignes `package`/`import`/`@Query`/`{@link}`, jamais de logique metier : a garder en
tete si un futur rename similaire touchait aussi du code fonctionnel, auquel cas cette
heuristique ("aval = sur-ensemble") ne tiendrait plus et une vraie fusion ligne a ligne serait
necessaire.

Revue par l'agent `code-reviewer` a deux reprises (une fois sur `feature/core-architecture`
seule, une fois sur l'etat final de `feature/auth` apres toute la cascade) : aucun ecart
bloquant trouve, la deuxieme revue a explicitement recompte les occurrences de `edgareldy` vs
`com.edgareldy` sur tout `src/` pour confirmer qu'elles etaient strictement egales (aucune
reference orpheline).

## Quatrieme lot : contraintes de validation metier sur feature/auth (11/8/26)

L'utilisateur a demande d'ajouter 4 ameliorations de validation a `feature/auth`, puis de
cascader les changements sur les branches suivantes jusqu'a `feature/user-management` :

1. **`@ValidPassword`** : contrainte Jakarta Bean Validation de niveau champ (regex :
   `^(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&+=!])\S{8,}$`, au moins une majuscule, un chiffre,
   un caractere special, 8+ caracteres sans espace). Packages dedies :
   `validation/password/ValidPassword.java` (annotation) et
   `validation/password/PasswordValidator.java` (implementor). Retourne `true` pour `null`
   (laisse `@NotBlank` gerer ce cas). Tests dedies : `PasswordValidatorTest` (8 tests purs),
   `PasswordsMatchValidatorTest` (2 tests via le stack Validator complet).

2. **`@PasswordsMatch`** : contrainte de niveau classe (cross-field), placee sur
   `RegisterRequest`. `PasswordsMatchValidator` lit les deux champs par reflexion et rapporte
   la violation sur le noeud `confirmPassword` via
   `context.buildConstraintViolationWithTemplate(...).addPropertyNode(confirmPasswordField)`.
   `confirmPassword` ajoute comme nouveau champ de `RegisterRequest`.

3. **`firstName`/`lastName`** sur `AppUser`, `RegisterRequest`, `UserResponse` :
   - Flyway **V9** (`V9__add_name_fields_to_app_users.sql`) : `ALTER TABLE app_users ADD COLUMN
     first_name VARCHAR(100), ADD COLUMN last_name VARCHAR(100)` (nullable, sans default, les
     deux colonnes ajoutees en une seule migration). Choix V9 pour ne pas entrer en conflit
     avec V5-V8 (branches posterieures).
   - `AppUser` : deux champs `@Column(name = "first_name/last_name") private String` ajoutes.
   - `RegisterRequest` : deux champs `@NotBlank` en tete du record, `toString()` mis a jour
     pour masquer les champs sensibles en conservant `firstName`/`lastName` visibles.
   - `UserResponse` : `firstName` (`@JsonProperty("first_name")`) et `lastName`
     (`@JsonProperty("last_name")`) ajoutes en positions 2 et 3 (apres `id`).
   - `AuthServiceImpl.register()` : `firstName(request.firstName())` et
     `lastName(request.lastName())` ajoutes au builder `AppUser`.
   - `DataSeeder` (`feature/user-management`) : `firstName("Admin")` et `lastName("User")`
     ajoutes au builder de l'admin bootstrap.

4. **`@Unique`** : contrainte Jakarta Bean Validation generique reutilisable, remplace les
   verifications manuelles d'unicite dans `AuthServiceImpl`. Packages dedies :
   `validation/unique/Unique.java` (annotation avec attributs `entityClass`, `fieldName`,
   `caseSensitive`) et `validation/unique/UniqueValidator.java` (implementor).
   - `EntityManager` injecte via **`@Autowired(required = false)`** et non `@PersistenceContext`.
     Raison : `@PersistenceContext` est traite par `PersistenceAnnotationBeanPostProcessor` qui
     leve une `BeanCreationException` si aucun `EntityManagerFactory` n'est disponible (cas des
     slices `@WebMvcTest`). `@Autowired(required = false)` est traite par
     `AutowiredAnnotationBeanPostProcessor` qui saute silencieusement l'injection quand le bean
     est absent, ce qui permet au validateur de se construire dans n'importe quel contexte de
     test. Garde explicite `if (entityManager == null) return true` dans `isValid()` : la
     contrainte de BD reste le garde-fou definitif (TOCTOU assume).
   - `@Unique` pose sur `username` (case-sensitive) et `email` (case-insensitive,
     `caseSensitive = false`) dans `RegisterRequest`, avec JPQL `LOWER()` pour le second cas.
   - Suppression des appels `existsByUsername` / `existsByEmailIgnoreCase` dans
     `AuthServiceImpl.register()` (remplaces par la validation declarative).
   - Tests : `UniqueValidatorTest` (`@SpringBootTest` + inline `@TestConfiguration` Testcontainers,
     3 tests d'integration via le stack Validator complet).

**Perimetre item 5 (Locale/i18n) : explicitement exclu par l'utilisateur ("5 n'est pas
necessaire").**

**Cascade branche par branche :**
- `feature/roles-permissions` : merge direct depuis `feature/auth`, aucun conflit.
- `feature/token-management` : un conflit sur un fichier de test, resolu proprement.
- `feature/user-management` : 5 fichiers en conflit lors du merge (`UserResponse.java`,
  `AuthServiceImpl.java`, `AuthControllerTest.java`, `SecurityAuthorizationTest.java`,
  `AuthServiceImplTest.java`). Strategie de resolution : garder la forme HEAD (plus evoluee,
  avec `RoleResponse`/`List<Role>` JPA) et y incorporer les champs firstName/lastName entrants.
  Egalement corrige un bug pre-existant dans `SecurityAuthorizationTest` : plusieurs tests
  (`_14_`, `_18_`, `_19_`, `_20_`) passaient le username au lieu de l'email dans `LoginRequest`,
  alors que `AuthServiceImpl.login()` utilise `request.email()` pour chercher l'utilisateur.
  Corrige en utilisant les emails corrects (`hopper@example.com`, etc.).
  Mot de passe change de `"password123"` a `"Password1@"` dans tous les appels a `RegisterRequest`
  (impose par la nouvelle contrainte `@ValidPassword`).

**Bug decouvert lors de la cascade vers feature/user-management :**
`UniqueValidatorTest.java` importait `com.edgareldy.springboottutorial.entity.user.Role`
(l'enum Role, present sur `feature/auth`) et utilisait `.role(Role.USER)` sur le builder
`AppUser`. Sur `feature/user-management`, l'enum Role a ete remplace par l'entite JPA `Role`
(`entity.Role`, plus de champ `.role()`), et `AppUser.roles` est un `@ManyToMany Set<Role>`
avec `@Builder.Default = new HashSet<>()`. Corrige en supprimant l'import et les appels
`.role(Role.USER)` : les AppUser de test sont crees sans roles, ce qui est valide.

**Bug decouvert lors du deuxieme passage de tests sur feature/user-management :**
`UserControllerTest` et `UserServiceImplTest` avaient 10 appels au constructeur
`UserResponse(Long, String, String, boolean, boolean, List)` (ancienne forme a 6 args) alors
que le record avait ete mis a jour vers 8 args (`Long id, String firstName, String lastName,
String username, String email, boolean enabled, boolean accountLocked, List<RoleResponse>`).
Le CLAUDE.md precedent mentionnait "4 classes de test mises a jour" mais ces deux classes
avaient ete omises. Corrige en inserant `firstName` et `lastName` en positions 2 et 3 sur
tous les sites d'appel (commits atomiques separes par fichier, 11/8/26).

**Tests verts a la cloture :**
- `feature/auth` : 207/207
- `feature/roles-permissions` : non recompte (merge propre sans modification)
- `feature/token-management` : non recompte (un conflit mineur resolu)
- `feature/user-management` : **347/347** (53 classes de test, 11/8/26)

## Affectation en lot : PATCH add/remove sur permissions de role et roles d'utilisateur (11/12/26)

Demande utilisateur : pouvoir ajouter **et** retirer une ou plusieurs permissions d'un role en
un appel, idem pour les roles d'un utilisateur. Le modele du projet frere
`postify-microservices/auth-service` (`POST` additif en lot + `DELETE` unitaire) a ete
explicitement ecarte, ainsi que `PUT` de remplacement complet (risque d'ecrasement entre deux
admins, liste complete a renvoyer).

**Modele retenu, identique pour les deux relations many-to-many :**
- `PATCH /api/v1/roles/{roleId}/permissions` (body `UpdateRolePermissionsRequest`, `dto/role`,
  introduit sur `feature/roles-permissions`)
- `PATCH /api/v1/users/{id}/roles` (body `UpdateUserRolesRequest`, `dto/user`, introduit sur
  `feature/user-management`)
- Corps : `{"add": [1, 2], "remove": [3]}`, les deux champs optionnels.
- Validation (400) via deux `@AssertTrue` sur des accesseurs derives du record (`@JsonIgnore`) :
  au moins un des deux ensembles non vide, aucun id present dans les deux.
- Service (`RoleService.updatePermissions`, `UserService.updateRoles`) : un seul `findAllById`
  sur l'union des ids, 404 listant les ids inconnus avant toute modification, semantique
  "assurer present / assurer absent" (ajouter un element deja present ou retirer un absent est
  un no-op, pas un 422), donc idempotent.
- Endpoints supprimes : `POST`/`DELETE /roles/{roleId}/permissions/{permissionId}`,
  `PUT /users/{id}/roles`, `DELETE /users/{id}/roles/{roleId}`, et le DTO `AssignRolesRequest`.

**Nommage** : un premier DTO generique partage (`dto/common/AssociationChangeRequest`) a ete
refuse par l'utilisateur ("pas un nom parlant"). Regle a retenir : preferer un DTO par usage,
nomme d'apres ce qu'il modifie (modele `UpdateProfileRequest`), meme au prix d'une petite
duplication.

**Correctif au passage** : `feature/roles-permissions` ne compilait pas seule
(`AuthController.tokenInfo` portait encore `@Operation`/`@ApiResponses` sans imports) ;
annotations retirees, aligne sur les branches aval.

**Etat** : commits atomiques sur `feature/roles-permissions`, cascade
`-> feature/token-management -> feature/user-management` (merges sans conflit), puis commits
roles utilisateur sur `feature/user-management`. `mvnw clean test-compile` OK sur les trois
branches ; **suite de tests non lancee** (c'est l'utilisateur qui la lance). Rien de pousse,
pas de PR. **Reste a faire** : mettre a jour `spring-boot-tutorial.insomnia.yaml` (present
uniquement sur `feature/docs`/`develop`, 3 requetes concernees : "Assign roles to user" et les
deux `/roles/1/permissions/1`).

**Suite (11/12/26)** : collection Insomnia mise a jour sur `feature/docs` (2 commits locaux) :
requetes PATCH add/remove, reset-password corrige (token en query param + `confirm_password`),
ajout de refresh-token et token-info, exemples d'ids coherents avec le seed.

## Commandes par client : GET /orders regroupe + commandes sur /customers (11/12/26)

Demande utilisateur : voir pour chaque client les produits commandes, leur quantite et leurs
totaux. Introduit sur `feature/orders`.

- **`GET /api/v1/orders`** : pagine par client (plus par commande). Chaque entree est un
  `CustomerOrdersResponse` : `customer` (`OrderCustomerResponse`, sans liste `products`),
  `products` (une ligne `OrderedProductResponse` par commande : `order_id`, `product_id`,
  `product_name`, `unit_price`, `quantity`, `total`) et `total` (somme arrondie au centime).
  Deux requetes quelle que soit la taille de page : `OrderRepository.findCustomersWithOrders`
  (EXISTS) puis `findAllProjectedByCustomerIds` (IN). Le `sort` porte sur les champs du client.
- **`GET /api/v1/customers` et `/customers/{id}`** : `CustomerResponse.products` est maintenant
  une liste de `OrderedProductResponse` (memes lignes) + champ `total`, peuples sur la liste
  (une seule requete IN pour la page) et sur le detail. `update()` renvoie aussi les commandes.
  `findDistinctProductsByCustomerId` et `CustomerMapper.toDetailResponse` supprimes
  (remplaces par `toResponseWithOrders`).
- `OrderResponse` (une commande : detail, create, update, `/customers/{id}/orders`) garde sa
  forme mais son `customer` est un `OrderCustomerResponse`.
- Une commande ne contient toujours qu'un seul produit en base (pas de table de lignes).

**Cascade et regle rappelee par l'utilisateur** : "merger en fonction de l'arborescence des
branches", donc toujours `feature/orders -> feature/auth -> feature/roles-permissions ->
feature/token-management -> feature/user-management`, sans sauter `feature/auth` meme si elle
est en retard. Le merge `orders -> auth` a donc rattrape 29 commits anciens de `feature/orders`
(6 conflits : `GlobalExceptionHandler` + son test, `CorsConfig`, `.env.example` repris tels
quels de `feature/roles-permissions` qui avait deja la bonne fusion ; README cote orders ;
`OrderControllerTest` les deux cotes). `auth -> roles-permissions` a aussi apporte le refacto
`AuthResponse` (`access_token`) que cette branche n'avait jamais recu. Piege evite : resoudre
un conflit par `git checkout --ours -- fichier` ecrase aussi les changements non conflictuels
de l'autre cote (ici `access_token` dans `SecurityAuthorizationTest`) ; resoudre bloc par bloc.

**Etat** : `mvnw clean test-compile` OK sur les 5 branches, **tests non lances**, rien de
pousse (push bloque par les permissions Claude Code, a faire par l'utilisateur).

## Environnement de dev : Docker/Testcontainers

Sur cette machine, Testcontainers echoue parfois avec `Could not find a valid Docker
environment` alors que `docker info`/`docker run` fonctionnent normalement en CLI. Cause :
Testcontainers sonde `unix:///var/run/docker.sock` par defaut, mais le contexte Docker actif
est `desktop-linux` (`unix:///home/edgareldy/.docker/desktop/docker.sock`), et l'utilisateur
n'est pas dans le groupe `docker`. **Si des tests Testcontainers echouent avec cette erreur,
relancer avec `DOCKER_HOST=unix:///home/edgareldy/.docker/desktop/docker.sock` explicitement
avant la commande Maven** plutot que de conclure a un bug de code. Ce n'est pas systematique
(ca fonctionne aussi parfois sans), mais c'est le premier reflexe de diagnostic si Docker
CLI repond mais que Testcontainers ne le trouve pas.

## Écarts par rapport au README initial

- Le projet a été initialement discuté avec Spring Boot 4.1.0 / Spring Framework 7, mais
  springdoc-openapi n'avait pas encore de version compatible sur Maven Central au moment de
  l'implémentation → **bascule décidée par l'utilisateur sur Spring Boot 3.5.16 / Spring
  Framework 6**, stack pleinement compatible avec tout l'écosystème requis (springdoc,
  MapStruct, jjwt). Le README a été mis à jour en conséquence et traduit en anglais.

## Commandes utiles du projet

```bash
# Build (Java 17 explicite, indépendamment du java par défaut du système)
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw clean install

# Tests
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw test

# Lancer l'app en local (profil dev)
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Stack complète via Docker Compose (app + db Postgres)
docker compose up --build

# Swagger UI une fois lancé
http://localhost:8080/swagger-ui.html
```

## Rappel pour toi-même (Claude)

- Relis ce fichier **avant** de reprendre l'implémentation à chaque nouvelle session.
- Tiens-le à jour au fil de l'eau : nouvelles décisions techniques, écarts avec le README,
  avancement des branches (case à cocher ci-dessus), commandes ajoutées/modifiées.
- Applique systématiquement les quatre règles impératives ci-dessus (pas de co-author Claude
  dans les commits, Javadoc anglais sur chaque nouveau type, pas de tiret cadratin dans le
  code, un commit atomique par fichier créé/modifié) sans qu'on ait à te le redemander.
- Ne recopie pas le README ici — ce fichier référence le README pour les détails stables
  (modèle de données, structure des packages, checklists de tâches) et ne stocke que ce qui
  change ou ce qui aide à reprendre le travail rapidement.
- Voir `.claude/GAP_ANALYSIS.md` pour la liste des concepts Spring Boot non couverts, des
  bonnes pratiques à corriger, et l'état des lieux SOLID (analyse du 9/3/26, à re-vérifier
  contre le code actuel avant d'agir dessus) — utile pour planifier un futur lot de concepts,
  sur le même modèle que les lots déjà documentés plus haut.
