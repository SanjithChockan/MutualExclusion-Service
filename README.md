## Roucairol and Carvalho’s distributed mutual exclusion algorithm

- Gaurantees one process at critical section at a time

To run the program:
    - Run launcher.sh with config file
    - After program ends, run TestModule.py to ensure correctness.
        - Ensures no two processes over lap in critical section and all happen separately
        - example command input: python3 Testmodule.py n
        - n is the number of machines
    - run cleanup.sh to kill all processes

To view graphs:
    - run experiment.py
        - example command input: python3 experiment.py n
        - graphs for message complexity, response time, and system throughput
    