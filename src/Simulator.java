// ************************************************************************************
// Author: Lachlan Forgan
// Student ID: 301080041
// Assignment: Homework 1
// Date: 02/17/2025
// Course: OS Internals
// Functionality: The purpose of this program is to simulate a hypothetical decimal
// machine. The overall purpose of this software will be to test an operating system
// without accessing or using hardware. In this decimal machine, the computing systems
// are binary. The machine has a memory size of 10000, 8 general purpose registers, and
// three special purpose registers. The program loads a machine language file into memory
// by loading each instruction into a location, then executes these instructions. It then
// dumps the memory to the console, which displays the contents stored by the machine.
// ************************************************************************************


// import java libraries

import java.io.*;
import java.util.*;
import java.util.InputMismatchException;

// class definition
public class Simulator {

    static FileWriter writer;    // Declare a file writer, necessary for writing

    // output to a file
    {
        try {
            writer = new FileWriter("simulation_output.txt");    // Create a new file writer with output file name
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Defining constants
    private static final int MEMORY_SIZE = 10000;    // Memory size (aka RAM size)
    private static final int NUM_REGISTERS = 8;    // Number of general purpose registers
    private static final int HALT_OPCODE = 0;    // Halt opcode
    private static final int ERROR_INVALID_ADDRESS = -1;    // Error code for invalid memory address
    private static final int ERROR_FILE_OPEN = -2;    // Error code for file open error
    private static final int ERROR_FILE_READ = -3;    // Error code for file read error
    private static final int GENERAL_PURPOSE_ERROR = -4;
    private static final int ERROR_INVALID_FORMAT = -5;
    private static final int ERROR_DIVIDE_BY_ZERO = -6;
    private static final int ERROR_WRITE_FAILED = -7;
    private static final int ERROR_NO_EOP = -8;
    private static final int ERROR_INVALID_SIZE = -9;
    private static final int ERROR_NO_FREE_MEMORY = -10;
    private static final int OK = 1;
    private static final int HALT_STATUS = 100;
    private static final long TIME_SLICE_EXPIRED = -11;
    private static final long START_INPUT_OPERATION_CODE = 12;
    private static final long START_OUTPUT_OPERATION_CODE = 13;
    private static final long INPUT_COMPLETION_EVENT = 14;
    private static final long OUTPUT_COMPLETION_EVENT = 15;


    private static final int DEFAULTPRIORITY = 128;
    private static final int ReadyState = 1;
    private static final int WaitingState = 2;
    private static final int ENDOFLIST = -1;
    private static final int TIMESLICE = 200;

    // PCB index refs
    private static final int NEXT_PCB_PTR_REF = 0;
    private static final int PID_REF = 1;
    private static final int STATE_REF = 2;
    private static final int REASON_REF = 3;
    private static final int PRIORITY_REF = 4;
    private static final int STACK_START_ADDRESS_REF = 5;
    private static final int STACK_SIZE_REF = 6;
    private static final int MESSAGE_QUEUE_StART_REF = 7;
    private static final int MESSAGE_QUEUE_SIZE_REF = 8;
    private static final int NUM_MESSAGES_REF = 9;
    private static final int GPR0_REF = 10;
    private static final int GPR1_REF = 11;
    private static final int GPR2_REF = 12;
    private static final int GPR3_REF = 13;
    private static final int GPR4_REF = 14;
    private static final int GPR5_REF = 15;
    private static final int GPR6_REF = 16;
    private static final int GPR7_REF = 17;
    private static final int SP_REF = 18;
    private static final int PC_REF = 19;
    private static final int PSR_REF = 20;

    private static int minUsedAddress = MEMORY_SIZE;
    private static int maxUsedAddress = 0;
    private static final int OS_LIST_START = 1000;
    private static final int USER_LIST_START = 2000;
    private static final int OS_LIST_END = 1999;
    private static final int USER_LIST_END = 2999;
    
    // Maximum number of memory blocks to track
    private static final int MAX_MEMORY_BLOCKS = 100;
    // Arrays to track allocated memory blocks
    private long[] allocatedAddresses = new long[MAX_MEMORY_BLOCKS];
    private long[] allocatedSizes = new long[MAX_MEMORY_BLOCKS];
    private int allocatedBlockCount = 0;

    private final long[] RAM = new long[MEMORY_SIZE];    // Declare memory
    long[] gpr = new long[8];    // Declare general purpose registers
    long MAR;    // Memory Address Register
    long MBR;    // Memory Buffer Register
    long IR;    // Instruction Register
    long sp;    // Stack Pointer
    long pc;    // Program Counter
    long psr;    // Program Status Register
    long clock;    // Clock

    //HW2 VARAIBLES FOR PCB
    long ProcessID = 1;
    long RQ = ENDOFLIST;
    long WQ = ENDOFLIST;
    long OSFreeList = ENDOFLIST;
    long UserFreeList = ENDOFLIST;
    private static final long UserMode = 2;
    private static final long OSMode = 1;
    private boolean ShutdownStatus;


    // ******************************************************
    // function: main
    //
    // Task Description: The main function creates a class, initializes the system,
    // loads the program into memory, runs the CPU (executes the program), and
    // dumps the memory.
    // 	…
    // Input parameters
    //	none
    // Output parameters
    //	none
    // Function return value
    //  none - Java does not allow return value in main method
    //	List all return error codes and their description here
    // ******************************************************
    public static void main(String[] args) throws IOException{
        Simulator system = new Simulator();    // Create a new instance of the class
        write("Running HYPO Decimal Machine...\n");
        system.initializeSystem();   // Initialize the system


        while (system.ShutdownStatus == false)    // While the shutdown status is false, run the program
        {
            system.checkAndProcessInterrupt();
            if (system.ShutdownStatus) {
                system.dumpMemory("System shutting down...\n", minUsedAddress, maxUsedAddress);
                //1system.dumpMemory("User Memory Area after CPU scheduling\n", USER_LIST_START, USER_LIST_END);
                System.exit(0);
            }
            system.DumpQueue("\nRQ: Before CPU scheduling\n", system.RQ);
            system.DumpQueue("\nWQ: Before CPU scheduling\n", system.WQ);

            // select next process from RQ to give CPU
            long runningPCBptr = system.selectProcessFromRQ();
            if (runningPCBptr == ENDOFLIST) {
                write(String.format("No process to run\n"));
                //system.ShutdownStatus = true;
                continue;
            }
            else
            {
                write("Process selected:\n");
                system.printPCB(runningPCBptr);
            }

            // perform restore context using dispatcher
            system.dispatcher(runningPCBptr);

            system.DumpQueue("RQ: After selecting process from RQ\n", system.RQ);

            // execute instructions of running process using cpu
            write("Executing cpu...\n");
            long status = system.CPU();

            // dump memory area
            //system.dumpMemory("Dynamic Memory Area after CPU scheduling\n", 2100, 100);
            // check return status - reason for giving up cpu
            if (status == TIME_SLICE_EXPIRED)
            {
                system.saveContext(runningPCBptr);    // save context
                system.insertIntoRQ(runningPCBptr);    // insert back into RQ
                runningPCBptr = ENDOFLIST;
            }
            else  if (status == HALT_STATUS || status < 0)
            {
                write(String.format("Completed process of PID: " + system.RAM[(int)runningPCBptr + PID_REF] + ". Clearing PCB...\n"));
                //system.dumpMemory("User Memory Area after CPU scheduling\n", USER_LIST_START, USER_LIST_END);
                system.terminateProcess(runningPCBptr);    // terminate process
                runningPCBptr = ENDOFLIST;
            }
            else if (status == START_INPUT_OPERATION_CODE)
            {
                system.RAM[(int)runningPCBptr + REASON_REF] = INPUT_COMPLETION_EVENT;
                system.saveContext(runningPCBptr);    // save context
                system.insertIntoWQ(runningPCBptr);    // insert into WQ
                runningPCBptr = ENDOFLIST;
            }
            else if (status == START_OUTPUT_OPERATION_CODE)
            {
                system.RAM[(int)runningPCBptr + REASON_REF] = OUTPUT_COMPLETION_EVENT;
                system.saveContext(runningPCBptr);    // save context
                system.insertIntoWQ(runningPCBptr);    // insert into WQ
                runningPCBptr = ENDOFLIST;
            }
            else
            {
                write("Unknown error\n");
            }
            write("OS FREE LIST:\n");
            system.dumpList(system.OSFreeList);
            write("USER FREE LIST:\n");
            system.dumpList(system.UserFreeList);
            // Dump allocated memory blocks
            system.dumpAllocatedMemory();
        }

        //system.dumpMemory("OS Memory Area after CPU scheduling\n", OS_LIST_START, OS_LIST_END);
        system.dumpMemory("User Memory Area after CPU scheduling\n", USER_LIST_START, USER_LIST_END);

        //write("OS is shutting down.\n");
        system.dumpMemory("OS is shutting down...\n", minUsedAddress, maxUsedAddress);

        writer.close();
    }

    // ************************************************************
    // Function: DumpRQ
    //
    // Task Description:
    // 	Dumps the contents of the Ready Queue (RQ) to the output.
    // 	Displays each PCB in the queue or "List is empty" if the queue is empty.
    //
    // Input Parameters
    //	s       String message to display before dumping the queue
    //  Qptr    Pointer to first element in queue
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    private void DumpQueue(String s, long Qptr) {
        write(s);
        long currentPCBptr = Qptr;
        if (currentPCBptr == ENDOFLIST)
        {
            write("Queue is empty.\n");
        }

        while (currentPCBptr != ENDOFLIST)
        {
            printPCB(currentPCBptr);
            currentPCBptr = RAM[(int)currentPCBptr + NEXT_PCB_PTR_REF];
        }
    }

    // ************************************************************
    // Function: DumpWQ
    //
    // Task Description:
    // 	Dumps the contents of the Wait Queue (WQ) to the output.
    // 	Displays each PCB in the queue or "List is empty" if the queue is empty.
    //
    // Input Parameters
    //	s       String message to display before dumping the queue
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    private void DumpWQ(String s)
    {
        write(s);
        if (WQ == ENDOFLIST)
        {
            write("List is empty.\n");
            return;
        }
        long temp = WQ;
        while (temp != ENDOFLIST)
        {
            printPCB(temp);
            temp = RAM[(int)temp + NEXT_PCB_PTR_REF];
        }
    }

    //************************************************************
    // Function: memoryAllocationTest
    //
    //  DESIGNED FOR HW2
    //
    // Task Description:
    // 	Test the memory allocation and deallocation functions
    //
    // Input Parameters
    //	None
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    private void memoryAllocationTest()
    {
        long ptr;
        write("\nUSER FREE LIST:\n");

        // CASE 1: allocate entire first block and free it
        dumpList(UserFreeList);     // Printing initial list

        // 1B: allocate memory of first block size
        write("CASE 1: allocate entire first block (size 100)\n");
        ptr = allocateUserMemory(100);    // try to allocate 100 block
        if (ptr == ERROR_NO_FREE_MEMORY)
            write("ERROR: NO FREE MEMORY. ALLOCATION FAILED.\n");
        else
            write("Successfully allocated block at address [" + ptr + "] size: 100\n");
        dumpList(UserFreeList);     // print resulting list

        // 1C: Free allocated memory, enter it in free list
        write("freeing allocated block at [2000]...\n");     // try to free the 100 block
        if (FreeUserMemory(2000, 100) == ERROR_INVALID_ADDRESS)
            write("ERROR: INVALID ADDRESS. COULD NOT FREE.\n");
        else
            write("SUCCESSFULLY FREED BLOCK.\n");
        dumpList(UserFreeList);     // print resulting list


        // CASE 2:  Allocate part of the first free block and free it
        // 2B: Allocate half of first block (size 50)
        write("CASE 2: allocate part of first block and free it\n");       // try to allocate 50 block
        ptr = allocateUserMemory(50);
        if (ptr == ERROR_NO_FREE_MEMORY)
            write("ERROR: NO FREE MEMORY. ALLOCATION FAILED.\n");
        else
            write("Successfully allocated block at address [" + ptr + "] size: 50\n");
        dumpList(UserFreeList);     // print resulting list

        // 2C: Free allocated block
        write("freeing allocated block at [2000]...\n");     // try to free the 50 block
        if (FreeUserMemory(2000, 50) == ERROR_INVALID_ADDRESS)
            write("ERROR: INVALID ADDRESS. COULD NOT FREE.\n");
        else
            write("SUCCESSFULLY FREED BLOCK.\n");
        dumpList(UserFreeList);     // print resulting list

        // CASE 3: Allocate part of free block in middle of list and free it
        // 3B: allocate half of 3rd block
        write("CASE 3: allocate part of middle block\n");       // try to allocate too-large block, should fail
        ptr = allocateUserMemory(150);
        if (ptr == ERROR_NO_FREE_MEMORY)
            write("ERROR: NO FREE MEMORY. ALLOCATION FAILED.\n");
        else
            write("Successfully allocated block at address [" + ptr + "] size: 150\n");
        dumpList(UserFreeList);     // display resulting list

        // 3C: free the block
        write("freeing allocated block...\n");        // try to free 150 block
        if (FreeUserMemory(2100, 150) == ERROR_INVALID_ADDRESS)
            write("ERROR: INVALID ADDRESS. COULD NOT FREE.\n");
        else
            write("SUCCESSFULLY FREED BLOCK.\n");
        dumpList(UserFreeList);     // display resulting list

        // CASE 4: allocate entire block from middle and free it
        // 4B: allocate entire block from middle
        write("CASE 4: allocate entire middle block\n");       // try to allocate 300 block
        ptr = allocateUserMemory(300);
        if (ptr == ERROR_NO_FREE_MEMORY)
            write("ERROR: NO FREE MEMORY. ALLOCATION FAILED.\n");
        else
            write("Successfully allocated block at address [" + ptr + "] size: 300\n");
        dumpList(UserFreeList);     // print resulting list

        // 4C: free block
        write("freeing allocated block...\n");        // try to free 100 block
        if (FreeUserMemory(2300, 300) == ERROR_INVALID_ADDRESS)
            write("ERROR: INVALID ADDRESS. COULD NOT FREE.\n");
        else
            write("SUCCESSFULLY FREED BLOCK.\n");
        dumpList(UserFreeList);     // display resulting list

        // CASE 5: allocate part of last free block and free it
        // 5B: allocate part of last block
        write("CASE 5: allocate part of last free block\n");       // try to allocate 300 block
        ptr = allocateUserMemory(350);
        if (ptr == ERROR_NO_FREE_MEMORY)
            write("ERROR: NO FREE MEMORY. ALLOCATION FAILED.\n");
        else
            write("Successfully allocated block at address [" + ptr + "] size: 350\n");
        dumpList(UserFreeList);     // print resulting list

        // 5C: free part of last block
        write("freeing allocated block...\n");        // try to free 100 block
        if (FreeUserMemory(2600, 350) == ERROR_INVALID_ADDRESS)
            write("ERROR: INVALID ADDRESS. COULD NOT FREE.\n");
        else
            write("SUCCESSFULLY FREED BLOCK.\n");
        dumpList(UserFreeList);     // display resulting list

        // CASE 6: allocate entire block and free it
        // 6B: allocate entire last block
        write("CASE 6: allocate entire block\n");       // try to allocate 300 block
        ptr = allocateUserMemory(350);
        if (ptr == ERROR_NO_FREE_MEMORY)
            write("ERROR: NO FREE MEMORY. ALLOCATION FAILED.\n");
        else
            write("Successfully allocated block at address [" + ptr + "] size: 350\n");
        dumpList(UserFreeList);     // print resulting list

        // 6C: free entire block
        write("freeing allocated block...\n");        // try to free 100 block
        if (FreeUserMemory(2600, 350) == ERROR_INVALID_ADDRESS)
            write("ERROR: INVALID ADDRESS. COULD NOT FREE.\n");
        else
            write("SUCCESSFULLY FREED BLOCK.\n");
        dumpList(UserFreeList);     // display resulting list

        // CASE 7: allocate block bigger than all free blocks
        // 7B: try to allocate too big block
        write("CASE 7: allocate too big block\n");       // try to allocate 300 block
        ptr = allocateUserMemory(500);
        if (ptr == ERROR_NO_FREE_MEMORY)
            write("ERROR: NO FREE MEMORY. ALLOCATION FAILED.\n");
        else
            write("Successfully allocated block at address [" + ptr + "] size: 500\n");
        dumpList(UserFreeList);     // print resulting list

        // OS FREE LIST
        // CASE 1: allocate entire first block and free it
        write("OS FREE LIST\n");
        dumpList(OSFreeList);     // Printing initial list

        // 1B: allocate memory of first block size
        write("CASE 1: allocate entire first block (size 100)\n");
        ptr = allocateOSMemory(100);    // try to allocate 100 block
        if (ptr == ERROR_NO_FREE_MEMORY)
            write("ERROR: NO FREE MEMORY. ALLOCATION FAILED.\n");
        else
            write("Successfully allocated block at address [" + ptr + "] size: 100\n");
        dumpList(OSFreeList);     // print resulting list

        // 1C: Free allocated memory, enter it in free list
        write("freeing allocated block at [2000]...\n");     // try to free the 100 block
        if (FreeOSMemory(1000, 100) == ERROR_INVALID_ADDRESS)
            write("ERROR: INVALID ADDRESS. COULD NOT FREE.\n");
        else
            write("SUCCESSFULLY FREED BLOCK.\n");
        dumpList(OSFreeList);     // print resulting list


        // CASE 2:  Allocate part of the first free block and free it
        // 2B: Allocate half of first block (size 50)
        write("CASE 2: allocate part of first block and free it\n");       // try to allocate 50 block
        ptr = allocateOSMemory(50);
        if (ptr == ERROR_NO_FREE_MEMORY)
            write("ERROR: NO FREE MEMORY. ALLOCATION FAILED.\n");
        else
            write("Successfully allocated block at address [" + ptr + "] size: 50\n");
        dumpList(OSFreeList);     // print resulting list

        // 2C: Free allocated block
        write("freeing allocated block at [2000]...\n");     // try to free the 50 block
        if (FreeOSMemory(1000, 50) == ERROR_INVALID_ADDRESS)
            write("ERROR: INVALID ADDRESS. COULD NOT FREE.\n");
        else
            write("SUCCESSFULLY FREED BLOCK.\n");
        dumpList(OSFreeList);     // print resulting list

        // CASE 3: Allocate part of free block in middle of list and free it
        // 3B: allocate half of 3rd block
        write("CASE 3: allocate part of middle block\n");       // try to allocate too-large block, should fail
        ptr = allocateOSMemory(150);
        if (ptr == ERROR_NO_FREE_MEMORY)
            write("ERROR: NO FREE MEMORY. ALLOCATION FAILED.\n");
        else
            write("Successfully allocated block at address [" + ptr + "] size: 150\n");
        dumpList(OSFreeList);     // display resulting list

        // 3C: free the block
        write("freeing allocated block...\n");        // try to free 150 block
        if (FreeOSMemory(1100, 150) == ERROR_INVALID_ADDRESS)
            write("ERROR: INVALID ADDRESS. COULD NOT FREE.\n");
        else
            write("SUCCESSFULLY FREED BLOCK.\n");
        dumpList(OSFreeList);     // display resulting list

        // CASE 4: allocate entire block from middle and free it
        // 4B: allocate entire block from middle
        write("CASE 4: allocate entire middle block\n");       // try to allocate 300 block
        ptr = allocateOSMemory(300);
        if (ptr == ERROR_NO_FREE_MEMORY)
            write("ERROR: NO FREE MEMORY. ALLOCATION FAILED.\n");
        else
            write("Successfully allocated block at address [" + ptr + "] size: 300\n");
        dumpList(OSFreeList);     // print resulting list

        // 4C: free block
        write("freeing allocated block...\n");        // try to free 100 block
        if (FreeOSMemory(1300, 300) == ERROR_INVALID_ADDRESS)
            write("ERROR: INVALID ADDRESS. COULD NOT FREE.\n");
        else
            write("SUCCESSFULLY FREED BLOCK.\n");
        dumpList(OSFreeList);     // display resulting list

        // CASE 5: allocate part of last free block and free it
        // 5B: allocate part of last block
        write("CASE 5: allocate part of last free block\n");       // try to allocate 300 block
        ptr = allocateOSMemory(350);
        if (ptr == ERROR_NO_FREE_MEMORY)
            write("ERROR: NO FREE MEMORY. ALLOCATION FAILED.\n");
        else
            write("Successfully allocated block at address [" + ptr + "] size: 350\n");
        dumpList(OSFreeList);     // print resulting list

        // 5C: free part of last block
        write("freeing allocated block...\n");        // try to free 100 block
        if (FreeOSMemory(1600, 350) == ERROR_INVALID_ADDRESS)
            write("ERROR: INVALID ADDRESS. COULD NOT FREE.\n");
        else
            write("SUCCESSFULLY FREED BLOCK.\n");
        dumpList(OSFreeList);     // display resulting list

        // CASE 6: allocate entire block and free it
        // 6B: allocate entire block
        write("CASE 6: allocate entire block\n");       // try to allocate 300 block
        ptr = allocateOSMemory(350);
        if (ptr == ERROR_NO_FREE_MEMORY)
            write("ERROR: NO FREE MEMORY. ALLOCATION FAILED.\n");
        else
            write("Successfully allocated block at address [" + ptr + "] size: 350\n");
        dumpList(OSFreeList);     // print resulting list

        // 6C: free entire block
        write("freeing allocated block...\n");        // try to free 100 block
        if (FreeOSMemory(1600, 350) == ERROR_INVALID_ADDRESS)
            write("ERROR: INVALID ADDRESS. COULD NOT FREE.\n");
        else
            write("SUCCESSFULLY FREED BLOCK.\n");
        dumpList(OSFreeList);     // display resulting list

        // CASE 7: allocate block bigger than all free blocks
        // 7B: try to allocate too big block
        write("CASE 7: allocate too big block\n");       // try to allocate 300 block
        ptr = allocateOSMemory(500);
        if (ptr == ERROR_NO_FREE_MEMORY)
            write("ERROR: NO FREE MEMORY. ALLOCATION FAILED.\n");
        else
            write("Successfully allocated block at address [" + ptr + "] size: 500\n");
        dumpList(OSFreeList);     // print resulting list


    }

    // ************************************************************
    // Function: createProcess
    //
    // Task Description:
    // 	Create a new process with the given filename and priority
    //
    // Input Parameters
    //	filename		Name of the file to load into memory
    //	priority		Priority of the process
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	ERROR_FILE_OPEN			Unable to open the file
    //	ERROR_INVALID_ADDRESS	Invalid memory address
    //	ERROR_FILE_READ			Unable to read the file
    //	ERROR_INVALID_FORMAT	File formatted improperly
    //	ERROR_NO_EOP			No end of program line
    //	ERROR_NO_FREE_MEMORY	No free memory available
    //	ERROR_INVALID_SIZE		Invalid size
    //	0 to MEMORY_SIZE-1		Valid address of the allocated block
    // ************************************************************
    public long createProcess(String filename, long priority)
    {
        // allocate space for process control block
        long PCBptr = allocateOSMemory(22);    // Allocate memory for the PCB
        if (PCBptr == ERROR_NO_FREE_MEMORY)    // If there is no free memory, return an error code
        {
            write("ERROR: NO FREE MEMORY. UNABLE TO CREATE PROCESS.\n");
            return ERROR_NO_FREE_MEMORY;
        }

        long value = AbsoluteLoader(filename);
        if (value < 0)    // If the value is less than 0, return an error code
        {
            write("ERROR: UNABLE TO LOAD FILE.\n");
            // Free the allocated PCB memory
            FreeOSMemory(PCBptr, 22);
            return value;
        }
        
        // initialize pcb only after successful file loading
        initializePCB(PCBptr);

        RAM[(int)PCBptr + PC_REF] = value;    // Set the program counter to the value

        // temporarily set stack size to 10;
        long StackSize = 10;
        long ptr = allocateUserMemory(StackSize);    // Allocate memory for the stack
        if (ptr < 0)
        {
            FreeUserMemory(PCBptr, 22);    // Free the memory
            write("ERROR: UNABLE TO ALLOCATE MEMORY FOR STACK.\n");
            return ptr;     // return error code
        }

        // store stack information in the PCB - sp, ptr, size
        // should this be -1?
        RAM[(int)PCBptr + SP_REF] = ptr + StackSize;    // Set the stack pointer to the pointer plus the stack size minus 1
        RAM[(int)PCBptr + STACK_START_ADDRESS_REF] = ptr;    // Set the stack start address to the pointer
        RAM[(int)PCBptr + STACK_SIZE_REF] = StackSize;    // Set the stack size to the stack size

        RAM[(int)PCBptr + PRIORITY_REF] = priority;   // Set the priority to the given priority
        write("Process created. Printing pcb...\n");
        printPCB(PCBptr);

        // insert PCB into ready queue according to scheduling algorith,
        insertIntoRQ(PCBptr);

        return OK;
    }   // end createProcess

    // ************************************************************
    // Function: printPCB
    //
    // Task Description:
    // 	Prints the contents of a Process Control Block at the specified address.
    // 	Displays PCB address, next PCB pointer, PID, state, PC, SP, priority,
    //  stack information, and general purpose register values.
    //
    // Input Parameters
    //	PCBptr     Address of the PCB to print
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    public void printPCB(long PCBptr)
    {
        write(String.format(" PCB ADDRESS = [" + PCBptr + "], "));
        write(String.format("Next PCB ptr = " + RAM[(int)PCBptr + NEXT_PCB_PTR_REF] + ", "));
        write(String.format("PID = " + RAM[(int)PCBptr + PID_REF] + ", "));
        write(String.format("State = " + RAM[(int)PCBptr + STATE_REF] + ", "));
        write(String.format("PC = " + RAM[(int)PCBptr + PC_REF] + ", "));
        write(String.format("SP = " + RAM[(int)PCBptr + SP_REF] + ", "));
        write(String.format("Priority = " + RAM[(int)PCBptr + PRIORITY_REF] + ", "));
        write(String.format("Stack info: start address = " + RAM[(int)PCBptr + STACK_START_ADDRESS_REF] + ", "));
        write(String.format("size = " + RAM[(int)PCBptr + STACK_SIZE_REF] + "\n"));

        write("\tGPRs: ");
        write("GPR0 = " + RAM[(int)PCBptr + GPR0_REF] + ", ");
        write("GPR1 = " + RAM[(int)PCBptr + GPR1_REF] + ", ");
        write("GPR2 = " + RAM[(int)PCBptr + GPR2_REF] + ", ");
        write("GPR3 = " + RAM[(int)PCBptr + GPR3_REF] + ", ");
        write("GPR4 = " + RAM[(int)PCBptr + GPR4_REF] + ", ");
        write("GPR5 = " + RAM[(int)PCBptr + GPR5_REF] + ", ");
        write("GPR6 = " + RAM[(int)PCBptr + GPR6_REF] + ", ");
        write("GPR7 = " + RAM[(int)PCBptr + GPR7_REF] + "\n");
    }   // end printPCB

    // ************************************************************
    // Function: insertIntoRQ
    //
    // Task Description:
    // 	Inserts a PCB into the Ready Queue (RQ) according to priority-based scheduling.
    // 	Higher priority PCBs are inserted before lower priority ones.
    //  If priorities are equal, new PCBs are inserted at the end of their priority group.
    //
    // Input Parameters
    //	PCBptr     Address of the PCB to insert into the Ready Queue
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	OK                     If insertion is successful
    //  ERROR_INVALID_ADDRESS  If PCB address is invalid
    // ************************************************************
    long insertIntoRQ(long PCBptr) {
        // insert pcb according to priority round robin algorithm
        // use priority in the pcb to find the correct place to insert
        long previousPtr = ENDOFLIST;
        long currentPtr = RQ;

        if (PCBptr < 0 || PCBptr > MEMORY_SIZE) {
            write("ERROR: INVALID ADDRESS. UNABLE TO INSERT INTO READY QUEUE.\n");
            return ERROR_INVALID_ADDRESS;
        }

        RAM[(int)PCBptr + STATE_REF] = ReadyState;
        RAM[(int)PCBptr + NEXT_PCB_PTR_REF] = ENDOFLIST;

        if (RQ == ENDOFLIST) {
            RQ = PCBptr;
            return OK;
        }

        // else, walk through RQ and find place to insert
        // PCB will be inserted at end of its priortiy
        while (currentPtr != ENDOFLIST)
        {
            if (RAM[(int) PCBptr + PRIORITY_REF] > RAM[(int) currentPtr + PRIORITY_REF])
            {
                if (previousPtr == ENDOFLIST)
                {
                    // enter pcb in front of list as first entry
                    RAM[(int) PCBptr + NEXT_PCB_PTR_REF] = RQ;
                    RQ = PCBptr;
                    return OK;
                }
                else
                {
                    RAM[(int)previousPtr + NEXT_PCB_PTR_REF] = PCBptr;    // set next ptr of previous to new pcb
                    RAM[(int)PCBptr + NEXT_PCB_PTR_REF] = currentPtr;    // set next ptr of new pcb to current
                    return OK;
                }
            }
            else // PCB to be inserted has lower or equal priority
            {
                    // go to next pcb in rq
                    previousPtr = currentPtr;
                    currentPtr = RAM[(int) currentPtr + NEXT_PCB_PTR_REF];
            }
        }   // end of while loop

        // insert at end of rq
        RAM[(int) previousPtr + NEXT_PCB_PTR_REF] = PCBptr;
        return OK;
    }   // end of insertIntoRQ

    // ************************************************************
    // Function: printGPRs
    //
    // Task Description:
    // 	Prints the values of all general purpose registers (GPR0-GPR7)
    // 	to the output.
    //
    // Input Parameters
    //	None
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    public void printGPRs()
    {
        write("GPR0 = " + gpr[0] + " ");
        write("GPR1 = " + gpr[1] + " ");
        write("GPR2 = " + gpr[2] + " ");
        write("GPR3 = " + gpr[3] + " ");
        write("GPR4 = " + gpr[4] + " ");
        write("GPR5 = " + gpr[5] + " ");
        write("GPR6 = " + gpr[6] + " ");
        write("GPR7 = " + gpr[7] + "\n");

    }

    public void initializePCB(long ptr)
    {
        RAM[(int)ptr + NEXT_PCB_PTR_REF] = ENDOFLIST;
        RAM[(int)ptr + PID_REF] = ProcessID++;
        RAM[(int)ptr + STATE_REF] = ReadyState;
        RAM[(int)ptr + REASON_REF] = 0;
        RAM[(int)ptr + PRIORITY_REF] = DEFAULTPRIORITY;
        RAM[(int)ptr + STACK_START_ADDRESS_REF] = 0;
        RAM[(int)ptr + STACK_SIZE_REF] = 0;
        RAM[(int)ptr + MESSAGE_QUEUE_StART_REF] = 0;
        RAM[(int)ptr + MESSAGE_QUEUE_SIZE_REF] = 0;
        RAM[(int)ptr + NUM_MESSAGES_REF] = 0;
        RAM[(int)ptr + GPR0_REF] = 0;
        RAM[(int)ptr + GPR1_REF] = 0;
        RAM[(int)ptr + GPR2_REF] = 0;
        RAM[(int)ptr + GPR3_REF] = 0;
        RAM[(int)ptr + GPR4_REF] = 0;
        RAM[(int)ptr + GPR5_REF] = 0;
        RAM[(int)ptr + GPR6_REF] = 0;
        RAM[(int)ptr + GPR7_REF] = 0;
        RAM[(int)ptr + SP_REF] = 0;
        RAM[(int)ptr + PC_REF] = 0;
        RAM[(int)ptr + PSR_REF] = 0;
    }   // end initializePCB

    public long printQueue(long Qptr)
    {
        long currentPCBptr = Qptr;
        if (currentPCBptr == ENDOFLIST)
        {
            write("Queue is empty.\n");
            return OK;
        }

        while (currentPCBptr != ENDOFLIST)
        {
            printPCB(currentPCBptr);
            currentPCBptr = RAM[(int)currentPCBptr + NEXT_PCB_PTR_REF];
        }
        return OK;
    }   // end printQueue

    // ************************************************************
    // Function: insertIntoWQ
    //
    // Task Description:
    // 	Inserts a PCB into the Wait Queue (WQ).
    // 	The PCB is always inserted at the front of the queue.
    //
    // Input Parameters
    //	PCBptr     Address of the PCB to insert into the Wait Queue
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	OK                     If insertion is successful
    //  ERROR_INVALID_ADDRESS  If PCB address is invalid
    // ************************************************************
    long insertIntoWQ(long PCBptr)
    {
          // insert given pcb at front of wq

        // check for invalid pcb memory address
        if (!isValidAddress(PCBptr)) {
            write("ERROR: INVALID ADDRESS. UNABLE TO INSERT INTO WAIT QUEUE.\n");
            return ERROR_INVALID_ADDRESS;
        }

        RAM[(int)PCBptr + STATE_REF] = WaitingState;    // set state to waiting
        RAM[(int)PCBptr + NEXT_PCB_PTR_REF] = WQ;    // set next pcb ptr to RQ

        WQ = PCBptr;
        return OK;
    }

    // ************************************************************
    // Function: selectProcessFromRQ
    //
    // Task Description:
    // 	Selects and removes the first process from the Ready Queue (RQ).
    // 	Returns the address of the selected PCB, or ENDOFLIST if the queue is empty.
    //
    // Input Parameters
    //	None
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	PCBptr    Address of the selected PCB
    //  ENDOFLIST If the Ready Queue is empty
    // ************************************************************
    long selectProcessFromRQ()
    {
        long PCBptr = RQ;       // first entry in RQ
        if (RQ != ENDOFLIST)
        {
            //remove first PCB from RQ;
            RQ = RAM[(int)RQ + NEXT_PCB_PTR_REF];
            RAM[(int)PCBptr + NEXT_PCB_PTR_REF] = ENDOFLIST;
        }
        return PCBptr;
    }

    // ************************************************************
    // Function: saveContext
    //
    // Task Description:
    // 	Saves the current CPU context (general purpose registers, SP, PC)
    //  into the specified PCB. Used when switching processes.
    //
    // Input Parameters
    //	PCBptr    Address of the PCB where the context should be saved
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    void saveContext(long PCBptr)
    {
        // Validate PCB pointer is valid
        if (PCBptr < 0 || PCBptr >= MEMORY_SIZE - 21) {
            write("Error: Invalid PCB pointer in saveContext: " + PCBptr + "\n");
            return;
        }

        for (int i = 0; i < NUM_REGISTERS; i++)
        {
            // Additional safety check for array bounds
            if (i < 0 || i >= NUM_REGISTERS) {
                continue; // Skip invalid indices (shouldn't happen with loop condition)
            }
            RAM[(int)PCBptr + GPR0_REF + i] = gpr[i];
        }

        RAM[(int)PCBptr + SP_REF] = sp;
        RAM[(int)PCBptr + PC_REF] = pc;
    }

    // ************************************************************
    // Function: dispatcher
    //
    // Task Description:
    // 	Restores CPU context (general purpose registers, SP, PC) from the
    //  specified PCB and sets PSR to UserMode. Used when dispatching a process.
    //
    // Input Parameters
    //	PCBptr    Address of the PCB from which to restore context
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    void dispatcher(long PCBptr)
    {
        // Validate PCB pointer is valid
        if (PCBptr < 0 || PCBptr >= MEMORY_SIZE - 21) {
            write("Error: Invalid PCB pointer in dispatcher: " + PCBptr + "\n");
            return;
        }

        for (int i = 0; i < NUM_REGISTERS; i++)
        {
            // Additional safety check for array bounds
            if (i < 0 || i >= NUM_REGISTERS) {
                continue; // Skip invalid indices (shouldn't happen with loop condition)
            }
            gpr[i] = RAM[(int)PCBptr + GPR0_REF + i];
        }

        sp = RAM[(int)PCBptr + SP_REF];
        pc = RAM[(int)PCBptr + PC_REF];

        psr = UserMode;
    }

    // ************************************************************
    // Function: terminateProcess
    //
    // Task Description:
    // 	Terminates a process by freeing its stack memory and PCB memory.
    // 	This function is called when a process completes execution or
    //  encounters a fatal error.
    //
    // Input Parameters
    //	PCBptr    Address of the PCB for the process to terminate
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    void terminateProcess(long PCBptr)
    {
        FreeUserMemory(RAM[(int)PCBptr + STACK_START_ADDRESS_REF], RAM[(int)PCBptr + STACK_SIZE_REF]);
        FreeOSMemory(PCBptr, 22);
    }

    // ************************************************************
    // Function: checkAndProcessInterrupt
    //
    // Task Description:
    // 	Prompts for and processes system interrupts.
    // 	Displays a menu of possible interrupts, reads the user's choice,
    //  and processes the selected interrupt by calling the appropriate
    //  interrupt service routine (ISR).
    //
    // Input Parameters
    //	None
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    void checkAndProcessInterrupt() {
        Scanner input = new Scanner(System.in);
        boolean validInput = false;
        long interruptID = 0;
        String inputStr = "";
        
        while (!validInput) {
            // prompt and read interrupt id
            write("Possible interrupts: \n");
            write("0 - no interrupt\n");
            write("1 - run program\n");
            write("2 - shutdown system\n");
            write("3 - input operation completion (io_getc)\n");
            write("4 - output operation completion (io_putc)\n");
            write("Enter interrupt...\n");
            
            try {
                inputStr = input.next();
                write(inputStr + "\n");
                
                try {
                    interruptID = Long.parseLong(inputStr);
                    
                    // Always write the interrupt ID regardless of validity
                    write("Interrupt ID: " + interruptID + "\n");
                    
                    // Check if the interrupt ID is valid (0-4)
                    if (interruptID >= 0 && interruptID <= 4) {
                        validInput = true;
                    } else {
                        write("Invalid interrupt ID. Please enter a number between 0 and 4.\n");
                        input.nextLine(); // Clear the input buffer
                    }
                } catch (NumberFormatException nfe) {
                    // Handle non-numeric input
                    write("Interrupt ID: " + inputStr + "\n");
                    write("Invalid input: Please enter a numeric value only.\n");
                    input.nextLine(); // Clear the input buffer
                }
            } catch (InputMismatchException e) {
                // Handle other input errors
                write("Invalid input: Please enter a valid value.\n");
                input.nextLine(); // Clear the invalid input from the scanner
            }
        }

        // process interrupt (now we have a valid interruptID)
        switch ((int) interruptID) {
            case 0: // no interrupt
                break;
            case 1: // run system
                ISRRunProgramInterrupt();
                break;
            case 2: // shutdown system
                ISRShutdownSystem();
                ShutdownStatus = true;
                break;
            case 3: // input operation completion
                ISRInputCompletionInterrupt();
                break;
            case 4: // output operation completion
                ISROutputCompletionInterrupt();
                break;
        }
    }   // end checkAndProcessInterrupt

    // ************************************************************
    // Function: ISRRunProgramInterrupt
    //
    // Task Description:
    // 	Interrupt service routine for running a new program.
    // 	Prompts for a program filename, then creates a new process
    //  to execute that program with default priority.
    //
    // Input Parameters
    //	None
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    void ISRRunProgramInterrupt()
    {
        Scanner input = new Scanner(System.in);
        write("Enter the name of the program file to load: \n");
        
        try {
            String filename = input.nextLine();   // Get the name of the program file
            if (filename == null || filename.trim().isEmpty()) {
                write("Invalid input: Filename cannot be empty.\n");
                return;
            }
            write(filename + "\n");
            createProcess(filename, DEFAULTPRIORITY);   // Create a new process with the given filename and default priority
        } catch (Exception e) {
            write("Error reading filename: " + e.getMessage() + "\n");
            return;
        }
    }   // end ISRRunProgramInterrupt

    // ************************************************************
    // Function: ISRInputCompletionInterrupt
    //
    // Task Description:
    // 	Interrupt service routine for handling input completion events.
    // 	Prompts for a PID, searches for the process in the Wait Queue,
    //  reads a character input, stores it in GPR1 of the process, and
    //  moves the process to the Ready Queue. If not found in WQ, searches
    //  in RQ and processes similarly.
    //
    // Input Parameters
    //	None
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    private void ISRInputCompletionInterrupt() {
        Scanner input = new Scanner(System.in);
        write("Enter PID of process: \n");
        long PID = 0;
        String inputStr = "";
        try {
            inputStr = input.next();
            write(inputStr + "\n");
            
            try {
                PID = Long.parseLong(inputStr);
                write("PID: " + PID + "\n");
            } catch (NumberFormatException nfe) {
                // Handle non-numeric input
                write("PID: " + inputStr + "\n");
                write("Invalid input: Please enter a numeric PID value only.\n");
                input.nextLine(); // Clear the input buffer
                return; // Exit the method
            }
        } catch (InputMismatchException e) {
            // Handle other input errors
            write("Invalid input: Please enter a valid value.\n");
            input.nextLine(); // Clear the invalid input from the scanner
            return; // Exit the method
        }

        // search WQ
        long currentPtr = searchAndRemovePCBfromWQ(PID);
        if (currentPtr != ENDOFLIST) {
            write("Enter character: \n");
            try {
                char c = input.next().charAt(0);
                write(String.format(c + "\n"));
                RAM[(int) currentPtr + GPR1_REF] = c;   // don't have to type cast because its redundant
                RAM[(int) currentPtr + STATE_REF] = ReadyState;
                insertIntoRQ(currentPtr);
            } catch (Exception e) {
                // Handle invalid character input
                write("Invalid input: Please enter a valid character.\n");
                input.nextLine(); // Clear the invalid input
                
                // Return process to WQ since input failed
                RAM[(int) currentPtr + STATE_REF] = WaitingState;
                RAM[(int) currentPtr + REASON_REF] = INPUT_COMPLETION_EVENT;
                insertIntoWQ(currentPtr);
            }
        }
        else
        {
            write("Not in WQ. Searching RQ.\n");
            currentPtr = searchRQ(PID);
            if (currentPtr == ENDOFLIST)
            {
                write("ERROR: INVALID PID: PID NOT FOUND IN WQ OR RQ.\n");
            }
            else
            {
                write("Found process with pid: " + PID + "at address: " + currentPtr + "\n");
                write(String.format("For clarification, PID equals: " + PID + "\n"));
                write(String.format("and in pcb, PID equals: " + RAM[(int)currentPtr + PID_REF] + "\n"));
                write("Enter character: \n");
                try {
                    char c = input.next().charAt(0);
                    RAM[(int) currentPtr + GPR1_REF] = (long) c;
                    write(String.format("GPR1 set to: " + (char)RAM[(int)currentPtr + GPR1_REF] + " for process: " + RAM[(int)currentPtr + PID_REF] + "\n"));
                    RAM[(int) currentPtr + STATE_REF] = ReadyState;
                } catch (Exception e) {
                    // Handle invalid character input
                    write("Invalid input: Please enter a valid character.\n");
                    input.nextLine(); // Clear the invalid input
                }
            }
        }
    }

    // ************************************************************
    // Function: ISROutputCompletionInterrupt
    //
    // Task Description:
    // 	Interrupt service routine for handling output completion events.
    // 	Prompts for a PID, searches for the process in the Wait Queue,
    //  displays the character stored in GPR1 of the process, and moves
    //  the process to the Ready Queue. If not found in WQ, searches in RQ
    //  and displays the character.
    //
    // Input Parameters
    //	None
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    public void ISROutputCompletionInterrupt() {
        Scanner input = new Scanner(System.in);
        write("Enter PID of process: \n");
        long PID;
        String inputStr = "";
        try {
            inputStr = input.next();
            write(inputStr + "\n");
            
            try {
                PID = Long.parseLong(inputStr);
                write("PID: " + PID + "\n");
            } catch (NumberFormatException nfe) {
                // Handle non-numeric input
                write("PID: " + inputStr + "\n");
                write("Invalid input: Please enter a numeric PID value only.\n");
                input.nextLine(); // Clear the input buffer
                return; // Exit the method
            }
        } catch (InputMismatchException e) {
            // Handle other input errors
            write("Invalid input: Please enter a valid value.\n");
            input.nextLine(); // Clear the invalid input from the scanner
            return; // Exit the method
        }

        // search WQ
        long currentPtr = searchAndRemovePCBfromWQ(PID);
        if (currentPtr != ENDOFLIST) {
            write(String.format("Character in GPR1: " + (char)RAM[(int)currentPtr + GPR1_REF] + "\n"));
            RAM[(int) currentPtr + STATE_REF] = ReadyState;
            insertIntoRQ(currentPtr);
        }
        else
        {
            currentPtr = searchRQ(PID);
            if (currentPtr == ENDOFLIST)
            {
                write("ERROR: INVALID PID: PID NOT FOUND IN WQ OR RQ.\n");
            }
            else
            {
                write(String.format("Character in GPR1: " + (char)RAM[(int)currentPtr + GPR1_REF] + "\n"));
            }
        }
    }

    // ************************************************************
    // Function: searchRQ
    //
    // Task Description:
    // 	Searches the Ready Queue (RQ) for a PCB with the specified PID.
    // 	Returns the address of the found PCB or ENDOFLIST if not found.
    //
    // Input Parameters
    //	PID      Process ID to search for
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	PCBptr    Address of the found PCB
    //  ENDOFLIST If no PCB with the given PID is found
    // ************************************************************
    private long searchRQ(long PID) {
        long currentPtr = RQ;
        while (currentPtr != ENDOFLIST)
        {
            if (RAM[(int)currentPtr + PID_REF] == PID)
            {
                break;
            }
            currentPtr = RAM[(int)currentPtr + NEXT_PCB_PTR_REF];
        }
        return currentPtr;
    }

    // ************************************************************
    // Function: ISRShutdownSystem
    //
    // Task Description:
    // 	Interrupt service routine for system shutdown.
    // 	Terminates all processes in both the Ready Queue and Wait Queue,
    //  freeing their resources. This prepares the system for shutdown.
    //
    // Input Parameters
    //	None
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    private void ISRShutdownSystem() {

        // terminate all processes in RQ one by one
        long ptr = RQ;
        while (ptr != ENDOFLIST)
        {
            RQ = RAM[(int)ptr + NEXT_PCB_PTR_REF];
            terminateProcess(ptr);
            ptr = RQ;
        }

        ptr = WQ;
        while (ptr != ENDOFLIST)
        {
            WQ = RAM[(int)ptr + NEXT_PCB_PTR_REF];
            terminateProcess(ptr);
            ptr = WQ;
        }
    }   // end ISRShutdownSystem

    // ************************************************************
    // Function: searchAndRemovePCBfromWQ
    //
    // Task Description:
    // 	Searches for a PCB with the specified PID in the Wait Queue (WQ)
    //  and removes it from the queue if found. Returns the address of the
    //  found PCB or ENDOFLIST if not found.
    //
    // Input Parameters
    //	pid      Process ID to search for
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	PCBptr    Address of the found and removed PCB
    //  ENDOFLIST If no PCB with the given PID is found
    // ************************************************************
    private long searchAndRemovePCBfromWQ(long pid) {

        long currentPCBptr = WQ;
        long previousPCBptr = ENDOFLIST;

        while (currentPCBptr != ENDOFLIST)
        {
            if (RAM[(int)currentPCBptr + PID_REF] == pid)
            {
                // remove from wq
                if (previousPCBptr == ENDOFLIST)
                {
                    WQ = RAM[(int)currentPCBptr + NEXT_PCB_PTR_REF];
                }
                else
                {
                    RAM[(int)previousPCBptr + NEXT_PCB_PTR_REF] = RAM[(int)currentPCBptr + NEXT_PCB_PTR_REF];
                }
                RAM[(int)currentPCBptr + NEXT_PCB_PTR_REF] = ENDOFLIST;
                return currentPCBptr;
            }
            previousPCBptr = currentPCBptr;
            currentPCBptr = RAM[(int)currentPCBptr + NEXT_PCB_PTR_REF];
        }
        //write(String.format("NO MATCHING PCB FOUND IN WQ: " + pid));
        return ENDOFLIST;
    }   // end searchAndRemovePCBfromWQ





    // ************************************************************
    // Function: allocateUserMemory
    //
    // Task Description:
    // 	Allocate memory for user process, remove block from userFreeList
    // 	and return the address of the allocated block
    //
    // Input Parameters
    //	size		Size of the memory block to allocate
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	ERROR_NO_FREE_MEMORY		No free memory available
    //	ERROR_INVALID_SIZE		Invalid size
    //	ERROR_INVALID_ADDRESS	Invalid address
    //	0 to MEMORY_SIZE-1		Valid address of the allocated block
    // ************************************************************
    public long allocateUserMemory(long size) {
        if (UserFreeList == ENDOFLIST) return ERROR_NO_FREE_MEMORY;
        if (size < 0) return ERROR_INVALID_SIZE;
        if (size == 1) size = 2;

        long currentPtr = UserFreeList;
        long previousPtr = ENDOFLIST;

        while (currentPtr != ENDOFLIST) {
            if (RAM[(int)currentPtr + 1] == size) {
                if (currentPtr == UserFreeList) {
                    UserFreeList = RAM[(int)currentPtr];
                } else {
                    RAM[(int)previousPtr] = RAM[(int)currentPtr];
                }
                RAM[(int)currentPtr] = ENDOFLIST;
                
                // Track this allocated memory block
                if (allocatedBlockCount < MAX_MEMORY_BLOCKS) {
                    allocatedAddresses[allocatedBlockCount] = currentPtr;
                    allocatedSizes[allocatedBlockCount] = size;
                    allocatedBlockCount++;
                }
                
                return currentPtr;
            } else if (RAM[(int)currentPtr + 1] > size) {
                long newBlock = currentPtr + size;
                RAM[(int)newBlock] = RAM[(int)currentPtr];
                RAM[(int)newBlock + 1] = RAM[(int)currentPtr + 1] - size;

                if (currentPtr == UserFreeList) {
                    UserFreeList = newBlock;
                } else {
                    RAM[(int)previousPtr] = newBlock;
                }
                RAM[(int)currentPtr] = ENDOFLIST;
                
                // Track this allocated memory block
                if (allocatedBlockCount < MAX_MEMORY_BLOCKS) {
                    allocatedAddresses[allocatedBlockCount] = currentPtr;
                    allocatedSizes[allocatedBlockCount] = size;
                    allocatedBlockCount++;
                }
                
                return currentPtr;
            }
            previousPtr = currentPtr;
            currentPtr = RAM[(int)currentPtr];
        }
        return ERROR_NO_FREE_MEMORY;
    }

    // ************************************************************
    // Function: FreeUserMemory
    //
    // Task Description:
    // 	Free memory for user process, add block to userFreeList
    // 	and return the address of the freed block
    //
    // Input Parameters
    //	ptr		Address of the memory block to deallocate
    //	size	Size of the memory block to deallocate
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	ERROR_INVALID_ADDRESS	Invalid address
    //	OK						Success
    // ************************************************************
    public long FreeUserMemory(long ptr, long size) {
        if (ptr < USER_LIST_START || ptr > USER_LIST_END)
        {
            return ERROR_INVALID_ADDRESS;
        }
        if (size == 1) size = 2;
        if (size < 1 || ptr + size > USER_LIST_END) return ERROR_INVALID_ADDRESS;
        
        // Remove from tracking if it exists
        for (int i = 0; i < allocatedBlockCount; i++) {
            if (allocatedAddresses[i] == ptr && allocatedSizes[i] == size) {
                // Remove this entry by moving the last entry to this position
                if (i < allocatedBlockCount - 1) {
                    allocatedAddresses[i] = allocatedAddresses[allocatedBlockCount - 1];
                    allocatedSizes[i] = allocatedSizes[allocatedBlockCount - 1];
                }
                allocatedBlockCount--;
                break;
            }
        }

        RAM[(int)ptr] = UserFreeList;
        RAM[(int)ptr + 1] = size;
        UserFreeList = ptr;
        return OK;
    }

    // ************************************************************
    // Function: allocateOSMemory
    //
    // Task Description:
    // 	Allocate memory for OS process, remove block from OSFreeList
    // 	and return the address of the allocated block
    //
    // Input Parameters
    //	size		Size of the memory block to allocate
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	ERROR_NO_FREE_MEMORY		No free memory available
    //	ERROR_INVALID_SIZE		Invalid size
    //	ERROR_INVALID_ADDRESS	Invalid address
    //	0 to MEMORY_SIZE-1		Valid address of the allocated block
    // ************************************************************
    public long allocateOSMemory(long size) {
        if (OSFreeList == ENDOFLIST)
            return ERROR_NO_FREE_MEMORY;
        if (size < 0) {
            return ERROR_INVALID_SIZE;
        }
        if (size == 1)
            size = 2;

        long currentPtr = OSFreeList;
        long previousPtr = ENDOFLIST;

        while (currentPtr != ENDOFLIST) {
            if (RAM[(int) currentPtr + 1] == size)
            {
                if (currentPtr == OSFreeList)
                    OSFreeList = RAM[(int) currentPtr];
                else
                    RAM[(int) previousPtr] = RAM[(int) currentPtr];
                RAM[(int) currentPtr] = ENDOFLIST;
                return currentPtr;
            }
            else if (RAM[(int) currentPtr + 1] > size)
            {
                long newBlock = currentPtr + size;
                RAM[(int) newBlock] = RAM[(int) currentPtr];
                RAM[(int) newBlock + 1] = RAM[(int) currentPtr + 1] - size;

                if (currentPtr == OSFreeList) {
                    OSFreeList = newBlock;
                } else
                    RAM[(int) previousPtr] = newBlock;
                RAM[(int)currentPtr] = ENDOFLIST;
                return currentPtr;
            }
            previousPtr = currentPtr;
            currentPtr = RAM[(int) currentPtr];
        }
        return ERROR_NO_FREE_MEMORY;
    }

    // ************************************************************
    // Function: FreeOSMemory
    //
    // Task Description:
    // 	Free memory for OS process, add block to OSFreeList
    // 	and return the address of the freed block
    //
    // Input Parameters
    //	ptr		Address of the memory block to deallocate
    //	size	Size of the memory block to deallocate
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	ERROR_INVALID_ADDRESS	Invalid address
    //	OK						Success
    // ************************************************************
    long FreeOSMemory(long ptr, long size)
    {
        if (ptr > OS_LIST_END || ptr < OS_LIST_START)
        {
            return ERROR_INVALID_ADDRESS;
        }
        if (size == 1)
        {
            size = 2;
        }
        else if (size < 1 || ptr + size > OS_LIST_END)
        {
            write("Invalid address.\n");
            return ERROR_INVALID_ADDRESS;
        }

        RAM[(int)ptr] = OSFreeList;
        RAM[(int)ptr + 1] = size;
        OSFreeList = ptr;

        return OK;
    }




    // ************************************************************
    // Function: InitializeSystem
    //
    // Task Description:
    // 	Set all global system hardware components to 0
    //
    // Input Parameters
    //	None
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    public void initializeSystem()
    {
        write("Initializing system...\n");

        Arrays.fill(RAM, 0);    // Fill memory with 0
        Arrays.fill(gpr, 0);    // Fill general purpose registers with 0
        MAR = 0;    // Memory Address Register
        MBR = 0;    // Memory Buffer Register
        clock = 0;    // Clock
        IR = 0;    // Instruction Register
        psr = 0;    // Program Status Register
        pc = 0;    // Program Counter
        sp = MEMORY_SIZE - 1;    // Stack Pointer

        initializeLists();      // Create several blocks in UserFreeList and OSFreeList

        createProcess("src/null.txt", 0);
        write("System initialized.\n");
    }

    // ************************************************************
    // Function: initializeLists
    //
    // Task Description:
    // 	Initialize the OS and User free lists
    //
    // Input Parameters
    //	None
    //
    // Output Parameters
    //	None... modifies RAM to set up lists and pointers
    //
    // Function Return Value
    //	void - no return value
    // ************************************************************
    private void initializeLists() {
        OSFreeList = 1000;                  // block 1 at 1000
        RAM[(int)OSFreeList + 1] = 100;     // size = 100

        RAM[(int)OSFreeList] = 1100;        // block 2 at 1100
        RAM[1100 + 1] = 200;                 // size = 200

        RAM[1100] = 1300;                   // block 3 at 1300
        RAM[1300 + 1] = 300;                // size = 300

        RAM[1300] = 1600;                   // block 4 at 1600
        RAM[1600 + 1] = 400;                // size 400

        RAM[1600] = ENDOFLIST;

        UserFreeList = 2000;                  // block 1 at 2000
        RAM[(int)UserFreeList + 1] = 100;     // size = 100

        RAM[(int)UserFreeList] = 2100;        // block 2 at 2100
        RAM[2100 + 1] = 200;                 // size = 200

        RAM[2100] = 2300;                   // block 3 at 2300
        RAM[2300 + 1] = 300;                // size = 300

        RAM[2300] = 2600;                   // block 4 at 2600
        RAM[2600 + 1] = 400;                // size 400

        RAM[2600] = ENDOFLIST;
    }

    // ********************************************************************
    // Function: AbsoluteLoader
    //
    // Task Description:
    // 	Open the file containing HYPO machine user program and
    //	load the content into HYPO memory.
    //	On successful load, return the PC value in the End of Program line.
    //	On failure, display appropriate error message and return appropriate error code
    //
    // Input Parameters
    //	filename			Name of the Hypo Machine executable file
    //
    // Output Parameters
    //	None
    //
    // Function Return Value will be one of the following:
    //	ERROR_FILE_OPEN			    Unable to open the file
    //	ERROR_INVALID_ADDRESS		Invalid address error
    //	ERROR_FILE_READ	            Unable to read the file
    //  ERROR_INVALID_FORMAT        File formatted improperly
    //  ERROR_NO_EOP                No end of program line
    //	0 to Valid address range	Successful Load, valid PC value
    // ************************************************************
    public long AbsoluteLoader(String filename) {
        String s = "Loading file " + filename + "... \n";
        write(s);
        try (BufferedReader reader = new BufferedReader(new FileReader(filename)))    // Try to open the file
        {
            String line;    // Declare line variable

            while ((line = reader.readLine()) != null)        // Read each line of the file while there are still lines
            {
                try
                {
                    String[] parts = line.split("\t");    // Split the line by tabs (which is how
                    // I formatted my machine lang program)
                    if (parts.length < 2) {   // If the length of the parts is less than 2, print an error message
                        s = "Error: Malformed line in program file: " + line + "\n";
                        write(s);   // Print error message
                        return ERROR_INVALID_FORMAT;    // Return error code
                    }

                    int address = Integer.parseInt(parts[0]);
                    long instruction = Long.parseLong(parts[1]);
                    if (address != -1)    // If the address is not -1, store the instruction in memory
                        if (isValidAddress(address)) {
                            RAM[address] = instruction;
                            if (address > maxUsedAddress)
                                maxUsedAddress = address;
                            if (address < minUsedAddress)
                                minUsedAddress = address;
                        }
                        else
                        {
                            write(String.format("Error: Invalid memory address: " + address + "\n"));    // Print error message
                            return ERROR_INVALID_ADDRESS;    // Return error code
                        }
                    else {
                        if (isValidAddress(instruction))    // If the instruction is a valid address, return the instruction
                        {
                            dumpMemory("Program loaded. Dumping memory:\n", minUsedAddress, maxUsedAddress);
                            return instruction;
                        } else    // If the instruction is not a valid address, print an error message and return an error code
                        {
                            write(String.format("Error: Invalid memory address: " + instruction + "\n"));
                            return ERROR_INVALID_ADDRESS;
                        }
                    }
                } catch (NumberFormatException e) {   // Catch number format exception
                    write(String.format("Error: Non-numeric data in file: " + line));   // Print error message
                    return ERROR_INVALID_FORMAT;    // Return error code
                }
            }

        } catch (FileNotFoundException e) {   // Catch file not found exception
            write(String.format("Error: File not found - " + filename + "\n"));
            return ERROR_FILE_OPEN;
        } catch (IOException e) {
            write(String.format("Error: Unable to read file - " + filename + "\n"));
            return ERROR_FILE_READ;
        }
        return ERROR_NO_EOP;    // Return error code
    }

    // ************************************************************
    // Function: CPUexecuteProgram
    //
    // Task Description:
    // 	 Execute the program loaded in memory
    //
    // Input Parameters
    //	None
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	GENERAL_PURPOSE_ERROR   General purpose error
    //  ERROR_INVALID_ADDRESS   Invalid memory address error
    //  ERROR_DIVIDE_BY_ZERO    Division by zero error
    //  HALT_STATUS - Program execution completed
    // ************************************************************
    public long CPU() {

        long timeLeft = TIMESLICE;    // Set time left to the time slice
        int opcode;        // Operation code
        int op1mode;    // Operand 1 mode
        int op1gpr;        // Operand 1 general purpose register
        int op2mode;    // Operand 2 mode
        int op2gpr;        // Operand 2 general purpose register
        int remainder;    // Remainder
        long[] op1data = new long[2];    // Operand 1 data (holds ADDRESS and VALUE)
        // An array is used here so that I can pass by reference into the
        // fetchOperand function and modify the values in the array. Java
        // does not allow for pass by reference on longs
        long[] op2data = new long[2];    // Operand 2 data (for ADDRESS and VALUE)
        // An array is used here so that I can pass by reference into the
        // fetchOperand function and modify the values in the array. Java
        // does not allow for pass by reference on longs
        int OP_ADDRESS_REF = 0;    // Operand address reference (use like - opdata[0])
        int OP_VALUE_REF = 1;    // Operand value reference (use like - opdata[1])
        long status;    // Status of fetchOperand function
        long result;    // Result of the operation


        boolean running = true;    // Set running to true for loop
        while (running)    // While running is true
        {
            //System.out.println("PC: " + pc + " instruction: " + RAM[(int)pc]);
            if (pc < minUsedAddress || pc > maxUsedAddress)    // If the program counter is out of bounds, print an error message and break
            {
                write("PC out of bounds. Halting execution.\n");
                break;    // Break out of the loop
            }
            MAR = pc;    // Set the memory address register to the program counter
            pc++;    // Increment the program counter
            MBR = RAM[(int) MAR];    // Set the memory buffer register to the value at the memory address register
            IR = MBR;
            opcode = (int) IR / 10000;        // Integer division to obtain opcode, gives quotient
            remainder = (int) IR % 10000;    // Modulus to get remainder

            op1mode = remainder / 1000;    // Integer division to obtain op1mode, gives quotient
            remainder = remainder % 1000;    // Modulus to update remainder
            op1gpr = remainder / 100;    // Integer division to obtain op1gpr, gives quotient
            remainder = remainder % 100;    // Modulus to update remainder
            op2mode = remainder / 10;    // Integer division to obtain op2mode, gives quotient
            remainder = remainder % 10;    // Modulus to update remainder
            op2gpr = remainder;    // Set op2gpr to the remainder
            switch (opcode)
            {    // Switch on the opcode
                case HALT_OPCODE:    // If the opcode is 0, halt the program

                    write("HALT encountered. Stopping execution.\n");
                    running = false;    // Set running to false
                    clock += 12;
                    timeLeft -= 12;
                    return HALT_STATUS;
                case 1: // Add
                    // Validate register indices before use
                    if (op1gpr < 0 || op1gpr >= NUM_REGISTERS || op2gpr < 0 || op2gpr >= NUM_REGISTERS) {
                        write(String.format("Error: Invalid GPR index (Add). GPR1: " + op1gpr + ", GPR2: " + op2gpr + "\n"));
                        return GENERAL_PURPOSE_ERROR;
                    }
                    status = FetchOperand(op1mode, op1gpr, op1data);    // Determine the first operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write(String.format("Error in line %d: Invalid operand 1.\n", pc));
                        return GENERAL_PURPOSE_ERROR;
                    }
                    status = FetchOperand(op2mode, op2gpr, op2data);    // Determine the second operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write(String.format("Error in line %d: Invalid operand 2.\n", pc));
                        return GENERAL_PURPOSE_ERROR;
                    }
                    result = op1data[OP_VALUE_REF] + op2data[OP_VALUE_REF];    // Perform operation, add the operands
                    if (op1mode == 1)    // If the first operand is  register mode
                    {
                        gpr[op1gpr] = result;    // Set the desired general purpose register to the result
                    } else if (op1mode == 6)    // Ff the first operand is immediate mode
                    {
                        write(String.format("Error in line %d: Cannot store result in immediate mode.\n", pc));
                        return GENERAL_PURPOSE_ERROR;
                    } else    // else store in memory
                    {
                        if (isValidProgramAddress((int) op1data[OP_ADDRESS_REF]))
                            RAM[(int) op1data[OP_ADDRESS_REF]] = result; // place result in memory (RAM)
                        else
                        {
                            write(String.format("Error in line " + pc + ": Invalid memory address: " + op1data[OP_ADDRESS_REF] + "\n"));
                            return ERROR_INVALID_ADDRESS;
                        }
                    }
                    clock += 3;    // Increment the clock
                    timeLeft -= 3;
                    break;    // Break out of the case

                case 2: // Subtract
                    // Validate register indices before use
                    if (op1gpr < 0 || op1gpr >= NUM_REGISTERS || op2gpr < 0 || op2gpr >= NUM_REGISTERS) {
                        write(String.format("Error: Invalid GPR index (Add). GPR1: " + op1gpr + ", GPR2: " + op2gpr + "\n"));
                        return GENERAL_PURPOSE_ERROR;
                    }
                    status = FetchOperand(op1mode, op1gpr, op1data);    // Determine the first operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write(String.format("Error in line %d: Invalid operand 1.\n", pc));
                        return GENERAL_PURPOSE_ERROR;
                    }
                    status = FetchOperand(op2mode, op2gpr, op2data);    // Determine the second operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write("Error: Invalid operand 2.\n");
                        return GENERAL_PURPOSE_ERROR;
                    }
                    result = op1data[OP_VALUE_REF] - op2data[OP_VALUE_REF];    // Perform operation, subtract the operands

                    if (op1mode == 1)    // If the first operand is register mode
                    {
                        gpr[op1gpr] = result;    // Set the desired general purpose register to the result
                    } else if (op1mode == 6)    // if first operand is immediate mode
                    {
                        write("Error: Cannot store result in immediate mode.\n");
                        return GENERAL_PURPOSE_ERROR;
                    } else
                    {
                        if (!isValidProgramAddress((int) op1data[OP_ADDRESS_REF]))    // If the address is not valid, print an error message
                        {
                            write(String.format("Error in line " + pc + ": Invalid memory address: " + op1data[OP_ADDRESS_REF] + "\n"));
                            return ERROR_INVALID_ADDRESS;
                        }
                        RAM[(int) op1data[OP_ADDRESS_REF]] = result;    // place result in memory (RAM)
                    }
                    clock += 3;    // Increment the clock
                    timeLeft -= 3;
                    break;    // Break out of the case

                case 3: // Multiply
                    // Validate register indices before use
                    if (op1gpr < 0 || op1gpr >= NUM_REGISTERS || op2gpr < 0 || op2gpr >= NUM_REGISTERS) {
                        write(String.format("Error: Invalid GPR index (Add). GPR1: " + op1gpr + ", GPR2: " + op2gpr + "\n"));
                        return GENERAL_PURPOSE_ERROR;
                    }
                    status = FetchOperand(op1mode, op1gpr, op1data);    // Determine the first operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write("Error: Invalid operand 1.\n");
                        return GENERAL_PURPOSE_ERROR;
                    }
                    status = FetchOperand(op2mode, op2gpr, op2data);    // Determine the second operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write("Error: Invalid operand 2.\n");
                        return GENERAL_PURPOSE_ERROR;
                    }
                    result = op1data[OP_VALUE_REF] * op2data[OP_VALUE_REF];    // Perform operation, multiply the operands

                    if (op1mode == 1)    // If the first operand is register mode
                    {
                        gpr[op1gpr] = result;    // Set the desired general purpose register to the result
                    } else if (op1mode == 6)    // if the first operand is immediate mode
                    {
                        write("Error: Cannot store result in immediate mode.\n");
                        return GENERAL_PURPOSE_ERROR;
                    } else {
                        if (!isValidAddress((int) op1data[OP_ADDRESS_REF]))    // If the address is not valid, print an error message
                        {
                            write(String.format("Error in line " + pc + ": Invalid memory address: " + op1data[OP_ADDRESS_REF] + "\n"));
                            return ERROR_INVALID_ADDRESS;
                        }
                        RAM[(int) op1data[OP_ADDRESS_REF]] = result;    // place result in memory (RAM)
                    }
                    clock += 6;    // Increment the clock
                    timeLeft -= 6;
                    break;    // Break out of the case

                case 4: // Divide
                    // Validate register indices before use
                    if (op1gpr < 0 || op1gpr >= NUM_REGISTERS || op2gpr < 0 || op2gpr >= NUM_REGISTERS) {
                        write(String.format("Error: Invalid GPR index (Add). GPR1: " + op1gpr + ", GPR2: " + op2gpr + "\n"));
                        return GENERAL_PURPOSE_ERROR;
                    }
                    status = FetchOperand(op1mode, op1gpr, op1data);    // Determine the first operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write("Error: Invalid operand 1.\n");
                        return GENERAL_PURPOSE_ERROR;
                    }
                    status = FetchOperand(op2mode, op2gpr, op2data);    // Determine the second operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write("Error: Invalid operand 2.\n");
                        return GENERAL_PURPOSE_ERROR;
                    }
                    // Check for division by zero BEFORE performing division
                    if (op2data[OP_VALUE_REF] == 0)        // Check for division by zero
                    {
                        write("Error: Division by zero.\n");
                        return ERROR_DIVIDE_BY_ZERO;
                    }
                    // If no division by zero:
                    result = op1data[OP_VALUE_REF] / op2data[OP_VALUE_REF];    // Perform operation, divide the operands

                    if (op1mode == 1)    // If the first operand is register mode
                    {
                        gpr[op1gpr] = result;    // Set the desired general purpose register to the result
                    } else if (op1mode == 6)    // If the first operand is immediate mode
                    {
                        write("Error: Cannot store result in immediate mode.\n");
                        return GENERAL_PURPOSE_ERROR;
                    } else {
                        if (!isValidProgramAddress((int) op1data[OP_ADDRESS_REF]))    // If the address is not valid, print an error message
                        {
                            write(String.format("Error in line " + pc + ": Invalid memory address: " + op1data[OP_ADDRESS_REF] + "\n"));
                            return ERROR_INVALID_ADDRESS;
                        }
                        RAM[(int) op1data[OP_ADDRESS_REF]] = result;    // place result in memory (RAM)
                    }
                    clock += 6;    // Increment the clock
                    timeLeft -= 6;
                    break;    // Break out of the case
                case 5: // Move
                    // Validate register indices before use
                    if (op1gpr < 0 || op1gpr >= NUM_REGISTERS || op2gpr < 0 || op2gpr >= NUM_REGISTERS) {
                        write(String.format("Error: Invalid GPR index (Add). GPR1: " + op1gpr + ", GPR2: " + op2gpr + "\n"));
                        return GENERAL_PURPOSE_ERROR;
                    }
                    status = FetchOperand(op1mode, op1gpr, op1data);    // Determine the first operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write("Error: Invalid operand 1.\n");
                        return GENERAL_PURPOSE_ERROR;
                    }
                    status = FetchOperand(op2mode, op2gpr, op2data);    // Determine the second operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write("Error: Invalid operand 2.\n");
                        return GENERAL_PURPOSE_ERROR;
                    }

                    result = op2data[OP_VALUE_REF];    // Perform operation, move the second operand to the first operand

                    if (op1mode == 1)    // If the first operand is register mode
                    {
                        gpr[op1gpr] = result;    // Set the desired general purpose register to the result
                    } else if (op1mode == 6)    // If the first operand is immediate mode
                    {
                        write("Error: Cannot store result in immediate mode.\n");
                        return GENERAL_PURPOSE_ERROR;
                    } else {
                        if (!isValidAddress((int) op1data[OP_ADDRESS_REF]))    // If the address is not valid, print an error message
                        {
                            write(String.format("Error in line " + pc + ": Invalid memory address: " + op1data[OP_ADDRESS_REF] + "\n"));
                            return ERROR_INVALID_ADDRESS;
                        }
                        RAM[(int) op1data[OP_ADDRESS_REF]] = result;    // place result in memory (RAM)
                    }
                    clock += 2;    // Increment the clock
                    timeLeft -= 2;
                    break;    // Break out of the case

                case 6: // BRANCH (Unconditional Branch)
                    if (isValidProgramAddress(pc))    // If the program counter is a valid address
                    {
                        // Verify that the branch target is also a valid address
                        long branchTarget = RAM[(int) pc];
                        if (isValidProgramAddress(branchTarget))
                        {
                            pc = branchTarget;    // move to the desired location
                        }
                        else
                        {
                            write(String.format("Error: Invalid branch target address: " + branchTarget + "\n"));
                            return ERROR_INVALID_ADDRESS;
                        }
                    } else    // If the program counter is not a valid address
                    {
                        write(String.format("Error: Invalid memory address: " + pc + "\n"));
                        return ERROR_INVALID_ADDRESS;
                    }
                    clock += 2;   // Increment the clock
                    timeLeft -= 2;
                    break;    // Break out of the case

                case 7: // BRONMINUS (Branch if negative)
                    status = FetchOperand(op1mode, op1gpr, op1data);    // Determine the first operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write("Error: Invalid operand 1.\n");
                        return GENERAL_PURPOSE_ERROR;
                    }
                    if (op1data[OP_VALUE_REF] < 0)    // If the first operand is less than 0
                    {
                        if (isValidProgramAddress(pc))    // If the program counter is a valid address
                        {
                            // Verify that the branch target is also a valid address
                            long branchTarget = RAM[(int) pc];
                            if (isValidProgramAddress(branchTarget))
                            {
                                pc = branchTarget;    // move to the desired location
                            }
                            else
                            {
                                write(String.format("Error: Invalid branch target address: " + branchTarget + "\n"));
                                return ERROR_INVALID_ADDRESS;
                            }
                        }
                        else {
                            write(String.format("Error: Invalid memory address: " + pc + "\n"));
                            return ERROR_INVALID_ADDRESS;
                        }
                    } else    // If the first operand is not less than 0
                    {
                        pc++;    // Don't go anyway, just increment pc
                    }
                    clock += 4;    // Increment the clock
                    timeLeft -= 4;
                    break;    // Break out of the case


                case 8: // BRONPLUS (Branch if positive)
                    status = FetchOperand(op1mode, op1gpr, op1data);    // Determine the first operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write("Error: Invalid operand 1.\n");
                        return GENERAL_PURPOSE_ERROR;
                    }
                    if (op1data[OP_VALUE_REF] > 0)    // If the first operand is greater than 0
                    {
                        if (isValidProgramAddress(pc))    // If the program counter is a valid address
                        {
                            // Verify that the branch target is also a valid address
                            long branchTarget = RAM[(int) pc];
                            if (isValidProgramAddress(branchTarget))
                            {
                                pc = branchTarget;    // move to the desired location
                            }
                            else
                            {
                                write(String.format("Error: Invalid branch target address: " + branchTarget + "\n"));
                                return ERROR_INVALID_ADDRESS;
                            }
                        }
                        else    // If the program counter is not a valid address
                        {
                            write(String.format("Error: Invalid memory address: " + pc + "\n"));    // Print error message
                            return ERROR_INVALID_ADDRESS;    // Return error code
                        }
                    } else    // If the first operand is not greater than 0
                    {
                        pc++;    // Don't go anywhere, just increment pc
                    }
                    clock += 4;    // Increment the clock
                    timeLeft -= 4;
                    break;    // Break out of the case

                case 9: // Branch on Zero
                    status = FetchOperand(op1mode, op1gpr, op1data);    // Determine the first operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write(String.format("Error in line " + pc + ": Invalid operand 1." + "\n"));
                        return GENERAL_PURPOSE_ERROR;
                    }
                    if (op1data[OP_VALUE_REF] == 0)    // If the first operand is equal to 0
                    {
                        if (isValidProgramAddress(pc))    // If the program counter is a valid address
                        {
                            // Verify that the branch target is also a valid address
                            long branchTarget = RAM[(int) pc];
                            if (isValidProgramAddress(branchTarget))
                            {
                                pc = branchTarget;    // move to the desired location
                            }
                            else
                            {
                                write(String.format("Error: Invalid branch target address: " + branchTarget + "\n"));
                                return ERROR_INVALID_ADDRESS;
                            }
                        }
                        else {
                            write(String.format("Error: Invalid memory address: " + pc + "\n"));
                            return ERROR_INVALID_ADDRESS;
                        }
                    } else    // If the first operand is not equal to 0
                    {
                        pc++;    // Don't go anywhere, just increment pc
                    }
                    clock += 4;    // Increment the clock
                    timeLeft -= 4;
                    break;
                case 10: // PUSH
                    // Validate register indices before use
                    if (op1gpr < 0 || op1gpr >= NUM_REGISTERS) {
                        write(String.format("Error: Invalid GPR index (Add). GPR1: " + op1gpr + ", GPR2: " + op2gpr + "\n"));
                        return GENERAL_PURPOSE_ERROR;
                    }
                    status = FetchOperand(op1mode, op1gpr, op1data);    // Determine the first operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write("Error: Invalid operand 1.\n");    // Print error message
                        return GENERAL_PURPOSE_ERROR;    // Return error code
                    }
                    if (isValidAddress(sp))    // If the stack pointer is a valid address
                    {
                        sp--;    // Increment the stack pointer
                        write(String.format(op1data[OP_VALUE_REF] + " being pushed onto stack at location: " + sp + "\n"));
                        RAM[(int) sp] = op1data[OP_VALUE_REF];    // Store the first operand in memory

                        clock += 2;    // Increment the clock
                    } else    // If the stack pointer is not a valid address
                    {
                        write("Error: Stack overflow.\n");
                        return ERROR_INVALID_ADDRESS;
                    }
                    clock += 2;    // Increment the clock
                    timeLeft -= 2;
                    break;    // Break out of the case
                case 11: // POP
                    // Validate register indices before use
                    if (op1gpr < 0 || op1gpr >= NUM_REGISTERS) {
                        write(String.format("Error: Invalid GPR index (Add). GPR1: " + op1gpr + ", GPR2: " + op2gpr + "\n"));
                        return GENERAL_PURPOSE_ERROR;
                    }
                    status = FetchOperand(op1mode, op1gpr, op1data);    // Determine the first operand
                    if (status < 0)    // If the status is less than 0, print an error message
                    {
                        write("Error: Invalid operand 1.\n");
                        return GENERAL_PURPOSE_ERROR;
                    }
                    if (isValidAddress(sp))    // If the stack pointer is a valid address
                    {
                        gpr[op1gpr] = RAM[(int) sp];    // Set the desired general purpose register to the value in memory
                        write(String.format(RAM[(int)sp] + " being popped from stack.\n"));
                        sp++;    // Increment the stack pointer
                        clock += 2;    // Increment the clock
                    } else    // If the stack pointer is not a valid address
                    {
                        write(String.format("Error: Stack underflow.\n"));
                        return ERROR_INVALID_ADDRESS;
                    }
                    clock += 2;    // Increment the clock
                    timeLeft -= 2;
                    break;    // Break out of the case
                case 12:    // system call
                    //status = FetchOperand(op1mode, op1gpr, op1data);
                    //if (status < 0)
                    //{
                    //    return status;      // return error status
                  //  }
                    long id = RAM[(int) pc++]; // Next word has system call ID
                    status = SystemCall(id);    // Call the system call function
                    if (status == START_INPUT_OPERATION_CODE || status == START_OUTPUT_OPERATION_CODE)
                    {
                        return status;
                    }
                    clock += 12;
                    timeLeft -= 12;
                    break;
                default:    // If the opcode is not any of the above options:
                    write(String.format("Invalid opcode %d. Halting execution.\n", opcode));    // Print error message
                    running = false;    // Set running to false, halt execution
                    break;    // Break out of the case
            }
            if (timeLeft <= 0)    // If time left is less than or equal to 0
            {
                write("Time slice expired. Halting execution.\n");
                return TIME_SLICE_EXPIRED;    // Return time slice expired
            }
        }
        write("Program execution completed.\n");
        return OK;
    }

    // ************************************************************
    // Function: SystemCall
    //
    // Task Description:
    // 	Processes system calls from user programs by switching to OS mode,
    //  handling the system call based on the ID, and returning to user mode.
    //  Available system calls include memory allocation/deallocation,
    //  I/O operations, and process management.
    //
    // Input Parameters
    //	systemCallID   ID of the system call to execute
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	OK                       If the system call is successful
    //  START_INPUT_OPERATION_CODE  For I/O input operations
    //  START_OUTPUT_OPERATION_CODE For I/O output operations
    //  GENERAL_PURPOSE_ERROR    For invalid system call IDs
    //  Error codes              Various error codes from specific calls
    // ************************************************************
    public long SystemCall(long systemCallID)
    {
        write(String.format("Enterring system call with ID: " + systemCallID + "\n"));
        psr = OSMode;

        long status = OK;

        switch ((int)systemCallID) {
            case 1: // create process - user process is creating a child process
                write("Create process system call not implemented.\n");
                break;
            case 2: // delete process
                write("Delete process system call not implemented.\n");
                break;
            case 3: // process inquiry
                write("Process inquiry system call not implemented.\n");
                break;
            case 4: // dynamic memory allocation
                write("Mem alloc system call.\n");
                status = MemAllocSystemCall();
                break;
            case 5: // dynamic memory deallocation
                write("Dealloc memory system call.\n");
                status = MemFreeSystemCall();
                break;
            case 6: // display memory
                write("Memory system call not implemented.\n");
                break;
            case 7: // display process table
                write("Process table system call not implemented.\n");
                break;
            case 8: // io_getc system call
                write("Io_getc System call.\n");
                status = io_getcSystemCall();
                break;
            case 9: // io_putc system call
                write("Io_putc system call.\n");
                status = io_putcSystemCall();
                break;
            default: //invalid system call id
                write("Invalid system call ID: " + systemCallID + "\n");
                status = GENERAL_PURPOSE_ERROR;
                break;
        }
        psr = UserMode;
        return status;
    }

    // ************************************************************
    // Function: MemAllocSystemCall
    //
    // Task Description:
    // 	System call implementation for memory allocation.
    // 	Takes the size from GPR2, allocates memory of that size,
    //  stores the address in GPR1, and returns status in GPR0.
    //
    // Input Parameters
    //	None (uses GPR2 for size)
    //
    // Output Parameters
    //	GPR0 - Status code (OK or error)
    //  GPR1 - Address of allocated memory
    //
    // Function Return Value
    //	OK                     If allocation is successful
    //  ERROR_NO_FREE_MEMORY   If no memory is available
    //  ERROR_INVALID_SIZE     If size is invalid
    //  Other error codes      From allocateUserMemory
    // ************************************************************
    public long MemAllocSystemCall()
    {
        long size = gpr[2];
        // why check for size out of range if allocateusermemory() does that?
        if (size == 1)
            size = 2;
        gpr[1] = allocateUserMemory(size);
        if (gpr[1] < 0)
        {
            gpr[0] = gpr[1];
        }
        else
            gpr[0] = OK;
        write("Mem_alloc system call\n");
        write(String.format("GPR0: " + gpr[0] + "\n"));
        write(String.format("GPR1: " + gpr[1] + "\n"));
        write(String.format("GPR2: " + gpr[2] + "\n"));
        return gpr[0];
    }   // end of memallocsystemcall()

    // ************************************************************
    // Function: MemFreeSystemCall
    //
    // Task Description:
    // 	System call implementation for memory deallocation.
    // 	Takes the address from GPR1 and size from GPR2, frees memory
    //  at that address, and returns status in GPR0.
    //
    // Input Parameters
    //	None (uses GPR1 for address and GPR2 for size)
    //
    // Output Parameters
    //	GPR0 - Status code (OK or error)
    //
    // Function Return Value
    //	OK                     If deallocation is successful
    //  ERROR_INVALID_ADDRESS   If address is invalid
    //  Other error codes      From FreeUserMemory
    // ************************************************************
    public long MemFreeSystemCall()
    {
        long size = gpr[2];
        if (size == 1)
            size = 2;
        dumpMemory("Freeing User Memory:\n", gpr[1], size);
        gpr[0] = FreeUserMemory(gpr[1], size);
        if (gpr[0] < 0)
        {
            gpr[0] = gpr[1];
        }
        else
            gpr[0] = OK;
        write("Mem_free system call\n");
        write(String.format("GPR0: " + gpr[0] + "\n"));
        write(String.format("GPR1: " + gpr[1] + "\n"));
        write(String.format("GPR2: " + gpr[2] + "\n"));
        return gpr[0];
    }

    // ************************************************************
    // Function: io_getcSystemCall
    //
    // Task Description:
    // 	System call implementation for character input operations.
    // 	Returns a special code that will cause the calling process
    //  to be moved to the Wait Queue until input is available.
    //
    // Input Parameters
    //	None
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	START_INPUT_OPERATION_CODE - Special code indicating input operation
    // ************************************************************
    public long io_getcSystemCall()
    {
        return START_INPUT_OPERATION_CODE;
    }

    // ************************************************************
    // Function: io_putcSystemCall
    //
    // Task Description:
    // 	System call implementation for character output operations.
    // 	Returns a special code that will cause the calling process
    //  to be moved to the Wait Queue until output is complete.
    //
    // Input Parameters
    //	None
    //
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	START_OUTPUT_OPERATION_CODE - Special code indicating output operation
    // ************************************************************
    public long io_putcSystemCall()
    {
        return START_OUTPUT_OPERATION_CODE;
    }

    // ************************************************************
// Function: dumpAllocatedMemory
//
// Task Description:
// 	Dumps all currently allocated memory blocks by iterating through
//  the tracking arrays and calling dumpMemory for each block.
//
// Input Parameters
//	None
//
// Output Parameters
//	None
//
// Function Return Value
//	void - no return value
// ************************************************************
public void dumpAllocatedMemory() {
    write("DUMPING ALL ALLOCATED MEMORY BLOCKS:\n");
    
    if (allocatedBlockCount == 0) {
        write("No allocated memory blocks to display.\n");
        return;
    }
    
    for (int i = 0; i < allocatedBlockCount; i++) {
        String message = "Memory Block " + (i+1) + " at address [" + allocatedAddresses[i] + 
                         "] with size " + allocatedSizes[i] + ":\n";
        dumpMemory(message, allocatedAddresses[i], allocatedSizes[i]);
    }
}

    // ************************************************************
// Function: DumpMemory
//
// Task Description:
//	Displays a string passed as one of the  input parameter.
// 	Displays content of GPRs, SP, PC, PSR, system Clock and
//	the content of specified memory locations in a specific format.
//
// Input Parameters
//	String				String to be displayed
//	StartAddress			Start address of memory location
//	Size				Number of locations to dump
// Output Parameters
//	None
//
// Function Return Value
//	Okay				        On successful dump
//	ERROR_INVALID_ADDRESS		If the address is invalid
// ************************************************************
    public long dumpMemory(String message, long startAddress, long size)    // Dump memory function
    {
        String s;   // temporary string variable used for writing to file
        write(message);
        if (!isValidAddress(startAddress))    // If the start address is not a valid address
        {
            write(String.format("Invalid memory address: " + startAddress + "\n"));
            return ERROR_INVALID_ADDRESS;
        } else if (!isValidAddress(startAddress + size - 1))    // If the end address is not a valid address
        {
            write(String.format("Invalid memory address: " + (startAddress + size - 1) + "\n"));
            return ERROR_INVALID_ADDRESS;
        } else if (size <= 0) {
            write(String.format("Invalid size: " + size + ". Size must be greater than 0.\n"));
            return ERROR_INVALID_SIZE;
        }

        // Print the GPRs, SP, PC, and PSR
        write("GPRS:\tG0\tG1\tG2\tG3\tG4\tG5\tG6\tG7\tSP\tPC\n");
        for (int i = 0; i < NUM_REGISTERS; i++)
        {   // Loop through the general purpose registers
            if (i < 0 || i >= NUM_REGISTERS) {
                // Additional safety check for array bounds
                continue;
            }
            s = Integer.toString((int) gpr[i]);
            write("\t" + s);
        }
        s = Integer.toString((int) sp);   // Write the stack pointer and program counter to file
        write("\t" + s);
        write("\t" + Integer.toString((int)pc));
        write("\nAddress:\t+0\t+1\t+2\t+3\t+4\t+5\t+6\t+7\t+8\t+9\n");
        int addr = (int) startAddress;  // Set the address to the start address
        int endAddr = (int) (startAddress + size);  // Set the end address to the start address plus the size
        while (addr < (endAddr)) {  // Loop through the memory addresses
            s = Integer.toString(addr / 10);
            write("\t" + Integer.toString(addr / 10) + "0 ");

            for (int i = 0; i < 10; i++) {  // Loop through the memory addresses
                write("\t" + Integer.toString((int) RAM[addr + i]));
            }
            write("\n");
            addr += 10; // Increment the address by 10
        }
        s = ("Clock: " + Integer.toString((int) clock) + " microseconds\n");
        write(s);
        write(String.format("PSR: %d\n", psr));
        s = ("PSR: " + Integer.toString((int) psr) + "\n");
        write(s);
        return OK;
    }

    // ************************************************************
    // Function: FetchOperand
    //
    // Task Description:
    // 	…
    //
    // Input Parameters
    //	OpMode			Operand mode value
    //	OpReg				Operand GPR value
    // Output Parameters
    //	OpAddress			Address of operand
    //	OpValue			Operand value when mode and GPR are valid
    //
    // Function Return Value
    //	Okay				        On successful fetch
    //	ERROR_INVALID_ADDRESS		If the address is invalid
    //	GENERAL_PURPOSE_ERROR		If GPR index is out of bounds
    // ************************************************************
    public long FetchOperand(int opmode, int opreg, long[] opdata) {    // Fetch operand function
        // Validate GPR index
        if (opreg < 0 || opreg >= NUM_REGISTERS) {
            write(String.format("Error: Invalid GPR index: " + opreg + "\n"));
            return GENERAL_PURPOSE_ERROR;
        }
        switch (opmode) {   // Switch on the operand mode
            case 0: // Null mode
                break;
            case 1: //Register mode
                opdata[0] = -1;   // Set the address to -1, RAM memory not going to be accessed here
                opdata[1] = gpr[opreg];  // Set the value to the desired general purpose register
                break;
            case 2: //Register deferred mode - Op addr is in GPR & value in memory
                opdata[0] = gpr[opreg]; // Set the address to the desired general purpose register
                if (isValidAddress(opdata[0])) {    // If the address is a valid address
                    opdata[1] = RAM[(int) opdata[0]];   // Set the value to the value in memory
                } else {
                    write(String.format("Error: Invalid memory address: " + opdata[0] + "\n"));    // Print error message
                    return ERROR_INVALID_ADDRESS;
                }
                break;
            case 3: //Autoincrement mode - Op addr is in GPR & value in memory
                // First validate that the address in GPR is valid before accessing memory
                if (!isValidProgramAddress(gpr[opreg])) {
                    write(String.format("Error: Invalid memory address in autoincrement mode: " + gpr[opreg] + "\n"));
                    return ERROR_INVALID_ADDRESS;
                }
                
                opdata[0] = gpr[opreg]; // Set the address to the desired general purpose register
                opdata[1] = RAM[(int) opdata[0]];   // Set the value to the value in memory
                gpr[opreg]++;   // Increment the desired general purpose register
                break;
                
            case 4: //Autodecrement mode - Op addr is in GPR & value in memory
                // First decrement the register
                gpr[opreg]--;   // Decrement the desired general purpose register
                
                // Then validate that the resulting address is valid
                if (!isValidProgramAddress(gpr[opreg])) {
                    write(String.format("Error: Invalid memory address in autodecrement mode: " + gpr[opreg] + "\n"));
                    // Restore the original value since we had an error
                    gpr[opreg]++; 
                    return ERROR_INVALID_ADDRESS;
                }
                
                opdata[0] = gpr[opreg]; // Set the address to the desired general purpose register
                opdata[1] = RAM[(int) opdata[0]];   // Set the value to the value in memory
                break;
            case 5: //Direct mode - Op addr is in instruction & value in memory
                if (!isValidProgramAddress(pc)) {    // If the program counter is not a valid address
                    write(String.format("Error: Invalid memory address: " + pc + "\n"));    // Print error message
                    return ERROR_INVALID_ADDRESS;
                }
                opdata[0] = RAM[(int) pc++];    // Set the address to the value in memory at the program counter
                if (isValidProgramAddress(opdata[0])) {    // If the address is a valid address
                    opdata[1] = RAM[(int) opdata[0]];   // Set the value to the value in memory
                } else {    // If the address is not a valid address
                    write(String.format("Error in line " + (pc - 1) + ": Invalid memory address: " + opdata[0] + "\n"));    // Print error message
                    return ERROR_INVALID_ADDRESS;  // Return error code
                }
                break;
            case 6: //Immediate mode
                if (isValidProgramAddress(pc)) {   // If the program counter is a valid address
                    opdata[0] = -1;  // Set the address to -1, RAM memory not going to be accessed here
                    opdata[1] = RAM[(int) pc++];    // Set the value to the value in memory at the program counter
                } else {    // If the program counter is not a valid address
                    write(String.format("Error: Invalid memory address: " + opdata[0] + "\n"));
                    return ERROR_INVALID_ADDRESS;
                }
                break;  // Break out of the case
            default:    // If the operand mode is not any of the above options
                throw new RuntimeException("Shouldn't have gotten here.");  // Throw an exception
        }
        return OK;
    }

    //****************************************************************
    // Checks if the given address is within the valid range of memory addresses.
    //
    // @param opaddress the address to check
    // @return true if the address is valid, false otherwise
    //****************************************************************
    private static boolean isValidAddress(long opaddress) {   // Check if the address is valid
        return opaddress >= 0 && opaddress < MEMORY_SIZE;   // Return true if the address is greater than or equal to 0 and less than the memory size
    }   // Return false otherwisep

    //************************************************************
    // Checks if the given address is within the valid range of program addresses.
    //
    // @param address the address to check
    // @return true if the address is valid, false otherwise
    //************************************************************
    private static boolean isValidProgramAddress(long address)
    {
        return address >= minUsedAddress && address <= maxUsedAddress;
    }

    // ************************************************************
    // Function: write
    //
    // Task Description:
    // 	Writes inputted string to output file
    //
    // Input Parameters
    //	message			string to be written to file
    // Output Parameters
    //	none
    //
    // Function Return Value
    //	None
    //	(will throw exception on unsuccessful write)
    // ************************************************************
    public static void write(String message) {
        System.out.print(message);
        try (FileWriter fileWriter = new FileWriter("Forgan_HW4Output.txt", true)) {    // Try to open the file
            fileWriter.write(message);  // Write the message to the file
        } catch (IOException e) {   // Catch IO exception
            // Avoid recursive call that would cause stack overflow
            System.out.println("Error: Unable to write to log file: " + e.getMessage());
        }
    }

    //************************************************************
    // Function: dumpList
    //
    // Task Description:
    // 	Dumps the list of memory addresses to the output file
    //
    // Input Parameters
    //	ptr				pointer to the list
    // Output Parameters
    //	None
    //
    // Function Return Value
    //	None
    //************************************************************
    public void dumpList(long ptr)
    {
        if (ptr == ENDOFLIST)
        {
            write("List is empty\n");
            return;
        }
        else if (ptr < 0 || ptr >= MEMORY_SIZE)       // check for invalid address
        {
            String s = "Invalid memory address: " + ptr + "\n";
            write(s);
            return;
        }
        write("free list:\n");
        while (ptr != ENDOFLIST)
        {
            // Validate the pointer is within bounds before accessing memory
            if (ptr < 0 || ptr >= MEMORY_SIZE || ptr + 1 >= MEMORY_SIZE) {
                write("\tERROR: Invalid memory address encountered: " + ptr + "\n");
                break;  // Exit the loop to prevent further errors
            }
            
            String s = "\taddress [" + ptr + "] size: " + RAM[(int)ptr + 1] + " next ptr -> " + RAM[(int)ptr] + "\n";
            write(s);
            
            // Get the next pointer but validate it's within bounds
            long nextPtr = RAM[(int)ptr];
            
            // Check for potential infinite loop (pointer pointing to itself)
            if (nextPtr == ptr) {
                write("\tERROR: Circular reference detected (pointer points to itself)\n");
                break;
            }
            
            ptr = nextPtr;    // iterate through list
        }
        write("\n");
        
    }
}