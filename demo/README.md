## Video description

###  Part 0 - setup [0:00 - 1:06]

- starts with the creation of the image for the workers
- then creates the lambda functions
- finally prepares the load balancer instance

### Part 1 - basic requests [1:06 - 1:35]

- makes one request of each type to the loadbalancer that is running on the load balancer instance in the port 80
- first request is the blur image
- second request is the enhance image
- third request is the raytracing

#### observations

- the prints in the console show the redirection of the requests to the worker

### Part 2 - scaling out [1:35 - 3:08]

- to scale out I make a big request and in this part of the video I accelerated a bit showing the evolution of the average cpu usage
- in the end of this part we can see the new instance created

### Part 3 - load balancing [3:08 - 4:24]

- here I make 2 big requests and then ssh to both the workers showing the top command and we can see they are both ocupied
- the list command shows the distribution of the work through the active workers and we can see both of them have requests

### Part 4 - lambdas [4:24 - 5:01]

- here the auto scaler is scaling so we can see the requests are sent to lambdas
- when load balancer invoke lambdas it prints the lambda function it invoked

### Part 5 - fault-tolerance [5:01 - 5:19]

- here we terminate the instance that has 2 requests on going
- our program has a bug that we just found after the submission but the bug does not interfere with the good work of the auto scaler and the load balancer
- the bug is that the load balancer retries many times to the terminated instance before redirecting to the other instances
- but we can see that after terminating the 2 requests are now distributed by the other 2 workers in the list command

### Part 6 - scaling in [5:19 - 6:07]

- just like the scaling out, we wait here the requests terminate
- we can see that the scale in does not terminate immediately the instance
- the instance is terminated only after the health check validates that the instance has no load anymore

### Part 7 - dynamodb [6:07 - 6:14]

- here we can see the entries of the table after this video was made
- in the beggining the table was clear