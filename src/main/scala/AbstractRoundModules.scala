package BipBip

import chisel3._
import chisel3.util._

object TableLoader {
    // Define the arrays with the actual values from the JSON file
    val BBB: Seq[UInt] = Seq("0x00","0x01","0x02","0x03","0x04","0x06","0x3e","0x3c","0x08","0x11","0x0e","0x17","0x2b","0x33","0x35","0x2d","0x19","0x1c","0x09","0x0c","0x15","0x13","0x3d","0x3b","0x31","0x2c","0x25","0x38","0x3a","0x26","0x36","0x2a","0x34","0x1d","0x37","0x1e","0x30","0x1a","0x0b","0x21","0x2e","0x1f","0x29","0x18","0x0f","0x3f","0x10","0x20","0x28","0x05","0x39","0x14","0x24","0x0a","0x0d","0x23","0x12","0x27","0x07","0x32","0x1b","0x2f","0x16","0x22").map(value => Integer.parseInt(value.substring(2), 16).asUInt(6.W))
    val P1: Seq[UInt] = Seq(1, 7, 6, 0, 2, 8, 12, 18, 19, 13, 14, 20, 21, 15, 16, 22, 23, 17, 9, 3, 4, 10, 11, 5).map(_.U)
    val P2: Seq[UInt] = Seq(0, 1, 4, 5, 8, 9, 2, 3, 6, 7, 10, 11, 16, 12, 13, 17, 20, 21, 15, 14, 18, 19, 22, 23).map(_.U)
    val P3: Seq[UInt] = Seq(16, 22, 11, 5, 2, 8, 0, 6, 19, 13, 12, 18, 14, 15, 1, 7, 21, 20, 4, 3, 17, 23, 10, 9).map(_.U)
    val P4: Seq[UInt] = Seq(0, 13, 26, 39, 52, 12, 25, 38, 51, 11, 24, 37, 50, 10, 23, 36, 49, 9, 22, 35, 48, 8, 21, 34, 47, 7, 20, 33, 46, 6, 19, 32, 45, 5, 18, 31, 44, 4, 17, 30, 43, 3, 16, 29, 42, 2, 15, 28, 41, 1, 14, 27, 40).map(_.U)
    val P5: Seq[UInt] = Seq(0, 11, 22, 33, 44, 2, 13, 24, 35, 46, 4, 15, 26, 37, 48, 6, 17, 28, 39, 50, 8, 19, 30, 41, 52, 10, 21, 32, 43, 1, 12, 23, 34, 45, 3, 14, 25, 36, 47, 5, 16, 27, 38, 49, 7, 18, 29, 40, 51, 9, 20, 31, 42).map(_.U)
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
