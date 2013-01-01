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

import scala.collection.mutable.HashMap
import scala.actors.Actor
import scala.actors.OutputChannel
import bj.hkeeping.NotOk
import bj.hkeeping.Ok
import bj.hkeeping.Reply
import bj.util.Logs

import scala.actors.remote.RemoteActor._
import scala.actors.remote.RemoteActor
import scala.actors.remote.Node
import scala.actors.remote.DelegateActor
import scala.actors.OutputChannel

/** This class implements the table static members */
object Table {
  val MIN_PLAYERS: Int = 1
  val MAX_PLAYERS: Int = 6

  var id: Int = -1

}

/**
 * This class implement table instances.
 * @param minBet Minimum bet for the table.
 */
class Table(val minBet: Double) extends Actor with Logs {
  /** Table's id */
  Table.id += 1
  val tid = Table.id

  /** Dealer for this table */
  var dealer = new Dealer

  /** True if this table is involved in a game */
  var trucking: Boolean = false

  /** Bet amounts by player id */
  var bets = HashMap[Int, Double]()

  /** Mailboxes by player id */
  var players = HashMap[Int, OutputChannel[Any]]()

  /** House mail box */
  var house: OutputChannel[Any] = null

  /** Map of player id to name */
  var names = HashMap[Int, String]()

  //Use this port for the house
  val PORT = 6000

  //Magic from remote actorchat
  RemoteActor.classLoader = getClass().getClassLoader()

  /** Starts the table */
  start

  /** Gives a string version of the table */
  override def toString: String = "table(" + tid + ", " + minBet + ")"

  /** This method receives messages */
  def act {

    //Use this port for the house
    alive(PORT)

    //Make a channel thing - WARNING ! USING "this" instead of self
    register(Symbol("table" + tid), this)

    loop {
      react {
        // Receives arrival of a player: mailbox is the player's
        case Arrive(mailbox: OutputChannel[Any], pid: Int, betAmt: Double) =>
          debug(this + " received ARRIVE from " + mailbox + " amt = " + betAmt)

          arrive(mailbox, pid, betAmt)

        case ArriveWithName(mailbox: OutputChannel[Any], pid: Int, betAmt: Double, name: String) =>

          arrive(mailbox, pid, betAmt, name)

        // Receive game over signal from the dealer
        case GameOver(pays: HashMap[Int, Outcome]) =>
          debug(this + " received game over for " + pays.size + " players")

          gameOver(pays)

        // Receives game start signal from the house
        case Go =>
          debug(this + " received Go for " + players.size + " players")

          go

        case GoWith(sd) =>
          debug(this + " received Go for " + players.size + " players with seed: " + sd)
          dealer ! Seed(sd)

          go
      }
    }
  }

  /**
   * Processes a player arrival
   * @param source Player's mailbox
   * @param pid Player's id
   * @param betAmt Player's bet amount
   */
  def arrive(source: OutputChannel[Any], pid: Int, betAmt: Double) {
    val reply = placed(source, Bet(pid, betAmt))

    debug(this + " bet = " + reply)

    source ! reply
  }

  def arrive(source: OutputChannel[Any], pid: Int, betAmt: Double, name: String) {
    val reply = placed(source, Bet(pid, betAmt))

    debug(this + " bet = " + reply)

    addPlayerName(pid, name)

    source ! reply
  }

  /** Handles game start */
  def go {
    val bettors = players.foldLeft(List[OutputChannel[Any]]())((xs, x) => xs ::: List(x._2))

    if (bettors.size != 0) {

      debug(this + " dealing " + bettors.size + " bettors")

      dealer ! GameStart(bettors)
    }
  }

  /**
   * Places the bet after validation.
   * @param mailbox Player's mailbox
   * @param bet Bet parameters
   */
  def placed(mailbox: OutputChannel[Any], bet: Bet): Reply = {
    debug("table: placing bet amt = " + bet.amt + " num bets = " + bets.size)
    if (bet.amt <= 0 || bets.size >= Table.MAX_PLAYERS)
      return NotOk

    players.get(bet.player) match {
      case None =>
        debug("table: adding new player id = " + bet.player)
        players += bet.player -> mailbox

        bets += bet.player -> bet.amt

      case Some(player) =>
        bets.get(bet.player) match {
          case Some(oldAmt) =>
            debug("table: updating bet for player id = " + bet.player)
            bets(bet.player) = (oldAmt + bet.amt)

          case None =>
            debug("table: got bad bet")

            return NotOk
        }
    }

    Ok
  }

  /** Handles game over */
  def gameOver(pays: HashMap[Int, Outcome]) = pays.foreach(p => pay(p))

  /**
   * Sends payment to player.
   * @param pid Player id
   * @parm outcome Game outcome for player pid
   */
  def pay(figure: (Int, Outcome)): Unit = {
    val (pid, outcome) = figure

    outcome match {
      case Win(gain) =>
        debug("player(" + pid + ") won " + gain)
        players(pid) ! outcome

      case Lose(gain) =>
        debug("player(" + pid + ") lost " + gain)
        players(pid) ! outcome

      case Push(gain) =>
        debug("player(" + pid + ") push " + gain)
        players(pid) ! outcome
    }
  }

  def removePlayerName(pid: Int, name: String) = {
    this.names.-=(pid)
  }

  def addPlayerName(pid: Int, name: String) = {
    this.names.+=((pid, name))
  }

  def updateBet(pid: Int, name: String) = {
    this.names.+=((pid, name))
  }

  /** Clears all the bets -- NOT USED */
  def clear: Unit = bets.clear
}