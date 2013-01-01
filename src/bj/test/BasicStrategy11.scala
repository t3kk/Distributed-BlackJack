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
import bj.actor.Hit
import bj.hkeeping.Broke
import bj.actor.BetterPlayer

/**
 * Test A+A vs. K: SPLIT
 *                 1st A: HIT(5 + 6 + 10) => BREAK
 *                 2nd A: HIT(9) => STAY
 */
class BasicStrategy11 extends Logs {
  // Tell log4j how to format output
  PropertyConfigurator.configure("log4j.properties")

  @Test
  def test {
    // Create a player with a bankroll of $100 and betting $25
    val player = new BetterPlayer("Ron", 100, 25, "127.0.0.1", 6000)

    // Start the player otherwise, the player can't receive messages
    player.start

    // Deal two cards
    player ! Card(Card.ACE, Card.HEART)
    player ! Card(Card.ACE, Card.SPADE)

    // Send the up-card to tell player it's its turn
    player ! Up(Card(Card.KING, Card.DIAMOND))

    // Wait for the player to respond
    receiveWithin(2000) {
      case Hit(11) =>
        ""

      case TIMEOUT =>
        debug("TEST FAILED player crashed?")
        assert(false)

      case huh =>
        debug("TEST FAILED got result = " + huh)
        assert(false)
    }
  }
}