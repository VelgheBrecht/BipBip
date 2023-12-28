package BipBip

import chisel3._
import chisel3.util._
import play.api.libs.json._
import java.io._

object TableLoader {
    var BBB: Seq[UInt] = Seq()
    var P1: Seq[UInt] = Seq()
    var P2: Seq[UInt] = Seq()
    var P3: Seq[UInt] = Seq()
    var P4: Seq[UInt] = Seq()
    var P5: Seq[UInt] = Seq()
    
    try{
        // Load JSON file
        val fileInputStream = new FileInputStream(new File("resources/tables.json"))
        val jsonString = scala.io.Source.fromInputStream(fileInputStream).mkString
        fileInputStream.close()

        // Parse JSON
        val json = Json.parse(jsonString)

        // Extract tables
        BBB = (json \ "BBB").as[Seq[String]].map(value => Integer.parseInt(value.substring(2), 16).asUInt(6.W))
        P1 = (json \ "P1").as[Seq[Int]].map(_.U)
        P2 = (json \ "P2").as[Seq[Int]].map(_.U)
        P3 = (json \ "P3").as[Seq[Int]].map(_.U)
        P4 = (json \ "P4").as[Seq[Int]].map(_.U)
        P5 = (json \ "P5").as[Seq[Int]].map(_.U)
    } catch {
        case e: Exception => 
        println(s"Error while reading the file: ${e.getMessage}")
        e.printStackTrace()
    }
}

abstract class RoundModule(val blockSize: Int) extends Module {
    val io = IO(new Bundle {
        val input = Input(UInt(blockSize.W))
        val output = Output(UInt(blockSize.W))
    })
    
    // Bit-Permutation with perm
    def BPL(input: UInt, perm: Vec[UInt]): UInt = {
        val output = Wire(Vec(blockSize, Bool()))
        for (i <- 0 until blockSize) {
        output(i) := input(perm(i))
        }
        output.asUInt
    }
}

abstract class DataPathRoundModule(blockSize: Int = 24, sboxWidth: Int = 6) extends RoundModule(blockSize) {
    val BBB = VecInit(TableLoader.BBB)

    // S-box layer S
    def SBL(input: UInt): UInt = {
        val output = Wire(Vec(blockSize, Bool()))
        for (j <- 0 until blockSize / sboxWidth) {
            val iStart = sboxWidth * j + sboxWidth - 1
            var a = 0.U(sboxWidth.W)
            for(i <- 0 until sboxWidth) {
                a = a ^ (input(iStart - i) << (sboxWidth - i - 1))
            }
            val lookupValue = BBB(a)
            
            for(i <- 0 until sboxWidth) {
                output(iStart - i) := (lookupValue >> (sboxWidth - i - 1)) & 1.U
            }
        }
        output.asUInt
    }
}

abstract class TweakScheduleRoundModule(blockSize: Int = 53) extends RoundModule(blockSize) {
    def LML(input: UInt): UInt
    // Generate the permutation vector for π4: ai ← a13i and π5: ai ← a11i
    val P4 = VecInit(TableLoader.P4)
    val P5 = VecInit(TableLoader.P5)
}
