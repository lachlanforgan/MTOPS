# HYPO Decimal Machine Simulator

## Overview
This project simulates a hypothetical decimal machine designed for testing operating system concepts without using actual hardware. The simulator creates a virtual environment with a defined memory size, registers, and instruction set to execute machine language programs.

## System Specifications
- **Memory Size**: 10,000 locations
- **Registers**:
  - 8 General Purpose Registers (GPR)
  - 3 Special Purpose Registers:
    - Memory Address Register (MAR)
    - Memory Buffer Register (MBR)
    - Instruction Register (IR)
  - Stack Pointer (SP)
  - Program Counter (PC)
  - Program Status Register (PSR)

## Features
- Process Control Block (PCB) implementation
- Memory management with dynamic allocation/deallocation
- CPU scheduling with time slicing
- Ready and Wait queues for process management
- I/O operations simulation
- Stack implementation

## Program Structure
The simulator:
1. Initializes the system
2. Loads machine language programs into memory
3. Executes processes using a simulated CPU
4. Manages memory allocation
5. Handles process scheduling
6. Performs context switching
7. Generates detailed output logs

## Sample Programs
The project includes several machine language programs:
- `program1.txt` - Basic program example
- `program2.txt` - Another program example
- `program3.txt` - Another program example
- `ForganProgram4.txt` - More advanced program example
- `ForganProgram5.txt` - More advanced program example
- `ForganProgram6.txt` - More advanced program example

## Usage
Compile and run the Java program:
```
javac hyposim4.java
java hyposim4
```

The program outputs will be written to a text file (e.g., `Forgan_HW4Output.txt`).

## Development Information
- Author: Lachlan Forgan
- Course: OS Internals
- Purpose: Educational simulation of operating system concepts