# Analyse des lacunes : concepts Spring Boot, bonnes pratiques, SOLID

> Analyse effectuee le 9/3/26 sur l'etat du projet a ce moment (toutes les branches
> `feature/*` terminees, refacto `ResponseEntity`/snake_case/DTOs imbriques + rename de
> package `com.edgareldy` cascades jusqu'a `master`). Ceci est un instantane de recommandations,
> pas une liste de taches actees : a re-verifier avant d'implementer quoi que ce soit, le code
> peut avoir change depuis. Ne pas dupliquer dans `CLAUDE.md` (memoire d'implementation) ni dans
> `README.md` (specification suivie pas a pas) : ce fichier est un rapport de gap analysis
> autonome, a consulter quand on planifie une nouvelle branche ou un futur lot de concepts.

## 1. Concepts Spring Boot non couverts

### Web / API
- Pas de **versioning au-dela du prefixe URL** (`/api/v1`) : pas de versioning par header
  `Accept`, pas de mecanisme de depreciation d'endpoint.
- Pas de **rate limiting** (Resilience4j `RateLimiter`, Bucket4j) : `/auth/login` et
  `/auth/register` sont exposes au brute-force sans aucune limite.
- Pas de **HATEOAS** (`spring-hateoas`) : aucune reponse ne contient de lien navigable.
- Pas de gestion **multipart / upload de fichiers**.
- Pas de **WebSocket** / STOMP.
- Pas de **content negotiation** avancee (XML, autre que JSON).

### Resilience
- Pas de **Resilience4j** (circuit breaker, retry, bulkhead, timeout) : pertinent des qu'un
  jour le projet appelle un service externe.
- Pas de **`@Async`** ni `@EnableAsync` : tout est synchrone, y compris
  `OrderCreatedEventListener` qui pourrait illustrer un traitement asynchrone apres commit.
- Pas de **`@Scheduled`** / taches planifiees.

### Data / persistance
- Pas de **Redis** ni de cache distribue : Caffeine est un cache local uniquement, ne scale
  pas horizontalement.
- Pas de **QueryDSL** / Criteria API avancee (le projet a `Specification`, c'est deja bien,
  mais reste basique : un seul filtre combine sur `Product`).
- Pas de **soft delete** (colonne `deleted_at` + filtre global) : chaque `delete()` est un
  DELETE physique.
- Pas de **batch processing** (Spring Batch, ou simplement `saveAll`/insert JDBC par lot).
- Pas de **routing datasource / read replica**. Egalement absent : `@Transactional(readOnly =
  true)` sur les methodes de lecture des services, optimisation Hibernate simple (desactive le
  dirty-checking) et bonne pratique standard non demontree nulle part.

### Observabilite
- Pas de **Micrometer + tracing distribue** (Zipkin/OTel) : Actuator est present mais sans
  metriques custom ni traces.
- Pas de **metriques metier custom** (`MeterRegistry.counter(...)`, ex. compter les commandes
  creees).
- Pas de **health indicators custom** (`HealthIndicator`) au-dela du defaut DB.

### Securite
- Pas de **refresh token** : `login()` ne renvoie qu'un access token a duree fixe (1h), aucun
  mecanisme de renouvellement.
- Pas de **revocation/blacklist de token** (logout reel impossible avec un JWT stateless pur).
- Pas de **verrouillage de compte** apres N echecs de connexion.
- Pas de **`@PreAuthorize`/`@PostAuthorize`** (securite methode) en complement de la securite
  par URL dans `SecurityConfig` : bon complement pedagogique a la securite declarative
  existante.
- Pas de **test dedie CORS** au-dela de la config de base (`CorsConfig`).

### Async / messaging
- Pas de **Kafka/RabbitMQ** : `OrderCreatedEvent` reste un evenement Spring in-process, jamais
  pousse vers un vrai broker.
- Pas de **transactional outbox pattern** (pertinent une fois qu'on veut publier vers un
  broker de facon fiable).

### Tests
- Pas d'**ArchUnit** (verifier automatiquement les regles de couches : le controller ne doit
  jamais importer un repository, etc.).
- Pas de **contract testing** (Pact) ni de test de **mutation** (PIT).
- Pas de test de **performance/charge** (Gatling, JMeter).

### i18n
- Pas de **`MessageSource`** : tous les messages d'erreur/validation sont en anglais en dur,
  aucune internationalisation.

## 2. Bonnes pratiques a corriger (trouvees dans le code actuel)

1. **Argent en `float`/`double` au lieu de `BigDecimal`** — `Product.unitPrice` (`float`,
   `entity/Product.java:54`) et `Order.total` (`double`, `entity/Order.java:54`). Anti-pattern
   classique : les flottants binaires ne representent pas exactement les valeurs decimales,
   source de bugs d'arrondi cumules. A corriger en priorite si le projet doit illustrer de
   bonnes pratiques.

2. **Aucune entite n'a d'`equals()`/`hashCode()`** — `Order`, `Product`, `Category`,
   `Customer`, `AppUser` reposent sur l'identite d'objet par defaut. Sans collection (`Set`)
   actuellement ca ne casse rien, mais c'est un piege JPA bien connu a documenter (equals
   base sur l'id metier, jamais sur tous les champs a cause du lazy loading).

3. **`GlobalExceptionHandler` n'a toujours pas de handlers specifiques** pour
   `HttpMessageNotReadableException` (JSON malforme), `HttpRequestMethodNotSupportedException`
   (405), `MethodArgumentTypeMismatchException` (mauvais type de parametre),
   `NoHandlerFoundException` (404 generique) : tout ca retombe sur le catch-all
   `Exception.class` -> 500. Point de vigilance deja note dans `CLAUDE.md` depuis
   `feature/core-architecture` et jamais traite a ce jour.

4. **Pas de `@Transactional(readOnly = true)`** sur les methodes de lecture des services (voir
   aussi section "Data / persistance" ci-dessus).

5. **`Instant.now()` appele en dur** dans `ApiResponse`/`GlobalExceptionHandler` plutot qu'un
   `Clock` injecte : rend ces classes impossibles a tester avec un temps fige.

6. **Pas de separation `@Valid` avec groupes de validation** — un seul jeu de contraintes par
   DTO ; pas de demonstration de regles differentes pour create vs update.

7. **`LoggingAspect` masque les champs sensibles via des `toString()` custom par DTO**, ce qui
   fonctionne mais est fragile (repose sur la discipline du developpeur a chaque nouveau DTO
   sensible) plutot que sur une exclusion declarative (ex. annotation `@Sensitive` custom +
   logique d'aspect qui la respecte automatiquement).

## 3. Principes SOLID — etat des lieux

**Bien applique :**
- **SRP** : decoupage net controller / service / repository / mapper, chaque classe a une
  seule raison de changer.
- **DIP** : les controllers dependent des interfaces (`CategoryService`, jamais
  `CategoryServiceImpl`), injection par constructeur partout (`@RequiredArgsConstructor`),
  jamais de `@Autowired` sur un champ.
- **ISP** : chaque interface de service est etroite et specifique a une ressource, pas
  d'interface fourre-tout.

**Plus discutable / a ameliorer :**
- **OCP** : `GlobalExceptionHandler` doit etre modifie a chaque nouvelle exception (normal pour
  ce pattern, mais montre la limite : envisager de documenter pourquoi c'est acceptable ici vs
  un vrai plugin d'exceptions). Idem pour `ProductServiceImpl.findAll`, qui bascule entre deux
  chemins (`Specification` vs `@EntityGraph`) via un `if` plutot qu'une strategie polymorphe :
  acceptable pour un tutoriel, mais pas un exemple pur d'OCP.
- **LSP** : peu testable ici vu qu'il n'y a quasiment pas d'heritage (bon signe en soi,
  favoriser la composition), donc rien de negatif a signaler, mais aussi rien d'illustre
  positivement (aucune hierarchie de classes n'existe pour demontrer LSP concretement).
- **DIP sur `SecurityConfig`** : `BCryptPasswordEncoder` est instancie directement, mais le
  bean expose bien le type `PasswordEncoder` (l'interface) : DIP est en fait deja respecte ici,
  faux probleme releve puis ecarte lors de l'analyse.

**Recommandation generale** : pour un tutoriel pedagogique, le point le plus rentable serait
d'ajouter une section dediee "Pourquoi ces choix respectent (ou non) SOLID" dans le README a
chaque branche, plutot que de sur-designer le code pour forcer une demonstration artificielle
de chaque principe. Certains principes (LSP notamment) ne se pretent pas naturellement a une
API CRUD sans hierarchie de types.
