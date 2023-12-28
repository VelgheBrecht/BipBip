package BipBip

import chisel3._
import chisel3.iotesters._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import com.sun.jna._  // For JNA functionality
import com.sun.jna.Library  // For defining the JNA interface
import java.nio.file.Paths
import treadle.executable.Big

trait BipBipLibrary extends Library {
  def BipBip(MK: Array[Long], encPtr: Long, decPtr: Array[Long]): Unit
}

object BipBipInterop {
  val instance: BipBipLibrary = Native.load("BipBip", classOf[BipBipLibrary]).asInstanceOf[BipBipLibrary]
}

class BipBipTester extends AnyFlatSpec with ChiselScalatestTester {
  val libPath = Paths.get("lib").toAbsolutePath.toString
  System.setProperty("jna.library.path", libPath)
  val rounds = 10

  "BipBip" should "match the C++ BipBip model" in {
    testBipBipModel()
  }

  def testBipBipModel(): Unit = {
    test(new BipBip()) { c =>
      for (i <- 0 until rounds) {
        val masterKeySize = 256
        // generate random master key
        val masterKey = Vector.fill(masterKeySize)(scala.util.Random.nextBoolean())
        val masterKeyBigInt = BigInt(masterKey.reverse.map(if(_) 1 else 0).mkString, 2)
        c.io.masterKey.poke(masterKeyBigInt.U)

        // generate random encPtr
        val encPtr = Math.abs(scala.util.Random.nextLong())
        c.io.encPtr.poke(encPtr.U)

        // Call Chisel BipBip function and get the output
        val decPtrBigInt = c.io.decPtr.peek().litValue

        // Prepare inputs for C++ functions
        val masterKeyArray = ArrayHelper.unflatten(masterKey.toArray, masterKeySize/64, 64).map(arr => BigInt(arr.reverse.map(if (_) 1 else 0).mkString, 2).toLong)
        val decPtr = Array.ofDim[Long](1)

        // Call C++ BipBip function
        BipBipInterop.instance.BipBip(masterKeyArray, encPtr, decPtr)

        println(s"BipBip test round $i: masterKey:$masterKeyBigInt encPtr:$encPtr decPtr:$decPtrBigInt expected:${BigInt(decPtr(0))}")

        Thread.sleep(4000)

        // Compare outputs
        assert(decPtrBigInt == BigInt(decPtr(0)))
      }
    }
  }
}
