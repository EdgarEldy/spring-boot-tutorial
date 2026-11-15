# Conventions de code

Les conventions ci-dessous s'appliquent à chaque fichier du projet. Elles maintiennent la
cohérence du code pour qu'un lecteur puisse comprendre n'importe quelle fonctionnalité sans
apprendre de nouveaux patterns. En cas de doute, lisez une fonctionnalité existante comme
implémentation de référence.

---

## Idiomes Java

**Les DTOs sont des records Java.**
Les records sont immuables par nature. Pas de setters, ce qui élimine le risque de mutation
accidentelle entre les couches. MapStruct génère le code de mapping à la compilation à
partir des noms des composants du record.

**Les services sont injectés via leur interface.**
Les contrôleurs et les tests déclarent un champ de type `CategoryService`, jamais
`CategoryServiceImpl`. Cela découple les appelants de l'implémentation et simplifie le mock.

**`@Transactional` sur l'implémentation, pas sur l'interface.**
Les frontières transactionnelles appartiennent près du code d'accès aux données. Les
opérations d'écriture reçoivent `@Transactional` ; les opérations de lecture reçoivent
`@Transactional(readOnly = true)` pour un léger gain de performance.

**snake_case dans le JSON via `@JsonProperty`.**
Les champs Java restent en camelCase (cohérent avec la convention du langage). Le contrat
JSON utilise le snake_case. `@JsonProperty("snake_case")` sur les composants individuels du
record découple les deux sans changer le nommage Java.

**Les entités JPA ne quittent jamais la couche service.**
Les contrôleurs reçoivent et retournent uniquement des DTOs. Les mappeurs MapStruct
convertissent entre entité et DTO à l'intérieur du service. Cela empêche le chargement
paresseux d'Hibernate de provoquer `LazyInitializationException` en dehors d'une transaction.

---

## Javadoc

La Javadoc est obligatoire sur chaque nouvelle `class`, `interface`, `enum` et `record`.
Utilisez ce format :

```java
/**
 * Description concise du rôle de ce type, en une à trois lignes.
 * <p>
 * Created edgar.muhamyangabo on 11/15/26
 * Author : edgar.muhamyangabo
 * Date : 11/15/26
 * Project : spring-boot-tutorial
 */
```

Règles :
- L'auteur est toujours `edgar.muhamyangabo`, quel que soit le nom d'utilisateur git sur la machine.
- Le format de date est `M/D/YY` sans zéro de tête (4 juillet 2026 correspond à `7/4/26`).
- Rédigée en anglais.
- Pas de tiret cadratin dans le texte Javadoc.

---

## Données sensibles dans les logs

`LoggingAspect` logue les arguments et les valeurs de retour de toutes les méthodes de
service via `toString()`. Les records génèrent automatiquement `toString()` à partir de
leurs composants. Tout record qui contient un mot de passe, un JWT ou d'autres données
sensibles doit surcharger `toString()` pour masquer ces champs :

```java
public record LoginRequest(String email, String password) {
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
```

Ne pas le faire exposera des identifiants dans `logs/app.log` en production.

---

## Pas de tiret cadratin

N'utilisez jamais le tiret cadratin (`-`) dans le code, la Javadoc, les commentaires, les
chaînes de caractères ou les logs. Utilisez un tiret simple (`-`) à la place, ou reformulez
en deux phrases. Cette règle s'applique à tous les fichiers du projet : Java, YAML, SQL,
Markdown, Dockerfile.

---

## Commits

Un fichier par commit. Chaque message de commit suit les Conventional Commits :

```
type(scope): résumé court à l'impératif

Le corps explique pourquoi ce changement a été effectué, rédigé pour un
développeur qui suit le tutoriel. Expliquez le concept ou la décision
illustrée ici, pas uniquement ce qui a changé.
```

Types courants : `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `build`.
Le scope est le package ou le domaine concerné : `dto`, `controller`, `security`, `flyway`.

Un corps est requis lorsque le changement illustre un concept qui mérite d'être expliqué
(le cas habituel). Il peut être omis pour les changements triviaux et auto-explicatifs.

N'ajoutez pas de ligne `Co-Authored-By: Claude` ni aucune attribution à une IA dans les
commits.

---

## Conventions d'API

- Tous les endpoints sont préfixés par `/api/v1`.
- Les chemins d'endpoints utilisent des noms au pluriel en kebab-case (`/product-categories`).
- Chaque endpoint possède une annotation `@Operation(summary = "...")`.
- Chaque réponse de contrôleur est encapsulée dans `ApiResponse<T>`.
- Les listes paginées utilisent `ApiResponse<PageResponse<T>>` comme type de réponse.
- Les messages d'erreur de validation sont rédigés sous forme de prose lisible, jamais comme
  le nom brut du champ.
