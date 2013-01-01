package bj.gui

import scala.swing.MainFrame
import scala.swing.GridBagPanel
import scala.swing.Label
import scala.swing.Swing
import scala.swing.Button
import scala.swing.GridPanel
import scala.swing.BoxPanel
import scala.swing.Orientation
import javax.swing.ImageIcon
import bj.card.Card
import bj.actor.Player
import javax.swing.UIManager
import scala.actors.remote.RemoteActor
import scala.swing.TextField
import scala.swing.BorderPanel
import java.awt.Dimension
import scala.swing.event.ButtonClicked
import scala.actors.Actor
import scala.actors.OutputChannel
import bj.util.Logs
import bj.actor.HumanPlayer
import bj.Game
import bj.LaunchWith
import bj.cPlay
import bj.actor.DoubleDown
import bj.actor.Hit
import bj.actor.Stay
import bj.actor.TableInfo
import scala.swing.Point
import bj.actor.WaitOnOthers
import bj.hkeeping.Broke
import bj.actor.Split
import bj.actor.GameStart
import bj.actor.Hole

//Cases that we want HumanGUI to react to
case class LoginDone(name: String, bankroll: Double, minBet: Double, ip: String, port: Int)
case class HitPressed
case class CardRecieved(card: Card, players: String)
case class UpRecieved(card: Card)
case class ObserveMessage
case class BetChange(change: Integer)
case class Gameover(newBankRoll: Double, outcome: String)

class HumanGUI extends MainFrame with Actor with Logs {

  //Prevent the resizing of this window
  this.resizable = false

  //Someone says this is magic to stop things from breaking
  RemoteActor.classLoader = getClass().getClassLoader()

  //Set look and feel to cross platform
  UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())

  // The HumanPlayer connected with this GUI
  var player: HumanPlayer = null

  //Set title bar
  title = "HumanGUI"

  val preferredX = 1000
  val preferredY = 600
  this.preferredSize = new Dimension(preferredX, preferredY)

  //Spawn GUI in the middle of the screen
  this.centerOnScreen()
  this.location_=(new Point(this.location.x - preferredX / 2, this.location.y - preferredY / 2))

  //Some key values
  val betIncreaseText = "+"
  val betDecreaseText = "-"
  val stayText = "Stay"
  val hitText = "Hit"
  val doubleDownText = "Double Down"
  val splitText = "Split"
  val surrenderText = "Surrender"

  //Make all the things we will want to modify as the game goes on here.
  val headerLabel = new Label("Let's play BlackJack!")
  val headerLabel2 = new Label(" ")
  val bankrollLabel = new Label("Bankroll: $FFF.FF") { border = Swing.EtchedBorder(Swing.Lowered) }
  val betLabel = new Label("Current Bet: FFF") { border = Swing.EtchedBorder(Swing.Lowered) }
  val playerName = new Label("player1")
  var tableIdValue = -1
  var tableIdLabel = new Label("Seated at table: " + tableIdValue)
  var tableMinBetValue = 5.0
  var tableMinBetLabel = new Label("Minimum bet amount: $" + tableMinBetValue)
  val bet = new Label("5")
  val magicTextFieldSize = new Dimension(200, 20)
  var players: List[Player] = null

  //Buttons and listeners
  val betIncrease = new Button { text = betIncreaseText }
  listenTo(betIncrease)
  val betDecrease = new Button { text = betDecreaseText }
  listenTo(betDecrease)
  val stay = new Button { text = stayText }
  listenTo(stay)
  val hit = new Button { text = hitText }
  listenTo(hit)
  val doubleDown = new Button { text = doubleDownText }
  listenTo(doubleDown)
  val split = new Button { text = splitText }
  listenTo(split)
  val surrender = new Button { text = surrenderText }
  listenTo(surrender)

  //Start the game with the play buttons disabled
  //Buttons enabled upon receiving an up card
  disablePlayButtons

  //Start the game with the bet buttons disabled
  //Buttons enabled after first hand
  disableBetButtons

  //Reactions go here.
  //My intent was to use the key values for the match, not sure how to make that happen.
  reactions += {
    case ButtonClicked(occurance) =>
      occurance.text match {
        case "+" =>
          debug("betIncrease clicked")

          // The maximum bet is 1000 but less than the bankroll
          if (this.bet.text.toInt < 1000 && this.bet.text.toInt <= this.player.bankroll) {
            this.bet.text = (this.bet.text.toInt + 1).toString()
            this.betDecrease.enabled = true
            player ! BetChange(1)
          } else {
            this.betIncrease.enabled = false
          }

        case "-" =>
          debug("betDecrease clicked")

          // Bet must be more than the minimum bet
          if (this.bet.text.toInt > this.tableMinBetValue) {
            this.bet.text = (this.bet.text.toInt - 1).toString()
            this.betIncrease.enabled = true
            player ! BetChange(-1)
          } else {
            this.betDecrease.enabled = false
          }

        case "Stay" =>
          debug("Stay button clicked")
          this.title_=("Waiting for other turns to complete...")
          this.headerLabel.text_=(" ")
          this.headerLabel2.text_=("Waiting on other players...")
          disablePlayButtons
          this.player ! Stay

        case "Hit" =>
          debug("Hit button clicked")
          this.player ! Hit

        case "Double Down" =>
          debug("Double down button clicked")
          this.title_=("Waiting for other turns to complete...")
          this.headerLabel.text_=(" ")
          this.headerLabel2.text_=("Waiting on other players...")
          disablePlayButtons
          this.player ! DoubleDown

        case "Split" =>
          debug("Split button clicked")
          this.player ! Split

        case _ =>
          debug("button " + occurance.text + "not implemented")
      }

    case dontKnow => debug(dontKnow.toString())
  }

  //Make table info panel
  val tableInfo = new BoxPanel(Orientation.Vertical) {
    //Make the server ID label and minimum bet label
    contents += tableIdLabel
    contents += tableMinBetLabel
    contents += new Label(" ")
    contents += new Label(" ")
  }

  val playerCards = new BoxPanel(Orientation.Horizontal) {}
  val playerHandValue = new Label("0")
  
  playerCards.preferredSize = new Dimension(350, 350)
  playerCards.maximumSize = new Dimension(350, 350)
  playerCards.maximumSize = new Dimension(350, 350)

  val playerPlayArea = new BoxPanel(Orientation.Vertical) {
    contents += new BoxPanel(Orientation.Vertical) {
      //contents += playerHandValue
      contents += playerCards
      contents += playerName
    }
  }

  playerPlayArea.preferredSize = new Dimension(350, 350)
  playerPlayArea.maximumSize = new Dimension(350, 350)
  playerPlayArea.maximumSize = new Dimension(350, 350)
  
  val dealerCards = new BoxPanel(Orientation.Horizontal) {}
  dealerCards.preferredSize = new Dimension(350, 350)
  dealerCards.maximumSize = new Dimension(350, 350)
  dealerCards.maximumSize = new Dimension(350, 350)

  val dealerPlayArea = new BoxPanel(Orientation.Vertical) {
    contents += new BoxPanel(Orientation.Vertical) {
      //contents += new Label("Value of hand")
      contents += dealerCards
      contents += new Label("Dealer")
    }
  }
  
  dealerPlayArea.preferredSize = new Dimension(350, 350)
  dealerPlayArea.maximumSize = new Dimension(350, 350)
  dealerPlayArea.maximumSize = new Dimension(350, 350)

  val botCards = new BoxPanel(Orientation.Horizontal) {}
  val botHandValue = new Label("0")
  
  botCards.preferredSize = new Dimension(350, 350)
  botCards.maximumSize = new Dimension(350, 350)
  botCards.maximumSize = new Dimension(350, 350)

  val botPlayArea = new BoxPanel(Orientation.Horizontal) {
    contents += new BoxPanel(Orientation.Vertical) {
      //contents += botHandValue
      contents += botCards
      contents += new Label("Bot player 1")
    }
  }
  
  botPlayArea.preferredSize = new Dimension(350, 350)
  botPlayArea.maximumSize = new Dimension(350, 350)
  botPlayArea.maximumSize = new Dimension(350, 350)

  //Make playerDash
  val playerDash = new BoxPanel(Orientation.Vertical) {

    contents += new Label(" ")
    contents += new Label(" ")
    contents += new Label(" ")
    
    //Show bet amount
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += new Label("Bet Amount:  $")
      contents += bet
      contents += new Label(" ")
      contents += betIncrease
      contents += betDecrease
    }

    contents += bankrollLabel

    contents += new BoxPanel(Orientation.Horizontal) {
      contents += stay
      contents += hit
      contents += doubleDown
      contents += split
      contents += surrender
    }

    contents += new Label(" ")
  }

  //Using a BoxPanel to hold everything - in the future I will break these into panes to make it more readable
  contents = new BoxPanel(Orientation.Vertical) {
    border = Swing.EmptyBorder(10, 10, 10, 10)

    contents += headerLabel
    contents += headerLabel2

    //This will hold the top row
    contents += new BoxPanel(Orientation.Horizontal) {
      //This is the login stuff
      contents += Swing.HGlue
      contents += tableInfo
    }
    contents += dealerPlayArea
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += Swing.HGlue
      contents += playerPlayArea
      contents += Swing.HGlue
      contents += botPlayArea
      contents += Swing.HGlue
    }
    contents += new BorderPanel() {
      add(playerDash, BorderPanel.Position.East)
    }

    contents += new Label(" ")
  }

  def act {
    val login = new HumanGUILogin(this)
    login.visible = true

    //This MAY become an issue now
    var humanPlayer = new HumanPlayer("bad", 0, 0, "0", 0)

    loop {
      react {
        //We will have a list with a player name, minimum bet and a bankroll sent from HumanGUILogin
        case LoginDone(name, bankroll, minBet, ip, port) =>
          debug("Login Done - Name: " + name + ", Bankroll: " + bankroll + ", Min bet: " + minBet)
          //hook the gui and human player together and kick things off
          //where to put human player is another question
          humanPlayer = new HumanPlayer(name, bankroll, minBet, ip, port)
          this.bankrollLabel.text_=("Bankroll: $" + bankroll.toInt)
          this.bet.text_=(minBet.toInt.toString)
          this.playerName.text_=(name)
          this.visible = (true)
          login.visible_=(false)

        case CardRecieved(card, whose) =>

          if (whose.toLowerCase().equals("player")) {
            playerCards.contents += card.toLabel
            playerHandValue.text_=((player.value).toString)
          } else if (whose.toLowerCase().equals("dealer")) {
            dealerCards.contents += card.toLabel
          } else if (whose.toLowerCase().equals("bot")) {
            botCards.contents += card.toLabel
          }

          disableBetButtons
          this.visible = true

        case UpRecieved(card) =>
          dealerCards.contents += card.back
          dealerCards.contents += card.toLabel
          this.title_=(playerName.text + "'s turn!")
          this.headerLabel.text_=("It is YOUR turn!")
          this.headerLabel2.text_=(" ")

          enablePlayButtons
          disableBetButtons
          this.visible = true

        case cPlay(plyr) =>
          debug("Received player: " + plyr)
          this.player = plyr

        case GameStart =>
          this.playerCards.contents.clear()
          this.dealerCards.contents.clear()
          this.botCards.contents.clear()

        case Gameover(newBankroll, outcome) =>
          this.bankrollLabel.text_=("Bankroll: $" + newBankroll.toInt.toString())
          this.visible = true
          this.title_=("Game over!")
          this.headerLabel.text_=("Game over! Outcome: " + outcome)
          this.headerLabel2.text_=("Starting next game... Place your bets now!")
          this.dealerCards.contents.remove(0)
          enableBetButtons

        case TableInfo(tableId, tableMinBet) =>
          debug("Received tableId: " + tableId + ", tableMinBet: " + tableMinBet)

          this.tableIdValue = tableId
          this.tableMinBetValue = tableMinBet
          tableIdLabel = new Label("Seated at table: " + tableIdValue)
          tableMinBetLabel = new Label("Minimum bet amount: $" + tableMinBetValue.toInt)

          this.tableInfo.contents.clear()
          this.tableInfo.contents += tableIdLabel
          this.tableInfo.contents += tableMinBetLabel
          this.tableInfo.contents += new Label(" ")
          this.tableInfo.contents += new Label(" ")
          this.visible = true

        case WaitOnOthers() =>
          this.title_=("Waiting for other turns to complete...")
          this.headerLabel.text_=(" ")
          this.headerLabel2.text_=("Waiting on other players...")
          this.visible = true

        case Broke =>
          this.title_=("Waiting for other turns to complete...")
          this.headerLabel.text_=("You broke!")
          this.headerLabel2.text_=("Waiting on other players...")
          disablePlayButtons
          this.visible = true

        case dontKnow =>
          // Got something we REALLY didn't expect
          debug(this + " got " + dontKnow)
      }
    }
  }

  /**
   * Disable play buttons between games
   */
  def disablePlayButtons() = {
    this.stay.enabled = false
    this.hit.enabled = false
    this.doubleDown.enabled = false
    this.split.enabled = false
    this.surrender.enabled = false
  }

  /**
   * Enable play buttons during gameplay
   */
  def enablePlayButtons() = {
    this.stay.enabled = true
    this.hit.enabled = true
    this.doubleDown.enabled = true
    //TODO
    //this.split.enabled = true
    //this.split.enabled = true
  }

  /**
   * Disable the bet buttons during gameplay
   */
  def disableBetButtons() = {
    this.betIncrease.enabled = false
    this.betDecrease.enabled = false
  }

  /**
   * Enable bet buttons between games if appropriate
   */
  def enableBetButtons() = {
    if (this.bet.text.toInt <= this.tableMinBetValue) this.betDecrease.enabled = false
    else this.betDecrease.enabled = true

    if (this.bet.text.toInt > 999 || this.bet.text.toInt > this.player.bankroll) this.betIncrease.enabled = false
    else this.betIncrease.enabled = true
  }
}

class HumanGUILogin(mainGUI: HumanGUI) extends MainFrame with Logs {

  //Set the title of the login GUI
  this.title = "Login"

  //Prevent the user from resizing the window
  this.resizable = false

  val preferredX = 250
  val preferredY = 250
  this.preferredSize = new Dimension(preferredX, preferredY)

  //Center the login GUI on the screen
  this.centerOnScreen()
  this.location_=(new Point(this.location.x - preferredX / 2, this.location.y - preferredY / 2))

  val magicTextFieldSize = new Dimension(200, 20)

  object pName extends TextField { text = "Ron"; size = magicTextFieldSize }
  object bRoll extends TextField { text = "1000"; size = magicTextFieldSize }
  object minBet extends TextField { text = "5"; size = magicTextFieldSize }
  object rSeed extends TextField { text = "1"; size = magicTextFieldSize }
  object serverIP extends TextField { text = "127.0.0.1"; size = magicTextFieldSize }
  object serverPort extends TextField { text = "1"; size = magicTextFieldSize }

  val pNameLabel = new Label("Player Name: ")
  val bRLabel = new Label("Bankroll: ")
  val minBetLabel = new Label("Minimum Bet: ")
  val seedLabel = new Label("Random seed: ")
  val ipLabel = new Label("IP address: ")
  val portLabel = new Label("Port: ")

  val welcome = new Label("Welcome! Log-in the play!           ")
  val warn = new Label() { text = " " }
  val filler = new Label(" ")
  val login = new Button { text = "Login" }

  var namePanel = new BoxPanel(Orientation.Horizontal) {
    contents += pNameLabel
    contents += Swing.HStrut(9)
    contents += pName
  }

  var bankPanel = new BoxPanel(Orientation.Horizontal) {
    contents += bRLabel
    contents += Swing.HStrut(34)
    contents += bRoll
  }

  var betPanel = new BoxPanel(Orientation.Horizontal) {
    contents += minBetLabel
    contents += Swing.HStrut(7)
    contents += minBet
  }

  var seedPanel = new BoxPanel(Orientation.Horizontal) {
    contents += seedLabel
    contents += Swing.HStrut(3)
    contents += rSeed
  }

  var ipPanel = new BoxPanel(Orientation.Horizontal) {
    contents += ipLabel
    contents += Swing.HStrut(20)
    contents += serverIP
  }

  var portPanel = new BoxPanel(Orientation.Horizontal) {
    contents += portLabel
    contents += Swing.HStrut(57)
    contents += serverPort
  }

  contents = new BoxPanel(Orientation.Vertical) {
    border = Swing.EmptyBorder(10, 10, 10, 10)

    contents += welcome
    contents += filler
    contents += warn
    contents += namePanel
    contents += bankPanel
    contents += betPanel
    contents += seedPanel
    contents += ipPanel
    contents += portPanel
    contents += filler
    contents += login
  }

  listenTo(login)

  reactions += {
    case ButtonClicked(login) =>
      debug("login clicked")

      // Update the labels so that if the user previously entered invalid data that is now valid,
      // the asterisk (*) will not appear next to the label anymore.
      resetLabels

      var bankRoll = 0
      var minimumBet = 0
      var name = pName.text
      var seed = 1
      var ip = "127.0.0.1"
      var port = 0
      var noError = true

      // Validate the value entered in the bankroll field
      try {
        bankRoll = bRoll.text.toInt
      } catch {
        case ex: NumberFormatException => {
          warn.text_=("Please check inputs")
          bRLabel.text_=("Bankroll:*")
          updateBankRollPanel(true)
          noError = false
        }
      }

      // Validate the value in the bet field
      try {
        minimumBet = minBet.text.toInt
      } catch {
        case ex: NumberFormatException => {
          warn.text_=("Please check inputs")
          minBetLabel.text_=("Minimum Bet:*")
          updateBetPanel(true)
          noError = false
        }
      }

      // Validate the value in the random seed field
      try {
        seed = rSeed.text.toInt
      } catch {
        case ex: NumberFormatException => {
          warn.text_=("Please check inputs")
          seedLabel.text_=("Random seed:*")
          updateSeedPanel(true)
          noError = false
        }
      }

      // Validate the value in the ip address field
      try {
        ip = serverIP.text
        val octets = ip.split('.')

        if (octets.length != 4) {
          noError = false
          warn.text_=("Please check inputs")
          ipLabel.text_=("IP address:*")
          updateIpAddressPanel(true)
          debug("IP address of " + ip + " is not a valid ip address. Octets = " + octets.length)
        } else {
          val oct1 = octets(0).toInt
          val oct2 = octets(1).toInt
          val oct3 = octets(2).toInt
          val oct4 = octets(3).toInt

          // Make sure octets are in the range of 0 to 255 inclusive.
          if ((oct1 > 255 || oct1 < 0) || (oct2 > 255 || oct2 < 0) || (oct3 > 255 || oct3 < 0) || (oct4 > 255 || oct4 < 0)) {
            noError = false
            warn.text_=("Please check inputs")
            ipLabel.text_=("IP address:*")
            updateIpAddressPanel(true)
            debug("IP address field not a valid ip address")
          }
        }
      } catch {
        case ex: Exception => {
          debug("Caugh exception " + ex + " on server ip field")
          warn.text_=("Please check inputs")
          ipLabel.text_=("IP address:*")
          updateIpAddressPanel(true)
          noError = false
        }
      }

      // Validate the value in the port field
      try {
        port = serverPort.text.toInt
      } catch {
        case ex: NumberFormatException => {
          warn.text_=("Please check inputs")
          portLabel.text_=("Port:*")
          updatePortPanel(true)
          noError = false
        }
      }

      //if minbet and bankroll are big enough then open the main GUI
      if (minimumBet >= 5 && bankRoll > minimumBet && noError) {
        debug("Login values seem ok")
        mainGUI ! LoginDone(name, bankRoll, minimumBet, ip, port)
        Game ! LaunchWith(name, bankRoll, minimumBet, seed, ip, port)

      } else {
        debug("Login values invalid")
      }

    case dontKnow => debug(dontKnow.toString())
  }

  def updateBankRollPanel(invalid: Boolean) = {
    bankPanel.contents.clear()
    bankPanel.contents += bRLabel
    if (invalid) bankPanel.contents += Swing.HStrut(32)
    else bankPanel.contents += Swing.HStrut(34)
    bankPanel.contents += bRoll
  }

  def updateBetPanel(invalid: Boolean) = {
    betPanel.contents.clear()
    betPanel.contents += minBetLabel
    if (invalid) betPanel.contents += Swing.HStrut(5)
    else betPanel.contents += Swing.HStrut(7)
    betPanel.contents += minBet
  }

  def updateSeedPanel(invalid: Boolean) = {
    seedPanel.contents.clear()
    seedPanel.contents += seedLabel
    if (invalid) seedPanel.contents += Swing.HStrut(1)
    else seedPanel.contents += Swing.HStrut(3)
    seedPanel.contents += rSeed
  }

  def updateIpAddressPanel(invalid: Boolean) = {
    ipPanel.contents.clear()
    ipPanel.contents += ipLabel
    if (invalid) ipPanel.contents += Swing.HStrut(18)
    else ipPanel.contents += Swing.HStrut(20)
    ipPanel.contents += serverIP
  }

  def updatePortPanel(invalid: Boolean) = {
    portPanel.contents.clear()
    portPanel.contents += portLabel
    if (invalid) portPanel.contents += Swing.HStrut(55)
    else portPanel.contents += Swing.HStrut(57)
    portPanel.contents += serverPort
  }

  def resetLabels() = {
    pNameLabel.text_=("Player Name: ")
    bRLabel.text_=("Bankroll: ")
    updateBankRollPanel(false)
    minBetLabel.text_=("Minimum Bet: ")
    updateBetPanel(false)
    seedLabel.text_=("Random seed: ")
    updateSeedPanel(false)
    ipLabel.text_=("IP address: ")
    updateIpAddressPanel(false)
    portLabel.text_=("Port: ")
    updatePortPanel(false)
  }

}

object HumanGUIMain {
  def main(args: Array[String]) {

  }
}