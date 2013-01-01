package bj.actor

import bj.card.BetterHand
import bj.card.Card
import bj.hkeeping.Broke
import bj.hkeeping.Ok
import scala.actors.TIMEOUT
import bj.gui.HumanGUI
import bj.GUI
import bj.gui.CardRecieved
import bj.gui.UpRecieved
import bj.gui.BetChange
import bj.gui.Gameover

case class TableInfo(tableId: Int, minBet: Double)
case class WaitOnOthers()

object AllDone extends Exception {}

class HumanPlayer(name: String, var humanBankroll: Double, var betAmt: Double, ip: String, port: Int) extends BetterPlayer(name, humanBankroll, betAmt, ip, port) with BetterHand {

  var gui: HumanGUI = null

  override def analyze(upcard: Card): Request = {

    var req: Request = Stay(pid)

    try {
      while (true) {
        receive {
          case Stay => req = Stay(pid); throw AllDone
          case Hit => req = Hit(pid); throw AllDone
          case DoubleDown => req = DoubleDown(pid); throw AllDone
          case Split => req = Split(pid); throw AllDone
          case TIMEOUT => req = Stay(pid); throw AllDone
          case dontKnow => req = Stay(pid); throw AllDone
        }
      }
    } catch {
      case AllDone => return req
    }

    return req
  }

  /** Places a bet with the house */
  override def bet {
    if (bankroll < betAmt)
      return

    house ! BetWithName(pid, betAmt, this.name)
  }

  /**
   * Processes the dealer's upcard.
   * @param dealer Dealer's mailbox
   * @param upcard Dealer's up-card
   */
  override def play(upcard: Card) {
    this.upcard = upcard

    val temp = sender

    // Compute my play strategy
    val request = analyze(upcard)

    debug(this + " request = " + request)

    // Don't send a request if we break since
    // deal will have moved on
    if (!this.broke) {
      temp ! request
    } else {
      gui ! Broke
    }

  }

  /** This method receives messages */
  override def act {

    loop {
      react {
        // Receives message to tell player to place its bet
        case Go =>
          debug(this + " received Go placing bet = " + betAmt + " from bankroll = " + bankroll)
          bet

        case GUI(humanGUI) =>
          this.gui = humanGUI

        // Receives the dealer's up-card which is player's cue to play
        case Up(card) =>
          debug(this + " received dealer's up card = " + card)
          gui ! UpRecieved(card)
          play(card)

        // Receives a card from the dealer
        case card: Card =>
          //hitMe(card) - using this doesnt cause the GUI to update
          this.hit(card) //the GUI updates with this one

          //Inform the GUI
          gui ! CardRecieved(card, "player")

          //Add check from hitMe to this....
          if (cards.size > 2)
            play(this.upcard)

        // Receives a double-down card from the dealer
        case DD(card) =>
          this.hit(card)
          debug(this + " received card = " + card + " after double down. Hand sz = " + cards.size + " value = " + value)
          gui ! CardRecieved(card, "player")

        // Receives broke message
        case Broke =>
          debug(this + " received BROKE")

        // Receives message about dealt card
        case Observe(card, player, shoeSize) =>
          debug(this + " observed: " + card)
          observe(card, player, shoeSize)

          // TODO FIX
          if (player.equals(0))
            gui ! CardRecieved(card, "bot")
          else
            gui ! CardRecieved(card, "dealer")

        case BetChange(change) =>
          betAmt = betAmt + change

        // Receives message about shoe re-shuffling
        case Reshuffle() =>
          debug(this + " observed the shoe being reshuffled.")
        // TODO: Reset card counting

        // Receives the table number I've been assigned to
        case TableNumber(tid: Int, minBet: Double) =>
          debug(this + " received table assignment tid = " + tid)
          assign(tid)
          gui ! TableInfo(tid, minBet)

        case Win(gain) =>
          val won = betAmt * gain

          bankroll += won
          debug(this + " received WIN " + won + " new bankroll = " + bankroll)

          endGameCleanup("Win!")

        case Lose(gain) =>
          val lost = betAmt * gain

          bankroll += lost
          debug(this + " received LOSE " + lost + " new bankroll = " + bankroll)

          endGameCleanup("Lose!")

        case Push(gain) =>
          debug(this + " received PUSH bankroll = " + bankroll)

          endGameCleanup("Push!")

        case ObserveBet(name, betAmount) =>
          debug(this.name + " observed a bet of $" + betAmount + " from player " + name)

        // Receives an OK
        case Ok =>
          debug(this + " received Ok")
          gui ! WaitOnOthers

        // Receives a GameStart
        case GameStart => gui ! GameStart

        // Receives something completely from left field
        case dontKnow =>
          // Got something we REALLY didn't expect
          debug(this + " received unexpected: " + dontKnow)
      }
    }
  }

  def endGameCleanup(outcome: String) {
    clearHand
    debug(this + " hand cleared.")

    gui ! Gameover(bankroll, outcome)
  }
}