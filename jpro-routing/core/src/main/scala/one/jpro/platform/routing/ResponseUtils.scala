package one.jpro.platform.routing

/**
 * ResponseUtils class.
 *
 * @author Besmir Beqiri
 */
object ResponseUtils {

  def redirect(to: String): Response = Redirect(to)

}
