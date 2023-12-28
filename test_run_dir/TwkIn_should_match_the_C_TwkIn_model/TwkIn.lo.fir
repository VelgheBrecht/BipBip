circuit TwkIn :
  module TwkIn :
    input clock : Clock
    input reset : UInt<1>
    input io_tweak : UInt<40>
    output io_extendedTweak : UInt<53>

    node io_extendedTweak_hi = cat(io_tweak, UInt<1>("h1")) @[Cat.scala 31:58]
    node _io_extendedTweak_T = cat(io_extendedTweak_hi, UInt<12>("h0")) @[Cat.scala 31:58]
    io_extendedTweak <= _io_extendedTweak_T @[TweakSchedule.scala 13:22]
