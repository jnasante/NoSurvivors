import matplotlib.pyplot as plot
import pandas as pd
import random

data = pd.read_csv('resource_delivery_training.csv')
actual = pd.read_csv('resource_delivery_test.csv')

features = [ "energy", "ship_to_asteroid", "asteroid_to_base", "resources_held", "ship_to_base"]
learnedFeature = "success"

# Generate beginning w array
w = []
for _ in range(0, len(features)+1):
	w.append(random.uniform(-10, 10))

print("Initial w:")
for weight in w:
  print(weight)

# Hold error over learning iterations
err_over_time = []

# Learn new w's
def learn_w(alpha):
  for _ in range(0, 10):
    sum = 0.0
    for row in range(0, data.shape[0]):
      x = [ 1 ]
      for feature in features:
        x.append(data[feature][row])
      
      y = data[learnedFeature][row]
      err = compute_error(w, x, y)
      sum += err**2
        
      for i in range(0, len(w)):
        w[i] = lms(w[i], err, alpha, x[i])
        print("wi: " + str(w[i]))

    err_over_time.append( sum )

def lms(wi, error, alpha, xij):
  return wi - alpha * error * xij

def predict(w, x):
  prediction = 0
  for i in range(0, len(w)):
    prediction += w[i]*x[i]
  return prediction

def compute_error(w, x, y):
  return predict(w, x) - y

# Define alpha
alpha = 0.00001

# Learn w's
learn_w(alpha)

# Output results

for weight in w:
  print(weight)

# for error in err_over_time:
#   print("error: " + str(error))

# Plot error over time
plot.plot(err_over_time)
plot.title("Error vs. Number of Iterations")
plot.xlabel("Numer of Iterations")
plot.ylabel("Error")
plot.show()