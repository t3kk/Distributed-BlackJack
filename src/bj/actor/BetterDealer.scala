//Copyright (C) 2011 Ron Coleman. Contact: ronncoleman@gmail.com
//
//This library is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either
//version 3 of the License, or (at your option) any later version.
//
//This library is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this library; if not, write to the Free Software
//Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

package bj.actor
import scala.actors.Actor
import bj.hkeeping.Ok
import bj.hkeeping.NotOk
import bj.hkeeping.NotOk
import bj.hkeeping.Ok
import bj.hkeeping.Broke
import bj.card.Shoe
import bj.card.Card
import bj.card.BetterHand
import scala.collection.mutable.HashMap
import bj.hkeeping.NotOk
import scala.actors.OutputChannel
import bj.util.Logs

import scala.actors.remote.RemoteActor._

case class DD(card: Card)
case class Seed(sd: Integer)

/**
 * This class implements the dealer.
 * @param table The table I'm associated with.
 */
class BetterDealer(val minBet: Double) extends Dealer with BetterHand {

  val PORT = 6000

  /** Copies of player hands */
  hands = List[Object with BetterHand]()

  /** This method receives messages */
  override def act {
    alive(PORT)
    loop {
      react {
        // Receives players at the table
        case GameStart(players: List[OutputChannel[Any]]) =>
          debug(this + " received game start with " + players.size + " player mailboxes")
          gameStart(players, sender)

        case Seed(sd) =>
          this.seed = sd

        // Receives hit request from a player
        case Hit(pid) =>
          debug(this + " received HIT from player(" + pid + ")")
          hit(pid, sender)

        // Receives a stay request from a player
        case Stay(pid) =>
          debug(this + " received STAY from player(" + pid + ")")
          stay(pid, sender)

        // Receives a double-down request from a player
        case DoubleDown(pid) =>
          debug(this + " received DOUBLEDOWN from player(" + pid + ")")
          doubleDown(pid, sender)

        // Receives something completely from left field
        case dontKnow =>
          // Got something we REALLY didn't expect
          debug(this + " got " + dontKnow)
      }
    }
  }

  /**
   * Requests double-down for the source pid.
   * @param pid Player id
   * @param source Source actor of the message.
   */
  def doubleDown(pid: Int, player: OutputChannel[Any]) {

    if (!valid(player))
      player ! NotOk

    else {
      val card = shoe.deal

      // Send the double-down card to the player
      player ! DD(card)

      // Broadcast the card to all observers
      for (i <- (0 until players.size)) {
        if (players(i) != player) {
          players(i) ! Observe(card, pid, shoe.size)
        }
      }

      curHand.hit(card)
      curHand.doubleDowned = true

      if (curHand.broke) player ! Broke else player ! Ok

      moveNext(pid)
    }
  }

  /**
   * Moves to the next player at the table.
   * @param pid Player id
   */
  override def moveNext(pid: Int) {
    // Save the hand to pid map -- we need later for pay outs
    handMap(curHand) = pid

    // Advance the player index
    playerIndex += 1

    // If there are more players, send my up-card which
    // signals the player that its turn begins
    if (playerIndex < players.size) {
      curPlayer = players(playerIndex)

      curHand = hands(playerIndex)

      curPlayer ! Up(cards(0))
    } else {
      // Close the game with dealer play
      close

      // Do the payouts
      payout

      // Reset the dealer to start a new game
      prepareNewGame
    }

  }

  def prepareNewGame = {

    Thread.sleep(15000)

    // Clear the dealer's hand
    clearHand

    // Clear the dealer's hands for the other players
    //for(hand <- hands) hand.clearHand

    hands = List[Object with BetterHand]()
  }

  /**
   * Deals initial cards to a player
   * @param mailbox The player's mailbox
   */
  override def deal(player: OutputChannel[Any]): Unit = {
    val card1 = shoe.deal
    val card2 = shoe.deal

    debug(this + " dealing " + card1)
    debug(this + " dealing " + card2)

    deal(player, card1)
    deal(player, card2)

    val hand = new Object with BetterHand

    hand.hit(card1)
    hand.hit(card2)

    hands = hands ::: List(hand)
  }

  /** Sends pay-out info to the table */
  override def payout {
    val pays = HashMap[Int, Outcome]()

    for (hand <- hands) {
      val pid = handMap(hand)
      val basePayout = if (hand.doubleDowned) 2 else 1

      // Player loses
      if (hand.broke)
        pays(pid) = Lose(-1.0 * basePayout)

      else if (!this.broke && hand.value < this.value)
        pays(pid) = Lose(-1.0 * basePayout)

      // Push
      else if (this.value == hand.value)
        pays(pid) = Push(0.0)

      // Player wins automatically with blackjack
      else if (hand.blackjack)
        pays(pid) = Win(1.5)

      // Player wins if hand value is greater
      else if (hand.value > this.value)
        pays(pid) = Win(1.0 * basePayout)

      // Player wins if dealer breaks
      else if (!hand.broke && this.broke)
        pays(pid) = Win(1.0 * basePayout)
    }

    table ! GameOver(pays)
  }
}