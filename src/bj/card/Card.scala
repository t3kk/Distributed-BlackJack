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

package bj.card

import scala.swing.Label
import javax.swing.ImageIcon

/** This object implements static members of card. */
object Card {
  // Note: these codes are not necessarily card values
  val ACE = 1
  val TEN = 10
  val JACK = 11
  val QUEEN = 12
  val KING = 13

  // Note: these code are not card values
  val SPADE = 1
  val CLUB = 2
  val DIAMOND = 3
  val HEART = 4
}

/** This class implement a card instance */
case class Card(number: Int, suite: Int) {
  /** Returns true if this card is an ace */
  def ace = number == 1

  /** Returns true if this card has value of a 10 */
  def ten = number >= 10

  /** Returns the "flat" value of the card */
  def value = flat(number)

  /** Flattens the value of card */
  def flat(value: Int) = if (value <= 10) value else 10

  def back: Label = {
    new Label { icon = new ImageIcon("card-images/cards/back.png") }
  }

  /**
   * Returns the image of card in a label
   * This should be a singleton eventually,
   * with one image per suit and number.
   * As is, I think it makes a new label
   * every time.  It SHOULD hit the GC.
   */
  def toLabel: Label = {
    var s: String = ""
    //Find the string for the suit
    suite match {
      case Card.SPADE =>
        s += "S"

      case Card.CLUB =>
        s += "C"

      case Card.HEART =>
        s += "H"

      case Card.DIAMOND =>
        s += "D"

      case _ =>
        s += "!!!"
    }
    //Find the string for the card
    number match {
      case n: Int if n >= 2 && n <= 10 =>
        s += n + ""

      case Card.JACK =>
        s += "J"

      case Card.QUEEN =>
        s += "Q"

      case Card.KING =>
        s += "K"

      case Card.ACE =>
        s += "A"

      case _ =>
        s += "???"
    }
    new Label { icon = new ImageIcon("card-images/cards/" + s + ".png") }
  }

  /** Returns the pretty value of the card */
  override def toString: String = {
    var s: String = ""

    number match {
      case n: Int if n >= 2 && n <= 10 =>
        s = n + ""

      case Card.JACK =>
        s = "Jack"

      case Card.QUEEN =>
        s = "Queen"

      case Card.KING =>
        s = "King"

      case Card.ACE =>
        s = "Ace"

      case _ =>
        s = "???"
    }

    s += " of "

    suite match {
      case Card.SPADE =>
        s += "Spades"

      case Card.CLUB =>
        s += "Clubs"

      case Card.HEART =>
        s += "Hearts"

      case Card.DIAMOND =>
        s += "Diamonds"

      case _ =>
        s += "???"
    }
    s
  }
}
