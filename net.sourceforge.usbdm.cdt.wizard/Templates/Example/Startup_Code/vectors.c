/*
 *  Vectors for MKL25Z128
 */
#include <stdint.h>

typedef void( *const intfunc )( void );

#define WEAK_DEFAULT_HANDLER __attribute__ ((weak, alias("Default_Handler")))

 __attribute__((__interrupt__))
void Default_Handler(void) {
   while (1) {
      asm("bkpt #0");
   }
}

void __cs3_reset(void) __attribute__((__interrupt__));
extern uint32_t __cs3_stack;

void NMI_Handler(void)            WEAK_DEFAULT_HANDLER;
void HardFault_Handler(void)      WEAK_DEFAULT_HANDLER;
void MemManage_Handler(void)      WEAK_DEFAULT_HANDLER;
void BusFault_Handler(void)       WEAK_DEFAULT_HANDLER;
void UsageFault_Handler(void)     WEAK_DEFAULT_HANDLER;
void SVC_Handler(void)            WEAK_DEFAULT_HANDLER;
void DebugMon_Handler(void)       WEAK_DEFAULT_HANDLER;
void PendSV_Handler(void)         WEAK_DEFAULT_HANDLER;
void SysTick_Handler(void)        WEAK_DEFAULT_HANDLER;

void DMA0_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void DMA1_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void DMA2_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void DMA3_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void MCM_IRQHandler(void)         WEAK_DEFAULT_HANDLER;
void FTFL_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void PMC_IRQHandler(void)         WEAK_DEFAULT_HANDLER;
void LLW_IRQHandler(void)         WEAK_DEFAULT_HANDLER;
void I2C0_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void I2C1_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void SPI0_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void SPI1_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void UART0_IRQHandler(void)       WEAK_DEFAULT_HANDLER;
void UART1_IRQHandler(void)       WEAK_DEFAULT_HANDLER;
void UART2_IRQHandler(void)       WEAK_DEFAULT_HANDLER;
void ADC0_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void CMP0_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void FTM0_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void FTM1_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void FTM2_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void RTC_Alarm_IRQHandler(void)   WEAK_DEFAULT_HANDLER;
void RTC_Seconds_IRQHandler(void) WEAK_DEFAULT_HANDLER;
void PIT_IRQHandler(void)         WEAK_DEFAULT_HANDLER;
void USBOTG_IRQHandler(void)      WEAK_DEFAULT_HANDLER;
void DAC0_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void TSI0_IRQHandler(void)        WEAK_DEFAULT_HANDLER;
void MCG_IRQHandler(void)         WEAK_DEFAULT_HANDLER;
void LPTimer_IRQHandler(void)     WEAK_DEFAULT_HANDLER;
void PORTA_IRQHandler(void)       WEAK_DEFAULT_HANDLER;
void PORTD_IRQHandler(void)       WEAK_DEFAULT_HANDLER;

__attribute__ ((section(".cs3.interrupt_vector")))
intfunc const __cs3_interrupt_vector_arm[] = {
    (intfunc)(&__cs3_stack),  /* The stack pointer after relocation           */
    __cs3_reset,              /* Reset Handler                                */
    NMI_Handler,              /* NMI Handler                                  */
    HardFault_Handler,        /* Hard Fault Handler                           */
    MemManage_Handler,        /* MPU Fault Handler                            */
    BusFault_Handler,         /* Bus Fault Handler                            */
    UsageFault_Handler,       /* Usage Fault Handler                          */
    0,                        /* Reserved                                     */
    0,                        /* Reserved                                     */
    0,                        /* Reserved                                     */
    0,                        /* Reserved                                     */
    SVC_Handler,              /* SVCall Handler                               */
    DebugMon_Handler,         /* Debug Monitor Handler                        */
    0,                        /* Reserved                                     */
    PendSV_Handler,           /* PendSV Handler                               */
    SysTick_Handler,          /* SysTick Handler                              */

                              /* External Interrupts */
    DMA0_IRQHandler,          /* DMA Channel 0 Transfer Complete and Error    */
    DMA1_IRQHandler,          /* DMA Channel 1 Transfer Complete and Error    */
    DMA2_IRQHandler,          /* DMA Channel 2 Transfer Complete and Error    */
    DMA3_IRQHandler,          /* DMA Channel 3 Transfer Complete and Error    */
    MCM_IRQHandler,           /* Normal Interrupt                             */
    FTFL_IRQHandler,          /* FTFL Interrupt                               */
    PMC_IRQHandler,           /* PMC Interrupt                                */
    LLW_IRQHandler,           /* Low Leakage Wake-up                          */
    I2C0_IRQHandler,          /* I2C0 interrupt                               */
    I2C1_IRQHandler,          /* I2C1 interrupt                               */
    SPI0_IRQHandler,          /* SPI0 Interrupt                               */
    SPI1_IRQHandler,          /* SPI1 Interrupt                               */
    UART0_IRQHandler,         /* UART0 Status and Error interrupt             */
    UART1_IRQHandler,         /* UART1 Status and Error interrupt             */
    UART2_IRQHandler,         /* UART2 Status and Error interrupt             */
    ADC0_IRQHandler,          /* ADC0 interrupt                               */
    CMP0_IRQHandler,          /* CMP0 interrupt                               */
    FTM0_IRQHandler,          /* FTM0 fault, overflow and channels interrupt  */
    FTM1_IRQHandler,          /* FTM1 fault, overflow and channels interrupt  */
    FTM2_IRQHandler,          /* FTM2 fault, overflow and channels interrupt  */
    RTC_Alarm_IRQHandler,     /* RTC Alarm interrupt                          */
    RTC_Seconds_IRQHandler,   /* RTC Seconds interrupt                        */
    PIT_IRQHandler,           /* PIT timer all channels interrupt             */
    Default_Handler,          /* Reserved interrupt 39/23                     */
    USBOTG_IRQHandler,        /* USB interrupt                                */
    DAC0_IRQHandler,          /* DAC0 interrupt                               */
    TSI0_IRQHandler,          /* TSI0 Interrupt                               */
    MCG_IRQHandler,           /* MCG Interrupt                                */
    LPTimer_IRQHandler,       /* LPTimer interrupt                            */
    Default_Handler,          /* Reserved interrupt 45/29                     */
    PORTA_IRQHandler,         /* Port A interrupt                             */
    PORTD_IRQHandler          /* Port D interrupt                             */
};
