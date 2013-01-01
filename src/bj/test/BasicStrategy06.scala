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
import bj.actor.DoubleDown
import bj.actor.BetterPlayer

/**
 * Test 5+6 vs. 3: DOUBLEDOWN(10)
 */
class BasicStrategy06 extends Logs {
  // Tell log4j how to format output
  PropertyConfigurator.configure("log4j.properties")

  @Test
  def test {
    // Create a player with a bankroll of $100 and betting $25
    val player = new BetterPlayer("Ron", 100, 25, "127.0.0.1", 6000)

    // Start the player otherwise, the player can't receive messages
    player.start

    // Deal two cards
    player ! Card(5, Card.HEART)
    player ! Card(6, Card.SPADE)

    // Send the up-card to tell player it's its turn
    player ! Up(Card(3, Card.SPADE))

    // Wait for the player to respond
    receiveWithin(2000) {
      case DoubleDown(6) =>
        // Send the player the DoubleDown card
        //player ! Card(Card.TEN, Card.HEART)

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