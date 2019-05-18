all: btcar.hex

btcar.hex: btcar.elf
	avr-objcopy  -j .text -j .data -O ihex $^ $@
	avr-size btcar.elf

btcar.elf: main.c usart.c
	avr-g++ -mmcu=atmega324p -DF_CPU=16000000 -Os -Wall -o $@ $^

clean:
	rm -rf btcar.elf btcar.hex
