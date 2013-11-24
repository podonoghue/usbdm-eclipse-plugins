/*
 * utilities-mk.h
 *
 *  Created on: May 13, 2013
 *      Author: PODonoghue
 */
#ifndef UTILTIES_H_
#define UTILTIES_H_

/*!
 *  Stop - used to reduce power
 */
#define __stop(x) __asm__("stop %0" :: "i" (x))

/*!
 *  Debug breakpoint
 */
#define __breakpoint() __asm__("halt")
#define __halt()       __asm__("halt")

#endif /* UTILTIES_H_ */
