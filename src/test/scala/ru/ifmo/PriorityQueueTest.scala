package ru.ifmo

import org.scalatest.{Matchers, FlatSpec}
import scala.collection.mutable
import java.util.PriorityQueue

class PriorityQueueTest extends FlatSpec with Matchers {

  "Scala priority queue" should "return the most element" in {
    val queue = mutable.PriorityQueue.empty[Int]
    queue += 1
    queue += 2
    queue.dequeue() should equal(2)
  }

  "Java priority queue" should "return the most element" in {
    val queue: PriorityQueue[Int] = new PriorityQueue()
    queue.add(1)
    queue.add(2)
    queue.poll() should equal(1)
  }


}
