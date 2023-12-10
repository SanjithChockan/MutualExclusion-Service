import sys
import ast

num_of_nodes = int(sys.argv[1])

criticalSectionEntries = {}

for i in range(num_of_nodes):
    with open("node-" + str(i) + "-CSTimeStamps.out", "r") as file:
        for line in file:
            data = (line.rstrip()).split(":")
            criticalSectionEntries[int(data[0])] = data[1]

cs_entries = list(criticalSectionEntries.keys())
cs_entries.sort()

print("Total number of critical sections: " + str(len(cs_entries)))
# test if current node's vector clock of critical section entry is > than previous node's vector clock of critical section exit
for i in range(1, len(cs_entries)):
    prev_cs = i
    curr_cs = i + 1
    
    node_previous_cs = (criticalSectionEntries[prev_cs]).split("-")
    node_current_cs = (criticalSectionEntries[curr_cs]).split("-")
    
    node_previous_cs_end = ast.literal_eval(node_previous_cs[1])
    node_current_cs_start = ast.literal_eval(node_current_cs[0])

    #print(str(node_previous_cs_end) + " -> " + str(node_current_cs_start))
    if (node_current_cs_start < node_previous_cs_end):
        print("Current CS entry vector clock is less than previous CS exit. Inconsistent...")
        sys.exit()

print("Successful! Only one node in critical section at a time...")
    


