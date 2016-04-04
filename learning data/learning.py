import matplotlib.pyplot as plot
import pandas as pd

# Read files
instance1 = pd.read_csv('instance1.csv')
scores1 = instance1["score"]
instance2 = pd.read_csv('instance2.csv')
scores2 = instance2["score"]
instance3 = pd.read_csv('instance3.csv')
scores3 = instance3["score"]
instance4 = pd.read_csv('instance4.csv')
scores4 = instance4["score"]

# Draw graph
fig, axes = plot.subplots(nrows=1, ncols=1)
axes.plot(range(1, len(scores1)+1), scores1)
axes.set_title('Generations vs. Fitness (instance 1)')
axes.set_xlabel('Generations')
axes.set_ylabel('Fitness')
plot.show()

fig, axes = plot.subplots(nrows=1, ncols=1)
axes.plot(range(1, len(scores2)+1), scores2)
axes.set_title('Generations vs. Fitness (instance 2)')
axes.set_xlabel('Generations')
axes.set_ylabel('Fitness')
plot.show()

fig, axes = plot.subplots(nrows=1, ncols=1)
axes.plot(range(1, len(scores3)+1), scores3)
axes.set_title('Generations vs. Fitness (instance 3)')
axes.set_xlabel('Generations')
axes.set_ylabel('Fitness')
plot.show()

fig, axes = plot.subplots(nrows=1, ncols=1)
axes.plot(range(1, len(scores4)+1), scores4)
axes.set_title('Generations vs. Fitness (instance 4)')
axes.set_xlabel('Generations')
axes.set_ylabel('Fitness')
plot.show()


