package me.nsmr
package collection
package immutable

import me.nsmr.util.java.{ FullyIndexableDictionary => JFID }

object FullyIndexableDictionary {
  def apply(values: Int*): FullyIndexableDictionary = new FullyIndexableDictionary(JFID.buildFrom(values:_*))
}

class FullyIndexableDictionary(jfid: JFID) extends Seq[Int] {

  override def length = jfid.size

  override def size = jfid.size

  override def iterator = (0 until jfid.size).iterator.map(jfid(_))

  override def apply(idx: Int): Int = jfid(idx)

  def rank(idx: Int): Int = jfid.rank(idx)

  def select(rank: Int): Int = jfid.select(rank)
}
