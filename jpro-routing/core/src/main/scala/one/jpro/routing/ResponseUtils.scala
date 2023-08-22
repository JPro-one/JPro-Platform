package one.jpro.routing

/**
 * ResponseUtils class.
 *
 * @author Besmir Beqiri
 */
object ResponseUtils {

  def redirect(to: String): Response = Redirect(to)

}
