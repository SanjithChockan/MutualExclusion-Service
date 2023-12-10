import sys
import numpy as np
import matplotlib.pyplot as plt

num_of_nodes = int(sys.argv[1])

cs_vars = [10, 20, 40, 80, 160]


# message complexity
msg_averages = []
for j in cs_vars:
    messages = []
    for i in range(num_of_nodes):
        with open("node-" + str(i) + "-MessageComplexity-" + str(j) +".out", "r") as file:
            temp_msg = []
            for line in file:
                data = (line.rstrip())
                temp_msg.append(float(data))
            messages.append(temp_msg.copy())

    matrix_msg = np.array(messages.copy())
    # Calculate the column averages
    column_averages = np.mean(matrix_msg, axis=0)
    message_avg = np.mean(column_averages)
    msg_averages.append(message_avg)

print(f"Message averages: {msg_averages}")
# Plotting the data
plt.plot(cs_vars, msg_averages)

# Adding labels and title
plt.xlabel('c')
plt.ylabel('Messages over per CS Entry')
plt.title('Message Complexity')

# Display the plot
plt.show()


# response time
rsp_averages = []
for j in cs_vars:
    response = []
    for i in range(num_of_nodes):
        with open("node-" + str(i) + "-ResponseTime-" + str(j) +".out", "r") as file:
            temp_msg = []
            for line in file:
                data = (line.rstrip())
                temp_msg.append(float(data))
            response.append(temp_msg.copy())
    matrix_rsp = np.array(response.copy())
    # Calculate the column averages
    column_averages = np.mean(matrix_rsp, axis=0)
    response_avg = np.mean(column_averages)
    rsp_averages.append(response_avg)

print(f"Response averages: {rsp_averages}")

plt.plot(cs_vars, rsp_averages)

# Adding labels and title
plt.xlabel('c')
plt.ylabel('Response time in ms')
plt.title('Response Time (ms)')

# Display the plot
plt.show()



# system throughtput
thrpt_averages = []
for j in cs_vars:
    throughput = []
    for i in range(num_of_nodes):
        with open("node-" + str(i) + "-SystemThroughput-" + str(j) +".out", "r") as file:
            temp_msg = []
            for line in file:
                data = (line.rstrip())
                temp_msg.append(float(data))
            throughput.append(temp_msg.copy())
    matrix_systhru = np.array(throughput.copy())
    # Calculate the column averages
    column_averages = np.mean(matrix_systhru, axis=0)
    systhru_avg = np.mean(column_averages)
    thrpt_averages.append(systhru_avg)

print(f"System throughput averages: {thrpt_averages}")
thrpt_averages = [element * 10000 for element in thrpt_averages]
plt.plot(cs_vars, thrpt_averages)

# Adding labels and title
plt.xlabel('c')
plt.ylabel('Requests fulfilled per 20 seconds')
plt.title('System throughput Complexity')

# Display the plot
plt.show()
