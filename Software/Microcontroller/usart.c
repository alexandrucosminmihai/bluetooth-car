#include "usart.h"

/*
 * Functie de initializare a controllerului USART cu parametrii default
 * pentru modulul Bluetooth HC-05.
 */
void USART0_init()
{

	// Setez pinul PD0 (RXD0) ca input si activez rezistena de pull up
	// DDRD &= ~(1 << PD0);
	// PORTD |= (1 << PD0);

	// DDRD |= (1 << PD1);



	// Initializez cu 0 toti registrii de status si comanda
	UCSR0A = 0;
	UCSR0B = 0;
	UCSR0C = 0;

	/* seteaza baud rate la 9600 */
	UBRR0H = 0;
	UBRR0L = 103;

	/* porneste transmitatorul si receptorul*/
	UCSR0B = (1<<TXEN0) | (1<<RXEN0);

	/* porneste intreruperea de receive */
	UCSR0B |= (1 << RXCIE0);

	/*
	* seteaza formatul frame-ului:
	* 8 biti de date,
	* 1 bit de stop,
	* fara verificare paritate
	*/
	// UCSR0C = 0;
	UCSR0C &= ~(1<<USBS0);
	UCSR0C |= (3<<UCSZ00); // TODO: Poate e 2 in loc de 3
}

/*
 * Functie ce transmite un caracter prin USART
 *
 * @param data - caracterul de transmis
 */
void USART0_transmit(char data)
{
    /* asteapta pana bufferul e gol */
    while(!(UCSR0A & (1<<UDRE0)));

    /* pune datele in buffer; transmisia va porni automat in urma scrierii */
    UDR0 = data;
}

/*
 * Functie ce primeste un caracter prin USART
 *
 * @return - caracterul primit
 */
char USART0_receive()
{
    /* asteapta cat timp bufferul e gol */
    while(!(UCSR0A & (1<<RXC0)));

    /* returneaza datele din buffer */
    return UDR0;
}

/*
 * Functie ce transmite un sir de caractere prin USART
 *
 * @param data - sirul (terminat cu '\0') de transmis
 */
void USART0_print(const char *data)
{
    while(*data != '\0')
        USART0_transmit(*data++);
}

/*
 * Functie ce transmite un caracter prin USART
 *
 * @param data - caracterul de transmis
 * @param stream - nefolosit
 *
 * @return - intotdeauna returneaza 0
 */
int USART0_printf(char data, FILE *stream)
{
    /* asteapta pana bufferul e gol */
    while(!(UCSR0A & (1<<UDRE0)));

    /* pune datele in buffer; transmisia va porni automat in urma scrierii */
    UDR0 = data;
    return 0;
}

// USART 1

/*
 * Functie de initializare a controllerului USART cu parametrii default
 * pentru modulul Bluetooth HC-05.
 */
void USART1_init()
{
	/* seteaza baud rate la 9600 */
	UBRR1H = 0;
	UBRR1L = 103;

	/* porneste transmitatorul si receptorul*/
	UCSR1B = (1<<TXEN1) | (1<<RXEN1);

	/* porneste intreruperea de receive */
	UCSR1B |= (1 << RXCIE1);

	/*
	 * seteaza formatul frame-ului:
	 * 8 biti de date,
	 * 1 bit de stop,
	 * fara verificare paritate
	 */
	// UCSR0C = 0;
	UCSR1C &= ~(1<<USBS1);
	UCSR1C |= (3<<UCSZ10); // TODO: Poate e 2 in loc de 3
}

/*
 * Functie ce transmite un caracter prin USART
 *
 * @param data - caracterul de transmis
 */
void USART1_transmit(char data)
{
	/* asteapta pana bufferul e gol */
	while(!(UCSR1A & (1<<UDRE1)));

	/* pune datele in buffer; transmisia va porni automat in urma scrierii */
	UDR1 = data;
}

/*
 * Functie ce primeste un caracter prin USART
 *
 * @return - caracterul primit
 */
char USART1_receive()
{
	/* asteapta cat timp bufferul e gol */
	while(!(UCSR1A & (1<<RXC1)));

	/* returneaza datele din buffer */
	return UDR1;
}

/*
 * Functie ce transmite un sir de caractere prin USART
 *
 * @param data - sirul (terminat cu '\0') de transmis
 */
void USART1_print(const char *data)
{
	while(*data != '\0')
		USART1_transmit(*data++);
}

/*
 * Functie ce transmite un caracter prin USART
 *
 * @param data - caracterul de transmis
 * @param stream - nefolosit
 *
 * @return - intotdeauna returneaza 0
 */
int USART1_printf(char data, FILE *stream)
{
	/* asteapta pana bufferul e gol */
	while(!(UCSR1A & (1<<UDRE1)));

	/* pune datele in buffer; transmisia va porni automat in urma scrierii */
	UDR1 = data;
	return 0;
}

