/*
** talker.c -- a datagram "client" demo
*/

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>

void print_ips(const unsigned char* buf, int TML) {
	int ip_index = 7;
	while (ip_index < TML) {
		printf("Resolved IP Addresses: %d.%d.%d.%d\n", buf[ip_index], buf[ip_index + 1], buf[ip_index + 2], buf[ip_index + 3]);
		ip_index+=4;
	}
 }
short compute_checksum(const unsigned char* msg, int TML) {
	short sum = 0;
	int i = 0;
	while(i < TML) {
		sum += (short) msg[i];
		sum = (sum & 0xFF) + (sum >> 8);
		i++;
	}
	
	return (sum);

}


int main(int argc, char *argv[])
{	
	int sockfd;
	static const int MAGIC_NUMBER_LSB = 0x12;
	static const int MAGIC_NUMBER_MSB = 0X34;
	static const int GROUP_ID = 11;
	struct timeval time;
	time.tv_sec = 10;
	time.tv_usec = 0;
	
	struct addrinfo hints, *servinfo, *p;
	struct sockaddr_storage their_addr;
	socklen_t addr_len;
	int rv;
	int numbytes;
		
	if (argc < 5) {
		fprintf(stderr,"usage: server portnum request_id h1 h2..\n");
		exit(1);
	}

	memset(&hints, 0, sizeof hints);
	hints.ai_family = AF_INET;
	hints.ai_socktype = SOCK_DGRAM;
	hints.ai_flags = AI_PASSIVE;
	static char* const SERVER = argv[1];
	static char* const PORT = argv[2];
	static uint32_t const REQUEST_ID = atoi(argv[3]);	 
	if ((rv = getaddrinfo(SERVER, PORT, &hints, &p)) != 0) {
		fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
		return 1;
	}
	unsigned int num_hosts = argc - 4;
	unsigned int header_len = 7;
	int num_trials = 0;
	int j = 4;
	int TML = header_len + num_hosts;
	while (j < argc) {
		TML += strlen(argv[j]);
		j++;
	}
	unsigned char*  msg = (unsigned char*)calloc(0, TML * sizeof(unsigned char) + 1);	
	int valid_response = 0;
	unsigned char buf[255];
	memset(&buf, 0,sizeof(buf));
	while (num_trials < 7 && valid_response == 0) {
	msg[0] = MAGIC_NUMBER_LSB;
	msg[1] =  MAGIC_NUMBER_MSB;
	msg[2] = (uint8_t)(TML >> 8) & 0xFF;
	msg[3] = (uint8_t)(TML >> 0) & 0xFF;
	msg[4] = NULL;
	msg[5] = GROUP_ID;
	msg[6] = REQUEST_ID;
	int c = 6;
	int i = 1;
	int host_counter = num_hosts; 
	while(host_counter > 0) {
		char* host_name = argv[argc - host_counter];
		int host_len = strlen(host_name);
		msg[c + i] = host_len;
		memcpy(&msg[c + (i + 1)], host_name, strlen(host_name) + 1);
		c+=strlen(host_name);
		i++;
		host_counter--; 	
	
	}
	msg[4] =  ~(compute_checksum(msg, TML));
	
	
	
	// loop through all the results and make a socket
	
		if ((sockfd = socket(p->ai_family, p->ai_socktype,
				p->ai_protocol)) == -1) {
			perror("talker: socket");
			
		}

		
	
	if (p == NULL) {
		perror("error creating socket");
		exit(1);
	}
	if ((numbytes = sendto(sockfd, msg, TML, 0, 
		p->ai_addr, p->ai_addrlen)) == -1) {

		perror("error sending message");	
		exit(1);
	
	}
	printf("Message sent.\n");
	addr_len = sizeof(their_addr);
	 
	
	memset(&buf,0,sizeof(buf));
	
	printf("Listening for data...\n\n");
	int bytes_rec = recvfrom(sockfd, buf, sizeof(buf), 0, (struct sockaddr *)&their_addr, &addr_len);
	if (bytes_rec < 0) {
		perror("recv");
		exit(1);
}
	printf("Received data...\n");
	unsigned int magic_number = (buf[0] << 8) | buf[1];
	TML = ((buf[2] << 8) | buf[3]);
	if (TML == 7) {
		if (buf[6] == 4) {
			printf("Attempt %d:\nReceived error code 4: Magic number invalid\n\n", num_trials + 1);
			num_trials++;
			continue;
		}
		if ( buf[6] == 1) {
			printf("Attempt %d:\nReceived error code 1: packet length mismatch\n\n", num_trials + 1);
			num_trials++;
			continue;
	
		}
	

		if (buf[6] == 2) {
			printf("Attempt %d:\nReceived error code 2: Bad checksum\n\n", num_trials + 1);
			num_trials++;
			continue;
		}
	}
	valid_response = 1;
	close(sockfd);	
	}
	if (valid_response) {
		print_ips(buf, TML);
	}
	else 
		printf("Number of trials exceeded 7. Exiting..\n\n");

	



}
