import matplotlib.pyplot as plot
import pandas as pd
import random

data = pd.read_csv('resource_delivery_training.csv')
actual = pd.read_csv('resource_delivery_test.csv')

features = [ "energy", "ship_to_asteroid", "asteroid_to_base", "ship_to_base"]
learnedFeature = "success"

# Generate beginning w array
w = []
for _ in range(0, len(features)+1):
	w.append(random.uniform(-1, 1))

# Hold error over learning iterations
err_over_time = []

# Learn new w's
def learn_w(alpha):
  for _ in range(0, 2000):
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
alpha = 0.00000001

# Learn w's
learn_w(alpha)

# Create predictions for every day
f = []
for row in range(0, actual.shape[0]):
  x = [ 1 ]
  for feature in features:
    x.append(actual[feature][row]) 
  
  probability_prediction = predict(w, x)
  if (probability_prediction < 0.0):
    f.append(0)
  elif (probability_prediction > 1.0):
    f.append(1)
  else:
    f.append(probability_prediction)

# Output results
print("w0: " + str(w[0]))
for i in range (1, len(w)):
  print(features[i-1] + " bias: " + str(w[i]))

# Get actual data
y1 = actual[learnedFeature]

# Predictions for probability
fig, axes = plot.subplots(nrows=1, ncols=1, sharey=False, sharex=False)
axes.scatter(range(len(y1)), y1, label='Actual data', color='blue')
axes.scatter(range(len(f)), f, label='Predicted probability', color='green')
axes.set_title('Linear Regression Predictions')
axes.set_ylim([-0.1, 1.1])
axes.legend()

# Predictions for probability
fig, axes = plot.subplots(nrows=1, ncols=1, sharey=False, sharex=False)
axes.plot(range(len(y1)), y1, label='Actual data', color='blue')
axes.plot(range(len(f)), f, label='Predicted probability', color='green')
axes.set_title('Linear Regression Predictions')
axes.set_ylim([-0.1, 1.1])
axes.legend()

# Error over time
fig, axes = plot.subplots(nrows=1, ncols=1, sharey=False, sharex=False)
axes.plot(range(len(err_over_time)), err_over_time)
axes.set_title("Sum Squared Error vs. Number of Iterations")
axes.set_ylim([0, 500])
plot.show()


# # for error in err_over_time:
# #   print("error: " + str(error))

# # Plot error over time
# plot.plot(err_over_time)
# plot.title("Sum Squared Error vs. Number of Iterations")
# plot.xlabel("Numer of Iterations")
# plot.ylabel("Error")
# plot.show()