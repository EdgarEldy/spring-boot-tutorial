package edgareldy.springboottutorial.entity.user;

/**
 * Authorization role assigned to an {@link AppUser}. Kept as a plain two
 * value enum since this tutorial does not need fine grained permissions.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public enum Role {
    ADMIN,
    USER
}
