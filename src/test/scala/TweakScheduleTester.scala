package BipBip

import chisel3._
import chisel3.iotesters._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import com.sun.jna._  // For JNA functionality
import com.sun.jna.Library  // For defining the JNA interface
import java.nio.file.Paths

trait TweakScheduleLibrary extends Library {
  def CHI(x: Array[Boolean]): Unit
  def LML2(x: Array[Boolean]): Unit
  def LML3(x: Array[Boolean]): Unit
  def RGC(x: Array[Boolean]): Unit
  def RGP(x: Array[Boolean]): Unit
  def TwkIn(T: Long, t: Array[Boolean]): Unit
  def TwkSc(t: Array[Boolean], trk: Array[Boolean], drk: Array[Boolean]): Unit
}

object TweakScheduleInterop {
  val instance: TweakScheduleLibrary = Native.load("BipBip", classOf[TweakScheduleLibrary]).asInstanceOf[TweakScheduleLibrary]
}

object ArrayHelper {
  def flatten(arr: Array[Array[Boolean]]): Array[Boolean] = {
    arr.flatten
  }

  def unflatten(flatArr: Array[Boolean], rows: Int, cols: Int): Array[Array[Boolean]] = {
    flatArr.grouped(cols).toArray
  }
}

class TweakScheduleTester extends AnyFlatSpec with ChiselScalatestTester {
  val libPath = Paths.get("lib").toAbsolutePath.toString
  System.setProperty("jna.library.path", libPath)
  val rounds = 10

  "TwkIn" should "match the C++ TwkIn model" in {
    testTwkIn()
  }

  "CHI" should "match the C++ CHI model" in {
    testCHIModel()
  }

  "LML2" should "match the C++ LML2 model" in {
    testLML2Model()
  }

  "LML3" should "match the C++ LML3 model" in {
    testLML3Model()
  }

  "RGC" should "match the C++ RGC model" in {
    testRGCModel()
  }

  "RGP" should "match the C++ RGP model" in {
    testRGPModel()
  }

  "TwkSc" should "match the C++ TwkSc model" in {
    testTwkScModel()
  }

  def testTwkIn(tweakSize: Int = 40, extendedTweakSize: Int = 53): Unit = {
    test(new TwkIn(tweakSize, extendedTweakSize)) { c =>
      for (i <- 0 until rounds) {
        // generate random tweak
        val tweak = Vector.fill(tweakSize)(scala.util.Random.nextBoolean())
        val tweakBigInt = BigInt(tweak.reverse.map(if(_) 1 else 0).mkString, 2)
        c.io.tweak.poke(tweakBigInt.U)

        // Call Chisel TwkIn function and get the output
        val chiselOutputBigInt = c.io.extendedTweak.peek().litValue

        // Call C++ TwkIn function
        val cppOutputArray = Array.ofDim[Boolean](extendedTweakSize)
        TweakScheduleInterop.instance.TwkIn(tweakBigInt.toLong, cppOutputArray)
        val expectedOutputBigInt = BigInt(cppOutputArray.reverse.map(if(_) 1 else 0).mkString, 2)

        println(s"TwkIn test round $i: input:$tweakBigInt output:$chiselOutputBigInt expected:$expectedOutputBigInt")
        
        // Compare outputs
        assert(chiselOutputBigInt == expectedOutputBigInt)
      }
    }
  }

  def testCHIModel(blockSize: Int = 53): Unit = {
    test(new CHI(blockSize)) { c =>
      for (i <- 0 until rounds) {
        val inputVector = Vector.fill(blockSize)(scala.util.Random.nextBoolean())
        val inputBigInt = BigInt(inputVector.reverse.map(if(_) 1 else 0).mkString, 2)
        c.io.input.poke(inputBigInt.U)

        // Call Chisel CHI function and get the output
        val chiselOutputBigInt = c.io.output.peek().litValue

        // Call C++ CHI function
        val cppInputArray = inputVector.toArray
        TweakScheduleInterop.instance.CHI(cppInputArray)
        val expectedOutputBigInt = BigInt(cppInputArray.reverse.map(if(_) 1 else 0).mkString, 2)

        println(s"CHI test round $i: input:$inputBigInt output:$chiselOutputBigInt expected:$expectedOutputBigInt")
        
        // Compare outputs
        assert(chiselOutputBigInt == expectedOutputBigInt)
      }
    }
  }

  def testLML2Model(blockSize: Int = 53): Unit = {
    test(new RGC(blockSize) {
      io.output := LML(io.input)
    }) { c =>
      for (i <- 0 until rounds) {
        val inputVector = Vector.fill(blockSize)(scala.util.Random.nextBoolean())
        val inputBigInt = BigInt(inputVector.reverse.map(if(_) 1 else 0).mkString, 2)
        c.io.input.poke(inputBigInt.U)

        // Call Chisel LML2 function and get the output
        val chiselOutputBigInt = c.io.output.peek().litValue

        // Call C++ LML2 function
        val cppInputArray = inputVector.toArray
        TweakScheduleInterop.instance.LML2(cppInputArray)
        val expectedOutputBigInt = BigInt(cppInputArray.reverse.map(if(_) 1 else 0).mkString, 2)

        println(s"LML2 test round $i: input:$inputBigInt output:$chiselOutputBigInt expected:$expectedOutputBigInt")
        
        // Compare outputs
        assert(chiselOutputBigInt == expectedOutputBigInt)
      }
    }
  }

  def testLML3Model(blockSize: Int = 53): Unit = {
    test(new RGP(blockSize) {
      io.output := LML(io.input)
    }) { c =>
      for (i <- 0 until rounds) {
        val inputVector = Vector.fill(blockSize)(scala.util.Random.nextBoolean())
        val inputBigInt = BigInt(inputVector.reverse.map(if(_) 1 else 0).mkString, 2)
        c.io.input.poke(inputBigInt.U)

        // Call Chisel LML3 function and get the output
        val chiselOutputBigInt = c.io.output.peek().litValue

        // Call C++ LML3 function
        val cppInputArray = inputVector.toArray
        TweakScheduleInterop.instance.LML3(cppInputArray)
        val expectedOutputBigInt = BigInt(cppInputArray.reverse.map(if(_) 1 else 0).mkString, 2)

        println(s"LML3 test round $i: input:$inputBigInt output:$chiselOutputBigInt expected:$expectedOutputBigInt")
        
        // Compare outputs
        assert(chiselOutputBigInt == expectedOutputBigInt)
      }
    }
  }

  def testRGCModel(blockSize: Int = 53): Unit = {
    test(new RGC(blockSize)) { c =>
      for (i <- 0 until rounds) {
        val inputVector = Vector.fill(blockSize)(scala.util.Random.nextBoolean())
        val inputBigInt = BigInt(inputVector.reverse.map(if(_) 1 else 0).mkString, 2)
        c.io.input.poke(inputBigInt.U)

        // Call Chisel RGC function and get the output
        val chiselOutputBigInt = c.io.output.peek().litValue

        // Call C++ RGC function
        val cppInputArray = inputVector.toArray
        TweakScheduleInterop.instance.RGC(cppInputArray)
        val expectedOutputBigInt = BigInt(cppInputArray.reverse.map(if(_) 1 else 0).mkString, 2)

        println(s"RGC test round $i: input:$inputBigInt output:$chiselOutputBigInt expected:$expectedOutputBigInt")
        
        // Compare outputs
        assert(chiselOutputBigInt == expectedOutputBigInt)
      }
    }
  }

  def testRGPModel(blockSize: Int = 53): Unit = {
    test(new RGP(blockSize)) { c =>
      for (i <- 0 until rounds) {
        val inputVector = Vector.fill(blockSize)(scala.util.Random.nextBoolean())
        val inputBigInt = BigInt(inputVector.reverse.map(if(_) 1 else 0).mkString, 2)
        c.io.input.poke(inputBigInt.U)

        // Call Chisel RGP function and get the output
        val chiselOutputBigInt = c.io.output.peek().litValue

        // Call C++ RGP function
        val cppInputArray = inputVector.toArray
        TweakScheduleInterop.instance.RGP(cppInputArray)
        val expectedOutputBigInt = BigInt(cppInputArray.reverse.map(if(_) 1 else 0).mkString, 2)

        println(s"RGP test round $i: input:$inputBigInt output:$chiselOutputBigInt expected:$expectedOutputBigInt")
        
        // Compare outputs
        assert(chiselOutputBigInt == expectedOutputBigInt)
      }
    }
  }

  def testTwkScModel(extendedTweakSize: Int = 53, amountTweakRoundKeys: Int = 7, 
            amountDataRoundKeys: Int = 12, blockSize: Int = 24): Unit = {
    test(new TwkSc(extendedTweakSize, amountTweakRoundKeys, amountDataRoundKeys, blockSize)) { c =>
      for (i <- 0 until rounds) {
        // generate random tweak
        val extendedTweak = Vector.fill(extendedTweakSize)(scala.util.Random.nextBoolean())
        val extendedTweakBigInt = BigInt(extendedTweak.reverse.map(if(_) 1 else 0).mkString, 2)
        c.io.extendedTweak.poke(extendedTweakBigInt.U)
        
        // generate random tweak round keys
        val tweakRoundKeys = Vector.fill(amountTweakRoundKeys)(Vector.fill(extendedTweakSize)(scala.util.Random.nextBoolean()))
        val tweakRoundKeysBigInt = tweakRoundKeys.map(x => BigInt(x.reverse.map(if(_) 1 else 0).mkString, 2))
        for ((value, index) <- tweakRoundKeysBigInt.zipWithIndex) {
          c.io.tweakRoundKeys(index).poke(value.U)
        }

        // Call Chisel TwkSc function and get the output
        val dataRoundKeysBigInt = c.io.dataRoundKeys.peek().map(_.litValue)

        // Prepare inputs for C++ functions
        val extendedTweakArray = extendedTweak.toArray
        val tweakRoundKeysArray = ArrayHelper.flatten(tweakRoundKeys.map(_.toArray).toArray)
        val dataRoundKeysArray = ArrayHelper.flatten(Array.ofDim[Boolean](amountDataRoundKeys, blockSize))

        // Call C++ TwkSc function
        TweakScheduleInterop.instance.TwkSc(extendedTweakArray, tweakRoundKeysArray, dataRoundKeysArray)

        // Retrieve expected outputs
        val unflattenedDataRoundKeysArray = ArrayHelper.unflatten(dataRoundKeysArray, amountDataRoundKeys, blockSize)
        val expectedDataRoundKeys = unflattenedDataRoundKeysArray.map(arr => BigInt(arr.reverse.map(if (_) 1 else 0).mkString, 2))

        println(s"TwkSc test round $i: tweak:$extendedTweakBigInt tweakRoundKeys:${tweakRoundKeysBigInt.mkString(",")} dataRoundKeys:${dataRoundKeysBigInt.mkString(",")} expected:${expectedDataRoundKeys.mkString(",")}")

        // Sleep for 5 second to allow for printing
        Thread.sleep(5000)

        // Compare outputs
        assert(dataRoundKeysBigInt sameElements expectedDataRoundKeys)
      }
    }
  }
}
