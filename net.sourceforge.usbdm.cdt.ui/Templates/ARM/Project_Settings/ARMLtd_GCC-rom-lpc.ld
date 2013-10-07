/* STM32 version
 * Linker script based on example scripts provided with ARM Ltd GCC
 *
 * It references the following symbols, which must be defined in code:
 * 
 *      Reset_Handler : Entry of reset handler
 * 
 * 
 * It defines the following symbols, which code can use without definition:
 *      __exidx_start
 *      __exidx_end
 *      __etext
 *      __data_start__
 *      __preinit_array_start
 *      __preinit_array_end
 *      __init_array_start
 *      __init_array_end
 *      __fini_array_start
 *      __fini_array_end
 *      __data_end__
 *      __bss_start__
 *      __bss_end__
 *      __end__
 *      end
 *      __HeapBottom
 *      __HeapLimit
 *      __StackLimit
 *      __StackTop
 *      __stack
 */
 
OUTPUT_FORMAT ("elf32-littlearm", "elf32-bigarm", "elf32-littlearm")
SEARCH_DIR(.)
SEARCH_DIR(../Standard_Peripheral_Library)
SEARCH_DIR(../Project_Settings)

/* Standard Peripheral Library
 
Use the appropriate library file in the GROUP statement according to chip in use 

libstm32_CL.a
libstm32_HD.a
libstm32_HD_VL.a
libstm32_LD.a
libstm32_LD_VL.a
libstm32_MD.a
libstm32_MD_VL.a
libstm32_XL.a

GROUP(libstm32_MD.a)
*/

/* Include external memory map defining ram and Flash memory etc */
INCLUDE "../Project_Settings/MemoryMap-$(targetDeviceName).ld"

ENTRY(Reset_Handler)

PROVIDE(__HardReset = Reset_Handler);

SECTIONS
{
	.text :
	{
		 __flash_start = .;
		
		/* Vector table */
		KEEP(*(.interrupt_vectors))
		/* Make sure we pulled in an lest a reset vector.  */
		ASSERT (. != __flash_start, "No interrupt vector");
	
		*(.text*)
	
		KEEP(*(.init))
		KEEP(*(.fini))
	
		/* .ctors */
		*crtbegin.o(.ctors)
		*crtbegin?.o(.ctors)
		*(EXCLUDE_FILE(*crtend?.o *crtend.o) .ctors)
		*(SORT(.ctors.*))
		*(.ctors)
	
		/* .dtors */
		*crtbegin.o(.dtors)
		*crtbegin?.o(.dtors)
		*(EXCLUDE_FILE(*crtend?.o *crtend.o) .dtors)
		*(SORT(.dtors.*))
		*(.dtors)
	
		*(.rodata*)
	
		KEEP(*(.eh_frame*))
	} > rom

	.ARM.extab : 
	{
		*(.ARM.extab* .gnu.linkonce.armextab.*)
	} > rom

	__exidx_start = .;
	.ARM.exidx :
	{
		*(.ARM.exidx* .gnu.linkonce.armexidx.*)
	} > rom
	__exidx_end = .;

	__etext = .;
		
	.data : AT (__etext)
	{
		__data_start__ = .;
		*(vtable)
		*(.data*)

		. = ALIGN(4);
		/* preinit data */
		PROVIDE_HIDDEN (__preinit_array_start = .);
		KEEP(*(.preinit_array))
		PROVIDE_HIDDEN (__preinit_array_end = .);

		. = ALIGN(4);
		/* init data */
		PROVIDE_HIDDEN (__init_array_start = .);
		KEEP(*(SORT(.init_array.*)))
		KEEP(*(.init_array))
		PROVIDE_HIDDEN (__init_array_end = .);


		. = ALIGN(4);
		/* finit data */
		PROVIDE_HIDDEN (__fini_array_start = .);
		KEEP(*(SORT(.fini_array.*)))
		KEEP(*(.fini_array))
		PROVIDE_HIDDEN (__fini_array_end = .);

		KEEP(*(.jcr*))
		. = ALIGN(4);
		/* All data end */
		__data_end__ = .;

	} > ram

	.bss :
	{
		. = ALIGN(4);
		__bss_start__ = .;
		*(.bss*)
		*(COMMON)
		. = ALIGN(4);
		__bss_end__ = .;
	} > ram
	
   /* Dummy HEAP - from top of BSS */
  .heap_dummy :
  {
    . = ALIGN(4);
    PROVIDE ( end = . );
    PROVIDE ( __end__ = . );
    PROVIDE ( _end = . );
   __HeapBottom = .;
	__heap_addr = .;
    . = . + __heap_size;
   __HeapLimit = .;
  } > ram

   /* Dummy STACK space - between HEAP and end of RAM*/
  .stack_dummy :
  {
    . = ALIGN(4);
    . = . + __stack_size;
  } > ram

  /* Set stack top to end of ram, and stack limit move down by
   * size of stack_dummy section 
   */
  __StackTop = ORIGIN(ram) + LENGTH(ram);
  __StackLimit = __StackTop - SIZEOF(.stack_dummy);
  PROVIDE(__stack = __StackTop);
  PROVIDE(__cs3_stack = __StackTop);

  /* Check if data + heap + stack exceeds ram limit */
  ASSERT(__StackLimit >= __HeapLimit, "region ram overflowed with stack")
}
