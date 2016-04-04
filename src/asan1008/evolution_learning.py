import matplotlib.pyplot as plot
import pandas as pd

# Read file
data = pd.read_csv('evolution_learning.csv')
scores = data["score"]

# Draw graph
fig, axes = plot.subplots(nrows=1, ncols=1)
axes.plot(range(1, len(scores)+1), scores)
axes.set_title('Generations vs. Fitness')
axes.set_xlabel('Generations')
axes.set_ylabel('Fitness')
plot.show()

