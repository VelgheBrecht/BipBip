package BipBip

import chisel3._
import chisel3.util._

// Initializing
class TwkIn(tweakSize: Int = 40, extendedTweakSize: Int = 53) extends Module {
    val io = IO(new Bundle {
        val tweak = Input(UInt(tweakSize.W))
        val extendedTweak = Output(UInt(extendedTweakSize.W))
    })
    
    io.extendedTweak := Cat(io.tweak, 1.U, 0.U(12.W))
}

// Chi Layer
class CHI(blockSize: Int = 53) extends RoundModule(blockSize) {

    val output = Wire(Vec(blockSize, Bool()))
    for (i <- 0 until blockSize) {
        output(i) := io.input(i) ^ (~io.input((i + 1) % blockSize) & io.input((i + 2) % blockSize))
    }
    io.output := output.asUInt
}

// Round Function: G Round
class RGC(blockSize: Int = 53) extends TweakScheduleRoundModule(blockSize) {
    // LML2
    // Linear Mixing Layer in Tweak Path: theta_t
    def LML(input: UInt): UInt = {
        val output = Wire(Vec(blockSize, Bool()))
        for (i <- 0 until blockSize) {
            output(i) := input(i) ^ input((i + 1) % blockSize) ^ input((i + 8) % blockSize)
        }
        output.asUInt
    }

    // Instantiate the CHI module
    val chi = Module(new CHI(blockSize))

    // permutation layer pi4
    val pi4 = BPL(io.input, P4)
    // mixing layer theta
    val theta = LML(pi4)
    // permutation layer pi5
    val pi5 = BPL(theta, P5)
    // chi layer
    chi.io.input := pi5
    io.output := chi.io.output
}

// Round Function: G' Round
class RGP(blockSize: Int = 53) extends TweakScheduleRoundModule(blockSize) {
    // LML3
    // Linear Mixing Layer in Tweak Path: theta_p
    def LML(input: UInt): UInt = {
        val output = Wire(Vec(blockSize, Bool()))
        for (i <- 0 until blockSize-1) {
            output(i) := input(i) ^ input(i + 1)
        }
        output(blockSize-1) := input(blockSize-1)
        output.asUInt
    }

    // Instantiate the CHI module
    val chi = Module(new CHI(blockSize))

    // permutation layer pi4
    val pi4 = BPL(io.input, P4)
    // mixing layer theta
    val theta = LML(pi4)
    // permutation layer pi5
    val pi5 = BPL(theta, P5)
    // chi layer
    chi.io.input := pi5
    io.output := chi.io.output
}

// BipBip Tweak Schedule
class TwkSc(extendedTweakSize: Int = 53, amountTweakRoundKeys: Int = 7, 
            amountDataRoundKeys: Int = 12, blockSize: Int = 24) extends Module {
    val io = IO(new Bundle {
        val tweakRoundKeys = Input(Vec(amountTweakRoundKeys, UInt(extendedTweakSize.W)))
        val extendedTweak = Input(UInt(extendedTweakSize.W))
        val dataRoundKeys = Output(Vec(amountDataRoundKeys, UInt(blockSize.W)))
    })

    // RKE0 and RKE1, offset is used to differentiate
    // round key extraction, if offset is true extraction is E1 if false extraction is E0
    def RKE(intermediate: UInt, offset: Boolean = false): UInt = {
        val drk = Wire(Vec(blockSize, Bool()))
        for(i <- 0 until blockSize) {
            drk(i) := intermediate(i*2 + (if (offset) 1 else 0))
        }
        drk.asUInt
    }

    // link all stages, hardcoded sequence of CHI, G, 3x(G, G'), G
    val allStages = Seq[RoundModule](
        Module(new CHI(extendedTweakSize)),
        Module(new RGC),
        Module(new RGC), Module(new RGP),
        Module(new RGC), Module(new RGP),
        Module(new RGC), Module(new RGP),
        Module(new RGC)
    )

    // Connect the first stage to the input
    allStages.head.io.input := io.extendedTweak ^ io.tweakRoundKeys(1)

    val keyIdxMapping = Array(2, 3, -1, 4, -1, 5, -1, 6)  // -1 indicates no XOR operation
    // Connect the rest of the stages
    allStages.zip(allStages.tail).zipWithIndex.foreach { case ((prevStage, currStage), idx) =>
        if (keyIdxMapping(idx) == -1) {
            // Between G and G' stages, no XOR
            currStage.io.input := prevStage.io.output
        } else {
            // Between other stages, XOR with the next tweakRoundKey
            currStage.io.input := prevStage.io.output ^ io.tweakRoundKeys(keyIdxMapping(idx))
        }
    }

    // Pass the fist tweak key to the first data key
    io.dataRoundKeys(0) := io.tweakRoundKeys(0)
    // Connect the cerresponding stage to the corresponding output
    allStages.zipWithIndex.foreach { case (prevStage, idx) =>
        if(idx < 2){
            io.dataRoundKeys(idx*2 + 1) := RKE(prevStage.io.output, false)
            io.dataRoundKeys(idx*2 + 2) := RKE(prevStage.io.output, true)
        } else if (idx > 2){
            io.dataRoundKeys(idx + 2) := RKE(prevStage.io.output, false)
        }
    }
    // Connect final output
    io.dataRoundKeys(amountDataRoundKeys - 1) := RKE(allStages.last.io.output, true)
}
