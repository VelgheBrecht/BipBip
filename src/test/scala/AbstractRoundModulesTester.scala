package BipBip

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import com.sun.jna._  // For JNA functionality
import com.sun.jna.Library  // For defining the JNA interface
import java.nio.file.Paths

// Assuming a JNA Interface to C++ code
trait AbstractModuleLibrary extends Library {
  def BPL1(x: Array[Boolean]): Unit
  def BPL2(x: Array[Boolean]): Unit
  def BPL3(x: Array[Boolean]): Unit
  def BPL4(x: Array[Boolean]): Unit
  def BPL5(x: Array[Boolean]): Unit
  def SBL(x: Array[Boolean]): Unit
}

object AbstractModuleInterop {
  val instance: AbstractModuleLibrary = Native.load("BipBip", classOf[AbstractModuleLibrary]).asInstanceOf[AbstractModuleLibrary]
}

class AbstractModulesTester extends AnyFlatSpec with ChiselScalatestTester {
  val libPath = Paths.get("lib").toAbsolutePath.toString
  System.setProperty("jna.library.path", libPath)
  val rounds = 10

  "BPL" should "match the C++ BPL1 model" in {
    testBPLModel("BPL1", TableLoader.P1, 24)
  }

  "BPL" should "match the C++ BPL2 model" in {
    testBPLModel("BPL2", TableLoader.P2, 24)
  }

  "BPL" should "match the C++ BPL3 model" in {
    testBPLModel("BPL3", TableLoader.P3, 24)
  }

  "BPL" should "match the C++ BPL4 model" in {
    testBPLModel("BPL4", TableLoader.P4, 53)
  }

  "BPL" should "match the C++ BPL5 model" in {
    testBPLModel("BPL5", TableLoader.P5, 53)
  }

  "SBL" should "match the C++ SBL model" in {
    testSBLModel()
  }

  def testBPLModel(modelName: String, perm: Seq[UInt], blockSize: Int): Unit = {
    test(new RoundModule(blockSize) {
      val permVec = VecInit(perm)
      val bplResult = BPL(io.input, permVec)
      io.output := bplResult
    }) { c =>
      for (i <- 0 until rounds) {
        val inputVector = Vector.fill(blockSize)(scala.util.Random.nextBoolean())
        val inputBigInt = BigInt(inputVector.reverse.map(if(_) 1 else 0).mkString, 2)
        c.io.input.poke(inputBigInt.U)

        // Call Chisel BPL function and get the output
        val chiselOutputBigInt = c.io.output.peek().litValue

        // Call C++ BPL1 function
        val cppInputArray = inputVector.toArray
        modelName match {
        case "BPL1" => AbstractModuleInterop.instance.BPL1(cppInputArray)
        case "BPL2" => AbstractModuleInterop.instance.BPL2(cppInputArray)
        case "BPL3" => AbstractModuleInterop.instance.BPL3(cppInputArray)
        case "BPL4" => AbstractModuleInterop.instance.BPL4(cppInputArray)
        case "BPL5" => AbstractModuleInterop.instance.BPL5(cppInputArray)
        }
        val expectedOutputBigInt = BigInt(cppInputArray.reverse.map(if(_) 1 else 0).mkString, 2)

        println(s"$modelName test round $i: input:$inputBigInt output:$chiselOutputBigInt expected:$expectedOutputBigInt")

        assert(chiselOutputBigInt == expectedOutputBigInt)
      }
    }
  }

  def testSBLModel(blockSize: Int = 24, sboxWidth: Int = 6): Unit = {
    test(new DataPathRoundModule(blockSize, sboxWidth) {
      io.output := SBL(io.input)
    }) { c =>
      for (i <- 0 until rounds) {
      // Generate a random input
      val inputVector = Vector.fill(blockSize)(scala.util.Random.nextBoolean())
      val inputBigInt = BigInt(inputVector.reverse.map(if(_) 1 else 0).mkString, 2)
      c.io.input.poke(inputBigInt.U)

      // Call Chisel SBL function and get the output
      val chiselOutputBigInt = c.io.output.peek().litValue

      // Call C++ SBL function
      val cppInputArray = inputVector.toArray
      AbstractModuleInterop.instance.SBL(cppInputArray)
      val expectedOutputBigInt = BigInt(cppInputArray.reverse.map(if(_) 1 else 0).mkString, 2)

      println(s"SBL test round $i: input:$inputBigInt output:$chiselOutputBigInt expected:$expectedOutputBigInt")
      
      // Compare outputs
      assert(chiselOutputBigInt == expectedOutputBigInt)
      }
    }
  }
}
