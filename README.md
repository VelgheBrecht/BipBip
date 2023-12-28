
# BipBip: A Low Latency Tweakable Block Cipher with Small Dimensions implemented in Chisel

## Overview
This repository contains an implementation of the BipBip low latency block cipher in Chisel. The implementation aims to provide a hardware-efficient encryption solution, with thorough testing against the reference C++ code provided by the original developers of BipBip.

## Reference
The BipBip block cipher is detailed in the following paper:
- [BipBip: A Low-Latency Tweakable Block Cipherwith Small Dimensions](https://doi.org/10.46586/tches.v2023.i1.326-368)
Please refer to the paper for an in-depth understanding of the cipher's design and objectives.

## Original Implementation
This implementation is tested against the official BipBip C++ code, which can be found at:
- [Official BipBip Repository](https://gitlab.science.ru.nl/shahramr/bipbip_tweakable_block_cipher)

## Website
For additional information, you can visit the BipBip website:
- [Official BipBip Website](https://cs.ru.nl/~joan/bipbip.html)

## Getting Started

### Prerequisites
- sbt (Scala Build Tool)
- Java Development Kit (JDK)
- Any prerequisites specific to running Chisel projects

### Installation
1. Clone this repository:
   ```bash
   git clone https://github.com/userVincent/BipBip.git
   ```
2. Navigate to the cloned directory:
   ```bash
   cd BipBip
   ```

### Running Tests
To run the tests and ensure the functionality matches that of the original C++ implementation:
```bash
sbt test
```

## Contact
For any queries or further discussion, please contact me at vincent@vansant.mozmail.com.
