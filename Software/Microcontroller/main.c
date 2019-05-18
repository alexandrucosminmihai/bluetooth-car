#include <stdio.h>
#include <stdlib.h>
#include <util/delay.h>

#include <avr/interrupt.h>
#include <avr/io.h>

#include "usart.h"

#define DEBUG 1
#define STEERING_ANGLE_MIN 60.0f
#define STEERING_ANGLE_MAX 110.0f
#define STEERING_ANGLE_IDLE 85.0f

/*
 * Receptia de comenzi se face in 2 pasi:
 * 1) Prima data primesc un octet ce reprezinta comanda care trebuie facuta
 * 2) Primesc un octet cu valoarea asociata comenzii
 */
// Starile automatului finit pentru receptie comenzi pe USART
#define RECVD_STATE_INIT 0
#define RECVD_STATE_CMD 1 // Am primit comanda (inainte, inapoi, viraj etc)
#define RECVD_STATE_ARG 2 // Am primit argumentul (duty cycle etc)

// Comenzile pe care le poate primi masinuta
#define CMD_MOVE_FORWARD 0
#define CMD_MOVE_BACKWARD 1
#define CMD_STEER 2
#define CMD_LIGHTS_ON 3
#define CMD_LIGHTS_OFF 4
#define CMD_HONK 5

// Sensul de deplasare pentru motoarele de tractiune
#define FORWARD 0
#define BACKWARD 1

// Variabile globale pentru starea masinutei
// OCR0A -> Duty Cycle tractiune
// OCR0B -> Duty Cycle virare
// PORTB[0](in1) si PORTB[1](in2) -> Sensul de deplasare pentru tractiune
int receive_state = RECVD_STATE_INIT;
int curr_command;
int should_run = 1;

void main_init() {
	// Ma asigur ca intreruperile sunt active la nivel global
	sei();

	// Configurare LED user de la pinul PD7
	// Setez pinul PD7 asociat LED-ului user ca iesire.
	DDRD |= (1 << PD7);
	// Configurare pin PD5 PWM timer 1 canal A ca iesire (OC1A)
	DDRD |= (1 << PD5);
	// Configurare pin PD6 pentru Buzzer
	DDRD |= (1 << PD6);

	// Configurare pini iesire LED-uri masinuta
	DDRA |= (1 << PA0) | (1 << PA1) | (1 << PA2) | (1 << PA3);
	// Sting LED-urile
	PORTA &= ~(1 << PA0 | 1 << PA1 | 1 << PA2 | 1 << PA3);

	// Configurare pini iesire sens rotire catre driver motoare L298N
	// si pinii OC0A(PB3) si OC0B(PB4) pentru PWM driver.
	DDRB |= (1 << PB0) | (1 << PB1) | (1 << PB3) | (1 << PB4) | (1 << PB5) | (1 << PB6);

	// Aprind LED-ul user(PD7)
	PORTD |= (1 << PD7);
}

/*
 *	Timer-ul 0 este folosit pentru a trimite semnalul PWM de control
 *	catre driver-ul L298N care controleaza cele 2 motoare de tractiune.
 */
void timer0_init() {
	// Setez Timerul 0 in modul Fast PWM
	TCCR0A |= (1 << WGM01) | (1 << WGM00);
	/* 
	 * Setez outputul pe canalele de PWM in modul non inverting
	 * (set on bottom si clear on compare)
	 * COM0A1 = 1 si COM0A0 = 0 respectiv
	 * COM0B1 = 1 si COM0B0 = 0
	 */
	TCCR0A |= (1 << COM0A1) | (1 << COM0B1);

	// Setez Duty Cycle la 50% pe canalul pentru tractiune
	OCR0A = (unsigned char)(0.5f * 255.0f);
	// OCR0A = (unsigned char)(0.5f * 256.0f - 1.0f);
	// Setez Duty Cycle la 0% pe canalul pentru tractiune 
	// TODO: Probabil nu e bine la 0%
	OCR0B = OCR0A;

	/*
	 * Setez prescalerul la 8 deoarece L298N functioneaza cu PWM la intrare
	 * cu frecventa maxima 40kHz.
	 */
	TCCR0B |= (1 << CS01); // => frecventa 7812,5Hz
}

/*
 *	Timer-ul 2 este folosit pentru a trimite frecventa buzzer-ului
 *	pasiv ce are rolul de claxon. Folosesc pinul PD6, adica OC2B
 */
void timer2_init() {
	// TODO: !!!!!
	TCCR2A = 0;
	// Setez Timerul 2 in modul CTC cu TOP la OCR2A ???
	TCCR2A |= (1 << WGM21);
	// TODO: Setez bitii COM ca sa faca Toggle la compare
	TCCR2A |= (1 << COM2B0);

	// Setez OCR2B a.i sa obtin impreuna cu prescalerul 256 frecventa 830Hz
	OCR2B = (unsigned char)(200);
	OCR2A = OCR2B;
	// Initial nu e niciun prescaler setat, adica iesirea este oprita
	TCCR2B &= ~((1 << CS22) | (1 << CS21) | (1 << CS20)); // TODO: ASA TREBUIE SA FIE
	// TCCR2B |= (1 << CS22) | (1 << CS21); // => frecventa 830Hz
}

/*
 *	Timer-ul 1 este folosit pentru a trimite semnalul PWM de control
 *	direct catre servomotorul din fata care asigura virarea.
 *	E nevoie de folosirea Timer-ului 1 deoarece doar el permite obtinerea
 *	unei frecvente de 50Hz.
 */
void timer1_init() {
	// Setez Timerul 1 in modul Fast PWM cu TOP la ICR1
	TCCR1A = (1 << WGM11);
	TCCR1B = (1 << WGM13) | (1 << WGM12);
	/* 
	 * Setez outputul pe canalul A de PWM in modul non inverting
	 * (set on bottom si clear on compare)
	 * COM1A1 = 1 si COM1A0 = 0
	 */
	TCCR1A |= (1 << COM1A1);


	// Configurari pentru obtinerea frecventei de 50Hz
	// Setare prescaler la 8
	TCCR1B |= (1 << CS11);
	// Setare TOP (in cazul de fata ICR1)
	// ICR1 = 40000; // ICR1 este pe 16 biti => 50Hz
	ICR1 = 60000; // => 33.33333Hz

	/*
	 * Controlul servomotorului, adica a unghiului la care este rotit,
	 * se face prin specificarea unui Duty Cycle in 5% si 10% unde
	 * 5%...-90 grade
	 * 10%...+90 grade
	 */
	// OCR1A = (unsigned int)(0.075f * ICR1); // Initial la 0 (90/180) grade
	OCR1A = (unsigned int)(0.01666f * ICR1 + (STEERING_ANGLE_IDLE / 180.0f) * (0.1f - 0.01666f) * ICR1); // Initial la 0 (90/180) grade
}

/*
 * Rutine de tratare a intreruperilor
 * (Interrupt Service Routines)
 */
/*
 * Rutina intrerupere receptie pachet nou pe USART0.
 */
ISR(USART0_RX_vect) {
	unsigned char recvd;
	
	recvd = USART0_receive();
	if (DEBUG)
		USART0_transmit(recvd);

	switch (receive_state)
	{
	case RECVD_STATE_INIT:
		/*
		 * Nu am primit nimic inainte, acum primesc tipul comenzii.
		 * Deci comportamentul este acelasi ca pentru starea in care 
		 * am primit argumentul comenzii anterioare si astept tipul
		 * noii comenzi.
		 */
	case RECVD_STATE_ARG:
		switch (recvd)
		{
		case 'f': // Tractiune inainte
			curr_command = CMD_MOVE_FORWARD;
			if (DEBUG)
				USART0_print("Am primit CMD_MOVE_FORWARD\n");
			break;

		case 'b': // Tractiune inapoi
			curr_command = CMD_MOVE_BACKWARD;
			if (DEBUG)
				USART0_print("Am primit CMD_MOVE_BACKWARD\n");
			break;

		case 's': // Vireaza
			curr_command = CMD_STEER;
			if (DEBUG)
				USART0_print("Am primit CMD_STEER\n");
			break;

		case 'l': // Aprinde lumini
			curr_command = CMD_LIGHTS_ON;
			if (DEBUG)
				USART0_print("Am primit CMD_LIGHTS_ON\n");
			break;
		
		case 'd': // Stinge lumini
			curr_command = CMD_LIGHTS_OFF;
			if (DEBUG)
				USART0_print("Am primit CMD_LIGHTS_OFF\n");
			break;

		case 'h': // Claxoneaza
			curr_command = CMD_HONK;
			if (DEBUG)
				USART0_print("Am primit CMD_HONK\n");
			break;

		default:
			// Comanda invalida
			if (DEBUG)
				USART0_print("Am primit comanda invalida\n");
			return;
		}
		// Am trecut in starea:
		receive_state = RECVD_STATE_CMD;

		break;
	
	// Am primit deja tipul comenzii, acum am primit argumentul
	case RECVD_STATE_CMD:
		if (DEBUG)
			USART0_print("Am primit argumentul\n");
		// In functie de comanda primita, fac ceva cu argumentul
		switch (curr_command)
		{
		case CMD_MOVE_FORWARD:
			// Setez duty cycle
			OCR0A = (unsigned char)((float)recvd / 100.0f * 255);
			OCR0B = OCR0A;
			// Setez sensul de deplasare
			PORTB |= (1 << PB0);
			PORTB &= ~(1 << PB1);
			// Si pentru celalalt motor
			PORTB |= (1 << PB5);
			PORTB &= ~(1 << PB6);

			break;
		
		case CMD_MOVE_BACKWARD:
			// Setez duty cycle
			OCR0A = (unsigned char)((float)recvd / 100.0f * 255);
			OCR0B = OCR0A;
			// Setez sensul de deplasare
			PORTB |= (1 << PB1);
			PORTB &= ~(1 << PB0);
			// Si pentru celalalt motor
			PORTB |= (1 << PB6);
			PORTB &= ~(1 << PB5);

			break;

		case CMD_STEER:
			/*
			 * Argumentul este un numar intre 0 si 180 si 
			 * reprezinta pozitia in grade la care vrem sa aducem
			 * servomotorul.
			 */
			if (recvd > STEERING_ANGLE_MAX)
				recvd = STEERING_ANGLE_MAX;
			else if (recvd < STEERING_ANGLE_MIN)
				recvd = STEERING_ANGLE_MIN;
				
			if (DEBUG)
				USART0_print("Am primit unghi virare\n");
			/*
			 * Valoare -90 grade (aka 0 grade) este la 5% duty cyle 
			 * (e pragul inferior). Vad cat la suta din 180 
			 * reprezinta valoarea primita si atat la suta din 
			 * diferenta pana la duty cycle 10% (valoarea lui 90 
			 * aka 180 grade) adun la pragul inferior ca sa obtin
			 * duty cycle-ul asociat unghiului primit argument.
			 */
			// OCR1A = (unsigned int)(0.05f * ICR1 + (float)recvd / 180.0f * 0.05f * ICR1);
			OCR1A = (unsigned int)(0.01666f * ICR1 + (float)recvd / 180.0f * (0.1f - 0.01666f) * ICR1);

			break;

		case CMD_LIGHTS_ON:
			PORTA |= (1 << PA0) | (1 << PA1) | (1 << PA2) | (1 << PA3);

			break;

		case CMD_LIGHTS_OFF:
			PORTA &= ~(1 << PA0 | 1 << PA1 | 1 << PA2 | 1 << PA3);

			break;

		case CMD_HONK:
			
			if (recvd == 0) {
				// Argumentul imi spune sa opresc claxonul
				TCCR2B &= ~((1 << CS22) | (1 << CS21) | (1 << CS20));
			} else if (recvd == 1) {
				// Argumentul imi spune sa pornesc claxonul
				// Modific prescalerul pentru Timer 2
				TCCR2B |= (1 << CS22); // => frecventa 830Hz
			}
			break;

		default:
			USART0_print("ERR: Argument pentru comanda invalida\n");

			break;
		}
		// Am trecut in starea:
		receive_state = RECVD_STATE_ARG;
		break;

	default:
		USART0_print("ERR: Invalid receive state\n");
		break;
	}
}


int main() {

	// Configurari globale
	main_init();

	// Configurari initiale pentru timer0 (tractiune)
	timer0_init();
	// Configurari initiale pentru timer1 (servomotor)
	timer1_init();
	// Configurari initiale pentru timer2 (buzzer)
	timer2_init();

	// Initializez USART0
	USART0_init();
	//USART1_init();

	// Bucla principala
	while(should_run) {
		// Nu fac nimic, astept intreruperea de receive pe USART
		// recv = USART0_receive();
		// USART0_transmit('A');
	}


	return 0;
}