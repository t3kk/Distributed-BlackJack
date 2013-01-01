package bj.actor

import scala.collection.mutable.HashMap
import scala.actors.Actor
import scala.actors.OutputChannel
import bj.hkeeping.NotOk
import bj.hkeeping.Ok
import bj.hkeeping.Reply
import bj.util.Logs

case class ObserveBet(name: String, betAmt: Int)

/**
 * This class implement table instances.
 * @param minBet Minimum bet for the table.
 */
class BetterTable(minBet: Double) extends Table(minBet) {

  /** Dealer for this table */
  dealer = new BetterDealer(minBet)

  /** Handles game over */
  override def gameOver(pays: HashMap[Int, Outcome]) = {
    pays.foreach(p => pay(p))

    // Starts a new game
    go
  }

  /** Handles game start */
  override def go {
    val bettors = players.foldLeft(List[OutputChannel[Any]]())((xs, x) => xs ::: List(x._2))

    if (bettors.size != 0) {

      debug(this + " dealing " + bettors.size + " bettors")

      val pidList = this.names.keys.toList

      //Let all the players observe the bets of the other players
      for (player <- bettors) {

        for (pid <- pidList) {
          player ! ObserveBet(names(pid), bets(pid).toInt)
        }
      }

      dealer ! GameStart(bettors)
    }
  }
}