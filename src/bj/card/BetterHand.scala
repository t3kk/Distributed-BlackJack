package bj.card

/**
 * This trait implements a hand
 */
trait BetterHand extends Hand {

  /**
   * Clears the hand
   */
  def clearHand = cards = List[Card]()
}