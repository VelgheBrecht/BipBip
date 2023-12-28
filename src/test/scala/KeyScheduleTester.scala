package BipBip

import chisel3._
import chisel3.iotesters._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import com.sun.jna._  // For JNA functionality
import com.sun.jna.Library  // For defining the JNA interface
import java.nio.file.Paths

trait KeyScheduleLibrary extends Library {
  def KeySc(MK: Array[Long], trk: Array[Boolean])
}

object KeyScheduleInterop {
  val instance: KeyScheduleLibrary = Native.load("BipBip", classOf[KeyScheduleLibrary]).asInstanceOf[KeyScheduleLibrary]
}

class KeyScheduleTester extends AnyFlatSpec with ChiselScalatestTester {
  val libPath = Paths.get("lib").toAbsolutePath.toString
  System.setProperty("jna.library.path", libPath)
  val rounds = 10

  "KeySc" should "match the C++ KeySc model" in {
    testKeyScModel()
  }

  def testKeyScModel(masterKeySize: Int = 256, amountDataRoundKeys: Int = 1, amountTweakRoundKeys: Int = 6,
            extendedTweakSize: Int = 53, blockSize: Int = 24): Unit = {
    test(new KeySc(masterKeySize, amountDataRoundKeys, amountTweakRoundKeys, extendedTweakSize, blockSize)) { c =>
      for (i <- 0 until rounds) {
        // generate random master key
        val masterKey = Vector.fill(masterKeySize)(scala.util.Random.nextBoolean())
        val masterKeyBigInt = BigInt(masterKey.reverse.map(if(_) 1 else 0).mkString, 2)
        c.io.masterKey.poke(masterKeyBigInt.U)

        // Call Chisel KeySc function and get the output
        val tweakRoundKeysBigInt = c.io.tweakRoundKeys.peek().map(_.litValue)

        // Prepare inputs for C++ functions
        val masterKeyArray = ArrayHelper.unflatten(masterKey.toArray, masterKeySize/64, 64).map(arr => BigInt(arr.reverse.map(if (_) 1 else 0).mkString, 2).toLong)
        val tweakRoundKeysArray = ArrayHelper.flatten(Array.ofDim[Boolean](amountDataRoundKeys+amountTweakRoundKeys, extendedTweakSize))

        // Call C++ KeySc function
        KeyScheduleInterop.instance.KeySc(masterKeyArray, tweakRoundKeysArray)

        // Retrieve expected outputs
        val unflattenedTweakRoundKeysArray = ArrayHelper.unflatten(tweakRoundKeysArray, amountTweakRoundKeys, extendedTweakSize)
        val expectedTweakRoundKeys = unflattenedTweakRoundKeysArray.map(arr => BigInt(arr.reverse.map(if (_) 1 else 0).mkString, 2))

        println(s"KeySc test round $i: masterKey:$masterKeyBigInt tweakRoundKeys:${tweakRoundKeysBigInt.mkString(",")} expected:${expectedTweakRoundKeys.mkString(",")}")

        // wait for everything to print
        Thread.sleep(4000)

        // Compare outputs
        assert(tweakRoundKeysBigInt sameElements expectedTweakRoundKeys)
      }
    }
  }
}
