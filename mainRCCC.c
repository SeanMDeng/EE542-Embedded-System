/* This program is made to remote control a model car from either a
computer or a cell phone. The model car will able to move forward, 
reverse, left, and right. A camera on the car campture first person view 
of the model car.*/

#include <stdio.h> //standard input and output library
#include <stdlib.h> //standard library for general functions
#include <stdint.h> //provides a set of typdefs
#include <wiringPi.h> //Include WiringPi Library
#include <string.h> //string  library
#include <pthread.h> //allocate thread to different cores
#include <sys/types.h>  // need for getpid()  
#include <unistd.h> //need for getpid()
#include <sys/socket.h>
#include <netinet/in.h>
#include <errno.h> 



//declare functions
/* Motion of the RC car will be control via the acceleromter data from 
 * phone. */ 
 
int *motion(); //function control motion of RC 
int *voltage(); //monitor power supply 
void *camera(); //live stream from browser

int main(void)
{
		int rc1 = 0, rc2 = 0, rc3 = 0;
	pthread_t thread1, thread2, thread3;
	
		/* Check wirePi library */
	 if (wiringPiSetup () == -1)
    exit (1) ;
    
	printf("\n///////// Program Start /////////\n");
	printf("From main process ID: %d\n" , (int) getpid());
	
	if (( rc1=pthread_create( &thread1, NULL, &motion, NULL)) ) //creat thread for motion function
	{
		printf("Thread creation failed %d\n", rc1);
	}
	
	if (( rc2=pthread_create( &thread2, NULL, &voltage, NULL)) ) //create thread for voltage function
	{
		printf("Thread creation failed %d\n", rc2);
	}
	
	if (( rc3=pthread_create( &thread3, NULL, &camera, NULL)) ) //create thread for camera function
	{
		printf("Thread creation failed %d\n", rc3);
	}
	
	
	pthread_join( thread1, NULL); //this stop the program from exiting
	pthread_join( thread2, NULL);
	pthread_join( thread3, NULL);
	
	printf("\n///////// Program end /////////\n");
	printf("From main process ID: %d\n", (int) getpid() );
	
	exit(EXIT_SUCCESS);
	return 0;

}
	
int *motion()
{
	int PORT=5555;
	int BUFSIZE=1024;
	struct sockaddr_in hostaddr;			//host address 
	struct sockaddr_in clientaddr;			// client address 
	socklen_t addrlen = sizeof(clientaddr);	// length of address
	int recvlen=0;							// # bytes received
	int socketfd;							// our socket */
	unsigned char buf[BUFSIZE];		    	// receive buffer
	
	int speedLeft; 							// speed of leftwheel
	int speedRight;							// speed of rightwheel
	char *token; 							// token data acceleromater
	float x;								// x-axis data
	float y;								// y-axis data
	char *stringstored;						// pointer of str to f
	int dataX;								// Change to int
	int dataY;								// change to int

    
	pinMode (1, PWM_OUTPUT) ;  //initialize PWM it is at GPIO 18
	pinMode (23, PWM_OUTPUT) ; //initialize PWM it is at GPIO 13

	/* specify type of communcation protocol. Return -1 if fail.
	 * AF_INET is IPv4
	 * Sock_DGRAM is datagram scoket
	 * 0 for system default protocol*/
	
	if((socketfd = socket(AF_INET , SOCK_DGRAM , 0)) < 0) 
	{
		perror("cannot create socket\n"); 
		exit(-1);
	}
	
	/* bind the socket to any valid IP address and a specific port */
	
	memset((char *) &hostaddr, 0, sizeof(hostaddr));
	hostaddr.sin_family = AF_INET; // IPv4 protocols 
	
	/* Not all computer sotre the bytes in the same order, some store 
	 * memory in Little Endian, other use Big Endian. Internet protecols 
	 * known as Network Byte order solves this problem.
	 */
	hostaddr.sin_addr.s_addr = htonl(INADDR_ANY); //Host to Network Long
	hostaddr.sin_port = htons(PORT); //Host to Netowrk Short
	
	
	/* Bind function assigns a local protocol address to a socket 
	 * first arg sockdf is a socket scriptor return by the socket function
	 * scond arg hostaddr is a pointer to struck sockaddr that contains local IP addresss and port
	 * third arg sizeof(hostaddr) is the size of the hostaddr/ sockaddress
	 */ 
	 
	if( bind(socketfd, (struct sockaddr *) &hostaddr, sizeof(hostaddr)) < 0 )
	{
		perror("bind failed");
		exit(-1);
	}
	

	
	/* now loop, receiving data and printing what we received */
	while(1)
	{
		printf("waiting on port %d\n", PORT);
		/* recvfrom funciton i used to received data from unconnected datagram sockets
		 * first arg socketfd is socket descriptor 
		 * second arg buf is the buffer
		 * third arg BUFSIZE is the length of the buffer
		 * fourth arg 0 is the flag is set to 0
		 * fifth arg clinent adddress is the where data are from
		 * sizth arg addrelen is set to the size of struct sockaddr
		 */ 
		
		recvlen = recvfrom(socketfd , buf, BUFSIZE , 0 , (struct sockaddr *) &clientaddr, &addrlen);
		printf("received %d bytes\n", recvlen);

		if(recvlen  > 0)
		{
				buf[recvlen] = 0;
				printf("\"%s\"\n",buf);
				
				/* The following is the control of two dc motor on 
				 * First extra the data from message 
				 * convert data to float and then int
				 * third assign the speed of motor base on the accelerm
				 */
				 
				token=strtok(buf, ","); // take string apart first token
				token=strtok(NULL, ","); // second token
				token=strtok(NULL, ",");
		
				x= strtof(token, &stringstored); //covert string to float
		
				token=strtok(NULL, ",");
				y=strtof(token, &stringstored); 
				


				speedRight= -x*102 - y*102; //Right Wheel PWM
				speedLeft=-x*102 + y*102; //Left Wheel PWM
				
				if (speedLeft >10 && speedRight > 10)
				{
				printf("This is speed Right %d \n", speedRight);
				printf("This is speed Left %d \n", speedLeft);
				pwmWrite(1, speedRight);
				pwmWrite(23, speedLeft);
				}
				
				else
				{
				pwmWrite(1,0);
				pwmWrite(23,0);	
				}		
		}	

	}
	
	return(1);
}	

int *voltage()
{
	int voltageValue;
	int piTemperature;
	char *token;
	char *tokenLed;
	char *stringstored;
	char *stringLed;
    
	FILE *temperatureFile;	//local temperature file pointer
	char temp[10];	//temperature string should be less than 10 characters

	FILE *pwrLedFile; //local LED file pointer
	char led[20]; //led string 
	
	while (1)
	{
		
	/* Read the temperature of PI
	 * export read to local file
	 * read the file as a string
	 * parse string into value
	 */ 

	system("/opt/vc/bin/vcgencmd measure_temp > /home/pi/590ProjectRemoteControl/temperature"); //export vcgencmd's temperature to <YOUR LOCAL PATH>
	temperatureFile = fopen("/home/pi/590ProjectRemoteControl/temperature", "r");	//open the local temperature file
	if (temperatureFile != NULL)
	{
	fscanf(temperatureFile, "%s", temp); //read the string from the file
	}
	fclose(temperatureFile);	//remember to close the file
	token=strtok(temp, "="); // take string apart first token
	token=strtok(NULL, "'"); // take string apart first token
	piTemperature = strtof(token, &stringstored); 
	
	/* wiringPi pin24 is the same as GPIO35, which is the 
	 * pin for power. PWR led brightness is stored in a root file
	 * copy the file to project and parse to int value
	 */ 
	system(" sudo cp -R /sys/class/leds/led1/brightness /home/pi/590ProjectRemoteControl/PWR_LED"); //copy  LED status to local file
	pwrLedFile = fopen("/home/pi/590ProjectRemoteControl/PWR_LED", "r");	//open the local led file file
	if (pwrLedFile != NULL)
	{
	fscanf(pwrLedFile, "%s", led); //read the string from the file
	}
	fclose(pwrLedFile);	//remember to close the file
	voltageValue = strtof(led, &stringLed); // low voltage 0 high voltage 255 value
	printf("This is the Pi Temperature %d and 0 = low voltage, current value %d\n", piTemperature, voltageValue);
	if (piTemperature  > 85 || led == 0)
	{
		system("sudo halt"); // if temperature is over 85 and low voltage
	}

	}	
	return 0;
	
	}
	
void *camera()
{
	// for(int i = 0; i < 1000000; i++)
system("cd /usr/src/mjpg-streamer/mjpg-streamer-experimental export LD_LIBRARY_PATH=. && ./mjpg_streamer -o \"output_http.so -w ./www\" -i \"input_raspicam.so -x 640 -y 480 -fps 20 -ex night\"");
	
	
	return NULL;
}

