package bj.actor

import bj.card.Card
import bj.hkeeping.Broke
import bj.hkeeping.Ok
import bj.card.BetterHand
import bj.card.BasicStrategy

class BetterPlayer(name: String, var betterbankroll: Double, betAmt: Double, ip: String, port: Int) extends Player(name, betterbankroll, betAmt, ip, port) with BetterHand with BasicStrategy {

  /**
   * Taken from player to be modified and made better.
   * Better player will
   * A.) Hit instead of surrendering.
   * B.) Split an arbitrary number of times provided the bankroll is available.
   * C.) Double down on splits if applicable.
   * D.) Never buy insurance.
   */
  override def analyze(upcard: Card): Request = {
    Thread.sleep(5000)
    var bs_Key: Any = " "

    // If the player only has two cards
    if (cards.size <= 2) {

      // If the two player cards have the same value
      if (cards(0).value == cards(1).value) bs_Key = List(cards(0).value, cards(1).value)

      // If one of the player's cards is an Ace
      else if (cards(0).ace || cards(1).ace) {
        if (cards(0).ace) bs_Key = List(1, cards(1).value)
        else bs_Key = List(1, cards(0).value)
      } // Use the hand's total value
      else bs_Key = value
    } else {
      // Use the hand's total value
      bs_Key = value
    }

    if (value > 21) return Stay(pid) // Player has broke... temporary fix 

    val playerMove = basicStrategy(bs_Key)(upcard.value)

    if (playerMove.equals("H")) Hit(pid)
    else if (playerMove.equals("S")) Stay(pid)
    else if (playerMove.equals("D")) DoubleDown(pid)
    else if (playerMove.equals("P")) Split(pid)
    else Stay(pid) // Should not reach here
  }

  /** This method receives messages */
  override def act {
    loop {
      react {
        // Receives message to tell player to place its bet
        case Go =>
          debug(this + " received Go placing bet = " + betAmt + " from bankroll = " + bankroll)
          bet

        // Receives the dealer's up-card which is player's cue to play
        case Up(card) =>
          debug(this + " received dealer's up card = " + card)

          play(card)

        // Receives a card from the dealer
        case card: Card =>
          hitMe(card)

        // Receives a double-down card from the dealer
        case DD(card) =>
          this.hit(card)
          debug(this + " received card = " + card + " after double down. Hand sz = " + cards.size + " value = " + value)

        // Receives broke message
        case Broke =>
          debug(this + " received BROKE")

        // Receives message about dealt card
        case Observe(card, player, shoeSize) =>
          debug(this + " observed: " + card)
          observe(card, player, shoeSize)

        // Receives message about shoe re-shuffling
        case Reshuffle() =>
          debug(this + " observed the shoe being reshuffled.")
        // TODO: Reset card counting

        // Receives the table number I've been assigned to
        case TableNumber(tid: Int, minBet: Double) =>
          debug(this + " received table assignment tid = " + tid)
          assign(tid)

        case Win(gain) =>
          val won = betAmt * gain

          bankroll += won
          debug(this + " received WIN " + won + " new bankroll = " + bankroll)

          clearHand
          debug(this + " hand cleared.")

        case Lose(gain) =>
          val lost = betAmt * gain

          bankroll += lost
          debug(this + " received LOSE " + lost + " new bankroll = " + bankroll)

          clearHand
          debug(this + " hand cleared.")

        case Push(gain) =>
          debug(this + " received PUSH bankroll = " + bankroll)

          clearHand
          debug(this + " hand cleared.")

        // Receives an OK
        case Ok =>
          debug(this + " received Ok")

        case ObserveBet(id, bet) => //Do nothing

        // Receives a GameStart
        case GameStart => //Do nothing

        // Receives something completely from left field
        case dontKnow =>
          // Got something we REALLY didn't expect
          debug(this + " received unexpected: " + dontKnow)
      }
    }
  }
}