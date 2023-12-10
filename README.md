# Roucairol and Carvalho’s distributed mutual exclusion algorithm

Gaurantees one process at critical section at a time

## Requirements

Service provides two function calls to the application: csenter() and cs-leave(). 

- The first function call cs-enter() allows an application to request permission
to start executing its critical section. The function call is blocking and returns only when the invoking application can execute its critical section. 

- The second function call cs-leave() allows an application to inform the service that it has finished executing its critical section


## Distributed System Implementation:

    Consists of n nodes numbered from 0 to n - 1:
        - reliable socket connections, implemented using either TCP or SCTP, between every pair of nodes, and all messages are exchanged over these connections. All connections are established in the beginning and stay intact until the program ends.
        - Each node randomly waits for a period of time that is exponentially probability distributed before generating a request

## Service Implementation: 

    Each node consists of two separate modules:
        - Top module implements the application (requests and executes critical sections)
        - Bottom module implements the mutual exclusion service

    The two modules interact using cs-enter() and cs-leave() functions.

## Configuration File:

    The first valid line of the configuration file contains four tokens. 
        - The first token is the number of nodes in the system.
        - The second token is the mean value for inter-request delay (in milliseconds).
        - The third token is the mean value for cs-execution time (in milliseconds).
        - The fourth token is the number of requests each node should generate.

    Next n lines consist of three tokens.
        - The first token is the node ID.
        - The second token is the host-name of the machine on which the node runs.
        - The third token is the port on which the node listens for incoming connections.

## To run the program:
    - Run launcher.sh with config file
    - After program ends, run TestModule.py to ensure correctness.
        - Ensures no two processes over lap in critical section and all happen separately
        - example command input: python3 Testmodule.py n
        - n is the number of machines
    - run cleanup.sh to kill all processes

# To view graphs:
    - run experiment.py
        - example command input: python3 experiment.py n
        - graphs for message complexity, response time, and system throughput
    