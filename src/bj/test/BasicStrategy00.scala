package bj.test
import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import org.apache.log4j.PropertyConfigurator
import bj.util.Logs
import bj.actor.House
import bj.actor.Player
import bj.actor.Go
import bj.card.Card
import bj.actor.Up
import scala.actors.Actor._
import bj.actor.Stay
import scala.actors.TIMEOUT
import bj.actor.BetterPlayer

/**
 * Test 10+10 vs. 2 => STAY
 * @author Ron Coleman, Ph.D.
 */
class BasicStrategy00 extends Logs {
  // Tell log4j how to format output
  PropertyConfigurator.configure("log4j.properties")

  @Test
  def test {
    // Create a player with a bankroll of $100 and betting $25
    val player = new BetterPlayer("Ron", 100, 25, "127.0.0.1", 6000)

    // Start the player otherwise, the player can't receive messages
    player.start

    // Deal two cards
    player ! Card(Card.TEN, Card.CLUB)
    player ! Card(Card.TEN, Card.DIAMOND)

    // Send the up-card to tell player it's its turn
    player ! Up(Card(2, Card.SPADE))

    // Wait for the player to respond
    receiveWithin(2000) {
      case Stay(0) =>
        debug("TEST PASSED")
        assert(true)

      case TIMEOUT =>
        debug("TEST FAILED player crashed?")
        assert(false)

      case huh =>
        debug("TEST FAILED got result = " + huh)
        assert(false)
    }
  }
}