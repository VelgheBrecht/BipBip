package BipBip

import chisel3._
import chisel3.util._

class BipBip(ptrSize: Int = 64, masterKeySize: Int = 256) extends Module {
    val io = IO(new Bundle {
        val encPtr = Input(UInt(ptrSize.W))
        val masterKey = Input(UInt(masterKeySize.W))
        val decPtr = Output(UInt(ptrSize.W))
    })

    // Instantiate all modules
    val KeySc = Module(new KeySc())
    val TwkIn = Module(new TwkIn())
    val TwkSc = Module(new TwkSc())
    val BipBipDec = Module(new BipBipDec())

    // connect keySc module inputs
    KeySc.io.masterKey := io.masterKey

    // connect TwkIn module inputs
    val power = io.encPtr(63, 58)
    var shiftedTweak = io.encPtr >> power
    shiftedTweak = shiftedTweak << power
    TwkIn.io.tweak := Cat(shiftedTweak(63, 58), shiftedTweak(33, 0)) // bit [0-33] + [58-63]
    
    // connect TwkSc module inputs
    TwkSc.io.extendedTweak := TwkIn.io.extendedTweak
    TwkSc.io.tweakRoundKeys := KeySc.io.tweakRoundKeys

    // connect BipBipDec module inputs
    BipBipDec.io.data := io.encPtr(57, 34) // bit [34-57] of encPtr
    BipBipDec.io.dataRoundKeys := TwkSc.io.dataRoundKeys

    // connect output
    io.decPtr := Cat(io.encPtr(63, 58), BipBipDec.io.output, io.encPtr(33, 0)) // bit [0-33] + BipBipDec.output + [58-63] of encPtr
}
