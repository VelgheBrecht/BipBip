package BipBip

import chisel3._
import chisel3.util._

// BipBip Key Schedule
class KeySc(masterKeySize: Int = 256, amountDataRoundKeys: Int = 1, amountTweakRoundKeys: Int = 6,
            extendedTweakSize: Int = 53, blockSize: Int = 24) extends Module {
    val io = IO(new Bundle {
        val masterKey = Input(UInt(masterKeySize.W))
        val tweakRoundKeys = Output(Vec(amountDataRoundKeys+amountTweakRoundKeys, UInt(extendedTweakSize.W)))
    })

    // Assign the data round keys
    var k: Long = 1
    for (i <- 0 until amountDataRoundKeys) {
        val dataRoundKey = Wire(Vec(blockSize, Bool()))
        for (j <- 0 until blockSize) {
            k *= 3
            dataRoundKey(j) := io.masterKey(k % masterKeySize)
        }
        io.tweakRoundKeys(i) := dataRoundKey.asUInt
    }

    // Assign the tweak round keys
    k = extendedTweakSize
    for (i <- amountDataRoundKeys until amountTweakRoundKeys+amountDataRoundKeys) {
        val tweakRoundKey = Wire(Vec(extendedTweakSize, Bool()))
        for (j <- 0 until extendedTweakSize) {
            tweakRoundKey(j) := io.masterKey(k % masterKeySize)
            k += 1
        }
        io.tweakRoundKeys(i) := tweakRoundKey.asUInt
    }
}
