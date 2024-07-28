package BipBip

import chisel3._
import chisel3.util._

// round function R: core round function
class RFC(blockSize: Int = 24, sboxWidth: Int = 6) extends DataPathRoundModule(blockSize, sboxWidth) {
    // permutation tables for pi1 and pi2
    val P1 = VecInit(TableLoader.P1)
    val P2 = VecInit(TableLoader.P2)

    // LML1
    // Linear Mixing Layer in Data Path: theta_d
    def LML(input: UInt): UInt = {
        val output = Wire(Vec(blockSize, Bool()))
        for (i <- 0 until blockSize) {
        output(i) := input(i) ^ input((i + 2) % blockSize) ^ input((i + 12) % blockSize)
        }
        output.asUInt
    }

    // core round function R
    // substitution layer
    val sboxOuput = SBL(io.input)
    // permutation layer pi1
    val pi1 = BPL(sboxOuput, P1)
    // mixing layer theta_d
    val theta_d = LML(pi1)
    // permutation layer pi2
    io.output := BPL(theta_d, P2)
}

// round function R': shell round function
class RFS(blockSize: Int = 24, sboxWidth: Int = 6) extends DataPathRoundModule {
    // permutation table for pi3
    val P3 = VecInit(TableLoader.P3)
    // shell round function R'
    // substitution layer
    val sboxOuput = SBL(io.input)
    // permutation layer pi3
    io.output := BPL(sboxOuput, P3)
}

class BipBipDec(x: Int = 3, y: Int = 5, z: Int = 3, blockSize: Int = 24, sboxWidth: Int = 6) extends Module {
    val io = IO(new Bundle {
        val dataRoundKeys = Input(Vec(x+y+z+1, UInt(blockSize.W)))
        val data = Input(UInt(blockSize.W))
        val output = Output(UInt(blockSize.W))
    })

    val initialShellRounds = Seq.fill(x)(Module(new RFS(blockSize, sboxWidth)))
    val coreRounds = Seq.fill(y)(Module(new RFC(blockSize, sboxWidth)))
    val finalShellRounds = Seq.fill(z)(Module(new RFS(blockSize, sboxWidth)))

    val allStages = initialShellRounds ++ coreRounds ++ finalShellRounds
    
    // Connect the stages together
    allStages.zipWithIndex.foreach { case (stage, idx) =>
        if (idx == 0) {
            stage.io.input := io.data ^ io.dataRoundKeys(idx)
        } else {
            stage.io.input := allStages(idx - 1).io.output ^ io.dataRoundKeys(idx)
        }
    }

    // Connect the output of the last stage to the module's output, XORed with the last round key
    io.output := allStages.last.io.output ^ io.dataRoundKeys.last
}
