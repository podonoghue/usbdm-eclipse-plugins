/*
 *  Vectors.c
 *
 *  Generic vectors and security for Coldfire V1
 *
 *  Created on: 07/12/2012
 *      Author: podonoghue
 */
#include <stdint.h>

#define $(targetDeviceSubFamily)

/*
 * Security information
 */
#if defined(DEVICE_SUBFAMILY_CFV1Plus)
typedef struct {
    uint8_t   backdoorKey[8];
    uint8_t   fprot[4];
    uint8_t   fdprot;
    uint8_t   feprot;
    uint8_t   fopt;
    uint8_t   fsec;
} SecurityInfo;

__attribute__ ((section(".cs3.security_information")))
const SecurityInfo securityInfo = {
    /* backdoor */ {0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF},
    /* fprot    */ {0xFF,0xFF,0xFF,0xFF,},
    /* fdprot   */ 0xFF,
    /* feprot   */ 0xFF,
    /* fopt     */ 0xFF,
    /* fsec     */ 0xFE,  /* KEYEN=3,MEEN=3,FSLACC=3,SEC=2 */
};
#elif defined(DEVICE_SUBFAMILY_CFV1)
typedef struct {
    uint8_t  backdoorKey[8];
    uint8_t  res1[5];
    uint8_t  nvprot;
    uint8_t  res2;
    uint8_t  nvopt;
} SecurityInfo;

__attribute__ ((section(".cs3.security_information")))
const SecurityInfo securityInfo = {
    /* backdoor */ {0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF},
    /* res1     */ {0xFF,0xFF,0xFF,0xFF,0xFF,},
    /* nvprot   */ 0xFF,
    /* res2     */ 0xFF,
    /* nvopt    */ 0xFF,
};
#else
#error "Device family not set"
#endif

/*
 * Vector table related
 */
typedef void( *const intfunc )( void );

#define WEAK_DEFAULT_HANDLER __attribute__ ((weak, alias("Default_Handler")))

 __attribute__((__interrupt__))
void Default_Handler(void) {
   while (1) {
      asm("halt");
   }
}

void __cs3_reset(void) __attribute__((__interrupt__));
extern uint32_t __cs3_stack;

void AccessError_Handler(void)            WEAK_DEFAULT_HANDLER;
void AddressError_Handler(void)      	  WEAK_DEFAULT_HANDLER;
void IllegalInstruction_Handler(void)     WEAK_DEFAULT_HANDLER;
void DivideBy0_Handler(void)       		  WEAK_DEFAULT_HANDLER;
void PrivilegeViolation_Handler(void)     WEAK_DEFAULT_HANDLER;
void Trace_Handler(void)       			  WEAK_DEFAULT_HANDLER;
void UnimplementedLineA_Handler(void)     WEAK_DEFAULT_HANDLER;
void UnimplementedLineF_Handler(void)     WEAK_DEFAULT_HANDLER;
void NonPCBreakpoint_Handler(void)        WEAK_DEFAULT_HANDLER;
void PCBreakpoint_Handler(void)           WEAK_DEFAULT_HANDLER;
void FormatError_Handler(void)            WEAK_DEFAULT_HANDLER;
void UninitializedInterrupt_Handler(void) WEAK_DEFAULT_HANDLER;
void SpuriousInt_Handler(void)            WEAK_DEFAULT_HANDLER;
void AutoVector1_Handler(void)            WEAK_DEFAULT_HANDLER;
void AutoVector2_Handler(void)            WEAK_DEFAULT_HANDLER;
void AutoVector3_Handler(void)            WEAK_DEFAULT_HANDLER;
void AutoVector4_Handler(void)            WEAK_DEFAULT_HANDLER;
void AutoVector5_Handler(void)            WEAK_DEFAULT_HANDLER;
void AutoVector6_Handler(void)            WEAK_DEFAULT_HANDLER;
void AutoVector7_Handler(void)            WEAK_DEFAULT_HANDLER;
void Trap0_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Trap1_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Trap2_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Trap3_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Trap4_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Trap5_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Trap6_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Trap7_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Trap8_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Trap9_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Trap10_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Trap11_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Trap12_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Trap13_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Trap14_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Trap15_Handler(void)                 WEAK_DEFAULT_HANDLER;

void Int48_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int49_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int50_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int51_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int52_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int53_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int54_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int55_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int56_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int57_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int58_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int59_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int60_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int61_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int62_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int63_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int64_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int65_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int66_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int67_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int68_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int69_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int70_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int71_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int72_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int73_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int74_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int75_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int76_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int77_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int78_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int79_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int80_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int81_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int82_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int83_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int84_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int85_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int86_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int87_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int88_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int89_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int90_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int91_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int92_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int93_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int94_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int95_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int96_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int97_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int98_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int99_Handler(void)                  WEAK_DEFAULT_HANDLER;
void Int100_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int101_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int102_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int103_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int104_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int105_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int106_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int107_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int108_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int109_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int110_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int111_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int112_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int113_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int114_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int115_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int116_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int117_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int118_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int119_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int120_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int121_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int122_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int123_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int124_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int125_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int126_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int127_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int128_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int129_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int130_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int131_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int132_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int133_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int134_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int135_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int136_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int137_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int138_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int139_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int140_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int141_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int142_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int143_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int144_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int145_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int146_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int147_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int148_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int149_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int150_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int151_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int152_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int153_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int154_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int155_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int156_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int157_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int158_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int159_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int160_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int161_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int162_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int163_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int164_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int165_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int166_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int167_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int168_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int169_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int170_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int171_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int172_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int173_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int174_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int175_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int176_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int177_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int178_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int179_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int180_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int181_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int182_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int183_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int184_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int185_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int186_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int187_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int188_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int189_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int190_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int191_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int192_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int193_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int194_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int195_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int196_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int197_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int198_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int199_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int200_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int201_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int202_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int203_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int204_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int205_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int206_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int207_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int208_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int209_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int210_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int211_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int212_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int213_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int214_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int215_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int216_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int217_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int218_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int219_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int220_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int221_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int222_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int223_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int224_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int225_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int226_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int227_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int228_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int229_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int230_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int231_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int232_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int233_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int234_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int235_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int236_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int237_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int238_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int239_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int240_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int241_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int242_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int243_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int244_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int245_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int246_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int247_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int248_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int249_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int250_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int251_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int252_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int253_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int254_Handler(void)                 WEAK_DEFAULT_HANDLER;
void Int255_Handler(void)                 WEAK_DEFAULT_HANDLER;

__attribute__ ((section(".cs3.interrupt_vector")))
intfunc const __cs3_interrupt_vector_coldfire[256] = {
    (intfunc)(&__cs3_stack),        /*  0 The stack pointer after relocation           */
    __cs3_reset,                    /*  1 Reset                                        */
    AccessError_Handler,            /*  2 Access Error                                 */
    AddressError_Handler,           /*  3 Address Error                                */
    IllegalInstruction_Handler,     /*  4 Illegal Instruction                          */
    DivideBy0_Handler,              /*  5 Divide by Zero                               */
    Default_Handler,                /*  6 Reserved                                     */
    Default_Handler,                /*  7 Reserved                                     */
    PrivilegeViolation_Handler,     /*  8 Privilege Violation                          */
    Trace_Handler,                  /*  9 Trace                                        */
    UnimplementedLineA_Handler,     /* 10 Unimplemented Line A                         */
    UnimplementedLineF_Handler,     /* 11 Unimplemented Line F                         */
    NonPCBreakpoint_Handler,        /* 12 Non-PC Breakpoint                            */
    PCBreakpoint_Handler,           /* 13 PC Breakpoint                                */
    FormatError_Handler,            /* 14 Format Error                                 */
    UninitializedInterrupt_Handler, /* 15 Uninitialized Interrupt                      */
    Default_Handler,                /* 16 Reserved                                     */
    Default_Handler,                /* 17 Reserved                                     */
    Default_Handler,                /* 18 Reserved                                     */
    Default_Handler,                /* 19 Reserved                                     */
    Default_Handler,                /* 20 Reserved                                     */
    Default_Handler,                /* 21 Reserved                                     */
    Default_Handler,                /* 22 Reserved                                     */
    Default_Handler,                /* 23 Reserved                                     */
    SpuriousInt_Handler,            /* 24 Spurious Interrupt                           */
    AutoVector1_Handler,            /* 25 AutoVector 1                                 */
    AutoVector2_Handler,            /* 26 AutoVector 2                                 */
    AutoVector3_Handler,            /* 27 AutoVector 3                                 */
    AutoVector4_Handler,            /* 28 AutoVector 4                                 */
    AutoVector5_Handler,            /* 29 AutoVector 5                                 */
    AutoVector6_Handler,            /* 30 AutoVector 6                                 */
    AutoVector7_Handler,            /* 31 AutoVector 7                                 */
    Trap0_Handler,                  /* 32 Trap 0                                       */
    Trap1_Handler,                  /* 33 Trap 1                                       */
    Trap2_Handler,                  /* 34 Trap 2                                       */
    Trap3_Handler,                  /* 35 Trap 3                                       */
    Trap4_Handler,                  /* 36 Trap 4                                       */
    Trap5_Handler,                  /* 37 Trap 5                                       */
    Trap6_Handler,                  /* 38 Trap 6                                       */
    Trap7_Handler,                  /* 39 Trap 7                                       */
    Trap8_Handler,                  /* 30 Trap 8                                       */
    Trap9_Handler,                  /* 41 Trap 9                                       */
    Trap10_Handler,                 /* 42 Trap 10                                      */
    Trap11_Handler,                 /* 43 Trap 11                                      */
    Trap12_Handler,                 /* 44 Trap 12                                      */
    Trap13_Handler,                 /* 45 Trap 13                                      */
    Trap14_Handler,                 /* 46 Trap 14                                      */
    Trap15_Handler,                 /* 47 Trap 15                                      */
    Int48_Handler,
    Int49_Handler,
    Int50_Handler,
    Int51_Handler,
    Int52_Handler,
    Int53_Handler,
    Int54_Handler,
    Int55_Handler,
    Int56_Handler,
    Int57_Handler,
    Int58_Handler,
    Int59_Handler,
    Int60_Handler,
    Int61_Handler,
    Int62_Handler,
    Int63_Handler,
    Int64_Handler,
    Int65_Handler,
    Int66_Handler,
    Int67_Handler,
    Int68_Handler,
    Int69_Handler,
    Int70_Handler,
    Int71_Handler,
    Int72_Handler,
    Int73_Handler,
    Int74_Handler,
    Int75_Handler,
    Int76_Handler,
    Int77_Handler,
    Int78_Handler,
    Int79_Handler,
    Int80_Handler,
    Int81_Handler,
    Int82_Handler,
    Int83_Handler,
    Int84_Handler,
    Int85_Handler,
    Int86_Handler,
    Int87_Handler,
    Int88_Handler,
    Int89_Handler,
    Int90_Handler,
    Int91_Handler,
    Int92_Handler,
    Int93_Handler,
    Int94_Handler,
    Int95_Handler,
    Int96_Handler,
    Int97_Handler,
    Int98_Handler,
    Int99_Handler,
    Int100_Handler,
    Int101_Handler,
    Int102_Handler,
    Int103_Handler,
    Int104_Handler,
    Int105_Handler,
    Int106_Handler,
    Int107_Handler,
    Int108_Handler,
    Int109_Handler,
    Int110_Handler,
    Int111_Handler,
    Int112_Handler,
    Int113_Handler,
    Int114_Handler,
    Int115_Handler,
    Int116_Handler,
    Int117_Handler,
    Int118_Handler,
    Int119_Handler,
    Int120_Handler,
    Int121_Handler,
    Int122_Handler,
    Int123_Handler,
    Int124_Handler,
    Int125_Handler,
    Int126_Handler,
    Int127_Handler,
    Int128_Handler,
    Int129_Handler,
    Int130_Handler,
    Int131_Handler,
    Int132_Handler,
    Int133_Handler,
    Int134_Handler,
    Int135_Handler,
    Int136_Handler,
    Int137_Handler,
    Int138_Handler,
    Int139_Handler,
    Int140_Handler,
    Int141_Handler,
    Int142_Handler,
    Int143_Handler,
    Int144_Handler,
    Int145_Handler,
    Int146_Handler,
    Int147_Handler,
    Int148_Handler,
    Int149_Handler,
    Int150_Handler,
    Int151_Handler,
    Int152_Handler,
    Int153_Handler,
    Int154_Handler,
    Int155_Handler,
    Int156_Handler,
    Int157_Handler,
    Int158_Handler,
    Int159_Handler,
    Int160_Handler,
    Int161_Handler,
    Int162_Handler,
    Int163_Handler,
    Int164_Handler,
    Int165_Handler,
    Int166_Handler,
    Int167_Handler,
    Int168_Handler,
    Int169_Handler,
    Int170_Handler,
    Int171_Handler,
    Int172_Handler,
    Int173_Handler,
    Int174_Handler,
    Int175_Handler,
    Int176_Handler,
    Int177_Handler,
    Int178_Handler,
    Int179_Handler,
    Int180_Handler,
    Int181_Handler,
    Int182_Handler,
    Int183_Handler,
    Int184_Handler,
    Int185_Handler,
    Int186_Handler,
    Int187_Handler,
    Int188_Handler,
    Int189_Handler,
    Int190_Handler,
    Int191_Handler,
    Int192_Handler,
    Int193_Handler,
    Int194_Handler,
    Int195_Handler,
    Int196_Handler,
    Int197_Handler,
    Int198_Handler,
    Int199_Handler,
    Int200_Handler,
    Int201_Handler,
    Int202_Handler,
    Int203_Handler,
    Int204_Handler,
    Int205_Handler,
    Int206_Handler,
    Int207_Handler,
    Int208_Handler,
    Int209_Handler,
    Int210_Handler,
    Int211_Handler,
    Int212_Handler,
    Int213_Handler,
    Int214_Handler,
    Int215_Handler,
    Int216_Handler,
    Int217_Handler,
    Int218_Handler,
    Int219_Handler,
    Int220_Handler,
    Int221_Handler,
    Int222_Handler,
    Int223_Handler,
    Int224_Handler,
    Int225_Handler,
    Int226_Handler,
    Int227_Handler,
    Int228_Handler,
    Int229_Handler,
    Int230_Handler,
    Int231_Handler,
    Int232_Handler,
    Int233_Handler,
    Int234_Handler,
    Int235_Handler,
    Int236_Handler,
    Int237_Handler,
    Int238_Handler,
    Int239_Handler,
    Int240_Handler,
    Int241_Handler,
    Int242_Handler,
    Int243_Handler,
    Int244_Handler,
    Int245_Handler,
    Int246_Handler,
    Int247_Handler,
    Int248_Handler,
    Int249_Handler,
    Int250_Handler,
    Int251_Handler,
    Int252_Handler,
    Int253_Handler,
    Int254_Handler,
    Int255_Handler,
};
