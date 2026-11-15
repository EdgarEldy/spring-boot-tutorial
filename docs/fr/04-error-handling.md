# Gestion des erreurs

Toutes les erreurs de l'application sont traitées de manière centralisée par
`GlobalExceptionHandler`. Chaque réponse d'erreur a la même structure d'enveloppe qu'une
réponse de succès, ce qui uniformise la gestion côté client.

---

## GlobalExceptionHandler

`GlobalExceptionHandler` est annoté avec `@RestControllerAdvice`. Chaque méthode
`@ExceptionHandler` retourne `ResponseEntity<ApiResponse<ProblemDetail>>`.

L'utilisation de `ProblemDetail` de Spring 6 (RFC 7807) comme champ `data` signifie que les
réponses d'erreur portent des métadonnées lisibles par les machines (`type`, `title`,
`status`, `detail`, `instance`) en plus du `message` lisible par les humains au niveau de
l'enveloppe.

---

## Types d'exceptions

| Exception | Statut HTTP | Quand elle est levée |
|---|---|---|
| `ResourceNotFoundException` | 404 | Entité non trouvée par son id (findById, update, delete) |
| `BusinessRuleException` | 422 | Contrainte de domaine violée (supprimer une catégorie qui a des produits, supprimer un client qui a des commandes) |
| `OptimisticLockingFailureException` | 409 | Conflit de mise à jour concurrente sur une entité versionnée |
| `AccessDeniedException` | 403 | L'utilisateur authentifié ne possède pas le rôle ou la permission requis |
| `AuthenticationException` | 401 | Identifiants absents ou invalides |
| `MethodArgumentNotValidException` | 400 | Contrainte Bean Validation violée sur un DTO de requête |
| `Exception` (catch-all) | 500 | Erreur inattendue |

---

## Exemples de réponses

**404 - Ressource non trouvée :**

```json
{
  "success": false,
  "message": "Resource not found",
  "data": {
    "type": "about:blank",
    "title": "Not Found",
    "status": 404,
    "detail": "Category with id 99 not found",
    "instance": "/api/v1/categories/99"
  },
  "timestamp": "2025-11-15T10:30:00"
}
```

**400 - Erreur de validation :**

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "type": "about:blank",
    "title": "Bad Request",
    "status": 400,
    "detail": "Validation failed",
    "instance": "/api/v1/categories",
    "fieldErrors": {
      "name": "Category name must not be blank"
    }
  },
  "timestamp": "2025-11-15T10:30:00"
}
```

La map `fieldErrors` est construite en itérant `MethodArgumentNotValidException.getBindingResult()`
et est ajoutée comme extension `ProblemDetail` via `problemDetail.setProperty("fieldErrors", ...)`.

---

## Pourquoi BusinessRuleException n'est pas un 400

Un `400 Bad Request` signale que la requête elle-même est mal formée : un champ manquant,
un mauvais type, une violation de format. Le client doit corriger la requête et réessayer.

Un `422 Unprocessable Entity` signale que la requête est syntaxiquement correcte mais ne
peut pas être traitée en raison d'une contrainte de domaine. Le client doit modifier l'état
du système avant de réessayer (par exemple, supprimer les produits avant de supprimer leur
catégorie). Utiliser 422 communique cette distinction clairement.

---

## Messages d'erreur de validation

Les messages Bean Validation doivent être rédigés sous forme de prose lisible, jamais comme
le nom brut du champ.

```java
// Correct
@NotBlank(message = "Category name must not be blank")
String name,

// Incorrect - expose un détail d'implémentation aux clients
@NotBlank(message = "name must not be blank")
String name,
```

Le message est retourné tel quel dans la map `fieldErrors`. C'est le seul retour que le
client reçoit pour savoir quoi corriger ; il doit donc être compréhensible sans contexte
supplémentaire.
