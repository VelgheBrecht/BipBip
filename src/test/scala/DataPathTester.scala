package BipBip

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import com.sun.jna._  // For JNA functionality
import com.sun.jna.Library  // For defining the JNA interface
import java.nio.file.Paths

trait DataPathLibrary extends Library {
  def LML1(x: Array[Boolean]): Unit
  def RFC(x: Array[Boolean]): Unit
  def RFS(x: Array[Boolean]): Unit
  def BipBipDec(x: Array[Boolean], drk: Array[Boolean]): Unit
}

object DataPathInterop {
  val instance: DataPathLibrary = Native.load("BipBip", classOf[DataPathLibrary]).asInstanceOf[DataPathLibrary]
}

class DataPathTester extends AnyFlatSpec with ChiselScalatestTester {
  val libPath = Paths.get("lib").toAbsolutePath.toString
  System.setProperty("jna.library.path", libPath)
  val rounds = 10

  "LML1" should "match the C++ LML1 model" in {
    testLML1Model()
  }

  "RFC" should "match the C++ RFC model" in {
    testRFCModel()
  }

  "RFS" should "match the C++ RFS model" in {
    testRFSModel()
  }

  "BipBipDec" should "match the C++ BipBipDec model" in {
    testBipBipDecModel()
  }

  def testLML1Model(blockSize: Int = 24, sboxWidth: Int = 6): Unit = {
    test(new RFC(blockSize, sboxWidth) {
      io.output := LML(io.input)
    }) { c =>
      for (i <- 0 until rounds) {
        val inputVector = Vector.fill(blockSize)(scala.util.Random.nextBoolean())
        val inputBigInt = BigInt(inputVector.reverse.map(if(_) 1 else 0).mkString, 2)
        c.io.input.poke(inputBigInt.U)

        // Call Chisel LML1 function and get the output
        val chiselOutputBigInt = c.io.output.peek().litValue

        // Call C++ LML1 function
        val cppInputArray = inputVector.toArray
        DataPathInterop.instance.LML1(cppInputArray)
        val expectedOutputBigInt = BigInt(cppInputArray.reverse.map(if(_) 1 else 0).mkString, 2)

        println(s"LML1 test round $i: input:$inputBigInt output:$chiselOutputBigInt expected:$expectedOutputBigInt")
        
        // Compare outputs
        assert(chiselOutputBigInt == expectedOutputBigInt)
      }
    }
  }

  def testRFCModel(blockSize: Int = 24, sboxWidth: Int = 6): Unit = {
    test(new RFC(blockSize, sboxWidth)) { c =>
      for (i <- 0 until rounds) {
        val inputVector = Vector.fill(blockSize)(scala.util.Random.nextBoolean())
        val inputBigInt = BigInt(inputVector.reverse.map(if(_) 1 else 0).mkString, 2)
        c.io.input.poke(inputBigInt.U)

        // Call Chisel RFC function and get the output
        val chiselOutputBigInt = c.io.output.peek().litValue

        // Call C++ RFC function
        val cppInputArray = inputVector.toArray
        DataPathInterop.instance.RFC(cppInputArray)
        val expectedOutputBigInt = BigInt(cppInputArray.reverse.map(if(_) 1 else 0).mkString, 2)

        println(s"RFC test round $i: input:$inputBigInt output:$chiselOutputBigInt expected:$expectedOutputBigInt")
        
        // Compare outputs
        assert(chiselOutputBigInt == expectedOutputBigInt)
      }
    }
  }

  def testRFSModel(blockSize: Int = 24, sboxWidth: Int = 6): Unit = {
    test(new RFS(blockSize, sboxWidth)) { c =>
      for (i <- 0 until rounds) {
        val inputVector = Vector.fill(blockSize)(scala.util.Random.nextBoolean())
        val inputBigInt = BigInt(inputVector.reverse.map(if(_) 1 else 0).mkString, 2)
        c.io.input.poke(inputBigInt.U)

        // Call Chisel RFS function and get the output
        val chiselOutputBigInt = c.io.output.peek().litValue

        // Call C++ RFS function
        val cppInputArray = inputVector.toArray
        DataPathInterop.instance.RFS(cppInputArray)
        val expectedOutputBigInt = BigInt(cppInputArray.reverse.map(if(_) 1 else 0).mkString, 2)

        println(s"RFS test round $i: input:$inputBigInt output:$chiselOutputBigInt expected:$expectedOutputBigInt")
        
        // Compare outputs
        assert(chiselOutputBigInt == expectedOutputBigInt)
      }
    }
  }

  def testBipBipDecModel(x: Int = 3, y: Int = 5, z: Int = 3, blockSize: Int = 24, sboxWidth: Int = 6): Unit = {
    test(new BipBipDec(x, y, z, blockSize, sboxWidth)) { c =>
      for (i <- 0 until rounds) {
        // generate random encryption data
        val encData = Vector.fill(blockSize)(scala.util.Random.nextBoolean())
        val encDataBigInt = BigInt(encData.reverse.map(if(_) 1 else 0).mkString, 2)
        c.io.data.poke(encDataBigInt.U)
        
        // generate random data round keys
        val dataRoundKeys = Vector.fill(x+y+z+1)(Vector.fill(blockSize)(scala.util.Random.nextBoolean()))
        val dataRoundKeysBigInt = dataRoundKeys.map(x => BigInt(x.reverse.map(if(_) 1 else 0).mkString, 2))
        for ((value, index) <- dataRoundKeysBigInt.zipWithIndex) {
          c.io.dataRoundKeys(index).poke(value.U)
        }

        // Call Chisel data path function and get the output
        val chiselOutputBigInt = c.io.output.peek().litValue

        // Prepare inputs for C++ functions
        val encDataArray = encData.toArray
        val dataRoundKeysArray = ArrayHelper.flatten(dataRoundKeys.map(_.toArray).toArray)

        // Call C++ function
        DataPathInterop.instance.BipBipDec(encDataArray, dataRoundKeysArray)

        // Retrieve expected outputs
        val expectedData = BigInt(encDataArray.reverse.map(if(_) 1 else 0).mkString, 2)

        // Print inputs and outputs
        println(s"BipBipDec test round $i: input:$encDataBigInt dataRoundKeys:${dataRoundKeysBigInt.mkString(",")} output:$chiselOutputBigInt expected:$expectedData")

        // Compare outputs
        assert(chiselOutputBigInt == expectedData)
      }
    }
  }
}
