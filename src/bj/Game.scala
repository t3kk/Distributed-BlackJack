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
package bj
import bj.actor.House
import bj.actor.Dealer
import scala.actors.Actor
import bj.actor.Go
import bj.util.Logs
import org.apache.log4j.Logger
import org.apache.log4j.Level
import org.apache.log4j.PropertyConfigurator
import bj.actor.BetterPlayer
import bj.actor.Player
import bj.gui.HumanGUI
import bj.actor.HumanPlayer
import scala.actors.Reactor
import bj.actor.GoWith

case class LaunchWith(pName: String, bankroll: Double, minBet: Double, sd: Int, ip: String, port: Int)
case class cPlay(plyr: HumanPlayer)
case class GUI(gui: HumanGUI)

object Game extends Actor with Logs {
  //  Logger.getLogger(getClass).setLevel(Level.DEBUG)

  PropertyConfigurator.configure("log4j.properties")
  var players = List[Player](new BetterPlayer("MrRoboto5", 100000000, 5, "127.0.0.1", 6000))
  val gui = new HumanGUI
  var seed = 1

  def main(args: Array[String]): Unit = {
    debug("Starting the House")
    House.start

    Thread.sleep(1000)

    //Starting
    gui.start
    this.start()

  }

  def act() {
    loop {
      react {
        // Received message from the GUI giving us the player values
        case LaunchWith(name, bankroll, minBet, sd, ip, port) =>
          this.seed = sd
          val humanPlayer = new HumanPlayer(name, bankroll, minBet, ip, port)
          humanPlayer ! GUI(gui)
          players = players ::: List(humanPlayer)
          /*//replacing the above with this breaks it....
           * 
           * humanPlayer.start
           * humanPlayer ! Go
           */

          gui ! cPlay(humanPlayer)

          this.startGame()

        // Don't know what we got, don't care what we got
        case dontKnow => debug("Game.scala cannot handle: " + dontKnow)
      }
    }
  }

  def startGame() {
    debug("Starting players")

    Player.start(players)

    Thread.sleep(1000)

    debug("Telling house go")
    House ! GoWith(this.seed)
  }
}