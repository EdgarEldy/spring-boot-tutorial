# Branche : feature/customers

Cette branche ajoute la ressource Customer. Elle est plus simple que les produits (pas de
cache, pas de verrouillage optimiste), mais elle introduit un pattern important : appliquer
l'unicité de l'email sans sensibilité à la casse au niveau du service et du repository, et
protéger l'intégrité référentielle avant la suppression.

---

## Ce que vous allez apprendre

- Comment appliquer l'unicité d'email sans sensibilité à la casse au niveau du service et du repository
- Comment implémenter une recherche par nom partiel avec `LIKE` et `LOWER()` en JPQL
- Comment protéger l'intégrité référentielle en vérifiant les enregistrements liés avant la suppression
- Comment remonter une violation de règle de domaine comme `BusinessRuleException` (422) plutôt que de laisser une violation de contrainte de base de données remonter en 500

---

## Ce que cette branche apporte

### Entité Customer et repository

`Customer` possède les champs `name`, `email` et `phone`. `CustomerRepository` fournit :

- `existsByEmailIgnoreCase(String email)` - utilisé par create et update pour imposer l'unicité
- `findByNameContainingIgnoreCase(String name, Pageable pageable)` - pour la recherche par nom partiel
- `findByEmailIgnoreCase(String email)` - pour le lookup par email

### CustomerServiceImpl

`create()` et `update()` appellent tous deux `existsByEmailIgnoreCase` avant de sauvegarder.
Si l'email est déjà pris (par un autre client lors d'une mise à jour), une
`BusinessRuleException` est levée avec un message lisible. Le code 422 signale au client que
la requête était bien formée mais ne peut pas être traitée en raison d'une contrainte de
domaine.

> **Le bug original :** La première version de `create()` utilisait `existsByEmail` (sensible
> à la casse), tandis que `update()` utilisait `equalsIgnoreCase`. Cela permettait à deux
> clients d'avoir la même adresse email si la casse différait : `ada@example.com` et
> `ADA@EXAMPLE.COM` étaient traités comme différents. Le correctif a renommé la méthode du
> repository en `existsByEmailIgnoreCase` et l'a utilisée de façon cohérente dans create et
> update.

### Garde de suppression

`CustomerServiceImpl.delete()` appelle `OrderRepository.existsByCustomerId(id)` avant de
supprimer le client. Si des commandes référencent ce client, une `BusinessRuleException` est
levée. Cela empêche qu'une `DataIntegrityViolationException` (la contrainte de clé étrangère
sur `orders.customer_id`) remonte au client sous forme d'erreur 500.

> **Pourquoi ne pas se reposer sur la contrainte de base de données ?** La contrainte de base
> de données est le filet de sécurité définitif, mais une violation brute de contrainte
> Hibernate produit une réponse 500 générique avec un message d'erreur interne. Vérifier au
> niveau du service donne le contrôle sur le message d'erreur, le code de statut HTTP et le
> format de la réponse.

### CustomerController

Endpoints CRUD standard sous `/api/v1/customers`. L'endpoint de liste supporte un paramètre
de requête `name` optionnel pour la recherche par nom partiel.

---

## Fichiers clés

| Fichier | Rôle |
|---|---|
| `src/main/java/.../entity/Customer.java` | Entité JPA Customer |
| `src/main/java/.../repository/CustomerRepository.java` | Méthodes de requête personnalisées |
| `src/main/java/.../service/CustomerService.java` | Interface de service |
| `src/main/java/.../service/impl/CustomerServiceImpl.java` | Garde de suppression + unicité |
| `src/main/java/.../controller/CustomerController.java` | Endpoints REST |
| `src/main/resources/db/migration/V1__init.sql` | Table customers (migration initiale) |
